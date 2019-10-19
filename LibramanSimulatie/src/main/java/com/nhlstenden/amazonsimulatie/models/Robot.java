package com.nhlstenden.amazonsimulatie.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * Deze class stelt een robot voor. Hij impelementeerd de class Object3D, omdat het ook een
 * 3D object is. Ook implementeerd deze class de interface Updatable. Dit is omdat
 * een robot geupdate kan worden binnen de 3D wereld om zich zo voort te bewegen.
 */
class Robot extends Obstacle implements Object3D, Updatable {

    private World world;

    private UUID uuid;

    private static double speed = 0.05;

    private boolean emptyTruck;
    private boolean refillTruck;

    private double sizeX;
    private double sizeY;
    private double sizeZ;

    private double x;
    private double y;
    private double z;

    private double rotationX;
    private double rotationY;
    private double rotationZ;

    private Truck targetTruck;
    private Rack targetRack;

    private Item currentItem;
    private Item reservedItem;

    private List<List<String>> allPaths = new ArrayList<>();
    private List<String> finalPath = new ArrayList<>();
    private List<String> backupPath = new ArrayList<>();
    //private String prevAction;

    //private boolean reachedDestination = false;

    private String currentGoal;

    public Robot(World world, double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX,
            double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.world = world;
        this.uuid = UUID.randomUUID();
    }

    /*
     * Deze update methode wordt door de World aangeroepen wanneer de World zelf
     * geupdate wordt. Dit betekent dat elk object, ook deze robot, in de 3D wereld
     * steeds een beetje tijd krijgt om een update uit te voeren. In de
     * updatemethode hieronder schrijf je dus de code die de robot steeds uitvoert
     * (bijvoorbeeld positieveranderingen). Wanneer de methode true teruggeeft
     * (zoals in het voorbeeld), betekent dit dat er inderdaad iets veranderd is en
     * dat deze nieuwe informatie naar de views moet worden gestuurd. Wordt false
     * teruggegeven, dan betekent dit dat er niks is veranderd, en de informatie
     * hoeft dus niet naar de views te worden gestuurd. (Omdat de informatie niet
     * veranderd is, is deze dus ook nog steeds hetzelfde als in de view)
     */
    @Override
    public boolean update() {
        this.x = getX();
        this.y = getY();
        this.z = getZ();
        this.rotationX = getRotationX();
        this.rotationY = getRotationY();
        this.rotationZ = getRotationZ();
        defineTarget();
        pathFinding();
        return true;
    }

    private void defineTarget() {
        // If Robot has an Item
        if (currentItem != null) {
            // If the Truck is emptying
            if (world.getTruck().isEmptying()) {
                // If the Robot already knew it should be emptying
                if (emptyTruck) {
                    // Deliver Item to Truck
                    setTarget(world.getTruck());
                }
                // Else the Robot needs to deliver it's Item first
                else {
                    for (Rack rack : world.getRacks()) {
                        if (currentItem.getCategory().equals(rack.getCategory())) {
                            setTarget(rack);
                        }
                    }
                    emptyTruck = true;
                    refillTruck = false;
                }
            }
            else if (world.getTruck().isRefilling()) {
                // If the Robot already knew it should be refilling
                if (refillTruck) {
                    // Deliver Item to Truck
                    setTarget(world.getTruck());
                }
                // Else the Robot needs to deliver it's  Item first
                else {
                    for (Rack rack : world.getRacks()) {
                        if (currentItem.getCategory().equals(rack.getCategory())) {
                            setTarget(rack);
                        }
                    }
                    emptyTruck = false;
                    refillTruck = true;
                }
            }
        }
        // If Robot doesn't have an Item
        else if (currentItem == null) {
            // If the Truck is emptying
            if (world.getTruck().isEmptying()) {
                setTarget(world.getTruck());
                emptyTruck = true;
                refillTruck = false;
            }
            else if (world.getTruck().isRefilling()) {
                for (Item item : world.getTruck().getRequiredItems()) {
                    if (!item.isReserved() || item.getReserver() == this) {
                        for (Rack rack : world.getRacks()) {
                            if (item.getCategory().equals(rack.getCategory())) {
                                setTarget(rack);
                            }
                        }
                    }
                }
                emptyTruck = false;
                refillTruck = true;
            }
        }
    }

