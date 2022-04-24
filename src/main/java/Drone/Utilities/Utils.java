package Drone.Utilities;

import Drone.Drone;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {


    public static void printDetail(String s, int detail) {
        if(Config.ERROR_DETAIL >= detail) {
            System.out.println(s);
        }
    }

    public static void printContext(String s, char context){

        if(Config.ERROR_CONTEXT == context){
            System.out.println(s);
        }
    }

    public static double distance(int[] DronePosition, int[] DeliveryPosition){
        int x1 = DronePosition[0];
        int y1 = DronePosition[1];
        int x2 = DeliveryPosition[0];
        int y2 = DeliveryPosition[1];

        return Math.sqrt(Math.pow((x2-x1), 2) + Math.pow((y2-y1), 2));
    }

    public static void notifyDeleteREST(Drone drone){
        Utils.printDetail("Rimozione drone dal server REST\n", 1);

        Client client = Client.create();
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/droneadmin/delete/" + drone.id);
        ClientResponse response = webResource.type(MediaType.APPLICATION_JSON_TYPE).delete(ClientResponse.class);

        if(response.getStatus() != 200) {
            System.out.println(response.getStatus());
            Utils.printDetail("[ASSIGN THREAD REST] qualcosa è andato storto", 1);
        }
    }

    public static Timestamp convertStringToTimestamp(String str_date, String pattern) {
        // converte la stringa (HH:MM:SS) in 1970:01:01 HH:MM:SS
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            Date date = formatter.parse(str_date);
            java.sql.Timestamp timeStampDate = new Timestamp(date.getTime());
            return timeStampDate;

        } catch (ParseException e) {
            e.printStackTrace();
            return  null;
        }
    }




}
