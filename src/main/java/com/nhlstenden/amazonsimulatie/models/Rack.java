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
    private int[] itemIndexes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };

    private String category;

    private List<Item> items = new ArrayList<Item>();

    public Rack(double sizeX, double sizeY, double sizeZ, double x, double y, double z, double rotationX,
            double rotationY, double rotationZ) {
        super(sizeX, sizeY, sizeZ, x, y, z, rotationX, rotationY, rotationZ);
        this.uuid = UUID.randomUUID();
        defineLoadingAreas();
    }

    /**
     * Defines the areas where the Robots can load and unload Items (left and right
     * of Rack)
     */
    private void defineLoadingAreas() {
        double[] loadAreaLeft = new double[3];
        loadAreaLeft[0] = getX() - (getSizeX() / 2) - 1;
        loadAreaLeft[1] = getY();
        loadAreaLeft[2] = getZ();
        double[] loadAreaRight = new double[3];
        loadAreaRight[0] = getX() + (getSizeX() / 2) + 1;
        loadAreaRight[1] = getY();
        loadAreaRight[2] = getZ();
        loadingAreas.add(loadAreaLeft);
        loadingAreas.add(loadAreaRight);
    }

    @Override
    public boolean update() {
        return true;
    }

    public void addItem(Item item, int index) {
        this.items.add(item);
        itemIndexes[index] = 1;
        item.setRack(this);
    }

    public void pushItem(Item item) {
        int indexCount = 0;
        for (int index : itemIndexes) {
            if (index == 0) {
                this.items.add(item);
                itemIndexes[indexCount] = 1;
                item.setIndex(indexCount);
                setItemPosition(item);
                item.setRack(this);
                return;
            }
            indexCount++;
        }
    }

    private void setItemPosition(Item item) {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        int count = 0;
        double resetz = z;
        for (int i = 0; i < item.getIndex(); i++) {
            z -= 1;
            count++;
            if (count == 3) {
                count = 0;
                y += 0.9;
                z = resetz;
            }
        }
        item.setX(x);
        item.setY(y);
        item.setZ(z + 0.8);
        item.setRotationY(0);
    }

    public void removeItem(Item item, int index) {
        int count = 0;
        for (Item item1 : this.items) {
            if (item1 == item) {
                this.items.remove(count);
                item.setRack(null);
                break;
            }
            count++;
        }
        itemIndexes[index] = 0;
    }

    public int[] getIndexes() {
        return this.itemIndexes;
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