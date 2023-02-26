import java.util.LinkedList;
import java.io.File; // Import the File class
import java.io.FileNotFoundException; // Import this class to handle errors
import java.util.Random;
import java.util.Scanner; // Import the Scanner class to read text files

//Note, the following is a matrix implementation of the CSP, this was done for efficiency reasons.
//It functions as a constraint graph implicitly, as opposed to explicitly.
public class forward_checking {

    public static Random rand = new Random();
    public static String heuristic = "";
    public static int nodeCount = 0;

    public static void main(String args[]) {

        if(args.length != 3){
            System.out.println("Please run the program with the file and heuristic choice, for example: ");
            System.out.println("java forward_checking 12W.txt H3");
            System.exit(1);
        }

        LinkedList<char[][]> problems = getProblems(args[0]);
        LinkedList<char[][]> solutions = new LinkedList<char[][]>();
        int[] nodesVisited = new int[problems.size()];

        if (args.length == 2)
            heuristic = args[1];

        if(heuristic.equals("H1") || heuristic.equals("H2") || heuristic.equals("H3")){
            for (int i = 0; i < problems.size(); i++) {
                System.out.println("Solving puzzle #" + (i + 1) + "...");
                solutions.add(backtrackingSearch(problems.get(i)));
                nodesVisited[i] = nodeCount; // nodeCount is a global to extract the nodes visited from the recursion
                //print the solution
                printSolution(problems.get(i), solutions.get(i), i, nodesVisited[i]);
            }

        }
        else{
            System.out.println("Invalid arguements.");
        }

    }

    public static void printSolution(char[][] problem, char[][] solution, int i, int nodesVisited){
        System.out.println("------------------- Puzzle " + (i + 1) + " -------------------\n");
        System.out.println("Puzzle:");
        printProblem(problem);
        System.out.print("\n");
        System.out.println("Solution:");
        if (solution != null)
            printProblem(solution);
        else
            System.out.println("Unsolvable");
        System.out.println("\nNodes visited:\n" + nodesVisited);
        System.out.print("\n");
    }

