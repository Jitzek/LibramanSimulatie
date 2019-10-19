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

    private String category;

    private List<Item> items = new ArrayList<Item>();

    public Rack(String category, double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX, double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.uuid = UUID.randomUUID();
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public String getUUID() {
        return this.uuid.toString();
    }

    public String getType() {
        return Rack.class.getSimpleName().toLowerCase();
    }

    
}