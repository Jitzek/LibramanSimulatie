package com.nhlstenden.amazonsimulatie.algorithms;

import java.util.ArrayList;
import java.util.List;

public class PathFinding {
    private Collision collision;

    private List<List<String>> allPaths = new ArrayList<>();

    private String type;
    private double speed;

    public PathFinding(String type, double speed) {
        this.type = type;
        this.speed = speed;
        this.collision = new Collision();
    }

    public List<List<String>> getAllPaths() {
        return this.allPaths;
    }


    /**
     * Algorithm which builds paths the robot can use to reach it's destination
     * Starts by either changing the X or Z axis by increasing or decreasing (depending on which will bring it closer to the X or Z of the destination)
     * If it comes across an obstacle it will call the method pathFindSubList() and continue with values it gets from that recursively
     * If it reaches the X but hasn't reached the Z it will switch from finding the X to finding the Z (and vice-versa)
     * If it has gone to the right (x+) before and wants to go the left (x-) next it will be denied to prevent infinite loops (same for z+ and z-)
     * If the X and Z have been reached it will add it's build path to the a list containing each path that has also reached it's destination
     * @param currentPath - Already defined path, starts empty but will increase in size as it's being called on recursively
     * @param pref - Preference of what axis to look for first, X or Z
     * @param currentX - The X coordinate it's currently at
     * @param currentY - The Y coordinate it's currently at
     * @param currentZ - The Z coordinate it's currently at
     * @param sizeX - The size of the Object on the X-axis
     * @param sizeY - The size of the Object on the Y-axis
     * @param sizeZ - The size of the Object on the Z-axis
     * @param goalX - The X coordinate of it's destination
     * @param goalY - The Y coordinate of it's destination
     * @param goalZ - The Z coordinate of it's destination
     * @param goalSizeX - The Size of it's destination on the X-axis
     * @param goalSizeY - The Size of it's destination on the Y-axis
     * @param goalSizeZ - The Size of it's destination on the Z-axis
     * @param obstaclesTypes - The types of the obstacles found
     * @param obstaclesCoordinates - The coordinates of the obstacles found
     * @param obstaclesSizes - The sizes of the obstacles found
     */
    public void buildPaths(List<String> currentPath, String pref, 
                                    double currentX, double currentY, double currentZ, 
                                    double sizeX, double sizeY, double sizeZ,
                                    double goalX, double goalY, double goalZ, 
                                    double goalSizeX, double goalSizeY, double goalSizeZ, 
                                    List<String> obstaclesTypes, List<double[]> obstaclesCoordinates, List<double[]> obstaclesSizes) {
        // Overflow Prevention
        if (allPaths.size() > 50) {
            System.out.println(fg_white + bg_red + "Overflow" + color_reset);
            return;
        }
        // Defines the span of it's Goal (the area it needs to get to)
        double goalXspanmin = goalX - speed - (goalSizeX / 2);
        double goalXspanmax = goalX + speed + (goalSizeX / 2);
        double goalZspanmin = goalZ - speed - (goalSizeZ / 2);
        double goalZspanmax = goalZ + speed + (goalSizeZ / 2);
        
        // Determines what to reach first
        // The Goal's X or the Goal's Z
        // This can influence the length of the path
        boolean findX = false;
        boolean findZ = false;
        if (pref.equals("x")) {
            findX = true;
            findZ = false;
        } else {
            findX = false;
            findZ = true;
        }

        // config decides how close the Object should be to other Objects
        double config = 0.2;
        if (currentPath.size() == 0) {
            sizeX += config;
            sizeY += config;
            sizeZ += config;
        }
        

        String prevAction = "";
        boolean finishedCalc = false;
        int count = 0;
        boolean reachedDestination = false;
        while (!finishedCalc) {
            // Overflow prevention
            if (currentPath.size() >= 1500) {
                return;
            }
            // Get previous Action
            if (currentPath.size() > 0) {
                prevAction = currentPath.get(currentPath.size() - 1);
                // If the previous action going x- but it needs to go x+ to reach it's Goal
                if (currentX < goalX && prevAction.equals("x-")) {
                    // Search for the Z instead
                    findX = false;
                    findZ = true;
                }
                // If the previous action going x+ but it needs to go x- to reach it's Goal
                else if (currentX > goalX && prevAction.equals("x+")) {
                    // Search for the Z instead
                    findX = false;
                    findZ = true;
                }
            }
            if (findX) {
                // If the Object's X lies inside the X span of the Goal
                if (goalXspanmin < currentX && currentX < goalXspanmax) {
                    findX = false;
                    // If the Object's Z lies inside the Z span of the Goal
                    if (goalZspanmin < currentZ && currentZ < goalZspanmax) {
                        // Path Complete
                        reachedDestination = true;
                    } else {
                        // Else the Object needs to reach the Z
                        findZ = true;
                    }
                }
                // If the current X is smaller than the X of the Goal  &  The previous action wasn't making the X smaller (backtracking) 
                else if (currentX < goalX && prevAction != "x-") {
                    boolean alter = false;
                    for (int j = 0; j < obstaclesTypes.size(); j++) {
                        // Increasing the Object's X by with it's speed causes a collision with another Object
                        if (collision.collisionDetection((currentX + speed), currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                            alter = true;
                            // Make two Sublists, One tries to go around the Object by going Up and the Other tries to go around the Object by goind Down
                            List<String> tempChoiceA = pathFindSubList("z+", "x+", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            List<String> tempChoiceB = pathFindSubList("z-", "x+", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                // Check if there are no collisions going this path
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceA);
                                    // Recursively continue with this path
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceB);
                                    // Recursively continue with this path
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            return;
                        }
                    }
                    if (!alter) {
                        currentPath.add("x+");
                        currentX += speed;
                    }
                }
                // If the current X is bigger than the X of the Goal  &  The previous action wasn't making the X bigger (backtracking) 
                else if (currentX > goalX && prevAction != "x+") {
                    boolean alter = false;
                    for (int j = 0; j < obstaclesTypes.size(); j++) {
                        if (collision.collisionDetection((currentX - speed), currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("z+", "x-", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            List<String> tempChoiceB = pathFindSubList("z-", "x-", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceA);
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceB);
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            return;
                        }
                    }
                    if (!alter) {
                        currentPath.add("x-");
                        currentX -= speed;
                    }
                }
            }
            else if (findZ) {
                if (currentPath.size() > 0) {
                    prevAction = currentPath.get(currentPath.size() - 1);
                    if (currentZ < goalZ && prevAction.equals("z-")) {
                        findX = true;
                        findZ = false;
                    } else if (currentZ > goalZ && prevAction.equals("z+")) {
                        findX = true;
                        findZ = false;
                    }
                }
                if (goalZspanmin < currentZ && currentZ < goalZspanmax) {
                    findZ = false;
                    if (goalXspanmin < currentX && currentX < goalXspanmax) {
                        reachedDestination = true;
                    } else {
                        findX = true;
                    }
                }
                // If the current Z is smaller than the Z of the Goal  &  The previous action wasn't making the Z smaller (backtracking) 
                else if (currentZ < goalZ && prevAction != "z-") {
                    boolean alter = false;
                    for (int j = 0; j < obstaclesTypes.size(); j++) {
                        if (collision.collisionDetection(currentX, currentY, (currentZ + speed), sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("x+", "z+", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            List<String> tempChoiceB = pathFindSubList("x-", "z+", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceA);
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceB);
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            return;
                        }
                    }
                    if (!alter) {
                        currentPath.add("z+");
                        currentZ += speed;
                    }
                }
                // If the current Z is bigger than the Z of the Goal  &  The previous action wasn't making the Z bigger (backtracking) 
                else if (currentZ > goalZ && prevAction != "z+") {
                    boolean alter = false;
                    for (int j = 0; j < obstaclesTypes.size(); j++) {
                        if (collision.collisionDetection(currentX, currentY, (currentZ + speed), sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("x+", "z-", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            List<String> tempChoiceB = pathFindSubList("x-", "z-", currentX, currentY, currentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j));
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceA);
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstaclesTypes.size(); k++) {
                                            if (collision.collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, sizeX, sizeY, sizeZ, obstaclesCoordinates.get(j), obstaclesSizes.get(j))) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                if (valid) {
                                    tempPath.addAll(currentPath);
                                    tempPath.addAll(tempChoiceB);
                                    buildPaths(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
                                }
                            }
                            return;
                        }
                    }
                    if (!alter) {
                        currentPath.add("z-");
                        currentZ -= speed;
                    }
                }
            }
            if (reachedDestination) {
                //System.out.println("finished");
                currentPath.add("finish");
                allPaths.add(currentPath);
                //System.out.println(allPaths.size());
                finishedCalc = true;
            }
            //System.out.println(count + " " + currentPath.get(count));
            count++;
        }
        return;
    }

    /**
     * Defines a List designed to go around an obstacle
     * @param pref - Preference of what axis to look for first, X or Z
     * @param coll - Collision, What'd cause the robot go collide with an obstacle? (right, left, up or down) or (x+, x-, z+, z-)
     * @param currentX - The X coordinate of the robot
     * @param robotY - The Y coordinate of the robot
     * @param currentZ - The Z coordinate of the robot
     * @param obstacleXspanmin - The minimal X coordinate the obstacle takes in by it's size
     * @param obstacleXspanmax - The maximum X coordinate the obstacle takes in by it's size
     * @param obstacleZspanmin - The minimal Y coordinate the obstacle takes in by it's size
     * @param obstacleZspanmax - The maximum Y coordinate the obstacle takes in by it's size
     * @return - Returns a List containing instructions to go around an obstacle
     */
    private List<String> pathFindSubList(String pref, String coll, 
                                        double currentX, double currentY, double currentZ,
                                        double sizeX, double sizeY, double sizeZ,
                                        double[] obstacleCoordinates, double[] obstacleSizes) {
        List<String> choices = new ArrayList<>();
        boolean foundPath = false;
        boolean backtrack = false;
        //Preference is increasing X
        if (pref.equals("x+")) {
            choices = new ArrayList<>();
            while (!foundPath) {
                // Collision is due to z+
                if (coll.equals("z+")) {
                    // If the Object is not backtracking || there is a collision going the selected way
                    if (backtrack || collision.collisionDetection(currentX, 0, (currentZ + speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection((currentX + speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("x+");
                            currentX += speed;
                            backtrack = false;
                        }  else {
                            choices.add("z-");
                            currentZ -= speed;
                            backtrack = true;
                        } 
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to z-
                else {
                    if (backtrack || collision.collisionDetection(currentX, 0, (currentZ - speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection((currentX + speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("x+");
                            currentX += speed;
                            backtrack = false;
                        } else {
                            choices.add("z+");
                            currentZ += speed;
                            backtrack = true;
                        }
                    } else {
                        foundPath = true;
                    }
                }
            }
        }
        //Preference is decreasing X
        else if (pref.equals("x-")) {
            choices = new ArrayList<>();
            while (!foundPath) {
                // Collision is due to z+
                if (coll.equals("z+")) {
                    if (backtrack || collision.collisionDetection(currentX, 0, (currentZ + speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection((currentX - speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("x-");
                            currentX -= speed;
                            backtrack = false;
                        } else {
                            choices.add("z-");
                            currentZ -= speed;
                            backtrack = true;
                        }
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to z-
                else {
                    if (backtrack || collision.collisionDetection(currentX, 0, (currentZ - speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection((currentX - speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("x-");
                            currentX -= speed;
                            backtrack = false;
                        } else {
                            choices.add("z+");
                            currentZ += speed;
                            backtrack = true;
                        } 
                    } else {
                        foundPath = true;
                    }
                }
                
            }
        }
        //Preference is increasing Z
        if (pref.equals("z+")) {
            choices = new ArrayList<>();
            while (!foundPath) {
                // Collision is due to x+
                if (coll.equals("x+")) {
                    if (backtrack || collision.collisionDetection((currentX + speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection(currentX, 0, (currentZ + speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("z+");
                            currentZ += speed;
                            backtrack = false;
                        } else {
                            choices.add("x-");
                            currentX -= speed;
                            backtrack = true;
                        }
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to x-
                else {
                    if (backtrack || collision.collisionDetection((currentX - speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection(currentX, 0, (currentZ + speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("z+");
                            currentZ += speed;
                            backtrack = false;
                        } else {
                            choices.add("x+");
                            currentX += speed;
                            backtrack = true;
                        }
                    } else {
                        foundPath = true;
                    }
                }
            }
        }
        // Preference is decreasing Z
        else if (pref.equals("z-")) {
            choices = new ArrayList<>();
            while (!foundPath) {
                // Collision is due to x+
                if (coll.equals("x+")) {
                    if (backtrack || collision.collisionDetection((currentX + speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection(currentX, 0, (currentZ - speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("z-");
                            currentZ -= speed;
                            backtrack = false;
                        } else {
                            choices.add("x-");
                            currentX -= speed;
                            backtrack = true;
                        } 
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to x-
                else {
                    if (backtrack || collision.collisionDetection((currentX - speed), 0, currentZ, sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                        if (!collision.collisionDetection(currentX, 0, (currentZ - speed), sizeX, sizeY, sizeZ, obstacleCoordinates, obstacleSizes)) {
                            choices.add("z-");
                            currentZ -= speed;
                            backtrack = false;
                        } else {
                            choices.add("x+");
                            currentX += speed;
                            backtrack = true;
                        } 
                    } else {
                        foundPath = true;
                    }
                }
            }
        }
        return choices;
    }
    private static final String color_reset = "\u001B[0m";

    private static final String fg_black = "\u001B[30m";
    private static final String fg_red = "\u001B[31m";
    private static final String fg_green = "\u001B[32m";
    private static final String fg_yellow = "\u001B[33m";
    private static final String fg_blue = "\u001B[34m";
    private static final String fg_purple = "\u001B[35m";
    private static final String fg_cyan = "\u001B[36m";
    private static final String fg_white = "\u001B[37m";

    private static final String bg_black = "\u001B[40m";
    private static final String bg_red = "\u001B[41m";
    private static final String bg_green = "\u001B[42m";
    private static final String bg_yellow = "\u001B[43m";
    private static final String bg_blue = "\u001B[44m";
    private static final String bg_purple = "\u001B[45m";
    private static final String bg_cyan = "\u001B[46m";
    private static final String bg_white = "\u001B[47m";
}