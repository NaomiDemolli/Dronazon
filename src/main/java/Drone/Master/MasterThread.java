package Drone.Master;

import Delivery.Delivery;
import Delivery.DeliveryQueue;
import Drone.Drone;
import Drone.Statistiche.Report;
import Drone.Statistiche.ReportQueue;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;

import java.util.ArrayList;

public class MasterThread extends Thread{

    private Drone masterDrone;
    private DeliveryQueue deliveryQueue;
    private ReportQueue reports;

    private int deliveryCounter;
    private int responseCounter;
    private int reportCounter;
    private int reElectionCounter;

    // Lock
    private Object lockChoosingDrone;
    private Object delCounterLock;
    private Object responseCounterLock;
    private Object reportCounterLock;
    private Object reElectionCounterLock;

    // Servizi
    private DeliveryMonitorThread monitorMqtt;
    private DeliveryManageThread manageDelivery;
    private ReportMonitorThread reportMonitorThread;

    public MasterThread(Drone masterDrone){
        this.deliveryQueue = new DeliveryQueue();
        this.masterDrone = masterDrone;
        this.deliveryCounter = 0;
        this.responseCounter = 0;
        this.reports = new ReportQueue();
        this.reportCounter = 0;
        this.reElectionCounter = 0;

        this.lockChoosingDrone = new Object();
        this.delCounterLock = new Object();
        this.responseCounterLock = new Object();
        this.reportCounterLock = new Object();
        this.reElectionCounterLock = new Object();

        this.monitorMqtt = new DeliveryMonitorThread(this, deliveryQueue);
        this.manageDelivery = new DeliveryManageThread(masterDrone, this, deliveryQueue);
        this.reportMonitorThread = new ReportMonitorThread(masterDrone, this);
    }

    @Override
    public void run() {
        try {
            getInfo();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        monitorMqtt.start(); // subscriber mqtt che rimpie queue
        manageDelivery.start(); // thread che prende da queue e crea un assignthread per affidare la consegna
        reportMonitorThread.start();
    }

    private void getInfo() throws InterruptedException {

        ArrayList<GettingInfoThread> threads = new ArrayList<>();

        for (Drone deliveryDrone : masterDrone.getDronesListCopy()) {
            GettingInfoThread t = new GettingInfoThread(masterDrone, this, deliveryDrone);
            threads.add(t);
            t.start();
        }
        for(GettingInfoThread t: threads){
            t.join();
        }

    }

    public ArrayList<Drone> getDroneListCopy(){
        return masterDrone.getDronesListCopy();
    }

    public Drone chooseDeliveryDrone(Delivery delivery) {
        synchronized (lockChoosingDrone) {
            ArrayList<Drone> dronesList = masterDrone.getDronesListCopy();
            Utils.printDetail("[MASTER THREAD] lista droni disponibili: \n"+ masterDrone.getDronesListCopy(), 2);
            Drone retDrone = null;
            double minDistance = Config.MAXIMUM_DISTANCE +1;
            double droneDistance;

            for (Drone drone : dronesList) {
                if (drone.isAvailable() && drone.getBattery() > 15 && !drone.isInCharge()) {
                    droneDistance = Utils.distance(drone.getPosition(), delivery.getFromPosition());
                    if (droneDistance < minDistance) {
                        minDistance = droneDistance;
                        retDrone = drone;
                    } else if (droneDistance == minDistance) {
                        if (drone.getBattery() > retDrone.getBattery()) {
                            retDrone = drone;
                        } else if (drone.getBattery() == retDrone.getBattery()) {
                            retDrone = (drone.id > retDrone.id) ? drone : retDrone;
                        }
                    }
                }
            }

            if (retDrone != null) {
                retDrone.setAvailable(false);
            }
            return retDrone;
        }
    }

    public void setAvailableUpdateDrone(Drone drone, int[] newpos, int newbattery){
        // Delivery assign sull'OnNext del drone che ha finito la consegna
         synchronized (lockChoosingDrone){
             drone.decreaseBattery(newbattery);
             if(drone.getBattery()<15){
                 drone.setPosition(newpos);
                 drone.setAvailable(false);
             }else{
                 drone.setPosition(newpos);
                 drone.setAvailable(true);
             }
        }

    }

    public void removeDrone(Drone drone){
        masterDrone.removeDrone(drone);
    }

    // Il master in uscita conta se ha ricevuto tutte le risposte
    public void increaseDeliveryCounter(){
        // Delivery Assign sull'onError in consegna rimetta la consegna in coda
        synchronized (delCounterLock){
            deliveryCounter++;
        }
    }
    public void increaseNotifyResponseCounter(){
        // Delivery Assign conta le risposte ricevute
        synchronized (responseCounterLock){
            responseCounter++;
            responseCounterLock.notify();
        }
    }
    public void increaseReportCounter(int reports){
        synchronized (reportCounterLock){
            reportCounter += reports;
            reportCounterLock.notify();
        }
    }

    // DeliveryAssign aggiungono report alla lista, ReportMonitor prende i report, calcola la statistica e la manda al REST
    public void addReport(Report report){
        reports.put(report);
    }
    public ArrayList<Report> getAllAndCleanReports(){
        ArrayList<Report> ret = new ArrayList<>(reports.readAllAndClean());
        return ret;
    }

    public int getReElectionCounter(){
        synchronized (reElectionCounterLock){
            return reElectionCounter;
        }
    }
    public void increaseReElectionCounter(){
        synchronized (reElectionCounterLock){
            reElectionCounter++;
        }
    }

    public void quitOnlyMaster(){
        monitorMqtt.quit();
        reportMonitorThread.quit();
        manageDelivery.quit();
    }

    public void quit() throws InterruptedException {

        monitorMqtt.quit();

        // assegna consegne pendenti ai droni
        while (deliveryCounter != responseCounter) {
            synchronized (responseCounterLock) {
                Utils.printDetail("[MASTER THREAD] il master sta aspettando delle risposte ... \n", 1);
                responseCounterLock.wait();
            }
        }

        // aspetta di mandare tutti i report al server REST
        while(reportCounter != deliveryCounter) {
            synchronized (reportCounterLock) {
                Utils.printDetail("[MASTER THREAD] il master sta consegnando tutti i report ... \n", 1);
                reportCounterLock.wait();
            }
        }

        Utils.printDetail("[MASTER THREAD QUIT] counter delle risposte ricevute: " + responseCounter, 2);
        Utils.printDetail("[MASTER THREAD QUIT] counter delle consegne: " + deliveryCounter, 2);
        Utils.printDetail("[MASTER THREAD QUIT] counter dei report: " + reportCounter, 2);

        masterDrone.quitGrpcServer();
        manageDelivery.quit();
        reportMonitorThread.quit();
        Utils.notifyDeleteREST(masterDrone);
        masterDrone.quitCmdQuit();
        masterDrone.quitPingThread();
        System.exit(0);
    }
}
