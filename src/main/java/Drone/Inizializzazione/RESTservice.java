package Drone.Inizializzazione;

import Drone.Drone;
import REST.beans.InitResponse;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;

public class RESTservice {

    protected Drone drone;

    public RESTservice(Drone drone) {
        this.drone = drone;
    }

    public Boolean start() throws JSONException {
        Utils.printDetail(" \n[REST INIT] Il drone " + drone.id + " vorrebbe entrare ...", 1);

        Client client = Client.create();
        JSONObject payload = createInitPayload();
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/droneadmin/add");

        try {
            ClientResponse response = webResource.type("application/json").post(ClientResponse.class, payload);

            switch (response != null ? response.getStatus() :0) {
                case 204:
                    Utils.printDetail("INGRESSO NON RIUSCITO: response dal serverREST = no content", 1);
                    break;
                case 200:
                    manageInitResponse(response.getEntity(InitResponse.class));
                    Utils.printDetail("[REST INIT] Il drone " + drone.id  + " ha settato i parametri", 2);
                    return true;
                case 403:
                    Utils.printDetail("INGRESSO NON CONSENTITO: un drone ha uguale id nella smart city, ritenta", 1);
                    break;
            }

        } catch (ClientHandlerException e) {
            Utils.printDetail("SERVER NON DISPONIBILE", 1);
            return false;
        }
        return false;
    }

    private JSONObject createInitPayload() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("id", drone.id);
        payload.put("ip", drone.getIp());
        payload.put("port", drone.getPort());
        return payload;
    }

    private void manageInitResponse(InitResponse response) throws JSONException {
        int x = response.getPosition()[0];
        int y = response.getPosition()[1];
        drone.setPosition(new int[]{x,y});

        ArrayList<Drone> dronelist = new ArrayList<Drone>();
        JSONArray list = new JSONArray(response.getDrones().getDronesList());

        for(int i=0; i<list.length(); i++) {
            REST.beans.Drone dronec = (REST.beans.Drone) list.get(i);
            Drone d = new Drone(dronec.getId(), dronec.getIp(), dronec.getPort());

            if (drone.id != d.id) {
                dronelist.add(d);
            }
        }

        if(dronelist.size() == 0) {
            drone.setMaster(true);
        }

        drone.setDronesList(dronelist);
    }

}
