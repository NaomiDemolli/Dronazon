package Drone.Consegna;

import Drone.Drone;
import Drone.Sensori.Measurement;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import com.example.consegna.ConsegnaOuterClass.*;

import java.sql.Timestamp;
import java.util.List;

public class ConsegnaThread extends Thread{

    private Drone deliveryDrone;
    private InfoDelivery infoDelivery;
    private Statistics statistics;
    private int newBattery;
    private double distance;
    private int[] newpos = new int[2];

    public ConsegnaThread(Drone deliveryDrone, InfoDelivery infoDelivery, Statistics stats) {
        this.deliveryDrone = deliveryDrone;
        this.infoDelivery = infoDelivery;
        this.statistics = stats;
    }

    @Override
    public void run() {
        delivery();
    }

    public void delivery(){
        try {
            Thread.sleep(5000);

            // punto di ritiro
            int x1 = infoDelivery.getFromPosition(0);
            int y1 = infoDelivery.getFromPosition(1);
            int[] frompos = {x1, y1};

            // punto di consegno
            int x2 = infoDelivery.getToPosition(0);
            int y2 = infoDelivery.getToPosition(1);
            newpos[0] = x2;
            newpos[1] = y2;

            // distanza punto di partenza/punto di ritiro/punto di consegna
            distance = Utils.distance(deliveryDrone.getPosition(), frompos);
            distance += Utils.distance(frompos, newpos);

            // int battery = deliveryDrone.getBattery() - Config.DECREASE_BATTERY;
            newBattery = deliveryDrone.getBattery() - Config.DECREASE_BATTERY;
            long datetime = System.currentTimeMillis();

            // calcola il drone la media sulle sue misurazioni e manda solo quella al master
            double totPollution = 0;
            List<Measurement> measurements = deliveryDrone.getPollutionMeasure();
            for(Measurement m: measurements){
                totPollution += m.getValue();
            }

            statistics = Statistics.newBuilder()
                    .setTimestamp(datetime)
                    .setBattery(newBattery)
                    .addNewPosition(newpos[0])
                    .addNewPosition(newpos[1])
                    .setDistance(distance)
                    .setAvgPollution(totPollution/measurements.size())
                    .setTimestamp(new Timestamp(datetime).getTime())
                    .build();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public Statistics getStat(){
        return statistics;
    }

    public int getNewBattery(){
        return newBattery;
    }

    public double getDistance(){
        return distance;
    }

    public int getPosition(int index){ return newpos[index];}
}
