package com.nhlstenden.amazonsimulatie.models;

import java.util.UUID;

class Item implements Object3D, Updatable {
    private UUID uuid;

    private Rack rack;

    private int index;

    private double x = 0;
    private double y = 0;
    private double z = 0;

    private double sizeX = 0;
    private double sizeY = 0;
    private double sizeZ = 0;

    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;

    private Robot selector;
    private boolean isReserved = false;

    private String product;
    private String category;

    public Item(String category, String product, int index) {
        this.uuid = UUID.randomUUID();
        this.category = category;
        this.product = product;
        this.index = index;
    }

    @Override
    public boolean update() {
        return true;
    }

    public String getUUID() {
        return this.uuid.toString();
    }

    public String getType() {
        return Item.class.getSimpleName().toLowerCase();
    }

    public void setRack(Rack rack) {
        this.rack = rack;
    }

    public Rack getRack() {
        return this.rack;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
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

    public void setX(double x) {
        this.x = x;
    }
    public void setY(double y) {
        this.y = y;
    }
    public void setZ(double z) {
        this.z = z;
    }
    
    public void setRotationX(double rotationX) {
        this.rotationX = rotationX;
    }
    public void setRotationY(double rotationY) {
        this.rotationY = rotationY;
    }
    public void setRotationZ(double rotationZ) {
        this.rotationZ = rotationZ;
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
        this.selector = robot;
        isReserved = true;
    }
    public void unReserveItem() {
        this.selector = null;
        isReserved = false;
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

    @Override
    public double getSizeX() {
        return this.sizeX;
    }

    @Override
    public double getSizeY() {
        return this.sizeY;
    }

    @Override
    public double getSizeZ() {
        return this.sizeZ;
    }
}