    public static LinkedList<char[][]> getProblems(String fileName) { // read problems into a list of 2d arrays. Each
                                                                      // problem must be separated by atleast one #.
                                                                      // LightupPuzzles.txt works, and so does
                                                                      // samples.txt
        LinkedList<char[][]> problems = new LinkedList<char[][]>();
        Boolean readingProblem = false;
        Boolean readingDimensions = false;
        int rowIndex = 0;

        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if (data.isEmpty() || data.charAt(0) == '#')
                    readingProblem = false;
                else if (!readingProblem)
                    readingDimensions = true;
                if (readingProblem) {
                    problems.getLast()[rowIndex] = data.toCharArray();
                    rowIndex++;
                }
                if (readingDimensions) {
                    rowIndex = 0;
                    String[] dimensions = data.split(" ");
                    int numRows = Integer.parseInt(dimensions[0]);
                    int numCols = Integer.parseInt(dimensions[1]);
                    problems.add(new char[numRows][numCols]);
                    readingDimensions = false;
                    readingProblem = true;
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return problems;
    }

    public static void printProblems(LinkedList<char[][]> list) { // print list of 2d arrays
        for (char[][] c : list) {
            printProblem(c);
            System.out.print("\n");
        }
    }

    public static void printProblem(char[][] c) {
        System.out.println(c.length + " x " + c[0].length);
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[i].length; j++) {
                System.out.print(c[i][j]);
            }
            System.out.print("\n");
        }
    }

    public static char[][] backtrackingSearch(char[][] csp) {
        nodeCount = 0;
        char[][] assignment = copyArray(csp);
        if (recursiveBacktracking(assignment)) {
            formatSolution(assignment);
            return assignment;
        } else
            return null;
    }

    public static Boolean recursiveBacktracking(char[][] assignment) { // main recursion (equivalent to pseudocode in slide 33)

        int[] var = selectUnassignedVariable(assignment);
        if (var == null)
            return true;
        char[] domain = forwardCheck(var, assignment);
        for (char value : domain) {
            nodeCount++; //we have made a choice for our variable -> increment node count
            if (partialAssignment(var, value, assignment)) {
                assignment[var[0]][var[1]] = value;
                Boolean result = recursiveBacktracking(assignment);
                if (result)
                    return result;
                else
                    assignment[var[0]][var[1]] = '_';
            }
        }
        return false;
    }

    public static char[] forwardCheck(int[] variable, char[][] assignment){
        var lit = copyArray(assignment);
        applyLighting(lit);
        var domain = calculateDomain(variable, lit);

        return domain;
    }

    public static int[] selectUnassignedVariable(char[][] assignment) { // This is where heuristic functions will be applied ------------------ Forward checking implementation
        int[] variable = null;
        char[][] lightingApplied = copyArray(assignment);

        switch (heuristic) {
            case "H1":
                variable = H1(lightingApplied);
                break;
            case "H2":
                variable = H2(lightingApplied);
                break;
            case "H3":
                variable = H3(lightingApplied);
                break;
        }
        return variable;
    }

    public static int[] H0(char[][] assignment) {
        for (int i = 0; i < assignment.length; i++) {
            for (int j = 0; j < assignment[i].length; j++) {
                if (assignment[i][j] == '_') {
                    return new int[] { i, j };
                }
            }
        }
        return null;
    }

    public static int[] H1(char[][] assignment) { // finds the most constrained variable
        LinkedList<int[]> list = getH1List(assignment);
        if (list != null)
            return list.get(rand.nextInt(list.size()));
        else
            return null;
    }

    public static LinkedList<int[]> getH1List(char[][] assignment) { // most constrained variable
                                                                    // Variables can only be assigned one of 'b' or 'n'. So we choose the variable
                                                                    // with the fewest of these two options remaining (b and n, just b, or just n).
        char[][] lightingApplied = copyArray(assignment);

        applyLighting(lightingApplied);
        LinkedList<int[]> option1 = new LinkedList<int[]>();
        LinkedList<int[]> option2 = new LinkedList<int[]>();
        for (int i = 0; i < assignment.length; i++) {
            for (int j = 0; j < assignment[i].length; j++) {
                if (assignment[i][j] == '_') {
                    int[] var = new int[] { i, j };
                    int numOptions = calculateOptions(var, lightingApplied);
                    if (numOptions == 1)
                        // return var;
                        option1.add(var);
                    else if (numOptions == 2)
                        option2.add(var);
                }
            }
        }
        if (!option1.isEmpty())
            return option1;
        else if (!option2.isEmpty())
            return option2;
        else
            return null;
    }

    public static int[] H3(char[][] assignment) { // Finds most constrained variable, that constrains the most other variables
        int[] selected = null;
        var selectedList = new LinkedList<int[]>();

        LinkedList<int[]> list = getH1List(assignment);
        if (list != null) {
            int bestDegree = Integer.MIN_VALUE;
            for (int i = 0; i < list.size(); i++) {
                int currDegree = calculateDegree(list.get(i), assignment);
                if (currDegree > bestDegree) {
                    bestDegree = currDegree;
                    selectedList = new LinkedList<int[]>(); //empty out old list
                    selectedList.add(list.get(i));
                }
                else if(currDegree == bestDegree){
                    selectedList.add(list.get(i));
                }
            }
        }

        if(selectedList.size() > 0)
            selected = selectedList.get(rand.nextInt(selectedList.size()));

        return selected;
    }

    public static int[] H2(char[][] assignment) { //returns the variable with the highest degree
        var selectedList = new LinkedList<int[]>();
        int[] selected = null;
        int bestValue = Integer.MIN_VALUE;

        for (int i = 0; i < assignment.length; i++) {
            for (int j = 0; j < assignment[i].length; j++) {
                if (assignment[i][j] == '_') {
                    int currValue = calculateDegree(new int[] { i, j }, assignment);
                    if (currValue > bestValue) {
                        selectedList = new LinkedList<int[]>(); //empty out old list
                        selectedList.add(new int[] { i, j });
                        bestValue = currValue;
                    }
                    else if (currValue == bestValue) //add ties to the list
                        selectedList.add(new int[] { i, j });
                }
            }
        }

        if(selectedList.size() > 0)
            selected = selectedList.get(rand.nextInt(selectedList.size()));

        return selected;
    }

    public static int calculateDegree(int[] var, char[][] assignment) {
        int currDegree = 0;
        int i = var[0];
        int j = var[1];

        //add up unassigned variables from surrounding walls (they share a wall constraint)
        if (i - 1 >= 0 && isAWall(assignment[i - 1][j])) {
            currDegree+=calcUnassignedSpacesAroundWall(new int[]{i - 1, j}, assignment);
        }
        if (i + 1 < assignment.length && isAWall(assignment[i + 1][j])) {
            currDegree+=calcUnassignedSpacesAroundWall(new int[]{i + 1, j}, assignment);
        }
        if (j - 1 >= 0 && isAWall(assignment[i][j - 1])) {
            currDegree+=calcUnassignedSpacesAroundWall(new int[]{i, j - 1}, assignment);
        }
        if (j + 1 < assignment[0].length && isAWall(assignment[i][j + 1])) {
            currDegree+=calcUnassignedSpacesAroundWall(new int[]{i, j + 1}, assignment);
        }
        
        //add up unassigned variables from surrounding column and row (they share column/row constraint)
        // check top
        for (int posI = i - 1; posI >= 0; posI--)
            if (assignment[posI][j] == '_')
                currDegree++;
             else if (isAWall(assignment[posI][j]))
                break;

        // check bottom
        for (int posI = i + 1; posI < assignment.length; posI++)
            if (assignment[posI][j] == '_')
                currDegree++;
             else if (isAWall(assignment[posI][j]))
                break;

        // check left
        for (int posJ = j - 1; posJ >= 0; posJ--)
            if (assignment[i][posJ] == '_')
                currDegree++;
             else if (isAWall(assignment[i][posJ]))
                break;

        // check right
        for (int posJ = j + 1; posJ < assignment[0].length; posJ++)
            if (assignment[i][posJ] == '_')
                currDegree++;
             else if (isAWall(assignment[i][posJ]))
                break;
        return currDegree;
    }

    public static int calcUnassignedSpacesAroundWall(int[] pos, char[][] assignment){
        int unassignedCount = 0;
        if(pos[0] - 1 >= 0)
            if(assignment[pos[0] - 1][pos[1]] == '_')
                unassignedCount++;
        if(pos[0] + 1 < assignment.length)
            if(assignment[pos[0] + 1][pos[1]] == '_')
                unassignedCount++;
        if(pos[1] - 1 >= 0)
            if(assignment[pos[0]][pos[1] - 1] == '_')
                unassignedCount++;
        if(pos[1] + 1 < assignment[pos[0]].length)
            if(assignment[pos[0]][pos[1] + 1] == '_')
                unassignedCount++;
        unassignedCount--;
        return unassignedCount;
    }

    public static int calculateOptions(int[] var, char[][] lightingApplied) { // returns the number of possible
                                                                              // values for a position
        if (lightingApplied[var[0]][var[1]] == 'x')
            return 1;
        else if (lightingApplied[var[0]][var[1]] == '_') {
            // check top
            if (var[0] - 1 >= 0 && isAWall(lightingApplied[var[0] - 1][var[1]])
                    && checkwall(new int[] { var[0] - 1, var[1] }, lightingApplied))
                return 1;
            // check bottom
            if (var[0] + 1 < lightingApplied.length && isAWall(lightingApplied[var[0] + 1][var[1]])
                    && checkwall(new int[] { var[0] + 1, var[1] }, lightingApplied))
                return 1;
            // check left
            if (var[1] - 1 >= 0 && isAWall(lightingApplied[var[0]][var[1] - 1])
                    && checkwall(new int[] { var[0], var[1] - 1 }, lightingApplied))
                return 1;
            // check right
            if (var[1] + 1 < lightingApplied[0].length && isAWall(lightingApplied[var[0]][var[1] + 1])
                    && checkwall(new int[] { var[0], var[1] + 1 }, lightingApplied))
                return 1;
        }
        return 2; // the only other option is '_' without constraints
    }

    public static char[] calculateDomain(int[] var, char[][] lightingApplied) { // returns the number of possible
        // values for a position
        if (lightingApplied[var[0]][var[1]] == 'x')
            return new char[]{'n'};
        else if (lightingApplied[var[0]][var[1]] == '_') {
            // check top
            if (var[0] - 1 >= 0 && isAWall(lightingApplied[var[0] - 1][var[1]]))
                    return forwardCheckwall(new int[] { var[0] - 1, var[1] }, lightingApplied);
            // check bottom
            if (var[0] + 1 < lightingApplied.length && isAWall(lightingApplied[var[0] + 1][var[1]]))
                     return forwardCheckwall(new int[] { var[0] + 1, var[1] }, lightingApplied);
            // check left
            if (var[1] - 1 >= 0 && isAWall(lightingApplied[var[0]][var[1] - 1]))
                    return forwardCheckwall(new int[] { var[0], var[1] - 1 }, lightingApplied);
            // check right
            if (var[1] + 1 < lightingApplied[0].length && isAWall(lightingApplied[var[0]][var[1] + 1]))
                    return forwardCheckwall(new int[] { var[0], var[1] + 1 }, lightingApplied);
        }
        return new char[] {'b','n'}; // the only other option is '_' without constraints
    }

    public static Boolean isAWall(char c) {
        Boolean wall = false;
        int character = c - '0';
        if (character >= 0 && character <= 4)
            wall = true;
        return wall;
    }

    public static Boolean checkwall(int[] pos, char[][] lightingApplied) { // determine if the wall requires a  
                                                                           // particular assignment to an adjacent
                                                                           // square given it's other surroundings

        Boolean requireAssignment = false;
        int value = lightingApplied[pos[0]][pos[1]] - '0';
        int numBulbs = 0;
        int numUnassigned = 0;

        // check top
        if (pos[0] - 1 >= 0)
            if (lightingApplied[pos[0] - 1][pos[1]] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0] - 1][pos[1]] == '_')
                numUnassigned++;
        // check bottom
        if (pos[0] + 1 < lightingApplied.length)
            if (lightingApplied[pos[0] + 1][pos[1]] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0] + 1][pos[1]] == '_')
                numUnassigned++;
        // check left
        if (pos[1] - 1 >= 0)
            if (lightingApplied[pos[0]][pos[1] - 1] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0]][pos[1] - 1] == '_')
                numUnassigned++;
        // check right
        if (pos[1] + 1 < lightingApplied[0].length)
            if (lightingApplied[pos[0]][pos[1] + 1] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0]][pos[1] + 1] == '_')
                numUnassigned++;

        if (numUnassigned == value - numBulbs || numBulbs == value) // assignment of b is required in the first case,
                                                                    // assignment of n is required in the second
            requireAssignment = true;

        return requireAssignment;
    }

    public static char[] forwardCheckwall(int[] pos, char[][] lightingApplied) { // determine if the wall requires a
        // particular assignment to an adjacent
        // square given it's other surroundings

        char[] wallDomain = {'b','n'}; //case that the wall is not constraining

        int value = lightingApplied[pos[0]][pos[1]] - '0';
        int numBulbs = 0;
        int numUnassigned = 0;

        // check top
        if (pos[0] - 1 >= 0)
            if (lightingApplied[pos[0] - 1][pos[1]] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0] - 1][pos[1]] == '_')
                numUnassigned++;
        // check bottom
        if (pos[0] + 1 < lightingApplied.length)
            if (lightingApplied[pos[0] + 1][pos[1]] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0] + 1][pos[1]] == '_')
                numUnassigned++;
        // check left
        if (pos[1] - 1 >= 0)
            if (lightingApplied[pos[0]][pos[1] - 1] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0]][pos[1] - 1] == '_')
                numUnassigned++;
        // check right
        if (pos[1] + 1 < lightingApplied[0].length)
            if (lightingApplied[pos[0]][pos[1] + 1] == 'b')
                numBulbs++;
            else if (lightingApplied[pos[0]][pos[1] + 1] == '_')
                numUnassigned++;

        // assignment of b is required in the first case,
        if (numUnassigned == value - numBulbs)
            wallDomain = new char[]{'b'};
        // assignment of n is required in the second
        else if   ( numBulbs == value)
            wallDomain = new char[]{'n'};

        return wallDomain;
    }
    public static Boolean partialAssignment(int[] var, char value, char[][] assignment) { // determines if the proposed
                                                                                          // value/variable is a valid
                                                                                          // partial assignment
        char[][] local = copyArray(assignment);
        local[var[0]][var[1]] = value;
        if (applyLighting(local))
            if (checkWalls(local))
                if (checkNoBulbs(local))
                    return true;
        return false;
    }

    public static Boolean checkNoBulbs(char[][] lightingApplied) { // check the no-bulb positions, and ensure they can
                                                                   // be lit in the future
        for (int i = 0; i < lightingApplied.length; i++) {
            for (int j = 0; j < lightingApplied[i].length; j++) {
                if (lightingApplied[i][j] == 'n') {
                    Boolean valid = false;
                    // check top
                    for (int posI = i - 1; posI >= 0; posI--)
                        if (lightingApplied[posI][j] == '_')
                            valid = true;
                        else if (isAWall(lightingApplied[posI][j]))
                            break;
                    // check bottom
                    for (int posI = i + 1; posI < lightingApplied.length; posI++)
                        if (lightingApplied[posI][j] == '_')
                            valid = true;
                        else if (isAWall(lightingApplied[posI][j]))
                            break;

                    // check left
                    for (int posJ = j - 1; posJ >= 0; posJ--)
                        if (lightingApplied[i][posJ] == '_')
                            valid = true;
                        else if (isAWall(lightingApplied[i][posJ]))
                            break;

                    // check right
                    for (int posJ = j + 1; posJ < lightingApplied[i].length; posJ++)
                        if (lightingApplied[i][posJ] == '_')
                            valid = true;
                        else if (isAWall(lightingApplied[i][posJ]))
                            break;

                    if (!valid) {
                        return valid;
                    }
                }
            }
        }
        return true;
    }

    public static Boolean checkWalls(char[][] lightingApplied) { // only use when lighting has been applied
        Boolean wallsConsistent = true;
        for (int i = 0; i < lightingApplied.length; i++) {
            for (int j = 0; j < lightingApplied[i].length; j++) {
                if (isAWall(lightingApplied[i][j])) {
                    int value = lightingApplied[i][j] - '0';
                    int numBulbs = 0;
                    int numUnassigned = 0;

                    // check top
                    if (i - 1 >= 0)
                        if (lightingApplied[i - 1][j] == 'b')
                            numBulbs++;
                        else if (lightingApplied[i - 1][j] == '_')
                            numUnassigned++;
                    // check bottom
                    if (i + 1 < lightingApplied.length)
                        if (lightingApplied[i + 1][j] == 'b')
                            numBulbs++;
                        else if (lightingApplied[i + 1][j] == '_')
                            numUnassigned++;
                    // check left
                    if (j - 1 >= 0)
                        if (lightingApplied[i][j - 1] == 'b')
                            numBulbs++;
                        else if (lightingApplied[i][j - 1] == '_')
                            numUnassigned++;
                    // check right
                    if (j + 1 < lightingApplied[0].length)
                        if (lightingApplied[i][j + 1] == 'b')
                            numBulbs++;
                        else if (lightingApplied[i][j + 1] == '_')
                            numUnassigned++;

                    if (!((numBulbs <= value) && ((numBulbs + numUnassigned) >= value))) {
                        wallsConsistent = false;
                        return wallsConsistent;
                    }

                }
            }
        }

        return wallsConsistent;
    }

    public static Boolean applyLighting(char[][] lightingApplied) { // dual purpose - checks if there is a lighting
                                                                    // violation between two bulbs and applies lighting
                                                                    // to supplied matrix
        Boolean lightingConsistent = true;
        // char[][] lightingApplied = copyArray(assignment);
        for (int i = 0; i < lightingApplied.length; i++) {
            for (int j = 0; j < lightingApplied[i].length; j++) {
                if (lightingApplied[i][j] == 'b') {

                    // light top
                    for (int posI = i - 1; posI >= 0; posI--)
                        if (lightingApplied[posI][j] == '_' || lightingApplied[posI][j] == 'x'
                                || lightingApplied[posI][j] == 'n')
                            lightingApplied[posI][j] = 'x';
                        else if (lightingApplied[posI][j] == 'b') {
                            lightingConsistent = false;
                            break;
                        } else
                            break; // wall
                    // light bottom
                    for (int posI = i + 1; posI < lightingApplied.length; posI++)
                        if (lightingApplied[posI][j] == '_' || lightingApplied[posI][j] == 'x'
                                || lightingApplied[posI][j] == 'n')
                            lightingApplied[posI][j] = 'x';
                        else if (lightingApplied[posI][j] == 'b') {
                            lightingConsistent = false;
                            break;
                        } else
                            break; // wall
                    // light left
                    for (int posJ = j - 1; posJ >= 0; posJ--)
                        if (lightingApplied[i][posJ] == '_' || lightingApplied[i][posJ] == 'x'
                                || lightingApplied[i][posJ] == 'n')
                            lightingApplied[i][posJ] = 'x';
                        else if (lightingApplied[i][posJ] == 'b') {
                            lightingConsistent = false;
                            break;
                        } else
                            break; // wall
                    // light right
                    for (int posJ = j + 1; posJ < lightingApplied[i].length; posJ++)
                        if (lightingApplied[i][posJ] == '_' || lightingApplied[i][posJ] == 'x'
                                || lightingApplied[i][posJ] == 'n')
                            lightingApplied[i][posJ] = 'x';
                        else if (lightingApplied[i][posJ] == 'b') {
                            lightingConsistent = false;
                            break;
                        } else
                            break; // wall
                }
            }
        }
        return lightingConsistent;
    }

    public static void formatSolution(char[][] assignment) {
        for (int i = 0; i < assignment.length; i++) {
            for (int j = 0; j < assignment[i].length; j++)
                if (assignment[i][j] == 'n')
                    assignment[i][j] = '_';
        }
    }

    public static char[][] copyArray(char[][] old) {
        char[][] current = new char[old.length][old[0].length];
        for (int i = 0; i < old.length; i++)
            for (int j = 0; j < old[i].length; j++)
                current[i][j] = old[i][j];
        return current;
    }
}