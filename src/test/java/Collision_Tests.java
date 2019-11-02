import org.junit.Assert;
import org.junit.Test;
import com.nhlstenden.amazonsimulatie.algorithms.Collision;

public class Collision_Tests {
    // Testing the validity
    @Test
    public void collisionDetection01() {
        Collision collision = new Collision();
        // Arrange

        // Act
        // Coordinates of the Obstacle
        double[] obstacleCoordinates = {1.5, 0, 1.0};
        // Sizes of the Obstacle
        double[] obstacleSizes = {2, 5, 5};
        // Coordinates and Sizes of the Object (x = 0, y = 0, z = 0
        //                                      sizeX = 1, sizeY = 0.5, sizeZ = 1)
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1, 0.5, 1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, false);
    }

    @Test
    public void collisionDetection02() {
        Collision collision = new Collision();
        // Arrange

        // Act
        double[] obstacleCoordinates = {1.0, 0, 3.0};
        double[] obstacleSizes = {2, 5, 5};
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1, 0.5, 1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, false);
    }

    @Test
    public void collisionDetection03() {
        Collision collision = new Collision();
        // Arrange

        // Act
        double[] obstacleCoordinates = {0, 0, 0};
        double[] obstacleSizes = {2, 5, 5};
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1, 0.5, 1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, true);
    }

    @Test
    public void collisionDetection04() {
        Collision collision = new Collision();
        // Arrange

        // Act
        double[] obstacleCoordinates = {1, 0, 1.5};
        double[] obstacleSizes = {2, 5, 5};
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1, 0.5, 1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, true);
    }


    // Testing the accuracy
    @Test
    public void collisionDetection05() {
        Collision collision = new Collision();
        // Arrange

        // Act
        double[] obstacleCoordinates = {1.49, 0, 1.0};
        double[] obstacleSizes = {2, 5, 5};
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1, 0.5, 1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, true);
    }

    @Test
    public void collisionDetection06() {
        Collision collision = new Collision();
        // Arrange

        // Act
        double[] obstacleCoordinates = {1.5, 0, 1.0};
        double[] obstacleSizes = {2, 5, 5};
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1.1, 0.5, 1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, true);
    }

    @Test
    public void collisionDetection07() {
        Collision collision = new Collision();
        // Arrange

        // Act
        double[] obstacleCoordinates = {1.0, 0, 2.9};
        double[] obstacleSizes = {2, 5, 5};
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1, 0.5, 1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, true);
    }

    @Test
    public void collisionDetection08() {
        Collision collision = new Collision();
        // Arrange

        // Act
        double[] obstacleCoordinates = {1.5, 0, 3.0};
        double[] obstacleSizes = {2, 5, 5};
        final boolean actual = collision.collisionDetection(0, 0, 0, 
                                                            1.1, 0.5, 1.1, 
                                                            obstacleCoordinates, obstacleSizes);

        // Assert
        Assert.assertEquals(actual, true);
    }
}