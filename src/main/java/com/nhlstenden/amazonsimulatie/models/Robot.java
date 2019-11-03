package com.nhlstenden.amazonsimulatie.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.nhlstenden.amazonsimulatie.algorithms.*;

/*
 * Deze class stelt een robot voor. Hij impelementeerd de class Object3D, omdat het ook een
 * 3D object is. Ook implementeerd deze class de interface Updatable. Dit is omdat
 * een robot geupdate kan worden binnen de 3D wereld om zich zo voort te bewegen.
 */
class Robot extends Obstacle implements Object3D, Updatable {

    private World world;

    private Collision collisionClass = new Collision();

    private UUID uuid;

    private Robot collisionRobot;

    // The change in coordinates the Robot would take when updating it's path
    private static double speed = 0.055;

    private String reaction = "";

    private boolean emptyTruck;
    private boolean refillTruck;
    private boolean finishedPath = false;
    private boolean displayGoal = true;
    private boolean isDelivering;
    private boolean isGetting;
    private boolean isStationary = false;
    private boolean goIdle;
    private boolean isIdle;

    private double sizeX;
    private double sizeY;
    private double sizeZ;

    private double x;
    private double y;
    private double z;
    // Height at which Items should be carried at
    private double itemHeight = this.getSizeY();

    private double rotationX;
    private double rotationY;
    private double rotationZ;

    private double targetRotationY;
    private int rotationSteps = 0;
    private int rotationStepsLimit = 35;
    private double rotationStep = 0;

    private double[] idleStation;
    private Truck targetTruck;
    private Rack targetRack;

    private Item currentItem;
    private Item reservedItem;

    private int pathProgress = 0;
    private List<String> actionList;

    private List<Obstacle> obstacles = new ArrayList<>();
    private List<String> obstacleTypes = new ArrayList<>();
    private List<double[]> obstacleCoordinates = new ArrayList<>();
    private List<double[]> obstacleRotations = new ArrayList<>();
    private List<double[]> obstacleSizes = new ArrayList<>();

    private List<List<String>> allPaths = new ArrayList<>();
    private List<String> finalPath = new ArrayList<>();
    private List<String> backupPath = new ArrayList<>();
    //private String prevAction;

    //private boolean reachedDestination = false;

    private String currentGoal;

    public Robot(World world, double[] idleStation, double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX,
            double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.world = world;
        this.idleStation = idleStation;
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
        if (displayGoal) {
            displayCurrentGoal();
        }
        if (hasTarget()) {
            goIdle = false;
            isIdle = false;
            main();
        } else if (!isIdle) {
            goIdle = true;
            main();
        }
        return true;
    }

    @Override
    public String getUUID() {
        return this.uuid.toString();
    }

    public boolean isStationary() {
        return this.isStationary;
    }

    public List<String> getCurrentPath() {
        return this.finalPath;
    }

    public int getCurrentPathProgress() {
        return this.pathProgress;
    }
        
    public Robot getCollisionRobot() {
        return this.collisionRobot;
    }

    public void setCollisionRobot(Robot robot) {
        this.collisionRobot = robot;
    }

    public void setIsStationary(boolean isStationary) {
        this.isStationary = isStationary;
    }

    private boolean hasTarget() {
        if (targetRack == null && targetTruck == null) {
            return false;
        }
        return true;
    }

    private String getCollisionPreventionAction() {
        return this.reaction;
    }

