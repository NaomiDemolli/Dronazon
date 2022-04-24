package Drone.Master;

import Drone.Drone;
import Drone.Statistiche.Report;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;


public class ReportMonitorThread extends Thread{

    private Drone drone;
    private MasterThread masterThread;

    private boolean quit = false;

    private ArrayList<Integer> droneIDs = new ArrayList<>();
    private int totDrones = 0;
    private int totBattery = 0;
    private double totPollution = 0;
    private double totDistance = 0;
    ArrayList<Report> reports;

    public ReportMonitorThread(Drone drone, MasterThread masterThread){
        this.drone = drone;
        this.masterThread = masterThread;
    }

    @Override
    public void run() {

        while(!quit){

            try {waitTime();}
            catch (InterruptedException e) {e.printStackTrace();}

            totDrones = 0;
            totBattery = 0;
            totDistance = 0;
            totPollution = 0;
            droneIDs.clear();

            reports = masterThread.getAllAndCleanReports();

            if(reports.size() != 0) {

                 /* ordinati per timestamp - dal più recente al più datato
                per ogni report salvo livello di inquinamento e distanza percorsa per quella consegna.
                Solo se l'ID del drone è nuovo, salvo l'id in una lista e salvo il livello di batteria perché
                devo salvare il livello di batteria più aggiornato */

                Collections.sort(reports);

                for (Report report : reports) {
                    totPollution += report.getPollution();
                    totDistance += report.getDistance();
                    if (!droneIDs.contains(report.getId())) {
                        droneIDs.add(report.getId());
                        totBattery += report.getBattery();
                    }
                }
                totDrones = droneIDs.size();

                try {
                    sendToRESTserver();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                masterThread.increaseReportCounter(reports.size());

            }else{
                Utils.printDetail("[REPORT MONITOR] nessun report ricevuto \n ",1);
            }
        }
    }

    public synchronized void waitTime() throws InterruptedException {
        wait(Config.SLEEPTIME_MONITOR_REPORT);
    }

    public void sendToRESTserver() throws JSONException {
        Client client = Client.create();
        JSONObject payload = createReportPayload();
        Utils.printDetail("[REPORT MONITOR] il master sta mandando il report " + payload + " al server REST", 2);
        Utils.printDetail("[REPORT MONITOR] il master sta inviando il report al server REST \n", 1);
        WebResource webResource = client.resource(Config.ADMINSERVER_ADDRESS + "/reportsadmin/add");

        try{
            ClientResponse response = webResource.type("application/json").post(ClientResponse.class, payload);

            if(response.getStatus() == 200){
                Utils.printDetail("[REPORT MONITOR] inserimento andato a buon fine", 2);
            }else{
                Utils.printDetail("[REPORT MONITOR] inserimento non riuscito", 1);
            }
        }catch (ClientHandlerException e){
            Utils.printDetail("SERVER NON DISPONIBILE", 1);
        }

    }

    public JSONObject createReportPayload() throws JSONException{
        JSONObject payload = new JSONObject();

        payload.put("id", drone.id);

        double delivery = reports.size()/totDrones;
        payload.put("delivery", (int) delivery);

        double distance = (totDistance/reports.size())*100;
        payload.put("distance", (int) distance);

        double pollution = (totPollution/reports.size())*100;
        payload.put("pollution",(int) pollution);

        double battery = (totBattery/totDrones);
        payload.put("battery", (int) battery);

        long timestamp = timestampHour();
        payload.put("timestamp", (int) timestamp);

        return payload;
    }

    public long timestampHour(){
        Timestamp time = new Timestamp(System.currentTimeMillis());
        String[] parts = time.toString().split(" ");
        Timestamp ts = Utils.convertStringToTimestamp(parts[1].substring(0,8), "HH:mm:ss");
        return ts.getTime();
    }

    public void quit(){
        Utils.printDetail("Interruzione thread per il monitoraggio dei report", 1);
        quit = true;
    }


}
