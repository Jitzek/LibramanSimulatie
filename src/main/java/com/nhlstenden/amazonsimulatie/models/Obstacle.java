package com.nhlstenden.amazonsimulatie.models;

import java.util.UUID;

class Obstacle implements Object3D {
    private double sizeX;
    private double sizeY;
    private double sizeZ;

    private double x = 0;
    private double y = 0;
    private double z = 0;

    private double rotationX = 0;
    private double rotationY = 0;
    private double rotationZ = 0;

    private UUID uuid;

    public Obstacle(double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX, double rotationY, double rotationZ) {
        this.uuid = UUID.randomUUID();
        this.sizeX= sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
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

    /**
     * @param rotationX the rotationX to set
     */
    public void setRotationX(double rotationX) {
        this.rotationX = rotationX;
    }

    public void setRotationY(double rotationY) {
        this.rotationY = rotationY;
    }

    public void setRotationZ(double rotationZ) {
        this.rotationZ = rotationZ;
    }

    public double getSizeX() {
        return this.sizeX;
    }

    public double getSizeY() {
        return this.sizeY;
    }

    public double getSizeZ() {
        return this.sizeZ;
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

    @Override
    public String getUUID() {
        return this.uuid.toString();
    }

    @Override
    public String getType() {
        return Obstacle.class.getSimpleName().toLowerCase();
    }
}