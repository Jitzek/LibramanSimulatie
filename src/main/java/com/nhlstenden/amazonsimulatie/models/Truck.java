package com.nhlstenden.amazonsimulatie.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

class Truck extends Obstacle implements Object3D, Updatable {
    private UUID uuid;

    private double sizeX;
    private double sizeY;
    private double sizeZ;

    private double x = 0;
    private double y = 0;
    private double z = 0;

    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;

    private boolean isEmptying;
    private boolean isRefilling;
    private boolean isEntering = false;
    private boolean hasEntered = false;
    private boolean isLeaving = false;
    private boolean hasLeft = false;
    private int count = 0;
    private int enteringDone = 200; // Amount of frames before Truck has entered
    private int leavingDone = 200; // Amount of frames before Truck has left

    private List<double[]> loadingAreas = new ArrayList<>();

    private List<Item> items = new ArrayList<Item>();
    private List<Item> requiredItems = new ArrayList<>();
    // Category 0 has Product 1
    // Category 2 has Product 3
    // Category 4 has Product 5 etc.

    public Truck(String[][] possibleItems, double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX,
            double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.uuid = UUID.randomUUID();
        this.isEmptying = true;
        this.isRefilling = false;
        this.items = createRandomItemList(possibleItems);
        this.requiredItems = createRandomItemList(possibleItems);
        defineLoadingAreas();
        isEntering = true;
        for (Item item : items) {
            System.out.println(item.getProduct());
        }
    }

    /**
     * Defines the areas where the Robots can load and unload Items (left and right of Rack)
     */
    private void defineLoadingAreas() {
        double[] loadAreaLeft = new double[3];
        double[] loadAreaRight = new double[3];
        String dominant = "x";
        boolean isPositive = false;
        switch (dominant) {
            case "x":
                if (isPositive) {
                    loadAreaLeft[0] = getX() + (getSizeX()/2) + 1;
                    loadAreaLeft[1] = getY();
                    loadAreaLeft[2] = getZ() - (getSizeZ()/2) - 2;
                
                    loadAreaRight[0] = getX() + (getSizeX()/2) + 1;
                    loadAreaRight[1] = getY();
                    loadAreaRight[2] = getZ() + (getSizeZ()/2) + 2;
                    loadingAreas.add(loadAreaLeft);
                    loadingAreas.add(loadAreaRight);
                    break;
                } else{
                    loadAreaLeft[0] = getX() - (getSizeX()/2) -1;
                    loadAreaLeft[1] = getY();
                    loadAreaLeft[2] = getZ() - (getSizeZ()/2) - 2;
                
                    loadAreaRight[0] = getX() - (getSizeX()/2) - 1;
                    loadAreaRight[1] = getY();
                    loadAreaRight[2] = getZ() + (getSizeZ()/2) + 2;
                    loadingAreas.add(loadAreaLeft);
                    loadingAreas.add(loadAreaRight);
                    break;
                }
                
            case "z":
                loadAreaLeft[0] = getX() - (getSizeX()/2) - 2;
                loadAreaLeft[1] = getY();
                loadAreaLeft[2] = getZ() - (getSizeZ()/2) - 1;
                
                loadAreaRight[0] = getX() + (getSizeX()/2) + 2;
                loadAreaRight[1] = getY();
                loadAreaRight[2] = getZ() - (getSizeZ()/2) - 1;
                loadingAreas.add(loadAreaLeft);
                loadingAreas.add(loadAreaRight);
                break;
        }
        
    }

    public List<double[]> getLoadingAreas() {
        return this.loadingAreas;
    }

    private List<Item> createRandomItemList(String[][] possibleItems) {
        int listSize = 10;
        List<Item> randomItemList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < listSize; i++) {
            int index = random.nextInt(possibleItems.length - 1);
            String category = possibleItems[index][0];
            String product = possibleItems[index][random.nextInt(possibleItems[index].length - 1) + 1];
            randomItemList.add(new Item(category, product));
        }
        return randomItemList;
    }

    @Override
    public boolean update() {
        if (isEntering) {
            if (truckEntering()) {
                isEntering = false;
                hasEntered = true;
            }
        }
        else if (isLeaving) {
            if (truckLeaving()) {
                isLeaving = false;
                hasLeft = true;
            }
        }
        else if (requiredItems.size() == 0) {
            isLeaving = true;
        }
        return true;
    }

    private boolean truckEntering() {
        if (count >= enteringDone) {
            count = 0;
            return true;
        }
        count++;
        return false;
    }

    private boolean truckLeaving() {
        if (count >= leavingDone) {
            count = 0;
            return true;
        }
        count++;
        return false;
    }

    public List<Item> getRequiredItems() {
        return this.requiredItems;
    }

    public void addItem(Item item) {
        this.items.add(item);
    }

    public void removeItem(Item item) {
        this.items.remove(item);
    }

    public void addRequiredItem(Item item) {
        this.requiredItems.add(item);
    }

    public void removeRequiredItem(Item item) {
        try {
            this.requiredItems.remove(item);
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

    public boolean isLeaving() {
        return this.isLeaving;
    }

    public boolean hasEntered() {
        return this.hasEntered;
    }

    public boolean hasLeft() {
        return this.hasLeft;
    }

    
}