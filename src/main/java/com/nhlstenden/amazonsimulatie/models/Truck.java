package com.nhlstenden.amazonsimulatie.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

class Truck extends Obstacle implements Object3D, Updatable {
    private UUID uuid;
    private double speed = 0.025;

    private double sizeX;
    private double sizeY;
    private double sizeZ;

    private double x = 0;
    private double y = 0;
    private double z = 0;

    private int maxListSize = 6;

    private double warehouseX;
    private double warehouseY;
    private double warehouseZ;

    private double roadX;
    private double roadY;
    private double roadZ;

    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;

    private List<String> pathList;

    private boolean isEmptying;
    private boolean isRefilling;
    private boolean isEntering = false;
    private boolean hasEntered = false;
    private boolean isLeaving = false;
    private boolean hasLeft = false;

    private List<double[]> loadingAreas = new ArrayList<>();

    private List<Item> items = new ArrayList<Item>();
    private List<Item> requiredItems = new ArrayList<>();
    // Category 0 has Product 1
    // Category 2 has Product 3
    // Category 4 has Product 5 etc.

    public Truck(double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX,
            double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.warehouseX = x;
        this.warehouseY = y;
        this.warehouseZ = z;
        this.roadX = warehouseX + 10;
        this.roadY = warehouseY;
        this.roadZ = warehouseZ;
        this.uuid = UUID.randomUUID();
        this.isEmptying = true;
        this.isRefilling = false;
        // this.items = createRandomItemList(possibleItems);

        setX(getX() + 10);

        isEntering = true;
        for (Item item : items) {
            System.out.println(item.getProduct());
        }
    }

    /**
     * Defines the areas where the Robots can load and unload Items (left and right
     * of Rack)
     */
    private void defineLoadingAreas() {
        double[] loadAreaLeft = new double[3];
        double[] loadAreaRight = new double[3];
        String dominant = "x";
        boolean isPositive = false;
        switch (dominant) {
        case "x":
            if (isPositive) {
                loadAreaLeft[0] = getX() + (getSizeX() / 2) + 1;
                loadAreaLeft[1] = getY();
                loadAreaLeft[2] = getZ() - (getSizeZ() / 2) - 0.5;

                loadAreaRight[0] = getX() + (getSizeX() / 2) + 1;
                loadAreaRight[1] = getY();
                loadAreaRight[2] = getZ() + (getSizeZ() / 2) + 0.5;
                loadingAreas.add(loadAreaLeft);
                loadingAreas.add(loadAreaRight);
                break;
            } else {
                loadAreaLeft[0] = getX() - (getSizeX() / 2) - 1;
                loadAreaLeft[1] = getY();
                loadAreaLeft[2] = getZ() - 1;

                loadAreaRight[0] = getX() - (getSizeX() / 2) - 1;
                loadAreaRight[1] = getY();
                loadAreaRight[2] = getZ() + 1;
                loadingAreas.add(loadAreaLeft);
                loadingAreas.add(loadAreaRight);
                break;
            }

        case "z":
            loadAreaLeft[0] = getX() - (getSizeX() / 2) - 2;
            loadAreaLeft[1] = getY();
            loadAreaLeft[2] = getZ() - (getSizeZ() / 2) - 1;

            loadAreaRight[0] = getX() + (getSizeX() / 2) + 2;
            loadAreaRight[1] = getY();
            loadAreaRight[2] = getZ() - (getSizeZ() / 2) - 1;
            loadingAreas.add(loadAreaLeft);
            loadingAreas.add(loadAreaRight);
            break;
        }

    }

    private void updateItemPositions() {
        for (Item item : getItems()) {
            item.setX(this.getX());
            item.setY(this.getY());
            item.setZ(this.getZ());
        }
    }

    public List<double[]> getLoadingAreas() {
        return this.loadingAreas;
    }

    public int getItemListSize() {
        return (new Random().nextInt(maxListSize) + 1);
    }

    public List<Item> createRandomItemList(String[][] possibleItems) {
        List<Item> randomItemList = new ArrayList<>();
        Random random = new Random();
        for (int i = (random.nextInt(maxListSize)); i < maxListSize; i++) {
            int index = random.nextInt(possibleItems.length - 1);
            String category = possibleItems[index][0];
            String product = possibleItems[index][random.nextInt(possibleItems[index].length - 1) + 1];
            Item item = new Item(category, product, -1);
            randomItemList.add(item);
        }
        return randomItemList;
    }

