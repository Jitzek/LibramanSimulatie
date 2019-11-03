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
    // Obstacles can't collide with eachother, removing Objects from this list will also remove their collision functionality
    private List<Obstacle> obstacles;

    private Truck truck;
    private List<Rack> racks;
    private List<Robot> robots;

    Random random = new Random();
    
    // {x, y, z}
    private static double[] truckSize = { 8.7, 5.0, 3.5 };
    private static double[] rackSize = { 1.2, 5.0, 4.0 };
    private static double[] robotSize = { 0.9, 0.3, 0.9 };

    // Coordinates where Objects should be deleted, setting an Object's x, y and z to these coordinates will delete their model
    // Don't forget to change the clients values when altering these
    private static double[] dump = {-1000, -1000, -1000};

    private static String[] Electronica = { "Electronica", "Vlex On Virtual Assistant™",
            "Vlex On Privacy Concerning Home Security™", "Micromax MucBuk Prü",
            "Hello Kitty Professional Dj Headphones", "Vlex On GetLost™", "OnePlus Tea" };
    private static String[] Movies = { "Movies", "Birdemic IMAX 3D", "Airplane Mode", "Shrek 3",
            "Bee Movie but every time they say \"bee\" the movie gets 300% faster",
            "Monthy Python and The Holy Grail" };
    private static String[] PetSupplies = { "Pet Supplies", "Spiked Hamster Ball", "Chocolate Dog Treats",
            "Chinese Cook Book" };
    private static String[] Baby = { "Baby", "Electric Toilet Seat for Potty Training (Belts & Batteries included)",
            "Meth", "Glock 26 Gen 4 (9MM)" };
    private static String[] VideoGames = { "Video Games", "Hello Kitty Island Adventure GOTY",
            "Fallout 76 (Math.abs(-99)% OFF)", "Soulja Boy Game Console", "Garfield Go Kart" };
    private static String[] Books = { "Books", "Hello Kitty Island Adventure Complete Lore",
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

        addRacks();

        // Filling Racks
        for (Rack rack : racks) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i][0].equals(rack.getCategory())) {
                    double x = rack.getX();
                    double y = rack.getY();
                    double z = rack.getZ();
                    int count = 0;
                    double resetz = z;
                    int indexCount = 0;
                    for (int j = random.nextInt(3); j < 3; j++) {
                        String product = categories[i][random.nextInt(categories[i].length - 1) + 1];
                        Item item = new Item(categories[i][0], product, indexCount);
                        for (int k = indexCount; k < rack.getIndexes().length; k++) {
                            if (rack.getIndexes()[k] == 0) {
                                item.setX(x);
                                item.setY(y);
                                item.setZ(z + 0.8);
                                rack.addItem(item, indexCount);
                                item.setRack(rack);
                                worldObjects.add(item);
                                break;
                            } else {
                                z -= 1;
                                count++;
                                if (count == 3) {
                                    count = 0;
                                    y += 0.9;
                                    z = resetz;
                                }
                                indexCount++;
                            }
                        }
                        z -= 1;
                        count++;
                        if (count == 3) {
                            count = 0;
                            y += 0.9;
                            z = resetz;
                        }
                        indexCount++;
                    }
                }
            }
        }

        double[] idleStation1_coordinates = new double[3];
        idleStation1_coordinates[0] = 12;
        idleStation1_coordinates[1] = 0;
        idleStation1_coordinates[2] = 14;
        double[] idleStation2_coordinates = new double[3];
        idleStation2_coordinates[0] = 12;
        idleStation2_coordinates[1] = 0;
        idleStation2_coordinates[2] = -14;
        double[] idleStation3_coordinates = new double[3];
        idleStation3_coordinates[0] = 0;
        idleStation3_coordinates[1] = 0;
        idleStation3_coordinates[2] = 14;

        Robot robot1 = new Robot(this, idleStation1_coordinates, robotSize[0], robotSize[1], robotSize[2], 12, 0, 12, 0,
                0, 0);
        Robot robot2 = new Robot(this, idleStation2_coordinates, robotSize[0], robotSize[1], robotSize[2], 12, 0, -12,
                0, 0, 0);
        Robot robot3 = new Robot(this, idleStation3_coordinates, robotSize[0], robotSize[1], robotSize[2], 0, 0, -12,
                0, 0, 0);
        
        this.worldObjects.add(robot1);
        obstacles.add(robot1);
        robots.add(robot1);

        this.worldObjects.add(robot2);
        obstacles.add(robot2);
        robots.add(robot2);

        // Removing the comments will add a third Robot to the simulation
        // Although not thoroughly tested it should hold up
        /*this.worldObjects.add(robot3);
        obstacles.add(robot3);
        robots.add(robot3);*/

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
            if (truck.isEmptying() && (truck.getItems() == null || truck.getItems().size() == 0)) {
                truck.setIsEmptying(false);
                truck.setIsRefilling(true);
            }
            // If Truck has left
            if (this.truck.hasLeft()) {
                this.truck.setX(dump[0]);
                this.truck.setY(dump[1]);
                this.truck.setZ(dump[2]);
                for (Item item : this.truck.getItems()) {
                    item.setX(dump[0]);
                    item.setY(dump[1]);
                    item.setZ(dump[2]);
                }
                obstacles.remove(this.truck);
                this.truck = null;
            }
        }
        // Else create Truck
        else {
            this.truck = new Truck(truckSize[0], truckSize[1], truckSize[2], 18, -0.15, -0.25, 0, 0, 0);
            List<Item> truckItemList = this.truck.createRandomItemList(categories);
            for (Item item : truckItemList) {
                item.setX(this.truck.getX());
                item.setY(this.truck.getY());
                item.setZ(this.truck.getZ());
                this.truck.addItem(item);
                worldObjects.add(item);
            }
            for (Rack rack : racks) {
                if (this.truck.getRequiredItems().size() >= this.truck.getItemListSize()) {
                    break;
                }
                if (rack.getItems().size() > 0) {
                    for (Item item : rack.getItems()) {
                        if (random.nextInt(3) == 1) {
                            this.truck.addRequiredItem(item);
                            this.worldObjects.add(item);
                            break;
                        }
                    }
                }
            }
            if (!(truck.getRequiredItems().size() > 2)) {
                int count = 0;
                for (Rack rack : racks) {
                    if (count == 2) {
                        break;
                    }
                    if (rack.getItems().size() > 0) {
                        for (Item item : rack.getItems()) {
                            boolean exists = false;
                            for (Item item1 : this.truck.getRequiredItems()) {
                                if (item1 == item) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                this.truck.addRequiredItem(item);
                                break;
                            }
                            
                        }
                    }
                    count++;
                }
            }
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
                    racks.add(rack1);
                    this.worldObjects.add(rack1);
                    // Assigns a random category to each three Racks (each row of Racks)
                    if (categoryIndex > categories.length - 1) {
                        if (prevIndex != categoryIndex || prevIndex == 0) {
                            prevIndex = categoryIndex;
                            randomIndex = random.nextInt(categories.length - 1);
                        }
                        rack1.setCategory(categories[randomIndex][0]);
                    }
                    // Ensures that each category is used atleast once 
                    else {
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
                    // Gives each three Racks one big collision box (for simplifying obstacle logic)
                    // However this adds the collision for this Rack as the type Obstacle
                    Obstacle obstacle = new Obstacle(rackSize[0], rackSize[1], rackSize[2] * 3, obstacleX, 0, obstacleZ,
                            0, 0, 0);
                    obstacles.add(obstacle);
                    this.worldObjects.add(obstacle);
                    z += 8.5;
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