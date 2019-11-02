package com.nhlstenden.amazonsimulatie.algorithms;

public class Collision {
    public Collision() {

    }
    /**
     * 
     * @param x                   - The X coordinate of the robot
     * @param y                   - The Y coordinate of the robot
     * @param z                   - The Z coordinate of the robot
     * @param sizeX               - The size of the Object on the X-axis
     * @param sizeY               - The size of the Object on the Y-axis
     * @param sizeZ               - The size of the Object on the Z-axis
     * @param obstacleCoordinates - The coordinates of the other Object
     * @param obstacleSizes       - The sizes of the other Object 
     * @return
     */
    public boolean collisionDetection(double x, double y, double z, double sizeX, double sizeY, double sizeZ,
            double[] obstacleCoordinates, double[] obstacleSizes) {
        double obstacleXspanmin = obstacleCoordinates[0] - (obstacleSizes[0] / 2);
        double obstacleXspanmax = obstacleCoordinates[0] + (obstacleSizes[0] / 2);
        double obstacleZspanmin = obstacleCoordinates[2] - (obstacleSizes[2] / 2);
        double obstacleZspanmax = obstacleCoordinates[2] + (obstacleSizes[2] / 2);
        if (obstacleXspanmin < (x + (sizeX / 2)) && (x - (sizeX / 2)) < obstacleXspanmax) {
            if (obstacleZspanmin < (z + (sizeZ / 2)) && (z - (sizeZ / 2)) < obstacleZspanmax) {
                return true;
            }
        }
        return false;
    }
}