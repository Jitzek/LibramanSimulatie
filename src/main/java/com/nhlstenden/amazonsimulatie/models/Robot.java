package com.nhlstenden.amazonsimulatie.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/*
 * Deze class stelt een robot voor. Hij impelementeerd de class Object3D, omdat het ook een
 * 3D object is. Ook implementeerd deze class de interface Updatable. Dit is omdat
 * een robot geupdate kan worden binnen de 3D wereld om zich zo voort te bewegen.
 */
class Robot extends Obstacle implements Object3D, Updatable {

    private World world;

    private UUID uuid;

    private Robot collisionRobot;

    private static double speed = 0.05;

    private String reaction = "";

    private boolean emptyTruck;
    private boolean refillTruck;
    private boolean finishedPath = false;
    private boolean displayGoal = true;
    private boolean isDelivering;
    private boolean isGetting;
    private boolean isStationary = false;
    private boolean isIdle;
    private boolean goingIdle = false;

    private double sizeX;
    private double sizeY;
    private double sizeZ;

    private double x;
    private double y;
    private double z;

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
    private int actionProgress = 0;
    private int actionLimit = 150; // Amount of frames before taking or giving item from/to object has finished

    List<Obstacle> obstacles = new ArrayList<>();
    List<String> obstacleTypes = new ArrayList<>();
    List<double[]> obstacleCoordinates = new ArrayList<>();
    List<double[]> obstacleRotations = new ArrayList<>();
    List<double[]> obstacleSizes = new ArrayList<>();

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
            goingIdle = false;
            isIdle = false;
            pathFinding();
        } else {
            goingIdle = true;
            if (goIdle()) {
                goingIdle = false;
                isIdle = true;
                setIsStationary(true);
            }
        }
        return true;
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

    private void displayCurrentGoal() {
        if (finalPath == null || finalPath.size() == 0) {
            System.out.println(fg_cyan + currentGoal + color_reset);
        }
    }
    
    /**
     * Defines the target of the Robot by looking at the it's previous action, whether the Truck if emptying or refilling and whether the Truck is entering / has entered or is leaving / has left
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
                //this.isStationary = true;
                return;
            }
            else if (!world.getTruck().hasEntered()) {
                if (currentGoal == "Awaiting Truck Arrival") { 
                    displayGoal = false;
                }
                currentGoal = "Awaiting Truck Arrival";
                //this.isStationary = true;
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
                        int count = 0;
                        for (Item item : world.getTruck().getItems()) {
                            if (item.isReserved() && item.getReserver() != this) {
                                count++;
                            }
                        }
                        // All Items are reserved by other Robots
                        if (count == world.getTruck().getItems().size()) {
                            targetTruck = null;
                            targetRack = null;
                            currentGoal = "No more available Items";
                            emptyTruck = false;
                            refillTruck = true;
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
                    for (Item item : world.getTruck().getRequiredItems()) {
                        if (!foundItem) {
                            if (!item.isReserved() || item.getReserver() == this) {
                                foundItem = true;
                                targetTruck = null;
                                setTarget(getClosestRack(item.getCategory()));
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

    @Override
    public String getUUID() {
        return this.uuid.toString();
    }

    public Item getItem() {
        return this.currentItem;
    }

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
     * Makes the Robot go to it's idle Station
     * @return - True if the Robot is at it's station - False if the Robot isn't at it's station yet
     */
    private boolean goIdle() {
        if (finalPath == null || !(finalPath.size() > 0)) {
            getObstacles();
            appendPathFindData(idleStation[0], idleStation[1], idleStation[2], 
                            0, 2, 0, obstacleTypes, obstacleCoordinates, obstacleSizes);
        } else {
            if (updatePathPosition()) {
                return true;
            }
        }
        return false;
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
     * Calls upon all the other methods assisting the proces
    */
    public boolean pathFinding() {
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
            double object_x_coordinate_span_min = coordinates.get(i)[0] - (sizes.get(i)[0] / 2);
            double object_x_coordinate_span_max = coordinates.get(i)[0] + (sizes.get(i)[0] / 2);
            double object_z_coordinate_span_min = coordinates.get(i)[2] - (sizes.get(i)[2] / 2);
            double object_z_coordinate_span_max = coordinates.get(i)[2] + (sizes.get(i)[2] / 2);
            collision = collisionDetection(getX(), getY(), getZ(), object_x_coordinate_span_min,
                        object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max);
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
            i++;
        }
        // Robot has Item
        if (currentItem != null) {
            isGetting = false;
            isDelivering = true;
            // If the Robot has a Rack as target
            if (targetRack != null) {
                // Needs to deliver to Rack
                // If the path has not been defined
                if (!(finalPath.size() > 0)) {
                    double[] targetArea1 = targetRack.getLoadingAreas().get(0);
                    double[] targetArea2 = targetRack.getLoadingAreas().get(1);
                        
                    appendPathFindData(targetArea1[0], 0, targetArea1[2], 
                                    0, 2, 0, 
                                    obstacleTypes, obstacleCoordinates, obstacleSizes);
                    appendPathFindData(targetArea2[0], 0, targetArea2[2], 
                                    0, 2, 0, 
                                    obstacleTypes, obstacleCoordinates, obstacleSizes);
                    /*appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                                    0.2, targetRack.getSizeY(), 0.2, 
                                    types, coordinates, sizes);*/
                    /*appendPathFindData(targetRack.getX(), targetRack.getY(), targetRack.getZ(), 
                                    targetRack.getSizeX(), targetRack.getSizeY(), targetRack.getSizeZ(), 
                                    types, coordinates, sizes);*/
                }
                // Else update the position of the robot according to the path 
                else {
                    if (updatePathPosition()) {
                        // Robot has Reached Destination
                        // Robot has delivered item, as such he has lost his current item
                        currentItem = null;
                        targetRack = null;
                    }
                }
                return true;
            }
            // Else if the Robot has the Truck as target 
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

                    /*appendPathFindData(targetTruck.getX(), targetTruck.getY(), targetTruck.getZ(), 
                                targetTruck.getSizeX(), targetTruck.getSizeY(), targetTruck.getSizeZ(), 
                                obstacleTypes, obstacleCoordinates, obstacleSizes);*/
                }
                // Else update the position of the robot according to the path 
                else {
                    if (updatePathPosition()) {
                        // Robot has Reached Destination
                        // Required Item is delivered
                        targetTruck.removeRequiredItem(currentItem);
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
                    if (reservedItem == null) {
                        // If the truck targeted has items
                        if (targetTruck.getItems().size() > 0) {
                            for (Item item : targetTruck.getItems()) {
                                // If the item is not reserved by another robot
                                if (!item.isReserved() || item.getReserver() == this) {
                                    // Reserve this item
                                    item.reserveItem(this);
                                    // Item is reserved but is not yet in the posession of the robot
                                    reservedItem = item;

                                    double[] targetArea1 = targetTruck.getLoadingAreas().get(0);
                                    double[] targetArea2 = targetTruck.getLoadingAreas().get(1);
                                    appendPathFindData(targetArea1[0], targetArea1[1], targetArea1[2], 
                                            0, 2, 0, 
                                            obstacleTypes, obstacleCoordinates, obstacleSizes);
                                    appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                                            0, 2, 0, 
                                            obstacleTypes, obstacleCoordinates, obstacleSizes);

                                    /*appendPathFindData(targetTruck.getX(), targetTruck.getY(), targetTruck.getZ(), 
                                                targetTruck.getSizeX(), targetTruck.getSizeY(), targetTruck.getSizeZ(), 
                                                obstacleTypes, obstacleCoordinates, obstacleSizes);*/
                                    break;
                                }
                            }
                        }
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
                    // Get Required Item
                    for (Item item : world.getTruck().getRequiredItems()) {
                        // If the Item category and the Rack category match
                        if (item.getCategory().equals(targetRack.getCategory())) {
                            // If the Item isn't reserved by another Robot
                            if (!item.isReserved() || item.getReserver() == this) {
                                // Reserve this Item
                                item.reserveItem(this);
                                reservedItem = item;
                                
                                double[] targetArea1 = targetRack.getLoadingAreas().get(0);
                                double[] targetArea2 = targetRack.getLoadingAreas().get(1);
                    
                                appendPathFindData(targetArea1[0], targetArea1[1], targetArea1[2], 
                                            0, 2, 0, 
                                            obstacleTypes, obstacleCoordinates, obstacleSizes);
                                appendPathFindData(targetArea2[0], targetArea2[1], targetArea2[2], 
                                            0, 2, 0, 
                                            obstacleTypes, obstacleCoordinates, obstacleSizes);

                                /*appendPathFindData(targetRack.getX(), targetRack.getY(), targetRack.getZ(), 
                                                targetRack.getSizeX(), targetRack.getSizeY(), targetRack.getSizeZ(), 
                                                types, coordinates, sizes);*/
                                break;
                            }
                        }
                    }
                }
                // Else if the Path has been defined
                else {
                    // Update Robot position
                    if (updatePathPosition()) {
                        // Robot has Reached Destination
                        // The reserved Item is now in posession of the Robot
                        currentItem = reservedItem;
                        reservedItem = null;
                        // The Rack isn't the Robot's target anymore
                        targetRack = null;
                    }
                }
                return true;
            }
            else {
                isGetting = false;
                isDelivering = false;
                if (!(finalPath.size() > 0)) {
                    appendPathFindData(idleStation[0], idleStation[1], idleStation[2], 
                                    0, 2, 0, 
                                    obstacleTypes, obstacleCoordinates, obstacleSizes);
                } else {
                    updatePathPosition();
                }
            }
        }
        return true;

        // Decide the space the obstacle takes in
        // By comparing the coordinates, the rotation and the size of the obstacle
    }

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
            }
        }
        return currentRack;
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
        double config = 0.1; // Decides how close Obstacles can be from eachother
                           // Default: 2
        if (object_x_coordinate_span_min < (x + (getSizeX() / 2) - config)
                && (x - (getSizeX() / 2) + config) < object_x_coordinate_span_max) {
            if (object_z_coordinate_span_min < (z + (getSizeZ() / 2) - config)
                    && (z - (getSizeZ() / 2) + config) < object_z_coordinate_span_max) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a Robot is colliding with another Robot and informs both Robots what to do
     * The Robots try to work together to prevent the collision
     * "" - No action
     * "wait" - Robot won't update it's path
     * "rebuild" - Robot will rebuild it's path according to the colliding's Robot's position and size
     * "continue" - Robot will continue it's path
     * @return - True if there is a collision - False if there is no collision
     */
    private boolean robotCollision() {
        boolean collision = false;
        String action = finalPath.get(pathProgress);
        boolean cont = true;
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
                    double object_x_coordinate_span_min = robot.getX() - (robot.getSizeX() / 2) - getSizeX();
                    double object_x_coordinate_span_max = robot.getX() + (robot.getSizeX() / 2) + getSizeX();
                    double object_z_coordinate_span_min = robot.getZ() - (robot.getSizeZ() / 2) - getSizeZ();
                    double object_z_coordinate_span_max = robot.getZ() + (robot.getSizeZ() / 2) + getSizeZ();
                    if (collisionDetection(getX() + speed, getY(), getZ(), 
                                                object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                ||
                                collisionDetection(getX() - speed, getY(), getZ(), 
                                                object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                ||
                                collisionDetection(getX(), getY(), getZ() + speed, 
                                                object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                ||
                                collisionDetection(getX(), getY(), getZ() - speed, 
                                                object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                        if (robot.isStationary()) {
                            setCollisionRobot(robot);
                            robot.setCollisionRobot(this);
                            if (robot.isIdle()) {
                                this.reaction = "rebuild";
                                robot.setCollisionPreventionAction("");
                            } else {
                                this.reaction = "wait";
                                robot.setCollisionPreventionAction("");
                            }
                            return true;
                        }
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
                            if (collisionDetection(getX() + speed, getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                collision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                // Other Robot is going same way
                                if (robot.getAction().equals("x+")) {
                                    // Other Robot has no collision when continueing it's path
                                    if (!collisionDetection(robot.getX() + speed, robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    }
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionDetection(robot.getX(), robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("continue");
                                        } else if (collisionDetection(getX(), getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "continue";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                    //this.reaction = "wait";
                                    //robot.setCollisionPreventionAction("");
                                    /*if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        robot.setCollisionPreventionAction("wait");
                                        this.reaction = "";
                                    } else {
                                        robot.setCollisionPreventionAction("");
                                        this.reaction = "wait";
                                    }*/
                                }
                                // Other Robot is coming head on 
                                else if (robot.getAction().equals("x-")) {
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        if (!collisionDetection(robot.getX(), robot.getY(), robot.getZ() + speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(robot.getX(), robot.getY(), robot.getZ() - speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    } else {
                                        if (!collisionDetection(getX(), getY(), getZ() + speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(getX(), getY(), getZ() - speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    
                                    /*if (robot.isStationary()) {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }
                                    // This Robot's path is smaller than the other Robot's path (This Robot is closer to target)
                                    if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    } 
                                    // The other Robot's path is smaller than this Robot's path (Other Robot is closer to target)
                                    else {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }*/
                                }
                                else if (robot.getAction().equals("z+") || robot.getAction().equals("z-")) {
                                    this.reaction = "wait";
                                    robot.setCollisionPreventionAction("continue");
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                        case "x-":
                            if (collisionDetection(getX() - speed, getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                collision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                // Other Robot is coming head on 
                                if (robot.getAction().equals("x+")) {
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        if (!collisionDetection(robot.getX(), robot.getY(), robot.getZ() + speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(robot.getX(), robot.getY(), robot.getZ() - speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    } else {
                                        if (!collisionDetection(getX(), getY(), getZ() + speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(getX(), getY(), getZ() - speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    /*if (robot.isStationary()) {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }
                                    // This Robot's path is smaller than the other Robot's path (This Robot is closer to target)
                                    else if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }
                                    // The other Robot's path is smaller than this Robot's path (Other Robot is closer to target) 
                                    else {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    }*/
                                }
                                // Other Robot is going same way
                                else if (robot.getAction().equals("x-")) {
                                    // Other Robot has no collision when continueing it's path
                                    if (!collisionDetection(robot.getX() - speed, robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    }
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionDetection(robot.getX(), robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("continue");
                                        } else if (collisionDetection(getX(), getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "continue";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                    //this.reaction = "wait";
                                    //robot.setCollisionPreventionAction("");
                                    /*if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        robot.setCollisionPreventionAction("wait");
                                        this.reaction = "";
                                    } else {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("");
                                    }*/
                                }
                                else if (robot.getAction().equals("z+") || robot.getAction().equals("z-")) {
                                    this.reaction = "wait";
                                    robot.setCollisionPreventionAction("continue");
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                        case "z+":
                            if (collisionDetection(getX(), getY(), getZ() + speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                collision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                if (robot.getAction().equals("x+") || (robot.getAction().equals("x-"))) {
                                    this.reaction = "wait";
                                    robot.setCollisionPreventionAction("");
                                }
                                // Other Robot is going same way
                                else if (robot.getAction().equals("z+")) {
                                    // Other Robot has no collision when continueing it's path
                                    if (!collisionDetection(robot.getX(), robot.getY(), robot.getZ() + speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    }
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionDetection(robot.getX(), robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("continue");
                                        } else if (collisionDetection(getX(), getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "continue";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                    //this.reaction = "wait";
                                    //robot.setCollisionPreventionAction("");
                                    /*if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        robot.setCollisionPreventionAction("wait");
                                        this.reaction = "";
                                    } else {
                                        robot.setCollisionPreventionAction("");
                                        this.reaction = "wait";
                                    }*/
                                }
                                // Other Robot is coming head on
                                else if (robot.getAction().equals("z-")) {
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        if (!collisionDetection(robot.getX() + speed, robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(robot.getX() - speed, robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    } else {
                                        if (!collisionDetection(getX() + speed, getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(getX() - speed, getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                             robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    /*if (robot.isStationary()) {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }
                                    // This Robot's path is smaller than the other Robot's path (This Robot is closer to target)
                                    else if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    }
                                    // The other Robot's path is smaller than this Robot's path (Other Robot is closer to target)
                                    else {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }*/
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                        case "z-":
                            if (collisionDetection(getX(), getY(), getZ() - speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                collision = true;
                                setCollisionRobot(robot);
                                robot.setCollisionRobot(this);
                                if (robot.getAction().equals("x+") || (robot.getAction().equals("x-"))) {
                                    this.reaction = "wait";
                                    robot.setCollisionPreventionAction("continue");
                                }
                                // Other Robot is coming head on
                                else if (robot.getAction().equals("z+")) {
                                    if ((this.getCurrentPath().size() - pathProgress) <= (robot.getCurrentPath().size() - robot.getCurrentPathProgress())) {
                                        if (!collisionDetection(robot.getX() + speed, robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(robot.getX() - speed, robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    } else {
                                        if (!collisionDetection(getX() + speed, getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                                                ||
                                            !collisionDetection(getX() - speed, getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                                this.reaction = "wait";
                                                robot.setCollisionPreventionAction("rebuild");
                                        } else {
                                            this.reaction = "rebuild";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                    }
                                    /*if (robot.isStationary()) {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }
                                    // This Robot's path is smaller than the other Robot's path (This Robot is closer to target)
                                    else if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        this.reaction = "rebuild";
                                        robot.setCollisionPreventionAction("wait");
                                    }
                                    // The other Robot's path is smaller than this Robot's path (Other Robot is closer to target) 
                                    else {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("rebuild");
                                    }*/
                                }
                                // Other Robot is going same way
                                else if (robot.getAction().equals("z-")) {
                                    // Other Robot has no Collision when continueing it's path
                                    if (!collisionDetection(robot.getX(), robot.getY(), robot.getZ() - speed, object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                        this.reaction = "wait";
                                        robot.setCollisionPreventionAction("continue");
                                    } 
                                    // Both Robots have collision when continueing their paths
                                    else {
                                        // There is a collision regardless
                                        if (collisionDetection(robot.getX(), robot.getY(), robot.getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "wait";
                                            robot.setCollisionPreventionAction("continue");
                                        } else if (collisionDetection(getX(), getY(), getZ(), object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
                                            this.reaction = "continue";
                                            robot.setCollisionPreventionAction("wait");
                                        }
                                        else {
                                            //
                                        }
                                    }
                                    //this.reaction = "wait";
                                    //robot.setCollisionPreventionAction("");
                                    /*if (this.finalPath.size() < robot.getCurrentPath().size()) {
                                        robot.setCollisionPreventionAction("");
                                        this.reaction = "wait";
                                    } else {
                                        robot.setCollisionPreventionAction("wait");
                                        this.reaction = "";
                                    }*/
                                }
                                else {
                                    setCollisionPreventionAction("rebuild");
                                }
                                cont = false;
                                return true;
                            }
                            break;
                    }
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
                        this.reaction = "";
                    } 
                    else if (getCollisionRobot().getCollisionPreventionAction().equals("rebuild")) {
                        return true;
                    }
                    else if (getCollisionRobot() != this) {
                        double object_x_coordinate_span_min = getCollisionRobot().getX() - (getCollisionRobot().getSizeX() / 2) - getSizeX();
                        double object_x_coordinate_span_max = getCollisionRobot().getX() + (getCollisionRobot().getSizeX() / 2) + getSizeX();
                        double object_z_coordinate_span_min = getCollisionRobot().getZ() - (getCollisionRobot().getSizeZ() / 2) - getSizeZ();
                        double object_z_coordinate_span_max = getCollisionRobot().getZ() + (getCollisionRobot().getSizeZ() / 2) + getSizeZ();
                        if (collisionDetection(getX() + speed, getY(), getZ(), 
                                            object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                            ||
                            collisionDetection(getX() - speed, getY(), getZ(), 
                                            object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                            ||
                            collisionDetection(getX(), getY(), getZ() + speed, 
                                            object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)
                                            ||
                            collisionDetection(getX(), getY(), getZ() - speed, 
                                            object_x_coordinate_span_min, object_x_coordinate_span_max, object_z_coordinate_span_min, object_z_coordinate_span_max)) {
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
                        double[] targetArea1 = targetRack.getLoadingAreas().get(0);
                        double[] targetArea2 = targetRack.getLoadingAreas().get(1);

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
                            } catch (IndexOutOfBoundsException e) {
                                this.reaction = "wait";
                                getCollisionRobot().setCollisionPreventionAction("rebuild");
                            }
                            
                        }
                        getCollisionRobot().setCollisionPreventionAction("");
                        //getCollisionRobot().setCollisionRobot(null);
                    }
                    else if (goingIdle) {
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
                                    this.finalPath = allPaths.get(random.nextInt(allPaths.size()));
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
                        getCollisionRobot().setCollisionPreventionAction("");
                    }
                    this.reaction = "";
                    return true;
            }
        }
        this.reaction = "";
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
        pathFindList(new ArrayList<String>(), "z", getX(), getY(), getZ(), 
                            targetX, targetY, targetZ, 
                            targetSizeX, targetSizeY, targetSizeZ, 
                            obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
        //reachedDestination = false;
        pathFindList(new ArrayList<String>(), "x", getX(), getY(), getZ(), 
                            targetX, targetY, targetZ, 
                            targetSizeX, targetSizeY, targetSizeZ, 
                            obstaclesTypes, obstaclesCoordinates, obstaclesSizes);
        
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
     * @return - True (Robot has reached it's destination) - False (Robot has not yet reached it's destination)
     */
    private boolean updatePathPosition() {
        //String action = finalPath.get(0);
        //System.out.println(finalPath.size() + " " + action);
        if (finishedPath) {
            //printProgress(actionLimit, actionProgress);
            if (!isIdle()) {
                if (itemAction()) {
                    pathProgress = 0;
                    finalPath.clear();
                    allPaths.clear();
                    backupPath.clear();
                    System.out.println("\r");
                    finishedPath = false;
                    System.out.println(bg_green + fg_black + "Finished Task");
                    System.out.println(color_reset);
                    return true;
                }
            } else {
                pathProgress = 0;
                finalPath.clear();
                allPaths.clear();
                backupPath.clear();
                System.out.println("\r");
                finishedPath = false;
                System.out.println(bg_green + fg_black + "At Station");
                System.out.println(color_reset);
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
                else {
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
                }
            }
        }
        return false;
    }

    /**
     * Changes the rotation of the robot based on the way it's heading
     * @param action - Action taken (up, down, left, right)
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
                setRotationY(getRotationY() - rotationStep);
                break;
            case "x-":
                setRotationY(getRotationY() + rotationStep);
                break;
            case "z+":
                setRotationY(getRotationY() - rotationStep);
                break;
            case "z-":
                setRotationY(getRotationY() + rotationStep);
                break;
        }
        rotationSteps++;
        isStationary = true;
        return false;
    }

    /**
     * Delay in frames for Item delivering or acquiring
     * @return - True if animation has finished || False if animations had not finished
     */
    private boolean itemAction() {
        if (actionProgress >= actionLimit) {
            setIsStationary(false);
            actionProgress = 0;
            return true;
        }
        setIsStationary(true);
        actionProgress++;
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
        if (allPaths.size() > 50) {
            System.out.println(fg_white + bg_red + "Overflow" + color_reset);
            return;
        }
        /*double goalXspanmin = goalX - speed - (goalSizeX / 2) - (getSizeX() / 2);
        double goalXspanmax = goalX + speed + (goalSizeX / 2) + (getSizeX() / 2);
        double goalZspanmin = goalZ - speed - (goalSizeZ / 2) - (getSizeZ() / 2);
        double goalZspanmax = goalZ + speed + (goalSizeZ / 2) + (getSizeZ() / 2);*/
        double goalXspanmin = goalX - speed - (goalSizeX / 2);
        double goalXspanmax = goalX + speed + (goalSizeX / 2);
        double goalZspanmin = goalZ - speed - (goalSizeZ / 2);
        double goalZspanmax = goalZ + speed + (goalSizeZ / 2);
        
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
            if (currentPath.size() >= 1500) {
                return;
            }
            // Get previous Action
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
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection((currentX + speed), currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("z+", "x+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("z-", "x+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                                }
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
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection((currentX - speed), currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("z+", "x-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("z-", "x-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
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
                } else if (currentZ < goalZ && prevAction != "z-") {
                    boolean alter = false;
                    for (int j = 0; j < obstacleTypes.size(); j++) {
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection(currentX, currentY, (currentZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("x+", "z+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("x-", "z+", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                                }
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
                        double obstacleXspanmin = obstaclesCoordinates.get(j)[0] - (obstacleSizes.get(j)[0]/2) - (getSizeX()/2);
                        double obstacleXspanmax = obstaclesCoordinates.get(j)[0] + (obstacleSizes.get(j)[0]/2) + (getSizeX()/2);
                        double obstacleZspanmin = obstaclesCoordinates.get(j)[2] - (obstacleSizes.get(j)[2]/2) - (getSizeZ()/2);
                        double obstacleZspanmax = obstaclesCoordinates.get(j)[2] + (obstacleSizes.get(j)[2]/2) + (getSizeZ()/2);
                        if (collisionDetection(currentX, currentY, (currentZ + speed), obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                            alter = true;
                            List<String> tempChoiceA = pathFindSubList("x+", "z-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            List<String> tempChoiceB = pathFindSubList("x-", "z-", currentX, currentY, currentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax);
                            if (!tempChoiceA.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceA) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
                                }
                            }
                            if (!tempChoiceB.isEmpty()) {
                                boolean valid = true;
                                double tempCurrentX = currentX;
                                double tempCurrentY = currentY;
                                double tempCurrentZ = currentZ;
                                for (String action : tempChoiceB) {
                                    if (action.equals("x+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX + speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX += speed;
                                    } else if (action.equals("x-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX - speed, tempCurrentY, tempCurrentZ, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentX -= speed;
                                    } else if (action.equals("z+")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ + speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
                                                valid = false;
                                                break;
                                            }
                                        }
                                        tempCurrentZ += speed;
                                    } else if (action.equals("z-")) {
                                        for (int k = 0; k < obstacleTypes.size(); k++) {
                                            obstacleXspanmin = obstaclesCoordinates.get(k)[0] - (obstacleSizes.get(k)[0]/2) - (getSizeX()/2);
                                            obstacleXspanmax = obstaclesCoordinates.get(k)[0] + (obstacleSizes.get(k)[0]/2) + (getSizeX()/2);
                                            obstacleZspanmin = obstaclesCoordinates.get(k)[2] - (obstacleSizes.get(k)[2]/2) - (getSizeZ()/2);
                                            obstacleZspanmax = obstaclesCoordinates.get(k)[2] + (obstacleSizes.get(k)[2]/2) + (getSizeZ()/2);
                                            if (collisionDetection(tempCurrentX, tempCurrentY, tempCurrentZ - speed, obstacleXspanmin, obstacleXspanmax, obstacleZspanmin, obstacleZspanmax)) {
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
                                    pathFindList(tempPath, pref, tempCurrentX, tempCurrentY, tempCurrentZ, goalX, goalY, goalZ, goalSizeX, goalSizeY, goalSizeZ, obstacleTypes, obstaclesCoordinates, obstacleSizes);
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