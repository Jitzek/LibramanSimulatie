package com.nhlstenden.amazonsimulatie.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.nhlstenden.amazonsimulatie.base.Command;
import com.nhlstenden.amazonsimulatie.models.Object3D;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/*
 * Deze class is de standaard websocketview. De class is een andere variant
 * van een gewone view. Een "normale" view is meestal een schermpje op de PC,
 * maar in dit geval is het wat de gebruiker ziet in de browser. Het behandelen
 * van een webpagina als view zie je vaker wanneer je te maken hebt met
 * serversystemen. In deze class wordt de WebSocketSession van de client opgeslagen,
 * waarmee de view class kan communiceren met de browser.
 */
public class DefaultWebSocketView implements View {
    private WebSocketSession session;
    private Command onClose;
    private String action;

    private List<String> robotuuids = new ArrayList<>();
    private List<double[]> robotxz = new ArrayList<>();
    private List<double[]> oldrobotxz = new ArrayList<>();

    public DefaultWebSocketView(WebSocketSession session) {
        this.session = session;
    }

    /*
     * Deze methode wordt aangroepen vanuit de controller wanneer er een update voor
     * de views is. Op elke view wordt dan de update methode aangroepen, welke een
     * JSON pakketje maakt van de informatie die verstuurd moet worden. Deze JSON
     * wordt naar de browser verstuurd, welke de informatie weer afhandeld.
     */
    @Override
    public void update(String event, Object3D data) {
        synchronized (session) {
            try {

                if (data.getType().toString().trim().equals("robot")) {
                    if (!robotuuids.contains(data.getUUID())) {
                        robotuuids.add(data.getUUID());
                        robotxz.add(new double[] { data.getX(), data.getZ() });
                        oldrobotxz.add(new double[] { data.getX(), data.getZ() });
                    }
                    int index = 0;
                    for (String uuid : robotuuids) {
                        if (data.getUUID().equals(uuid)) {
                            robotxz.get(index)[0] = data.getX();
                            robotxz.get(index)[1] = data.getZ();
                            double positionx = robotxz.get(index)[0];
                            double oldpositionx = oldrobotxz.get(index)[0];
                            double positionz = robotxz.get(index)[1];
                            double oldpositionz = oldrobotxz.get(index)[1];
                            if ((positionx - oldpositionx) < 0.0) {
                                action = "x-";
                                oldpositionx = positionx;
                            } else if ((positionx - oldpositionx) > 0.0) {
                                action = "x+";
                                oldpositionx = positionx;
                            } else if ((positionz - oldpositionz) < 0.0) {
                                action = "z-";
                                oldpositionz = positionz;
                            } else if ((positionz - oldpositionz) > 0.0) {
                                action = "z+";
                                oldpositionz = positionz;
                            }
                            oldrobotxz.get(index)[0] = positionx;
                            oldrobotxz.get(index)[1] = positionz;
                            break;
                        }
                        index++;
                    }

                }
                if (this.session.isOpen()) {
                    this.session
                            .sendMessage(new TextMessage("{" + surroundString("command") + ": " + surroundString(event)
                                    + "," + surroundString("parameters") + ": " + jsonifyObject3D(data) + "}"));
                } else {
                    this.onClose.execute();
                }

            } catch (IOException e) {
                this.onClose.execute();
            }
        }
    }

    @Override
    public void onViewClose(Command command) {
        onClose = command;
    }

    /*
     * Deze methode maakt van een Object3D object een JSON pakketje om verstuurd te
     * worden naar de client.
     */
    private String jsonifyObject3D(Object3D object) {
        if (object.getType().toString().trim().equals("robot")) {
            return "{" + surroundString("uuid") + ":" + surroundString(object.getUUID()) + "," + surroundString("type")
                    + ":" + surroundString(object.getType()) + "," + surroundString("x") + ":" + object.getX() + ","
                    + surroundString("y") + ":" + object.getY() + "," + surroundString("z") + ":" + object.getZ() + ","
                    + surroundString("rotationX") + ":" + object.getRotationX() + "," + surroundString("rotationY")
                    + ":" + object.getRotationY() + "," + surroundString("rotationZ") + ":" + object.getRotationZ()
                    + "," + surroundString("sizeX") + ":" + object.getSizeX() + "," + surroundString("sizeY") + ":"
                    + object.getSizeY() + "," + surroundString("sizeZ") + ":" + object.getSizeZ() + ","
                    + surroundString("action") + ":" + surroundString(action) + "}";
        }
        return "{" + surroundString("uuid") + ":" + surroundString(object.getUUID()) + "," + surroundString("type")
                + ":" + surroundString(object.getType()) + "," + surroundString("x") + ":" + object.getX() + ","
                + surroundString("y") + ":" + object.getY() + "," + surroundString("z") + ":" + object.getZ() + ","
                + surroundString("rotationX") + ":" + object.getRotationX() + "," + surroundString("rotationY") + ":"
                + object.getRotationY() + "," + surroundString("rotationZ") + ":" + object.getRotationZ() + ","
                + surroundString("sizeX") + ":" + object.getSizeX() + "," + surroundString("sizeY") + ":"
                + object.getSizeY() + "," + surroundString("sizeZ") + ":" + object.getSizeZ() + "}";
    }

    private String surroundString(String s) {
        return "\"" + s + "\"";
    }
}