    /*
     * Sets a target for the robot to go to
    */
    public void setTarget(Truck truck) {
        this.targetTruck = truck;
    }
    public void setTarget(Rack rack) {
        this.targetRack = rack;
    }
    public Truck getTargetTruck() {
        if (targetTruck != null) {
            return targetTruck;
        }
        return null;
    }
    public Rack getTargetRack() {
        if (targetRack != null) {
            return targetRack;
        }
        return null;
    }

    @Override
    public String getUUID() {
        return this.uuid.toString();
    }

    public Item getItem() {
        return this.currentItem;
    }

    @Override
    public String getType() {
        /*
         * Dit onderdeel wordt gebruikt om het type van dit object als stringwaarde
         * terug te kunnen geven. Het moet een stringwaarde zijn omdat deze informatie
         * nodig is op de client, en die verstuurd moet kunnen worden naar de browser.
         * In de javascript code wordt dit dan weer verder afgehandeld.
         */
        return Robot.class.getSimpleName().toLowerCase();
    }

    /*
     * Main PathFinding Method
     * Gets all the obstacles in the world with their necessary information
     * Calls upon all the other methods assisting the proces
    */
    public boolean pathFinding() {
        // Get obstacles
        List<Obstacle> obstacles = world.getObstacles();
        List<String> types = new ArrayList<>();
        List<double[]> coordinates = new ArrayList<>();
        List<double[]> rotations = new ArrayList<>();
        List<double[]> sizes = new ArrayList<>();

        for (Obstacle obstacle : obstacles) {
            // In this if you can determine what the robot should ignore
            if (obstacle != this && obstacle != targetRack && obstacle != targetTruck) {
                // Get type of the obstacle
                String type = obstacle.getClass().getSimpleName().toLowerCase();
                types.add(type);

                // Get coördinates of the obstacle
                double[] cxyz = new double[3];
                cxyz[0] = obstacle.getX();
                cxyz[1] = obstacle.getY();
                cxyz[2] = obstacle.getZ();
                coordinates.add(cxyz);

                // Get the rotation of the obstacle
                double[] rxyz = new double[3];
                rxyz[0] = obstacle.getRotationX();
                rxyz[1] = obstacle.getRotationY();
                rxyz[2] = obstacle.getRotationZ();
                rotations.add(rxyz);

                // Get the size of the object
                double[] size = new double[3];
                size[0] = obstacle.getSizeX();
                size[1] = obstacle.getSizeY();
                size[2] = obstacle.getSizeZ();
                sizes.add(size);
            }
        }
        boolean collision = false;
        int i = 0;
        for (String type : types) {
            if (type.equals("robot")) {
                // robot logic
            }
            if (type.equals("rack") || type.equals("truck")) {
                // rack logic

                double object_x_coordinate_span_min = coordinates.get(i)[0] - (sizes.get(i)[0] / 2);
                double object_x_coordinate_span_max = coordinates.get(i)[0] + (sizes.get(i)[0] / 2);
                double object_z_coordinate_span_min = coordinates.get(i)[2] - (sizes.get(i)[2] / 2);
                double object_z_coordinate_span_max = coordinates.get(i)[2] + (sizes.get(i)[2] / 2);

                collision = collisionDetection(getX(), getY(), getZ(), object_x_coordinate_span_min,
                        object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max);
                if (collision) {
                    System.out.println("!!!Collision Detected!!!");
                    // What should happen on collision with a rack or truck?
                }
            }
            i++;
        }
        // If the robot is not colliding with an obstacle
        if (!collision) {
            // Robot has Item
            if (currentItem != null) {
                // If the Robot has a Rack as target
                if (targetRack != null) {
                    // Needs to deliver to Rack
                    currentGoal = "Deliver Item to Rack";
                    // If the path has not been defined
                    if (!(finalPath.size() > 0)) {
                        appendPathFindData(targetRack.getX(), targetRack.getY(), targetRack.getZ(), 
                                        targetRack.getSizeX(), targetRack.getSizeY(), targetRack.getSizeZ(), 
                                        types, coordinates, sizes);
                    }
                    // Else update the position of the robot according to the path 
                    else {
                        if (updatePathPosition()) {
                            // Robot has Reached Destination
                            System.out.println("Reached Destination");
                            // Robot has delivered item, as such he has lost his current item
                            currentItem = null;
                            targetRack = null;
                        }
                    }
                    return true;
                } else if (targetTruck != null) {
                   // Needs to deliver to Truck
                   currentGoal = "Deliver Item to Truck";
                }
            }
            // Robot doesn't have Item
            else {
                if (targetTruck != null) {
                    // Needs to get Item from Truck
                    currentGoal = "Get Item from Truck";
                    // If the truck targeted has items
                    if (targetTruck.getItems().size() > 0) {
                        for (Item item : targetTruck.getItems()) {
                            // If the item is not reserved by another robot
                            if (!item.isReserved() || item.getReserver() == this) {
                                // Reserve this item
                                item.reserveItem(this);
                                // Item is reserved but is not yet in the posession of the robot
                                reservedItem = item;
                                // If the path has not yet been defined
                                if (!(finalPath.size() > 0)) {
                                    appendPathFindData(targetTruck.getX(), targetTruck.getY(), targetTruck.getZ(), 
                                                targetTruck.getSizeX(), targetTruck.getSizeY(), targetTruck.getSizeZ(), 
                                                types, coordinates, sizes);
                                }
                                // Else if the path has been defined 
                                else {
                                    // Update robot position
                                    if (updatePathPosition()) {
                                        // Robot has Reached Destination
                                        System.out.println("Reached Destination");
                                        // If the Item is still available
                                        if (reservedItem != null) {
                                            // The reserved is now in posession of the robot
                                            currentItem = reservedItem;
                                            // The item taken has been removed from the truck
                                            targetTruck.removeItem(currentItem);
                                        }
                                        // The item isn't available anymore
                                        else {
                                            // Look for new Item 
                                        }
                                        // The truck isn't the robot's target anymore
                                        targetTruck = null;
                                        break;
                                    }
                                }
                                return true;
                            }
                        }
                    }
                } else if (targetRack != null) {
                    // Needs to get Item from Scaffolding
                    currentGoal = "Get Item from Scaffolding";
                }
            }
        }
        return true;

        // Decide the space the obstacle takes in
        // By comparing the coordinates, the rotation and the size of the obstacle
    }

