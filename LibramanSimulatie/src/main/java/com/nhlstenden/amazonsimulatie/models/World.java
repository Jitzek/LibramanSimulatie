package com.nhlstenden.amazonsimulatie.models;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

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

    private static double[] truckSize = { 2.0, 2.5, 2.0 };
    private static double[] scaffoldingSize = { 4.0, 2.0, 8.0 };
    private static double[] robotSize = { 0.9, 0.3, 0.9 };
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
        List<Item> requiredItems = new ArrayList<>();
        requiredItems.add(new Item("Electronics", "Microwave"));
        requiredItems.add(new Item("Pet Supplies", "Bob's Hamster Cocaine"));

        this.truck = new Truck(requiredItems, truckSize[0], truckSize[1], truckSize[2], 17, 0, 32, 0, 0, 0);
        truck.addItem(new Item("Electronics", "Laptop"));
        truck.addItem(new Item("Video Games", "Hello Kitty Island Adventure"));
        /// ---------------
        Rack rack1 = new Rack("Electronics", scaffoldingSize[0], scaffoldingSize[1], scaffoldingSize[2], 10, 0, 10, 0,
                0, 0);
        Rack rack2 = new Rack("Books", scaffoldingSize[0], scaffoldingSize[1], scaffoldingSize[2], 17, 0, 10, 0, 0, 0);
        Rack rack3 = new Rack("Video Games", scaffoldingSize[0], scaffoldingSize[1], scaffoldingSize[2], 24, 0, 10, 0,
                0, 0);
        Rack rack4 = new Rack("Sports", scaffoldingSize[0], scaffoldingSize[1], scaffoldingSize[2], 10, 0, 22, 0, 0, 0);
        Rack rack5 = new Rack("Fashion", scaffoldingSize[0], scaffoldingSize[1], scaffoldingSize[2], 17, 0, 22, 0, 0,
                0);
        Rack rack6 = new Rack("Pet Supplies", scaffoldingSize[0], scaffoldingSize[1], scaffoldingSize[2], 24, 0, 22, 0,
                0, 0);
        Robot robot1 = new Robot(this, robotSize[0], robotSize[1], robotSize[2], 9, 0, 0, 0, 0, 0);
        this.worldObjects.add(this.truck);
        this.worldObjects.add(robot1);
        this.worldObjects.add(rack1);
        this.worldObjects.add(rack2);
        this.worldObjects.add(rack3);
        this.worldObjects.add(rack4);
        this.worldObjects.add(rack5);
        this.worldObjects.add(rack6);

        // obstacles.add(truck);
        obstacles.add(rack1);
        obstacles.add(rack2);
        obstacles.add(rack3);
        obstacles.add(rack4);
        obstacles.add(rack5);
        obstacles.add(rack6);
        obstacles.add(robot1);
        racks.add(rack1);
        racks.add(rack2);
        racks.add(rack3);
        racks.add(rack4);
        racks.add(rack5);
        racks.add(rack6);
        robots.add(robot1);
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
        if (truck != null) {
            // If the Truck's item list reaches 0
            if (truck.getItems() == null || truck.getItems().size() == 0) {
                truck.setIsEmptying(false);
                truck.setIsRefilling(true);
            }
            // If Truck is done emptying and refilling
            if (truck.isRefilling() && (truck.getRequiredItems() == null || truck.getRequiredItems().size() == 0)) {
                // Remove Truck
                this.truck = null;
            }
        }

        for (Object3D object : this.worldObjects) {
            if (object instanceof Updatable) {
                if (((Updatable) object).update()) {
                    pcs.firePropertyChange(Model.UPDATE_COMMAND, null, new ProxyObject3D(object));
                }
            }
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
}