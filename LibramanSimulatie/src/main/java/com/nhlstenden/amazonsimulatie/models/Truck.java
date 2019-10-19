package com.nhlstenden.amazonsimulatie.models;

import java.util.ArrayList;
import java.util.List;
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

    private List<Item> items = new ArrayList<Item>();
    private List<Item> requiredItems = new ArrayList<>();

    public Truck(List<Item> requiredItems, double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX, double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.uuid = UUID.randomUUID();
        this.isEmptying = true;
        this.isRefilling = false;
    }

    @Override
    public boolean update() {
        return true;
    }

    public List<Item> getRequiredItems() {
        return this.requiredItems;
    }

    public void addItem(Item item) {
        this.items.add(item);
    }

    public void removeItem (Item item) {
        this.items.remove(item);
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
}