    /**
     * Decides whether or not the robot is colliding with an obstacle
     * @param x - The X coordinate of the robot
     * @param y - The Y coordinate of the robot
     * @param z - The Z coordinate of the robot
     * @param object_x_coordinate_span_min - The minimal X coordinate the obstacle takes in by it's size
     * @param object_x_coordinate_span_max - The maximum X coordinate the obstacle takes in by it's size
     * @param object_z_coordinate_span_min - The minimal Z coordinate the obstacle takes in by it's size
     * @param object_z_coordinate_span_max - The maximum Z coordinate the obstacle takes in by it's size
     * @return - True (Robot is colliding with obstacle) - False (Robot isn't colliding with obstacle)
     */
    private boolean collisionDetection(double x, double y, double z, double object_x_coordinate_span_min,
            double object_x_coordinate_span_max, double object_z_coordinate_span_min,
            double object_z_coordinate_span_max) {
        if (object_x_coordinate_span_min < (x + (getSizeX() / 2))
                && (x - (getSizeX() / 2)) < object_x_coordinate_span_max) {
            if (object_z_coordinate_span_min < (z + (getSizeZ() / 2))
                    && (z - (getSizeZ() / 2)) < object_z_coordinate_span_max) {
                return true;
            }
        }
        return false;
    }

