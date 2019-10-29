package com.nhlstenden.amazonsimulatie.models;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * Deze class is een versie van het model van de simulatie. In dit geval is het
 * de 3D wereld die we willen modelleren (magazijn). De zogenaamde domain-logic,
 * de logica die van toepassing is op het domein dat de applicatie modelleerd, staat
 * in het model. Dit betekent dus de logica die het magazijn simuleert.
 */
public class World implements Model {
    // {x, y, z}
    private List<Obstacle> obstacles;
    private Truck truck;
    private List<Rack> racks;
    private List<Robot> robots;

    private static double[] truckSize = { 3.0, 2.5, 2.0 };
    private static double[] rackSize = { 1.2, 5.0, 3.5 };
    private static double[] robotSize = { 0.9, 0.3, 0.9 };

    private static String[] Electronica = { "Electronica", "Vlex On Virtual Assistant™",
            "Vlex On Privacy Concerning Home Security™", "Micromax MucBuk Prü",
            "Hello Kitty Professional Dj Headphones", "Vlex On GetLost™", "OnePlus Tea" };
    private static String[] Movies = { "Movies", "Birdemic IMAX 3D", "Airplane Mode", "Shrek 3",
            "Bee Movie but every time they say \"bee\" the movie gets 30% faster", "Monthy Python and The Holy Grail" };
    private static String[] PetSupplies = { "Pet Supplies", "Spiked Hamster Ball", "Chocolate Dog Treats",
            "Chinese Cook Book" };
    private static String[] Baby = { "Baby", "Electric Toilet Seat for Potty Training (Belts & Batteries included)",
            "Meth", "Glock 26 Gen 4 (9MM)" };
    private static String[] VideoGames = { "Video Games", "Hello Kitty Island Adventure GOTY", "Fallout 76 (99% OFF)",
            "Soulja Boy Game Console", "Garfield Go Kart" };
    private static String[] Books = { "Books", "Hello Kitty Islan Adventure Complete Lore",
            "How to tie a noose, and other fun party tricks", "De complete professional" };
    private static String[][] categories = { Electronica, Movies, PetSupplies, Baby, VideoGames, Books };

    /*
     * De wereld bestaat uit objecten, vandaar de naam worldObjects. Dit is een
     * lijst van alle objecten in de 3D wereld. Deze objecten zijn in deze
     * voorbeeldcode alleen nog robots. Er zijn ook nog meer andere objecten die ook
     * in de wereld voor kunnen komen. Deze objecten moeten uiteindelijk ook in de
     * lijst passen (overerving). Daarom is dit een lijst van Object3D onderdelen.
     * Deze kunnen in principe alles zijn. (Robots, vrachrtwagens, etc)
     */
    private List<Object3D> worldObjects;

    /*
     * Dit onderdeel is nodig om veranderingen in het model te kunnen doorgeven aan
     * de controller. Het systeem werkt al as-is, dus dit hoeft niet aangepast te
     * worden.
     */
    PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /*
     * De wereld maakt een lege lijst voor worldObjects aan. Daarin wordt nu één
     * robot gestopt. Deze methode moet uitgebreid worden zodat alle objecten van de
     * 3D wereld hier worden gemaakt.
     */
    public World() {
        this.worldObjects = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        this.racks = new ArrayList<>();
        this.robots = new ArrayList<>();

        // World, sizeX, sizeY, sizeZ, X, Y, Z, rotationX, rotationY, rotationZ

        /// Creating Truck
        /// ---------------
        /*
         * Rack rack1 = new Rack(rackSize[0], rackSize[1], rackSize[2], 10, 0, 10, 0, 0,
         * 0); Rack rack2 = new Rack(rackSize[0], rackSize[1], rackSize[2], 16, 0, 10,
         * 0, 0, 0); Rack rack3 = new Rack(rackSize[0], rackSize[1], rackSize[2], 22, 0,
         * 10, 0, 0, 0); Rack rack4 = new Rack(rackSize[0], rackSize[1], rackSize[2],
         * 10, 0, 22, 0, 0, 0); Rack rack5 = new Rack(rackSize[0], rackSize[1],
         * rackSize[2], 16, 0, 22, 0, 0, 0); Rack rack6 = new Rack(rackSize[0],
         * rackSize[1], rackSize[2], 22, 0, 22, 0, 0, 0);
         * 
         * rack1.setCategory("Electronics"); rack2.setCategory("Books");
         * rack3.setCategory("Video Games"); rack4.setCategory("Sports");
         * rack5.setCategory("Fashion"); rack6.setCategory("Pet Supplies");
         */
        addRacks();

        double[] idleStation1_coordinates = new double[3];
        idleStation1_coordinates[0] = 12;
        idleStation1_coordinates[1] = 0;
        idleStation1_coordinates[2] = 12;
        double[] idleStation2_coordinates = new double[3];
        idleStation2_coordinates[0] = 12;
        idleStation2_coordinates[1] = 0;
        idleStation2_coordinates[2] = -12;

        Robot robot1 = new Robot(this, idleStation1_coordinates, robotSize[0], robotSize[1], robotSize[2], 12, 0, 12, 0,
                0, 0);
        Robot robot2 = new Robot(this, idleStation2_coordinates, robotSize[0], robotSize[1], robotSize[2], 12, 0, -12, 0,
                0, 0);

        this.worldObjects.add(robot1);
        this.worldObjects.add(robot2);
        /*
         * this.worldObjects.add(rack1); this.worldObjects.add(rack2);
         * this.worldObjects.add(rack3); this.worldObjects.add(rack4);
         * this.worldObjects.add(rack5); this.worldObjects.add(rack6);
         */

        // obstacles.add(truck);
        /*
         * obstacles.add(rack1); obstacles.add(rack2); obstacles.add(rack3);
         * obstacles.add(rack4); obstacles.add(rack5); obstacles.add(rack6);
         */
        obstacles.add(robot1);
        obstacles.add(robot2);
        /*
         * racks.add(rack1); racks.add(rack2); racks.add(rack3); racks.add(rack4);
         * racks.add(rack5); racks.add(rack6);
         */
        robots.add(robot1);
        robots.add(robot2);
    }

