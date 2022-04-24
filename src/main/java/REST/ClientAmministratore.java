package REST;

import REST.beans.Drones;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ClientAmministratore {
    public static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException, JSONException {

        print();

        int cmd = 0;
        boolean exit = false;

        while (!exit){
            try{
                cmd = scanner.nextInt();

                switch (cmd) {
                    case 1: getDrones(); break;

                    case 2: getRecentReports(); break;

                    case 3: getReports(); break;

                    case 4: getDelivery(); break;

                    case 5: getKilo(); break;

                    case 6: exit = true;

                    default:
                        Utils.printDetail("Inserire un comando", 1);
                }

            }catch (NumberFormatException n){
                Utils.printDetail("E' necessario specificare un numero come secondo parametro", 1);
            }catch (InputMismatchException e) {
               // TODO cicla all'infinito
                Utils.printDetail("Comando non riconosciuto", 1);
                cmd = 0;
            }


        }
    }

    public static void print(){

        Utils.printDetail(" \n COMANDI:\n" +
                " - 1) per elenco droni nella rete \n" +
                " - 2) per ultime n statistiche globali \n" +
                " - 3) per tutte le statistiche globali \n" +
                " - 4) per numero medio di consegne tra t1-t2 \n" +
                " - 5) per numero medio kilometri tra t1-t2 \n" +
                " - 6) per uscire ",1);
    }

    public static void getDrones() {
        Client client = Client.create();
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/droneadmin");
        ClientResponse response = webResource.type("application/json").get(ClientResponse.class);

        Drones drones = response.getEntity(Drones.class);

        if(drones.size() == 0){
            Utils.printDetail("Non sono presenti droni nella smart city", 1);
        }

        for(int i=0; i<drones.size(); i++){
            System.out.println("- " + drones.get(i));
        }
    }

    public static void getReports() throws JSONException {
        Client client = Client.create();
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/reportsadmin");
        ClientResponse response = webResource.type("application/json").get(ClientResponse.class);

        JSONObject object = new JSONObject(response.getEntity(String.class));
        JSONArray array = object.getJSONArray("reports_list");

        if(array.length() == 0 ){
            Utils.printDetail("Non sono ancora presenti report", 1);
        }

        for(int i=0; i<array.length();i++){
            JSONObject obj = array.getJSONObject(i);
            double distance = (double) obj.get("distance");
            double pollution = (double) obj.get("pollution");
            int time = (int) obj.get("timestamp");
            DateFormat targetFormat = new SimpleDateFormat("HH:mm:ss");
            String formatted = targetFormat.format(time);

            Utils.printDetail((i+1) + ") report: distance " + distance/100 + "km, pollution " + pollution/100
                    + ", battery " + obj.get("battery") + ", delivery " + obj.get("delivery")
                    + ", timestamp " + formatted ,1);
        }
    }

    public static void getRecentReports() throws JSONException {

        Utils.printDetail("Inserire il numero di report desiderati", 1);
        int n = scanner.nextInt();

        Client client = Client.create();
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/reportsadmin");
        ClientResponse response = webResource.type("application/json").get(ClientResponse.class);

        JSONObject object = new JSONObject(response.getEntity(String.class));
        JSONArray array = object.getJSONArray("reports_list");

        if(array.length() == 0){
            Utils.printDetail("Non sono ancora presenti report", 1);
        }else{
            int count = 1;
            for(int i=array.length()-1; i>= array.length()-n; i--){
                JSONObject obj = array.getJSONObject(i);
                double distance = (double) obj.get("distance");
                double pollution = (double) obj.get("pollution");
                int time = (int) obj.get("timestamp");
                DateFormat targetFormat = new SimpleDateFormat("HH:mm:ss");
                String formatted = targetFormat.format(time);

                Utils.printDetail((count) + ") report: distance " + distance/100 + "km, pollution " + pollution/100
                        + ", battery " + obj.get("battery") + ", delivery " + obj.get("delivery")
                        + ", timestamp " + formatted,1);
                count++;
            }
        }


    }

    public static void getDelivery(){

        scanner.nextLine(); // prende \n non preso da nextInt
        Utils.printDetail("Inserire i timestamp HH:MM:SS", 1);

        String stringTimestamp1 = scanner.nextLine();
        Timestamp t1 = Utils.convertStringToTimestamp(stringTimestamp1,"HH:mm:ss");

        String  stringTimestamp2 = scanner.nextLine();
        Timestamp t2 = Utils.convertStringToTimestamp(stringTimestamp2,"HH:mm:ss");

        while(t2.before(t1)){
            Utils.printDetail("dev'essere posteriore al primo timestamp", 1);
            stringTimestamp2 = scanner.nextLine();
            t2 = Utils.convertStringToTimestamp(stringTimestamp2,"HH:mm:ss");
        }

        int ts1 = (int) t1.getTime();
        int ts2 = (int) t2.getTime();

        Client client = Client.create();
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/reportsadmin/delivery/" + ts1 + "-" + ts2);
        ClientResponse response = webResource.type("application/json").get(ClientResponse.class);

        String del = response.getEntity(String.class);
        double numDelivery = Double.parseDouble(del);

        if(numDelivery == 0){
            Utils.printDetail("Non sono presenti report nell'intervallo specificato", 1);
        }else{
            Utils.printDetail("Numero medio di consegne effettuate da ogni drone nell'intervallo: " + numDelivery, 1);
        }

    }

    public static void getKilo(){

        scanner.nextLine(); // prende \n non preso da nextInt
        Utils.printDetail("Inserire i timestamp HH:MM:SS", 1);

        String  stringTimestamp1 = scanner.nextLine();
        Timestamp t1 = Utils.convertStringToTimestamp(stringTimestamp1,"HH:mm:ss");

        String  stringTimestamp4 = scanner.nextLine();
        Timestamp t2 = Utils.convertStringToTimestamp(stringTimestamp4,"HH:mm:ss");

        while(t2.before(t1)){
            Utils.printDetail("dev'essere posteriore al primo timestamp", 1);
            stringTimestamp4 = scanner.nextLine();
            t2 = Utils.convertStringToTimestamp(stringTimestamp4,"HH:mm:ss");
        }

        int ts1 = (int) t1.getTime();
        int ts2 = (int) t2.getTime();

        Client client = Client.create();
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/reportsadmin/kilometers/" + ts1 + "-" + ts2);
        ClientResponse response = webResource.type("application/json").get(ClientResponse.class);

        String kilo = response.getEntity(String.class);
        double numKilo = Double.parseDouble(kilo);

        if(numKilo == 0){
            Utils.printDetail("Non sono presenti report nell'intervallo specificato", 1);
        }else{
            Utils.printDetail("Numero medio di kilometri percorsi da ogni drone nell'intervallo: " + numKilo/100 + "km", 1);
        }

    }


}
