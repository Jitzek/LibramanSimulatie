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

        /// Creatin Robot
        Robot robot1 = new Robot(this, robotSize[0], robotSize[1], robotSize[2], 9, 0, 0, 0, 0, 0);
        /// --------------

        /// Creating Rack
        Rack rack1 = new Rack("Electronics", scaffoldingSize[0], scaffoldingSize[1], scaffoldingSize[2], 10, 0, 10, 0, 0, 0);
        /// --------------

        this.worldObjects.add(this.truck);
        this.worldObjects.add(robot1);
        this.worldObjects.add(rack1);

        // obstacles.add(truck);
        obstacles.add(rack1);
        obstacles.add(robot1);
        racks.add(rack1);
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
        // If the Truck's item list reaches 0
        if (truck.getItems() == null || truck.getItems().size() == 0) {
            truck.setIsEmptying(false);
            truck.setIsRefilling(true);
        }

        for (Robot robot : robots) {
            // If the Robot has a Truck as target
            if (robot.getTargetTruck() != null && robot.getTargetRack() == null) {

            } 
            // If the Robot has a Rack as target
            else if (robot.getTargetTruck() == null && robot.getTargetRack() != null) {

            }
            // If the Robot doesn't have a target
            else if (robot.getTargetTruck() == null && robot.getTargetRack() == null) {
                // If the Robot has an Item
                if (robot.getItem() != null) {
                    // If the Truck is emptying
                    if (truck.isEmptying()) {
                        // Find a Rack to deliver to
                        for (Rack rack : racks) {
                            // If the Category of the Item corresponds to the Category of the Rack
                            if (robot.getItem().getCategory().equals(rack.getCategory())) {
                                // Deliver the Item to the Rack
                                robot.setTarget(rack);
                            }
                        }
                    }
                    // If the Truck is refilling
                    else if (truck.isRefilling()) {
                        // Get an Item from the Truck
                        robot.setTarget(truck);
                    }
                }
                // If the Robot doesn't have an Item
                else if (robot.getItem() == null) {
                    // If the Truck is emptying
                    if (truck.isEmptying()) {
                        // Get an Item from the Truck
                        robot.setTarget(truck);
                    }
                    // If the Truck is refilling
                    else if (truck.isRefilling()) {
                        // Find a Rack to get an Item from
                        for (Rack rack : racks) {
                            if (robot.getItem().getCategory().equals(rack.getCategory())) {
                                // Get an Item from the Rack
                                robot.setTarget(rack);
                            }
                        }
                    }
                }
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