    @Override
    public boolean update() {
        if (isEntering) {
            hasLeft = false;
            updateItemPositions();
            if (this.pathList != null) {
                if (moveTruck(pathList, speed, warehouseX, warehouseY, warehouseZ)) {
                    defineLoadingAreas();
                    isEntering = false;
                    hasEntered = true;
                    this.pathList = null;
                }
            }
            else {
                this.pathList = pathList(getX(), getY(), getZ(), warehouseX, warehouseY, warehouseZ);
            }
        } else if (isLeaving) {
            hasEntered = false;
            updateItemPositions();
            if (this.pathList != null) {
                if (moveTruck(pathList, speed, roadX, roadY, roadZ)) {
                    isLeaving = false;
                    hasLeft = true;
                    this.pathList = null;
                }
            }
            else {
                this.pathList = pathList(getX(), getY(), getZ(), roadX, roadY, roadZ);
            }
        } else if (requiredItems.size() == 0) {
            isLeaving = true;
        }
        return true;
    }

    private List<String> pathList(double x, double y, double z, double goalX, double goalY, double goalZ) {
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

    private boolean moveTruck (List<String> actionList, double speed, double goalX, double goalY, double goalZ) {
        if (actionList.size() == 0) {
            return true;
        }
        switch (actionList.get(0)) {
            case "x+":
                if (!(getX() + speed > goalX)) {
                    setX(getX() + speed);
                } else {
                    setX(goalX);
                    actionList.remove("x+");
                }
                break;
            case "x-":
                if (!(getX() - speed < goalX)) {
                    setX(getX() - speed);
                } else {
                    setX(goalX);
                    actionList.remove("x-");
                }
                break;
            case "y+":
                if (!(getY() + speed > goalY)) {
                    setY(getY() + speed);
                } else {
                    setY(goalY);
                    actionList.remove("y+");
                }
                break;
            case "y-":
                if (!(getY() - speed < goalY)) {
                    setY(getY() - speed);
                } else {
                    setY(goalY);
                    actionList.remove("y-");
                }
                break;
            case "z+":
                if (!(getZ() + speed > goalZ)) {
                    setZ(getZ() + speed);
                } else {
                    setZ(goalZ);
                    actionList.remove("z+");
                }
                break;
            case "z-":
                if (!(getZ() - speed < goalZ)) {
                    setZ(getZ() - speed);
                } else {
                    setZ(goalZ);
                    actionList.remove("z-");
                }
                break;
            default:
                return true;
        }
        return false;
    }

    public List<Item> getRequiredItems() {
        return this.requiredItems;
    }

    public void addItem(Item item) {
        item.setIndex(-1);
        this.items.add(item);
    }

    public void removeItem(Item item) {
        int count = 0;
        for (Item item1 : this.items) {
            if (item1 == item) {
                this.items.remove(count);
                break;
            }
            count++;
        }
        this.items.remove(item);
    }

    public void addRequiredItem(Item item) {
        this.requiredItems.add(item);
    }

    public void removeRequiredItem(Item item) {
        try {
            int count = 0;
            for (Item item1 : this.requiredItems) {
                if (item1 == item) {
                    this.requiredItems.remove(count);
                    break;
                }
                count++;
            }
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<Item> getItems() {
        return this.items;
    }

    public String getUUID() {
        return this.uuid.toString();
    }

    public String getType() {
        return Truck.class.getSimpleName().toLowerCase();
    }

    public boolean isEmptying() {
        return this.isEmptying;
    }

    public boolean isRefilling() {
        return this.isRefilling;
    }

    public void setIsEmptying(boolean isEmptying) {
        this.isEmptying = isEmptying;
    }

    public void setIsRefilling(boolean isRefilling) {
        this.isRefilling = isRefilling;
    }

    public boolean isEntering() {
        return this.isEntering;
    }

    public void setIsEntering(boolean isEntering) {
        this.isEntering = isEntering;
        this.hasEntered = false;
    }

    public boolean isLeaving() {
        return this.isLeaving;
    }

    public void setIsLeaving(boolean isLeaving) {
        this.isLeaving = isLeaving;
        this.hasLeft = false;
    }

    public boolean hasEntered() {
        return this.hasEntered;
    }

    public boolean hasLeft() {
        return this.hasLeft;
    }

}