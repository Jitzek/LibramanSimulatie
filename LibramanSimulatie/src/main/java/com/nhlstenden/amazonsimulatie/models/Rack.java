package com.nhlstenden.amazonsimulatie.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class Rack extends Obstacle implements Object3D, Updatable {
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

    private List<double[]> loadingAreas = new ArrayList<>();

    private String category;

    private List<Item> items = new ArrayList<Item>();

    public Rack(double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX, double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.uuid = UUID.randomUUID();
        defineLoadingAreas();
    }

    /**
     * Defines the areas where the Robots can load and unload Items (left and right of Rack)
     */
    private void defineLoadingAreas() {
        double[] loadAreaLeft = new double[3];
        loadAreaLeft[0] = getX() - (getSizeX()/2) - 1;
        loadAreaLeft[1] = getY();
        loadAreaLeft[2] = getZ();
        double[] loadAreaRight = new double[3];
        loadAreaRight[0] = getX() + (getSizeX()/2) + 1;
        loadAreaRight[1] = getY();
        loadAreaRight[2] = getZ();
        loadingAreas.add(loadAreaLeft);
        loadingAreas.add(loadAreaRight);
    }

    @Override
    public boolean update() {
        return true;
    }

    public void addItem(Item item) {
        this.items.add(item);
    }

    public void removeItem(Item item) {
        this.items.remove(item);
    }

    public List<Item> getItems() {
        return this.items;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getCategory() {
        return category;
    }

    public String getUUID() {
        return this.uuid.toString();
    }

    public String getType() {
        return Rack.class.getSimpleName().toLowerCase();
    }

    public List<double[]> getLoadingAreas() {
        return this.loadingAreas;
    }

}