    /**
     * Appends data to the method building the paths
     * Checks whether or not a path actually reaches it's destination
     * Checks which path that reaches the destination of the target is the shortest
     * @param targetX - The X coordinate of the Target
     * @param targetY - The Y coordinate of the Target
     * @param targetZ - The Z coordinate of the Target
     * @param targetSizeX - The size of the target the x-axis takes in
     * @param targetSizeY - The size of the target the y-axis takes in
     * @param targetSizeZ - The size of the target the z-axis takes in
     * @param obstaclesTypes - The types of the obstacles found
     * @param obstaclesCoordinates - The coordinates of the obstacles found
     * @param obstaclesSizes - The sizes of the obstacles found
     */
    private void appendPathFindData(double targetX, double targetY, double targetZ, double targetSizeX, double targetSizeY, double targetSizeZ, List<String> obstaclesTypes, List<double[]> obstaclesCoordinates, List<double[]> obstaclesSizes) {
        //reachedDestination = false;
        pathFindList(new ArrayList<String>(), "x", getX(), getY(), getZ(), 
                            targetX, targetY, targetZ, 
                            targetSizeX, targetSizeY, targetSizeZ, 
                            obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
        //reachedDestination = false;
        pathFindList(new ArrayList<String>(), "z", getX(), getY(), getZ(), 
                            targetX, targetY, targetZ, 
                            targetSizeX, targetSizeY, targetSizeZ, 
                            obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
        
        double goalXspanmin = targetX - speed - (targetSizeX / 2) - (getSizeX() / 2);
        double goalXspanmax = targetX + speed + (targetSizeX / 2) + (getSizeX() / 2);
        double goalZspanmin = targetZ - speed - (targetSizeZ / 2) - (getSizeZ() / 2);
        double goalZspanmax = targetZ + speed + (targetSizeZ / 2) + (getSizeZ() / 2);
        List<String> tempPath = new ArrayList<>();
        for (List<String> path : allPaths) {
            if (path.size() > 0) {
                if (path.size() < tempPath.size() || !(tempPath.size() > 0)){
                    // Check if Path reaches Destination
                    double tempX = getX();
                    double tempZ = getZ();
                    for (String action : path) {
                        switch (action) {
                            case "x+":
                                tempX += speed;
                                break;
                            case "x-":
                                tempX -= speed;
                                break;
                            case "z+":
                                tempZ += speed;
                                break;
                            case "z-":
                                tempZ -= speed;
                                break;
                            case "finish":
                                if (goalXspanmin < tempX && tempX < goalXspanmax) {
                                    if (goalZspanmin < tempZ && tempZ < goalZspanmax) {
                                        tempPath = path;
                                    }
                                }
                            break;
                        }
                    }
                }
            }
        }
        finalPath = tempPath;

        // Forces Path
        //finalPath = allPaths.get(0);
    }

    /**
     * Updates the robot's X, Y and Z accordingly to the instruction defined in the path defined in the class
     * Clears each list when done
     * @return - True (Robot has reached it's destination) - False (Robot has not yet reached it's destination)
     */
    private boolean updatePathPosition() {
        String action = finalPath.get(0);
        System.out.println(finalPath.size() + " " + action);
        switch (finalPath.get(0)) {
            case "x+":
                setX(x + speed);
                break;
            case "x-":
                setX(x - speed);
                break;
            case "z+":
                setZ(z + speed);
                break;
            case "z-":
                setZ(z - speed);
                break;
            case "finish": 
                finalPath.clear();
                allPaths.clear();
                backupPath.clear();
                // Done
                return true;
            }
            try {
                backupPath.add(finalPath.get(0));
                finalPath.remove(0);
            } catch (IndexOutOfBoundsException e) {

            }
            // Not done yet
            return false;
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
     * @param goalX - The X coordinate of it's destination
     * @param goalY - The Y coordinate of it's destination
     * @param goalZ - The Z coordinate of it's destination
     * @param goalSizeX - The Size of it's destination on the X-axis
     * @param goalSizeY - The Size of it's destination on the Y-axis
     * @param goalSizeZ - The Size of it's destination on the Z-axis
     * @param obstacleTypes - The types of the obstacles found
     * @param obstaclesCoordinates - The coordinates of the obstacles found
     * @param obstacleSizes - The sizes of the obstacles found
     */
    private void pathFindList(List<String> currentPath, String pref, double currentX, double currentY, double currentZ, 
                                    double goalX, double goalY, double goalZ, double goalSizeX, double goalSizeY, double goalSizeZ,
                                    List<String> obstacleTypes, List<double[]> obstaclesCoordinates, List<double[]> obstacleSizes) {
        List<String> tempp = currentPath;
        double goalXspanmin = goalX - speed - (goalSizeX / 2) - (getSizeX() / 2);
        double goalXspanmax = goalX + speed + (goalSizeX / 2) + (getSizeX() / 2);
        double goalZspanmin = goalZ - speed - (goalSizeZ / 2) - (getSizeZ() / 2);
        double goalZspanmax = goalZ + speed + (goalSizeZ / 2) + (getSizeZ() / 2);
        
        boolean findX = false;
        boolean findZ = false;
        if (pref.equals("x")) {
            findX = true;
            findZ = false;
        } else {
            findX = false;
            findZ = true;
        }
        String prevAction = "";
        boolean finishedCalc = false;
        int count = 0;
        boolean reachedDestination = false;
        while (!finishedCalc) {
            // Overflow prevention
            if (currentPath.size() >= 10000) {
                return;
            }
            if (currentPath.size() > 0) {
                prevAction = currentPath.get(currentPath.size() - 1);
                if (currentX < goalX && prevAction.equals("x-")) {
                    findX = false;
                    findZ = true;
                } else if (currentX > goalX && prevAction.equals("x+")) {
                    findX = false;
                    findZ = true;
                }
            }
            if (findX) {
                if (goalXspanmin < currentX && currentX < goalXspanmax) {
                    findX = false;
                    if (goalZspanmin < currentZ && currentZ < goalZspanmax) {
                        reachedDestination = true;
                    } else {
                        findZ = true;
                    }
                } else if (currentX < goalX && prevAction != "x-") {
                    boolean alter = false;
                    for (int j = 0; j < obstacleTypes.size(); j++) {
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - speed - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + speed + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - speed - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + speed + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection((currentX + speed), currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("z+", "x+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("z-", "x+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceA);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                            }
                            if (!tempChoiceB.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceB);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                            }
                            return;
                        }
                    }
                    if (!alter) {
                        currentPath.add("x+");
                        currentX += speed;
                    }
                } else if (currentX > goalX && prevAction != "x+") {
                    boolean alter = false;
                    for (int j = 0; j < obstacleTypes.size(); j++) {
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - speed - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + speed + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - speed - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + speed + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection((currentX - speed), currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("z+", "x-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("z-", "x-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceA);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                            }
                            if (!tempChoiceB.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceB);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
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
                } else if (currentZ < goalZ && prevAction != "z-") {
                    boolean alter = false;
                    for (int j = 0; j < obstacleTypes.size(); j++) {
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - speed - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + speed + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - speed - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + speed + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection(currentX, currentY, (currentZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("x+", "z+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("x-", "z+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceA);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                            }
                            if (!tempChoiceB.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceB);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                            }
                            return;
                        }
                    }
                    if (!alter) {
                        currentPath.add("z+");
                        currentZ += speed;
                    }
                } else if (currentZ > goalZ && prevAction != "z+") {
                    boolean alter = false;
                    for (int j = 0; j < obstacleTypes.size(); j++) {
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - speed - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + speed + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - speed - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + speed + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection(currentX, currentY, (currentZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("x+", "z-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("x-", "z-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceA);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                            }
                            if (!tempChoiceB.isEmpty()) {
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        tempCurrentZ -= speed;
                                    }
                                }
                                List<String> tempPath = new ArrayList<>();
                                tempPath.addAll(currentPath);
                                tempPath.addAll(tempChoiceB);
                                pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
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
                System.out.println("finished");
                currentPath.add("finish");
                allPaths.add(currentPath);
                System.out.println(allPaths.size());
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
     * @param robotX - The X coordinate of the robot
     * @param robotY - The Y coordinate of the robot
     * @param robotZ - The Z coordinate of the robot
     * @param obstacleXspanmin - The minimal X coordinate the obstacle takes in by it's size
     * @param obstacleXspanmax - The maximum X coordinate the obstacle takes in by it's size
     * @param obstacleZspanmin - The minimal Y coordinate the obstacle takes in by it's size
     * @param obstacleZspanmax - The maximum Y coordinate the obstacle takes in by it's size
     * @return - Returns a List containing instructions to go around an obstacle
     */
    private List<String> pathFindSubList(String pref, String coll, double robotX, double robotY, double robotZ, 
                                        double obstacleXspanmin, double obstacleXspanmax, double obstacleZspanmin, double obstacleZspanmax) {
        List<String> choices = new ArrayList<>();
        boolean foundPath = false;
        boolean backtrack = false;
        //Preference is increasing X
        if (pref.equals("x+")) {
            choices = new ArrayList<>();
            while (!foundPath) {
                // Collision is due to z+
                if (coll.equals("z+")) {
                    if (backtrack || collisionDetection(robotX, 0, (robotZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection((robotX + speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("x+");
                            robotX += speed;
                            backtrack = false;
                        }  else {
                            choices.add("z-");
                            robotZ -= speed;
                            backtrack = true;
                        } 
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to z-
                else {
                    if (backtrack || collisionDetection(robotX, 0, (robotZ - speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection((robotX + speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("x+");
                            robotX += speed;
                            backtrack = false;
                        } else {
                            choices.add("z+");
                            robotZ += speed;
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
                    if (backtrack || collisionDetection(robotX, 0, (robotZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection((robotX - speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("x-");
                            robotX -= speed;
                            backtrack = false;
                        } else {
                            choices.add("z-");
                            robotZ -= speed;
                            backtrack = true;
                        }
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to z-
                else {
                    if (backtrack || collisionDetection(robotX, 0, (robotZ - speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection((robotX - speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("x-");
                            robotX -= speed;
                            backtrack = false;
                        } else {
                            choices.add("z+");
                            robotZ += speed;
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
                    if (backtrack || collisionDetection((robotX + speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection(robotX, 0, (robotZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("z+");
                            robotZ += speed;
                            backtrack = false;
                        } else {
                            choices.add("x-");
                            robotX -= speed;
                            backtrack = true;
                        }
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to x-
                else {
                    if (backtrack || collisionDetection((robotX - speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection(robotX, 0, (robotZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("z+");
                            robotZ += speed;
                            backtrack = false;
                        } else {
                            choices.add("x+");
                            robotX += speed;
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
                    if (backtrack || collisionDetection((robotX + speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection(robotX, 0, (robotZ - speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("z-");
                            robotZ -= speed;
                            backtrack = false;
                        } else {
                            choices.add("x-");
                            robotX -= speed;
                            backtrack = true;
                        } 
                    } else {
                        foundPath = true;
                    }
                }
                // Collision is due to x-
                else {
                    if (backtrack || collisionDetection((robotX - speed), 0, robotZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                        if (!collisionDetection(robotX, 0, (robotZ - speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            choices.add("z-");
                            robotZ -= speed;
                            backtrack = false;
                        } else {
                            choices.add("x+");
                            robotX += speed;
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
}