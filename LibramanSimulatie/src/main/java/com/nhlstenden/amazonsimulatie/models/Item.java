package com.nhlstenden.amazonsimulatie.models;

import java.util.UUID;

class Item implements Object3D, Updatable {
    private UUID uuid;

    private double x = 0;
    private double y = 0;
    private double z = 0;

    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;

    private Robot selector;
    private boolean isReserved = false;

    private String product;
    private String category;

    public Item(String category, String product) {
        this.uuid = UUID.randomUUID();
        this.category = category;
        this.product = product;
    }

    @Override
    public boolean update() {
        return true;
    }

    public String getUUID() {
        return this.uuid.toString();
    }

    public String getType() {
        return Rack.class.getSimpleName().toLowerCase();
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public double getZ() {
        return z;
    }
    
    public double getRotationX() {
        return rotationX;
    }
    public double getRotationY() {
        return rotationY;
    }
    public double getRotationZ() {
        return rotationZ;
    }
    public void reserveItem(Robot robot) {
        selector = robot;
        isReserved = true;
    }
    public Robot getReserver() {
        return this.selector;
    }
    public boolean isReserved() {
        return isReserved;
    }

    public String getProduct() {
        return this.product;
    }

    public String getCategory() {
        return category;
    }
}