    /*
     * Deze methode wordt gebruikt om de wereld te updaten. Wanneer deze methode
     * wordt aangeroepen, wordt op elk object in de wereld de methode update
     * aangeroepen. Wanneer deze true teruggeeft betekent dit dat het onderdeel
     * daadwerkelijk geupdate is (er is iets veranderd, zoals een positie). Als dit
     * zo is moet dit worden geupdate, en wordt er via het pcs systeem een
     * notificatie gestuurd naar de controller die luisterd. Wanneer de
     * updatemethode van het onderdeel false teruggeeft, is het onderdeel niet
     * veranderd en hoeft er dus ook geen signaal naar de controller verstuurd te
     * worden.
     */

    @Override
    public void update() {
        // If Truck exists
        if (truck != null) {
            // If the Truck's item list reaches 0
            if (truck.getItems() == null || truck.getItems().size() == 0) {
                truck.setIsEmptying(false);
                truck.setIsRefilling(true);
            }
            // If Truck has left
            if (truck.hasLeft()) {
                // Remove Truck
                this.truck = null;
            }
        }
        // Else create Truck
        else {
            this.truck = new Truck(categories, truckSize[0], truckSize[1], truckSize[2], 17, 0, -1, 0, 0, 0);
            obstacles.add(this.truck);
            this.worldObjects.add(this.truck);
        }

        for (Object3D object : this.worldObjects) {
            if (object instanceof Updatable) {
                if (((Updatable) object).update()) {
                    pcs.firePropertyChange(Model.UPDATE_COMMAND, null, new ProxyObject3D(object));
                }
            }
        }
    }

    /**
     * Adds racks in the world z = z from bottomcorner of plane x = x from
     * bottomcorner of plane zracks = total racks on z axis xracks = total racks on
     * x axis counter is for getting a hallway after 3 rows of racks
     * 
     */
    public void addRacks() {
        Random random = new Random();
        int categoryIndex = 0;
        int randomIndex = 0;
        int prevIndex = 0;
        double obstacleX = 0.0;
        double obstacleZ = 0.0;
        double xracks = 5;
        double zracks = 6;
        double x = -12.4;
        for (int o = 0; o < xracks; o++) {
            double z = -11.4;
            int counterz = 0;
            for (int i = 0; i < zracks; i++) {
                if (x <= 12.4 && z <= 11.4) {
                    Rack rack1 = new Rack(rackSize[0], rackSize[1], rackSize[2], x, 0, z, 0, 0, 0);
                    // Rack rack2 = new Rack(rackSize[0], rackSize[1], rackSize[2], x + 1.5, 0, z,
                    // 0, 0, 0);
                    racks.add(rack1);
                    this.worldObjects.add(rack1);
                    // racks.add(rack2);
                    // this.worldObjects.add(rack2);
                    // obstacles.add(rack2);
                    if (categoryIndex > categories.length - 1) {
                        // Random
                        if (prevIndex != categoryIndex || prevIndex == 0) {
                            prevIndex = categoryIndex;
                            randomIndex = random.nextInt(categories.length - 1);
                        }
                        rack1.setCategory(categories[randomIndex][0]);
                    } else {
                        rack1.setCategory(categories[categoryIndex][0]);
                    }
                }
                counterz++;
                if (counterz == 2) {
                    // Is middle
                    obstacleX = x;
                    obstacleZ = z;
                }
                if (counterz == 3) {
                    Obstacle obstacle = new Obstacle(rackSize[0], rackSize[1], rackSize[2] * 3, obstacleX, 0, obstacleZ,
                            0, 0, 0);
                    obstacles.add(obstacle);
                    this.worldObjects.add(obstacle);
                    z += 7;
                    counterz = 0;
                    categoryIndex++;
                } else {
                    z += 3.5;
                }
            }
            x += 5;
            // x <= topcornerx && z <= topcornerz
        }
    }

    /*
     * Standaardfunctionaliteit. Hoeft niet gewijzigd te worden.
     */
    @Override
    public void addObserver(PropertyChangeListener pcl) {
        pcs.addPropertyChangeListener(pcl);
    }

    /*
     * Deze methode geeft een lijst terug van alle objecten in de wereld. De lijst
     * is echter wel van ProxyObject3D objecten, voor de veiligheid. Zo kan de
     * informatie wel worden gedeeld, maar kan er niks aangepast worden.
     */
    @Override
    public List<Object3D> getWorldObjectsAsList() {
        ArrayList<Object3D> returnList = new ArrayList<>();

        for (Object3D object : this.worldObjects) {
            returnList.add(new ProxyObject3D(object));
        }

        return returnList;
    }

    public void addObstacle(Obstacle obstacle) {
        obstacles.add(obstacle);
    }

    public void removeObstacle(Obstacle obstacle) {
        obstacles.remove(obstacle);
    }

    public List<Obstacle> getObstacles() {
        return this.obstacles;
    }

    public Truck getTruck() {
        return this.truck;
    }

    public List<Rack> getRacks() {
        return this.racks;
    }

    public List<Robot> getRobots() {
        return this.robots;
    }
}