    private void setCollisionPreventionAction(String reaction) {
        this.reaction = reaction;
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

    /**
     * Gets the target the Robot is going to
     * @return The current target of the Robot (Truck or Rack)
     */
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
    
    public Item getItem() {
        return this.currentItem;
    }

    /**
     * Gets last action taken by this Robot
     * @return The last action taken by this Robot or an empty String if this Robot has no path assigned
     */
    public String getAction() {
        if (finalPath != null && finalPath.size() > 0) {
            return finalPath.get(pathProgress);
        }
        return "";
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

    public boolean isIdle() {
        return this.isIdle;
    }

    /**
     * Displays the Robot's currently defined goal into the Console
     */
    private void displayCurrentGoal() {
        if (finalPath == null || finalPath.size() == 0) {
            System.out.println(fg_cyan + currentGoal + color_reset);
        }
    }

    /**
     * Defines the target of the Robot by looking at it's previous action, whether the Truck if emptying or refilling and whether the Truck is entering / has entered or is leaving / has left
     */
    private void defineTarget() {
        if (finalPath == null || finalPath.size() == 0) {
            // If there is no Truck
            if (world.getTruck() == null || world.getTruck().isLeaving()) {
                if (currentGoal != "Awaiting new Truck") {
                    System.out.println(bg_yellow + fg_black + "Awaiting Task..." + color_reset);
                    displayGoal = false;
                }
                currentGoal = "Awaiting new Truck";
                targetTruck = null;
                return;
            }
            else if (!world.getTruck().hasEntered()) {
                if (currentGoal == "Awaiting Truck Arrival") { 
                    displayGoal = false;
                }
                currentGoal = "Awaiting Truck Arrival";
                return;
            }
            
            // If Robot has an Item
            else if (currentItem != null) {
                // If the Truck is emptying
                if (world.getTruck().isEmptying()) {
                    // If the Robot already knew it should be emptying
                    if (emptyTruck) {
                        // Deliver Item to Rack
                        targetTruck = null;
                        setTarget(getClosestRack(getItem().getCategory()));
                        currentGoal = "Emptying Truck: Delivering Item to Rack";
                    }
                    // Else the Robot is still refilling needs to deliver it's Item first
                    else {
                        targetRack = null;
                        setTarget(world.getTruck());
                        currentGoal = "Refilling Truck: Delivering Item to Truck";
                        emptyTruck = true;
                        refillTruck = false;
                    }
                }
                else if (world.getTruck().isRefilling()) {
                    // If the Robot already knew it should be refilling
                    if (refillTruck) {
                        // Deliver Item to Truck
                        targetRack = null;
                        setTarget(world.getTruck());
                        currentGoal = "Refilling Truck: Delivering Item to Truck";
                    }
                    // Else the Robot is still emptying and needs to deliver it's Item first
                    else {
                        targetTruck = null;
                        setTarget(getClosestRack(getItem().getCategory()));
                        currentGoal = "Emptying Truck: Delivering Item to Rack";
                        emptyTruck = false;
                        refillTruck = true;
                    }
                }
            }
            // If Robot doesn't have an Item
            else if (currentItem == null) {
                // If the Truck is emptying
                if (world.getTruck().isEmptying()) {
                    // Truck has no more Items
                    if (!(world.getTruck().getItems().size() > 0)) {
                        targetTruck = null;
                        targetRack = null;
                        emptyTruck = false;
                        refillTruck = true;
                    }
                    else {
                        emptyTruck = true;
                        refillTruck = false;
                        int count = 0;
                        for (Item item : world.getTruck().getItems()) {
                            if (item.isReserved() && item.getReserver() != this) {
                                count++;
                            } else {
                                this.reservedItem = item;
                                item.reserveItem(this);
                                break;
                            }
                        }
                        // All Items are reserved by other Robots
                        if (count == world.getTruck().getItems().size()) {
                            if (currentGoal == bg_yellow + fg_black + "Emptying Truck: No more available Items" + color_reset) {
                                displayGoal = false;
                            } else {
                                displayGoal = true;
                            }
                            targetTruck = null;
                            targetRack = null;
                            currentGoal = bg_yellow + fg_black + "Emptying Truck: No more available Items" + color_reset;
                            emptyTruck = false;
                            refillTruck = true;
                            return;
                        }
                        else {
                            targetRack = null;
                            setTarget(world.getTruck());
                            currentGoal = "Emptying Truck: Getting Item from Truck";
                            emptyTruck = true;
                            refillTruck = false;
                        }
                    }
                }
                else if (world.getTruck().isRefilling()) {
                    boolean foundItem = false;
                    for (Item requiredItem : world.getTruck().getRequiredItems()) {
                        if (!foundItem) {
                            if (!requiredItem.isReserved() || requiredItem.getReserver() == this) {
                                requiredItem.reserveItem(this);
                                reservedItem = requiredItem;
                                foundItem = true;
                                targetTruck = null;
                                setTarget(reservedItem.getRack());
                            }
                        } else {
                            break;
                        }
                    }
                    if (!foundItem) {
                        if (currentGoal.equals(bg_yellow + fg_black + "Refilling Truck: No more Items needed")) {
                            displayGoal = false;
                        }
                        targetTruck = null;
                        targetRack = null;
                        emptyTruck = false;
                        refillTruck = false;
                        currentGoal = bg_yellow + fg_black + "Refilling Truck: No more Items needed";
                        return;
                    } else {
                        currentGoal = "Refilling Truck: Getting Item from Rack";
                        emptyTruck = false;
                        refillTruck = true;
                    }
                }
            }
            displayGoal = true;
        }
    }

    /**
     * Gets the obstacles of the World and assigns them to the global lists
     */
    private void getObstacles() {
        // Get obstacles
        obstacles = world.getObstacles();
        obstacleTypes = new ArrayList<>();
        obstacleCoordinates = new ArrayList<>();
        obstacleRotations = new ArrayList<>();
        obstacleSizes = new ArrayList<>();

        for (Obstacle obstacle : obstacles) {
            // In this you can determine what the robot should ignore
            // the robotCollision() method deals with Robot collisions
            if (obstacle != this && !(obstacle instanceof Robot)) {
                // Get type of the obstacle
                String type = obstacle.getClass().getSimpleName().toLowerCase();
                obstacleTypes.add(type);

                // Get coördinates of the obstacle
                double[] cxyz = new double[3];
                cxyz[0] = obstacle.getX();
                cxyz[1] = obstacle.getY();
                cxyz[2] = obstacle.getZ();
                obstacleCoordinates.add(cxyz);

                // Get the rotation of the obstacle
                double[] rxyz = new double[3];
                rxyz[0] = obstacle.getRotationX();
                rxyz[1] = obstacle.getRotationY();
                rxyz[2] = obstacle.getRotationZ();
                obstacleRotations.add(rxyz);

                // Get the size of the object
                double[] size = new double[3];
                size[0] = obstacle.getSizeX();
                size[1] = obstacle.getSizeY();
                size[2] = obstacle.getSizeZ();
                obstacleSizes.add(size);
            }
        }
        for (Robot robot : world.getRobots()) {
            if (robot.isIdle() && robot != this) {
                // Get type of the obstacle
                String type = robot.getClass().getSimpleName().toLowerCase();
                obstacleTypes.add(type);

                // Get coördinates of the obstacle
                double[] cxyz = new double[3];
                cxyz[0] = robot.getX();
                cxyz[1] = robot.getY();
                cxyz[2] = robot.getZ();
                obstacleCoordinates.add(cxyz);

                // Get the rotation of the obstacle
                double[] rxyz = new double[3];
                rxyz[0] = robot.getRotationX();
                rxyz[1] = robot.getRotationY();
                rxyz[2] = robot.getRotationZ();
                obstacleRotations.add(rxyz);

                // Get the size of the object
                double[] size = new double[3];
                size[0] = robot.getSizeX();
                size[1] = robot.getSizeY();
                size[2] = robot.getSizeZ();
                obstacleSizes.add(size);
            }
        }
    }

    /*
     * Main PathFinding Method
     * Gets all the obstacles in the world with their necessary information
     * Calls upon all the other methods assisting the process
    */
    public boolean main() {
        getObstacles();
        boolean collision = false;
        int i = 0;
        List<String> types = new ArrayList<>();
        types.addAll(obstacleTypes);
        List<double[]> coordinates = new ArrayList<>();
        coordinates.addAll(obstacleCoordinates);
        List<double[]> sizes = new ArrayList<>();
        sizes.addAll(obstacleSizes);
        for (Robot robot : world.getRobots()) {
            if (robot != this) {
                types.add(robot.getType());
                double[] robotCoordinates = {robot.getX(), robot.getY(), robot.getZ()};
                coordinates.add(robotCoordinates);
                double[] robotSizes = {robot.getSizeX(), robot.getSizeY(), robot.getSizeZ()};
                sizes.add(robotSizes);
            }
        }
        for (String type : types) {
            collision = collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ(), this.getSizeX(), this.getSizeY(), this.getSizeZ(), 
                                                coordinates.get(i), sizes.get(i));
            // Robot logic
            if (type.equals("robot")) {
                // What should happen when colliding with Rack or Truck
                if (collision) {
                    System.out.println(bg_red + fg_black + "Robot Collision" + color_reset);
                    String prevAction = backupPath.get(backupPath.size() - 1);
                    switch (prevAction) {
                        case "x+":
                            setX(getX() - speed);
                            break;
                        case "x-":
                            setX(getX() + speed);
                            break;
                        case "z+":
                            setZ(getZ() - speed);
                            break;
                        case "z-":
                            setZ(getZ() + speed);
                            break;
                    }
                    break;
                }
            }
            // Rack logic
            if (type.equals("rack") || type.equals("truck")) {
                // What should happen when colliding with Rack or Truck
                if (collision) {
                    System.out.println(bg_red + fg_black + "Rack or Truck Collision" + color_reset);
                    String prevAction = backupPath.get(backupPath.size() - 1);
                    switch (prevAction) {
                        case "x+":
                            setX(getX() - speed);
                            break;
                        case "x-":
                            setX(getX() + speed);
                            break;
                        case "z+":
                            setZ(getZ() - speed);
                            break;
                        case "z-":
                            setZ(getZ() + speed);
                            break;
                    }
                    break;
                }
            }
            // Global obstacle logic
            if (type.equals("obstacle")) {
                // What should happen when colliding with an Obstacle of unknown origin
                if (collision) {
                    System.out.println(bg_red + fg_black + "Collision" + color_reset);
                    String prevAction = backupPath.get(backupPath.size() - 1);
                    switch (prevAction) {
                        case "x+":
                            setX(getX() - speed);
                            break;
                        case "x-":
                            setX(getX() + speed);
                            break;
                        case "z+":
                            setZ(getZ() - speed);
                            break;
                        case "z-":
                            setZ(getZ() + speed);
                            break;
                    }
                    break;
                }
            }
            i++;
        }
        // Robot is going Idle
        if (goIdle) {
            if (!(finalPath.size() > 0)) {
                isGetting = false;
                isDelivering = false;
                appendPathFindData(idleStation[0], idleStation[1], idleStation[2], 
                                    0, 2, 0, 
                                    obstacleTypes, obstacleCoordinates, obstacleSizes);
            } else {
                if (updatePathPosition()) {
                    isIdle = true;
                }
            }
        }
        // Robot has an Item
        else if (currentItem != null) {
            isGetting = false;
            isDelivering = true;
            // If the Robot has a Rack as target
            if (targetRack != null) {
                // Needs to deliver to Rack
                // If the path has not been defined
                if (!(finalPath.size() > 0)) {
                    double[] targetArea1 = new double[3];
                    double[] targetArea2 = new double[3];

                    double x = targetRack.getX();
                    double y = targetRack.getY();
                    double z = targetRack.getZ();
                    double resetz = z;
                    int count = 0;
                    for (double index : targetRack.getIndexes()) {
                        // Empty spot
                        if (index == 0) {
                            targetArea1[0] = x - (targetRack.getSizeX()/2) - 1;
                            targetArea1[1] = y;
                            targetArea1[2] = z + 0.8;

                            targetArea2[0] = x + (targetRack.getSizeX()/2) + 1;
                            targetArea2[1] = y;
                            targetArea2[2] = z + 0.8;
                            break;
                        } else {
                            z -= 1;
                            count++;
                            if (count == 3) {
                                count = 0;
                                y += 0.9;
                                z = resetz;
                            }
                        }
                    }
                        
                    appendPathFindData(targetArea1[0], 0, targetArea1[2], 
                                    0, 2, 0, 
                                    obstacleTypes, obstacleCoordinates, obstacleSizes);
                    appendPathFindData(targetArea2[0], 0, targetArea2[2], 
                                    0, 2, 0, 
                                    obstacleTypes, obstacleCoordinates, obstacleSizes);
                }
                // Else update the position of the robot according to the path 
                else {
                    if (updatePathPosition()) {
                        // Robot has Reached Destination
                        targetRack.pushItem(currentItem);
                        // Robot has delivered item, as such he has lost his current item
                        currentItem.unReserveItem();
                        currentItem = null;
                        targetRack = null;
                    }
                }
                return true;
            }
            // Else if the Robot has a Truck as target 
            else if (targetTruck != null) {
                // Needs to deliver to Truck
                // If the path has not been defined
                if (!(finalPath.size() > 0)) {
                    double[] targetArea1 = targetTruck.getLoadingAreas().get(0);
                    double[] targetArea2 = targetTruck.getLoadingAreas().get(1);
                    appendPathFindData(targetArea1[0], targetArea1[1], targetArea1[2], 
                            0, 2, 0, 
                            obstacleTypes, obstacleCoordinates, obstacleSizes);
                    appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                            0, 2, 0, 
                            obstacleTypes, obstacleCoordinates, obstacleSizes);
                }
                // Else update the position of the robot according to the path 
                else {
                    if (updatePathPosition()) {
                        // Robot has Reached Destination
                        // Required Item is delivered
                        currentItem.unReserveItem();
                        targetTruck.removeRequiredItem(currentItem);
                        targetTruck.addItem(currentItem);
                        // Robot has delivered item, as such he has lost his current item
                        currentItem = null;
                        targetTruck = null;
                    }
                }
                return true;
            }
        }
        // Robot doesn't have Item
        else {
            // Needs to get Item from Truck
            if (targetTruck != null) {
                isGetting = true;
                isDelivering = false;
                // If the path has not yet been defined
                if (!(finalPath.size() > 0)) {
                    if (reservedItem != null) {
                        double[] targetArea1 = targetTruck.getLoadingAreas().get(0);
                        double[] targetArea2 = targetTruck.getLoadingAreas().get(1);

                        appendPathFindData(targetArea1[0], targetArea1[1], targetArea1[2], 
                                0, 2, 0, 
                                obstacleTypes, obstacleCoordinates, obstacleSizes);
                        appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                                0, 2, 0, 
                                obstacleTypes, obstacleCoordinates, obstacleSizes);
                    }
                }
                // Else if the path has been defined 
                else {
                    // Update Robot position
                    if (updatePathPosition()) {
                        // Robot has Reached Destination
                        // The reserved Item is now in posession of the Robot
                        currentItem = reservedItem;
                        reservedItem = null;
                        // The Item taken has been removed from the Truck
                        targetTruck.removeItem(currentItem);
                        // The Truck isn't the Robot's target anymore
                        targetTruck = null;
                    }
                }
                return true;
            } 
            // Needs to get Item from Rack
            else if (targetRack != null) {
                isGetting = true;
                isDelivering = false;
                // Path has not been defined
                if (!(finalPath.size() > 0)) {
                    // Get Required Item's Position
                    double[] targetArea1 = {(reservedItem.getX() - (targetRack.getSizeX()/2) - 1.0), reservedItem.getY(), reservedItem.getZ()};
                    double[] targetArea2 = {(reservedItem.getX() + (targetRack.getSizeX()/2) + 1.0), reservedItem.getY(), reservedItem.getZ()};
                            
                    appendPathFindData(targetArea1[0], targetArea1[1], targetArea1[2], 
                            0, 2, 0, 
                            obstacleTypes, obstacleCoordinates, obstacleSizes);
                    appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                            0, 2, 0, 
                            obstacleTypes, obstacleCoordinates, obstacleSizes);
                }
                // Else if the Path has been defined
                else {
                    // Update Robot position
                    if (updatePathPosition()) {
                        // Robot has Reached Destination
                        targetRack.removeItem(reservedItem, reservedItem.getIndex());
                        // The reserved Item is now in posession of the Robot
                        currentItem = reservedItem;
                        reservedItem = null;
                        // The Rack isn't the Robot's target anymore
                        targetRack = null;
                    }
                }
                return true;
            }
        }
        return true;
    }

    /**
     * Gets the closest Rack of it's category
     * @param category - The category of the Rack
     * @return The Rack which is the closest to the Robot
     */
    private Rack getClosestRack(String category) {
        Rack currentRack = null;
        double value = 0;
        for (Rack rack : world.getRacks()) {
            if (rack.getCategory().equals(category)) {
                double newValue = Math.abs(rack.getX() - getX() + (rack.getZ() - getZ()));
                if (value == 0) {
                    value = newValue;
                    currentRack = rack;
                }
                else if (newValue < value) {
                    currentRack = rack;
                }
                break;
            }
        }
        return currentRack;
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
        PathFinding pathFinding = new PathFinding(this.getType(), speed);
        pathFinding.buildPaths(new ArrayList<String>(), "z", 
                            this.getX(), this.getY(), this.getZ(), 
                            this.getSizeX(), this.getSizeY(), this.getSizeZ(), 
                            targetX, targetY, targetZ, targetSizeX, targetSizeY, targetSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
        pathFinding.buildPaths(new ArrayList<String>(), "x", 
                            this.getX(), this.getY(), this.getZ(), 
                            this.getSizeX(), this.getSizeY(), this.getSizeZ(), 
                            targetX, targetY, targetZ, targetSizeX, targetSizeY, targetSizeZ, obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
        this.allPaths.addAll(pathFinding.getAllPaths());
        
        double goalXspanmin = targetX - speed - (targetSizeX / 2) - (getSizeX() / 2);
        double goalXspanmax = targetX + speed + (targetSizeX / 2) + (getSizeX() / 2);
        double goalZspanmin = targetZ - speed - (targetSizeZ / 2) - (getSizeZ() / 2);
        double goalZspanmax = targetZ + speed + (targetSizeZ / 2) + (getSizeZ() / 2);
        
        List<String> tempPath = new ArrayList<>();
        if (finalPath != null) {
            tempPath = finalPath;
        }
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
     * @return - True: The Robot has reached it's destination || False: The Robot has not reached it's destination
     */
    private boolean updatePathPosition() {
        //String action = finalPath.get(0);
        //System.out.println(finalPath.size() + " " + action);
        if (finishedPath) {
            //printProgress(actionLimit, actionProgress);
            if (!isIdle()) {
                if ((targetTruck == null && targetRack == null) || itemAction()) {
                    setIsStationary(false);
                    pathProgress = 0;
                    finalPath.clear();
                    allPaths.clear();
                    backupPath.clear();
                    System.out.println("\r");
                    finishedPath = false;
                    System.out.println(bg_green + fg_black + "Finished Task");
                    System.out.println(color_reset);
                    return true;
                } else {
                    setIsStationary(true);
                }
            } else {
                pathProgress = 0;
                finalPath.clear();
                allPaths.clear();
                backupPath.clear();
                System.out.println("\r");
                finishedPath = false;
                return true;
            }
        }
        else {
            if (finalPath == null) {
                return false;
            }
            rotateLogic(finalPath.get(pathProgress));
            if (rotateRobot(finalPath.get(pathProgress))) {
                if (robotCollision()) {
                    return false;
                }
                else if (!finalPath.isEmpty()) {
                    isStationary = false;
                    switch (finalPath.get(pathProgress)) {
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
                        //printProgress(finalPath.size(), pathProgress + 1);
                        finishedPath = true;
                        if (isGetting) {
                            System.out.println(fg_cyan + "\r\n Loading Item..." + color_reset);
                        } else if (isDelivering) {
                            System.out.println(fg_cyan + "\r\n Unloading Item..." + color_reset);
                        }
                    }
                    //printProgress(finalPath.size(), pathProgress + 1);
                    if (!finishedPath) {
                        pathProgress++;
                    }
                    try {
                        backupPath.add(finalPath.get(pathProgress));
                        //finalPath.remove(0);
                    } catch (IndexOutOfBoundsException e) {

                    }
                    if (getItem() != null) {
                        this.currentItem.setX(getX());
                        this.currentItem.setY(this.itemHeight);
                        this.currentItem.setZ(getZ());
                    }
                }
            }
        }
        return false;
    }

    /**
     * Changes the rotation of the robot based on the way it's heading
     * @param action - Action taken (x+, x-, z+, z-)
     */
    private void rotateLogic(String action) {
        switch (action) {
            case "x+":
                targetRotationY = Math.PI/2;
                break;
            case "x-":
                targetRotationY = -(Math.PI/2);
                break;
            case "z+":
                targetRotationY = 0;
                break;
            case "z-":
                targetRotationY = 0;
                break;
        }
    }

    /**
     * Rotates the Robot to it's target Rotation
     * @param action - Action taken which led to the Robot having to rotate
     * @return True: The Robot has finished rotating || False: The Robot hasn't finished rotating
     */
    private boolean rotateRobot(String action) {
        if (getRotationY() == targetRotationY || rotationSteps == rotationStepsLimit) {
            setIsStationary(false);
            setRotationY(targetRotationY);
            rotationSteps = 0;
            rotationStep = 0;
            return true;
        } 
        if (rotationStep == 0) {
            rotationStep = (getRotationY() - targetRotationY)/rotationStepsLimit;
        }
        switch (action) {
            case "x+":
            case "z+":
                setRotationY(getRotationY() - rotationStep);
                if (getItem() != null) {
                    getItem().setRotationY(this.getRotationY());
                }
                break;
            case "x-":
            case "z-":
                setRotationY(getRotationY() + rotationStep);
                if (getItem() != null) {
                    getItem().setRotationY(this.getRotationY());
                }
                break;
        }
        rotationSteps++;
        isStationary = true;
        return false;
    }

    /**
     * Animates an Item being delivered or being taken
     * @return - True if animation has finished || False if animation has not finished
     */
    private boolean itemAction() {
        // Robot has Item
        if (currentItem != null) {
            // Robot is delivering to Truck
            if (targetTruck != null) {
                getItem().setRotationY(targetTruck.getRotationY());
                if (actionList != null) {
                    if (actionList == null || actionList.size() == 0) {
                        this.actionList = null;
                        return true;
                    }
                    moveItem(actionList, speed, currentItem, targetTruck.getX(), targetTruck.getY(), targetTruck.getZ());
                }
                else {
                    actionList = itemPath(currentItem.getX(), currentItem.getY(), currentItem.getZ(), 
                                        targetTruck.getX(), targetTruck.getY(), targetTruck.getZ());
                }
            } 
            // Robot is delivering to Rack
            else if (targetRack != null) {
                getItem().setRotationY(targetRack.getRotationY());
                if (actionList != null) {
                    if (actionList == null || actionList.size() == 0) {
                        this.actionList = null;
                        return true;
                    }
                    moveItem(actionList, speed, currentItem, targetRack.getX(), targetRack.getY(), targetRack.getZ());
                }
                else {
                    actionList = itemPath(currentItem.getX(), currentItem.getY(), currentItem.getZ(), 
                                        targetRack.getX(), targetRack.getY(), targetRack.getZ());
                }
            }
        }
        // Robot is Getting Item
        else {
            if (targetTruck != null) {
                if (actionList != null) {
                    if (actionList == null || actionList.size() == 0) {
                        this.actionList = null;
                        return true;
                    }
                    moveItem(actionList, speed, reservedItem, getX(), this.itemHeight, getZ());
                }
                else {
                    actionList = itemPath(reservedItem.getX(), reservedItem.getY(), reservedItem.getZ(), 
                                        getX(), this.itemHeight, getZ());
                }
            }
            else if (targetRack != null) {
                if (actionList != null) {
                    if (actionList == null || actionList.size() == 0) {
                        this.actionList = null;
                        return true;
                    }
                    moveItem(actionList, speed, reservedItem, getX(), this.itemHeight, getZ());
                }
                else {
                    actionList = itemPath(reservedItem.getX(), reservedItem.getY(), reservedItem.getZ(), 
                                        getX(), this.itemHeight, getZ());
                }
            }
        }
        return false;
    }

    /**
     * Decides the path of the animation of an Item being delivered or taken
     * @param x - The current X-Coordinate of the Item
     * @param y - The current Y-Coordinate of the Item
     * @param z - The current Z-Coordinate of the Item
     * @param goalX - The current X-Coordinate of it's destination
     * @param goalY - The current Y-Coordinate of it's destination
     * @param goalZ - The current Z-Coordinate of it's destination
     * @return A list containing the order of actions that should be taken for the Item to reach it's goal
     */
    private List<String> itemPath(double x, double y, double z, double goalX, double goalY, double goalZ) {
        List<String> itemPath = new ArrayList<>();
        double xDistance = x - goalX;
        double yDistance = y - goalY;
        double zDistance = z - goalZ;
        // Positive X
        if (xDistance > 0) {
            itemPath.add("x-");
        } else {
            itemPath.add("x+");
        }
        // Positive Y
        if (yDistance > 0) {
            itemPath.add("y-");
        } else {
            itemPath.add("y+");
        }
        // Positive Z
        if (zDistance > 0) {
            itemPath.add("z-");
        } else {
            itemPath.add("z+");
        }
        return itemPath;
    }

    /**
     * Moves the Item to it's goal by a given speed
     * @param actionList - The order in which it's actions should be taken
     * @param speed - The speed at which the Item should travel at
     * @param item - The Item
     * @param goalX - The X-Coordinate of the Item it's goal
     * @param goalY - The Y-Coordinate of the Item it's goal
     * @param goalZ - The Z-Coordinate of the Item it's goal
     * @return True: The Item has reached it's goal || False: The Item hasn't reached it's goal
     */
    private boolean moveItem(List<String> actionList, double speed, Item item, double goalX, double goalY, double goalZ) {
        switch (actionList.get(0)) {
            case "x+":
                if (!(item.getX() + speed > goalX)) {
                    item.setX(item.getX() + speed);
                } else {
                    item.setX(goalX);
                    actionList.remove("x+");
                }
                break;
            case "x-":
                if (!(item.getX() - speed < goalX)) {
                    item.setX(item.getX() - speed);
                } else {
                    item.setX(goalX);
                    actionList.remove("x-");
                }
                break;
            case "y+":
                if (!(item.getY() + speed > goalY)) {
                    item.setY(item.getY() + speed);
                } else {
                    item.setY(goalY);
                    actionList.remove("y+");
                }
                break;
            case "y-":
                if (!(item.getY() - speed < goalY)) {
                    item.setY(item.getY() - speed);
                } else {
                    item.setY(goalY);
                    actionList.remove("y-");
                }
                break;
            case "z+":
                if (!(item.getZ() + speed > goalZ)) {
                    item.setZ(item.getZ() + speed);
                } else {
                    item.setZ(goalZ);
                    actionList.remove("z+");
                }
                break;
            case "z-":
                if (!(item.getZ() - speed < goalZ)) {
                    item.setZ(item.getZ() - speed);
                } else {
                    item.setZ(goalZ);
                    actionList.remove("z-");
                }
                break;
            default:
                return true;
        }
        return false;
    } 

    /**
     * Determines whether a Robot is colliding with another Robot and informs both Robots what to do
     * The Robots try to work together to prevent the collision
     * "" - No action
     * "wait" - Robot won't update it's path
     * "rebuild" - Robot will rebuild it's path according to the colliding's Robot's position and size
     * "continue" - Robot will continue it's path regardless of possible collisions (use with caution)
     * 
     * This Method is configurable
     * 
     * @return - True if there is a collision - False if there is no collision
     */
    private boolean robotCollision() {
        /* Misc Configuration Section */     // Expansion Suggestions: -Make configuration file   -Make error log (when robots collide)

        // Decides the distance the Robots should keep from eachother
        // The closer to zero (not negative) would allow the Robots to drive closer to eachother but could increase the chance of collisions happening
        // Lower this when a Robot is struggling with tights spaces when trying to navigate around other Robots
        double robot_main_distance_config = 0.6;

        // Decides the distance the other Robot (Collision Robot) should be from this Robot before continueing it's path
        // Setting it close to zero (not negative) decreases this distance (not advised)
        // This configuration setting will only be set in effect when the reaction if this Robot is "wait"
        double robot_wait_distance_config = 1.2;

        /* -------------------- */

        boolean hasCollision = false;

        // Get the last action taken by the Robot
        String action = finalPath.get(pathProgress);
        boolean cont = true;

        // What should happen if both Robots are waiting?
        if (getCollisionRobot() != null && getCollisionRobot().getAction().equals("wait")) {
            if (this.reaction == "wait") {
                this.reaction = "rebuild";
                getCollisionRobot().setCollisionPreventionAction("wait");
                return true;
            }
        }

        // If Robot hasn't already been given a counter action
        if (this.reaction.equals("")) {
            for (Robot robot : world.getRobots()) {
                if (!cont) {
                    break;
                }
                if (robot != this) {
                    // Config decides the distance the Robots should keep from eachother
                    // The closer to zero (not negative) would allow the Robots to drive closer to eachother but could increase the chance of collisions happening

                    double[] thisCoordinates = new double[]{this.getX(), this.getY(), this.getZ()};
                    double[] thisSize = new double[]{this.getSizeX() + robot_main_distance_config, this.getSizeY() + robot_main_distance_config, this.getSizeZ() + robot_main_distance_config};
                    double[] robotCoordinates = new double[]{robot.getX(), robot.getY(), robot.getZ()};
                    double[] robotSize = new double[]{robot.getSizeX() + robot_main_distance_config, robot.getSizeY() + robot_main_distance_config, robot.getSizeZ() + robot_main_distance_config};


                    if (collisionClass.collisionDetection(getX() + speed, getY(), getZ(), 
                                                getSizeX(), getSizeY(), getSizeZ(),
                                                robotCoordinates, robotSize)
                                                ||
                        collisionClass.collisionDetection(getX() - speed, getY(), getZ(), 
                                                getSizeX(), getSizeY(), getSizeZ(),
                                                robotCoordinates, robotSize)
                                                ||
                        collisionClass.collisionDetection(getX(), getY(), getZ() + speed, 
                                                getSizeX(), getSizeY(), getSizeZ(),
                                                robotCoordinates, robotSize)
                                                ||
                        collisionClass.collisionDetection(getX(), getY(), getZ() - speed, 
                                                getSizeX(), getSizeY(), getSizeZ(),
                                                robotCoordinates, robotSize)) {
                        // Other Robot is standing still (could be Rotating, (un)loading Item or Idle)
                        if (robot.isStationary()) {
                            setCollisionRobot(robot);
                            robot.setCollisionRobot(this);
                            // What should happen when the other Robot is currently Idle?
                            if (robot.isIdle()) {
                                this.reaction = "rebuild";
                                robot.setCollisionPreventionAction("");
                            } 
                            // What should happen when the other Robot isn't Idle?
                            else {
                                this.reaction = "wait";
                                robot.setCollisionPreventionAction("");
                            }
                            return true;
                        }
                        // What should happen if the other Robot doesn't have a path assigned?
                        if (robot.getCurrentPath().isEmpty()) {
                            setCollisionRobot(robot);
                            robot.setCollisionRobot(this);
                            this.reaction = "rebuild";
                            robot.setCollisionPreventionAction("");
                            return true;
                        }
                    }
                    switch (action) {
                        case "x+":
                            // If going x+ causes a collision with another Robot
                            if (collisionClass.collisionDetection(this.getX() + speed, this.getY(), this.getZ(), this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                    robotCoordinates, robotSize)) {
                                hasCollision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                // Other Robot is going same way
                                if (robot.getAction().equals("x+")) {
                                    // Other Robot has no collision when continueing it's path
                                    if (!collisionClass.collisionDetection(robot.getX() + speed, robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    }
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(), 
                                                                thisCoordinates, thisSize)) {
                                            this.setX(this.getX() - speed);
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                }
                                // Other Robot is coming head on 
                                else if (robot.getAction().equals("x-")) {
                                    // If this Robot is closer to it's Target
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ() + speed, robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)
                                                                ||
                                            !collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ() - speed, robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    // If the other Robot is closer to it's Target
                                    else {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ() + speed, this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)
                                                                ||
                                            !collisionClass.collisionDetection(getX(), getY(), getZ() - speed, this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                                this.reaction = "rebuild";
                                                robot.setCollisionPreventionAction("wait");
                                        } else {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("rebuild");
                                        }
                                    }
                                }
                                // Other Robot is crossing this Robot's path 
                                else if (robot.getAction().equals("z+") || robot.getAction().equals("z-")) {
                                    if (collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ(), this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                        this.reaction = "continue";
                                        robot.setCollisionPreventionAction("wait");
                                    } else {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    }
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                        case "x-":
                            if (collisionClass.collisionDetection(this.getX() - speed, this.getY(), this.getZ(), this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                hasCollision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                // Other Robot is coming head on 
                                if (robot.getAction().equals("x+")) {
                                    // If this Robot is closer to it's Target
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ() + speed, robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)
                                                                ||
                                            !collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ() - speed, robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    // If the other Robot is closer to it's Target
                                    else {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ() + speed, this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)
                                                                ||
                                            !collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ() - speed, this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                                this.reaction = "rebuild";
                                                robot.setCollisionPreventionAction("wait");
                                        } else {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("rebuild");
                                        }
                                    }
                                }
                                // Other Robot is going same way
                                else if (robot.getAction().equals("x-")) {
                                    // Other Robot has no collision when continueing it's path
                                    if (!collisionClass.collisionDetection(robot.getX() - speed, robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    }
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                                getCollisionRobot().setX(getCollisionRobot().getX() + speed);
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                }
                                // Other Robot is crossing this Robot's path
                                else if (robot.getAction().equals("z+") || robot.getAction().equals("z-")) {
                                    if (collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ(), this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                        this.reaction = "continue";
                                        robot.setCollisionPreventionAction("wait");
                                    } else {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    }
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                        case "z+":
                            if (collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ() + speed, this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                hasCollision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                // Other Robot is crossing this Robot's path
                                if (robot.getAction().equals("x+") || (robot.getAction().equals("x-"))) {
                                    if (collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ(), this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                        this.reaction = "continue";
                                        robot.setCollisionPreventionAction("wait");
                                    } else {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    }
                                }
                                // Other Robot is going same way
                                else if (robot.getAction().equals("z+")) {
                                    // Other Robot has no collision when continueing it's path
                                    if (!collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ() + speed, robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    }
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                }
                                // Other Robot is coming head on
                                else if (robot.getAction().equals("z-")) {
                                    // If this Robot is closer to it's Target
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(robot.getX() + speed, robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)
                                                                ||
                                            !collisionClass.collisionDetection(robot.getX() - speed, robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    // If the other Robot is closer to it's Target
                                    else {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(getX() + speed, getY(), getZ(),  this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)
                                                                ||
                                            !collisionClass.collisionDetection(getX() - speed, getY(), getZ(),  this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                                this.reaction = "rebuild";
                                             robot.setCollisionPreventionAction("wait");
                                        } else {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("rebuild");
                                        }
                                    }
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                        case "z-":
                            if (collisionClass.collisionDetection(getX(), getY(), getZ() - speed,  this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                hasCollision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                // Other Robot is crossing this Robot's path
                                if (robot.getAction().equals("x+") || (robot.getAction().equals("x-"))) {
                                    if (collisionClass.collisionDetection(this.getX(), this.getY(), this.getZ(), this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                        if (!goIdle) {
                                            this.reaction = "continue";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                        
                                    } else {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    }
                                }
                                // Other Robot is coming head on
                                else if (robot.getAction().equals("z+")) {
                                    // If this Robot is closer to it's Target
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(robot.getX() + speed, robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)
                                                                ||
                                            !collisionClass.collisionDetection(robot.getX() - speed, robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    // If the other Robot is closer to it's Target
                                    else {
                                        // Check what who has more room to navigate
                                        if (!collisionClass.collisionDetection(this.getX() + speed, this.getY(), this.getZ(),  this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)
                                                                ||
                                            !collisionClass.collisionDetection(this.getX() - speed, this.getY(), this.getZ(),  this.getSizeX(), this.getSizeY(), this.getSizeZ(),
                                                                robotCoordinates, robotSize)) {
                                                this.reaction = "rebuild";
                                                robot.setCollisionPreventionAction("wait");
                                        } else {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("rebuild");
                                        }
                                    }
                                }
                                // Other Robot is going same way
                                else if (robot.getAction().equals("z-")) {
                                    // Other Robot has no Collision when continueing it's path
                                    if (!collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ() - speed, robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    } 
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionClass.collisionDetection(robot.getX(), robot.getY(), robot.getZ(), robot.getSizeX(), robot.getSizeY(), robot.getSizeZ(),
                                                                thisCoordinates, thisSize)) {
                                            getCollisionRobot().setZ(getCollisionRobot().getZ() + speed);
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("rebuild");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                    }
                    //hasCollision = false;
                }
            }
        } 
        // Else Robot has been given a counter action already
        else {
            switch (reaction) {
                case "continue":
                    setCollisionPreventionAction("");
                    getCollisionRobot().setCollisionPreventionAction("wait");
                    return false;
                case "wait":
                    if (this.isStationary()) {
                        return true;
                    }
                    else if (getCollisionRobot() == null || getCollisionRobot().getCollisionPreventionAction().equals("wait")) {
                        getCollisionRobot().setCollisionPreventionAction("");
                    } 
                    else if (getCollisionRobot().getCollisionPreventionAction().equals("rebuild")) {
                        return true;
                    }
                    else if (getCollisionRobot() != this) {
                        // Checks if there's still a collision
                        double[] robotCoordinates = new double[]{getCollisionRobot().getX(), getCollisionRobot().getY(), getCollisionRobot().getZ()};
                        double[] robotSize = new double[]{getCollisionRobot().getSizeX() + robot_wait_distance_config, getCollisionRobot().getSizeY() + robot_wait_distance_config, getCollisionRobot().getSizeZ() + robot_wait_distance_config};
                        boolean collision = false;
                        switch (action) {
                            case "x+":
                                if (collisionClass.collisionDetection(this.getX() + speed, this.getY(), this.getZ(), 
                                                    this.getSizeX() + robot_wait_distance_config, this.getSizeY() + robot_wait_distance_config, this.getSizeZ() + robot_wait_distance_config, 
                                                    robotCoordinates, robotSize)) {
                                        collision = true;
                                    }
                                break;
                            case "x-":
                                if (collisionClass.collisionDetection(this.getX() - speed, this.getY(), this.getZ(), 
                                                    this.getSizeX() + robot_wait_distance_config, this.getSizeY() + robot_wait_distance_config, this.getSizeZ() + robot_wait_distance_config, 
                                                    robotCoordinates, robotSize)) {
                                        collision = true;
                                    }
                                break;
                            case "z+":
                                if (collisionClass.collisionDetection(getX(), getY(), getZ() + speed, 
                                                    this.getSizeX() + robot_wait_distance_config, this.getSizeY() + robot_wait_distance_config, this.getSizeZ() + robot_wait_distance_config, 
                                                    robotCoordinates, robotSize)) {
                                        collision = true;
                                    }
                                break;
                            case "z-":
                                if (collisionClass.collisionDetection(getX(), getY(), getZ() - speed, 
                                                    this.getSizeX() + robot_wait_distance_config, this.getSizeY() + robot_wait_distance_config, this.getSizeZ() + robot_wait_distance_config, 
                                                    robotCoordinates, robotSize)) {
                                        collision = true;
                                    }
                                break;
                        }
                        if (collision) {
                            return true;
                        }
                        else {
                            setCollisionPreventionAction("");
                            getCollisionRobot().setCollisionPreventionAction("");
                            return false;
                        }
                    }
                case "rebuild":
                    allPaths.clear();
                    finalPath.clear();;
                    pathProgress = 0;
                    if (targetTruck != null) {
                        double[] targetArea1 = targetTruck.getLoadingAreas().get(0);
                        double[] targetArea2 = targetTruck.getLoadingAreas().get(1);

                        List<String> types = new ArrayList<>();
                        List<double[]> coordinates = new ArrayList<>();
                        List<double[]> sizes = new ArrayList<>();
                        types.add(getCollisionRobot().getType());
                        types.addAll(obstacleTypes);
                        double[] coordinateArr = new double[3];
                        {
                            coordinateArr[0] = getCollisionRobot().getX();
                            coordinateArr[1] = getCollisionRobot().getY();
                            coordinateArr[2] = getCollisionRobot().getZ();
                        }
                        coordinates.add(coordinateArr);
                        coordinates.addAll(obstacleCoordinates);
                        double[] sizeArr = new double[3];
                        {
                            sizeArr[0] = getCollisionRobot().getSizeX();
                            sizeArr[1] = getCollisionRobot().getSizeY();
                            sizeArr[2] = getCollisionRobot().getSizeZ();
                        }
                        sizes.add(sizeArr);
                        sizes.addAll(obstacleSizes);

                        appendPathFindData(targetArea1[0], targetArea1[1], targetArea1[2], 
                                        0, 2, 0, 
                                        obstacleTypes, obstacleCoordinates, obstacleSizes);
                        appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                                        0, 2, 0, 
                                        obstacleTypes, obstacleCoordinates, obstacleSizes);
                        boolean found_path = false;
                        
                        switch (getCollisionRobot().getAction()) {
                            case "x+":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("x-")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            case "x-":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("x+")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            case "z+":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("z-")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            case "z-":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("z+")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            default:
                                Random random = new Random();
                                this.finalPath = allPaths.get(random.nextInt(allPaths.size()));
                        }
                        if (!found_path) {
                            try {
                                String startsWith = finalPath.get(0);
                                int count = 0;
                                for (List<String> choice : allPaths) {
                                    if (startsWith.equals(choice.get(0))) {
                                        count++;
                                    }
                                }
                                if (count == allPaths.size() || allPaths.size() == 0) {
                                    this.reaction = "wait";
                                    getCollisionRobot().setCollisionPreventionAction("rebuild");
                                    break;
                                } else {
                                    Random random = new Random();
                                    this.finalPath = allPaths.get(random.nextInt(allPaths.size()));
                                    break;
                                }
                            }
                            catch (IndexOutOfBoundsException e) {
                                this.reaction = "wait";
                                getCollisionRobot().setCollisionPreventionAction("rebuild");
                            }
                        }
                        getCollisionRobot().setCollisionPreventionAction("wait");
                        //getCollisionRobot().setCollisionRobot(null);
                    } else if (targetRack != null) {
                        double[] targetArea1 = new double[3];
                        double[] targetArea2 = new double[3];
                        // If Robot is delivering an Item
                        if (getItem() != null) {
                            double x = targetRack.getX();
                            double y = targetRack.getY();
                            double z = targetRack.getZ();
                            double resetz = z;
                            int count = 0;
                            for (double index : targetRack.getIndexes()) {
                                // Empty spot
                                if (index == 0) {
                                    targetArea1[0] = x - (targetRack.getSizeX()/2) - 1;
                                    targetArea1[1] = y;
                                    targetArea1[2] = z + 0.8;
        
                                    targetArea2[0] = x + (targetRack.getSizeX()/2) + 1;
                                    targetArea2[1] = y;
                                    targetArea2[2] = z + 0.8;
                                    break;
                                } else {
                                    z -= 1;
                                    count++;
                                    if (count == 3) {
                                        count = 0;
                                        y += 0.9;
                                        z = resetz;
                                    }
                                }
                            }
                        }
                        // If Robot is getting an Item
                        else if (this.reservedItem != null) {
                            targetArea1 = new double[]{(reservedItem.getX() - (targetRack.getSizeX()/2) - 1.0), reservedItem.getY(), reservedItem.getZ()};
                            targetArea2 = new double[]{(reservedItem.getX() + (targetRack.getSizeX()/2) + 1.0), reservedItem.getY(), reservedItem.getZ()};
                        } else {
                            targetArea1 = targetRack.getLoadingAreas().get(0);
                            targetArea2 = targetRack.getLoadingAreas().get(1);
                        }

                        List<String> types = new ArrayList<>();
                        List<double[]> coordinates = new ArrayList<>();
                        List<double[]> sizes = new ArrayList<>();
                        types.add(getCollisionRobot().getType());
                        types.addAll(obstacleTypes);
                        double[] coordinateArr = new double[3];
                        {
                            coordinateArr[0] = getCollisionRobot().getX();
                            coordinateArr[1] = getCollisionRobot().getY();
                            coordinateArr[2] = getCollisionRobot().getZ();
                        }
                        coordinates.add(coordinateArr);
                        coordinates.addAll(obstacleCoordinates);
                        double[] sizeArr = new double[3];
                        {
                            sizeArr[0] = getCollisionRobot().getSizeX();
                            sizeArr[1] = getCollisionRobot().getSizeY();
                            sizeArr[2] = getCollisionRobot().getSizeZ();
                        }
                        sizes.add(sizeArr);
                        sizes.addAll(obstacleSizes);

                        appendPathFindData(targetArea1[0], targetArea1[1], targetArea1[2], 
                                        0, targetRack.getSizeY(), 0, 
                                        types, coordinates, sizes);
                        appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                                        0, targetRack.getSizeY(), 0, 
                                        types, coordinates, sizes);
                                        boolean found_path = false;
                        switch (getCollisionRobot().getAction()) {
                            case "x+":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("x-")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            case "x-":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("x+")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            case "z+":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("z-")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            case "z-":
                                for (List<String> path : this.allPaths) {
                                    if (found_path) {
                                        break;
                                    }
                                    if (!path.get(0).equals("z+")) {
                                        this.finalPath = path;
                                        found_path = true;
                                    }
                                }
                                break;
                            default:
                                Random random = new Random();
                                if (allPaths.size() > 0) {
                                    this.finalPath = allPaths.get(random.nextInt(allPaths.size()));
                                }
                        }
                        if (!found_path) {
                            try {
                                String startsWith = finalPath.get(0);
                                int count = 0;
                                for (List<String> choice : allPaths) {
                                    if (startsWith.equals(choice.get(0))) {
                                        count++;
                                    }
                                }
                                if (count == allPaths.size() || allPaths.size() == 0) {
                                    this.reaction = "wait";
                                    getCollisionRobot().setCollisionPreventionAction("rebuild");
                                    break;
                                } else {
                                    Random random = new Random();
                                    this.finalPath = allPaths.get(random.nextInt(allPaths.size()));
                                    break;
                                }
                            } catch (IndexOutOfBoundsException e) {
                                this.reaction = "wait";
                                getCollisionRobot().setCollisionPreventionAction("rebuild");
                            }
                            
                        }
                        getCollisionRobot().setCollisionPreventionAction("wait");
                        //getCollisionRobot().setCollisionRobot(null);
                    }
                    else {
                        List<String> types = new ArrayList<>();
                        List<double[]> coordinates = new ArrayList<>();
                        List<double[]> sizes = new ArrayList<>();
                        types.add(getCollisionRobot().getType());
                        types.addAll(obstacleTypes);
                        double[] coordinateArr = new double[3];
                        {
                            coordinateArr[0] = getCollisionRobot().getX();
                            coordinateArr[1] = getCollisionRobot().getY();
                            coordinateArr[2] = getCollisionRobot().getZ();
                        }
                        coordinates.add(coordinateArr);
                        coordinates.addAll(obstacleCoordinates);
                        double[] sizeArr = new double[3];
                        {
                            sizeArr[0] = getCollisionRobot().getSizeX();
                            sizeArr[1] = getCollisionRobot().getSizeY();
                            sizeArr[2] = getCollisionRobot().getSizeZ();
                        }
                        sizes.add(sizeArr);
                        sizes.addAll(obstacleSizes);

                        appendPathFindData(idleStation[0], idleStation[1], idleStation[2], 
                                        0, 2, 0, 
                                        types, coordinates, sizes);
                        boolean found_path = false;
                        for (List<String> path : this.allPaths) {
                            if (found_path) {
                                break;
                            }
                            switch (getCollisionRobot().getAction()) {
                                case "x+":
                                    if (finalPath.get(0).equals("x-")) {
                                        if (!path.get(0).equals("x-")) {
                                            this.finalPath = path;
                                            found_path = true;
                                        }
                                    } else {
                                        found_path = true;
                                    }
                                    break;
                                case "x-":
                                    if (finalPath.get(0).equals("x+")) {
                                        if (!path.get(0).equals("x+")) {
                                            this.finalPath = path;
                                            found_path = true;
                                        }
                                    } else {
                                        found_path = true;
                                    }
                                    break;
                                case "z+":
                                    if (finalPath.get(0).equals("z-")) {
                                        if (!path.get(0).equals("z-")) {
                                            this.finalPath = path;
                                            found_path = true;
                                        }
                                    } else {
                                        found_path = true;
                                    }
                                    break;
                                case "z-":
                                    if (finalPath.get(0).equals("z+")) {
                                        if (!path.get(0).equals("z+")) {
                                            this.finalPath = path;
                                            found_path = true;
                                        }
                                    } else {
                                        found_path = true;
                                    }
                                    break;
                                default:
                                    Random random = new Random();
                                    if (allPaths.size() > 0) {
                                        this.finalPath = allPaths.get(random.nextInt(allPaths.size()));
                                    }
                            }
                        }
                        if (!found_path) {
                            if (!(finalPath.size() > 0)) {
                                this.reaction = "wait";
                                break;
                            }
                            String startsWith = finalPath.get(0);
                            int count = 0;
                            for (List<String> choice : allPaths) {
                                if (startsWith.equals(choice.get(0))) {
                                    count++;
                                }
                            }
                            if (count == allPaths.size() || allPaths.size() == 0) {
                                this.reaction = "wait";
                                getCollisionRobot().setCollisionPreventionAction("rebuild");
                                break;
                            } else {
                                Random random = new Random();
                                this.finalPath = allPaths.get(random.nextInt(allPaths.size()));
                                break;
                            }
                        }
                        getCollisionRobot().setCollisionPreventionAction("wait");
                    }
                    this.reaction = "";
                    return true;
            }
        }
        this.reaction = "";
        return hasCollision;
    }

    private static void printProgress(long total, long current) {
        StringBuilder string = new StringBuilder(140);   
        int percent = (int) (current * 100 / total);
        string
            .append('\r')
            .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
            .append(String.format(" %d%% [", percent))
            .append(String.join("", Collections.nCopies(percent/10, "=")))
            .append('>')
            .append(String.join("", Collections.nCopies(10 - percent/10, " ")))
            .append(']')
            //.append(String.join("", Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
            .append(String.format(" %d/%d", current, total));

        System.out.print(string);
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