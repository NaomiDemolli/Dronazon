package Drone;

import Drone.Altro.PingMasterThread;
import Drone.Altro.PrintInfoThread;
import Drone.Altro.TimerDelivery;
import Drone.Elezione.ElectionThread;
import Drone.Master.MasterThread;
import Drone.Ricarica.RechargeMonitorThread;
import Drone.Ricarica.RechargeQueue;
import Drone.Ricarica.RechargeRequest;
import Drone.Sensori.Measurement;
import Drone.Sensori.PollutionSensor;
import Drone.Inizializzazione.GRPCserver;
import Drone.Inizializzazione.InitPresentationThread;
import Drone.Inizializzazione.RESTservice;
import Drone.Altro.CommandLineThread;
import Drone.Uscita.QuittingThread;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import org.codehaus.jettison.json.JSONException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;

public class Drone implements Comparable<Drone> {

    public int id;
    private String ip;
    private int port;
    public int[] position = new int[2];
    private volatile ArrayList<Drone> dronesList;

    // Thread e servizi
    private RESTservice RESTservice;
    private GRPCserver grpcServer;
    private PollutionSensor pollutionSensor;
    private PrintInfoThread printInfo;
    private MasterThread masterThread;
    private CommandLineThread cmdQuitThread;
    private PingMasterThread pingMasterThread;
    private RechargeMonitorThread rechargeThread;

    private TimerDelivery timerDelivery;

    private int battery;
    private Object batteryLock;

    private boolean isMaster;
    private Object masterLock;

    private boolean isAvailable;
    private Object availableLock;

    private boolean isPartecipant;
    private Object partecipantLock;

    private volatile boolean isQuitting;
    private Object isQuittingLock;

    private int numDelivery;
    private Object numDeliveryLock;

    private double numKilo;
    private Object numKiloLock;

    // coordina l'update del drone di ConsegnaImpl e la lettura di PrintInfo
    private Object updateDroneLock;

    private boolean inCharge;
    private Object chargeLock;

    private boolean wannaCharge;
    private Object wannaChargeLock;

    // Recharge
    private RechargeQueue rechargeReqQueue;
    private long rechargeTimestamp;

    public Drone() {}

    public Drone(int id, String ip, int port) {

        this.id = id;
        this.ip = ip;
        this.port = port;
        this.position = new int[2];
        this.rechargeReqQueue = new RechargeQueue();
        this.battery = 100;
        this.isMaster = false;
        this.isAvailable = true;
        this.isQuitting = false;
        this.isPartecipant = false;
        this.numDelivery = 0;
        this.numKilo = 0;
        this.inCharge = false;
        this.wannaCharge = false;
        this.rechargeTimestamp = 0;
        this.timerDelivery = new TimerDelivery(10000, System.currentTimeMillis());

        // Lock
        this.masterLock = new Object();
        this.availableLock = new Object();
        this.batteryLock = new Object();
        this.isQuittingLock = new Object();
        this.partecipantLock = new Object();
        this.numDeliveryLock = new Object();
        this.numKiloLock = new Object();
        this.updateDroneLock = new Object();
        this.chargeLock = new Object();
        this.wannaChargeLock = new Object();

        // Thread e servizi
        this.RESTservice = new RESTservice(this);
        this.grpcServer = new GRPCserver(this);
        this.printInfo = new PrintInfoThread(this);
        this.pollutionSensor = new PollutionSensor(this);
        this.rechargeThread = new RechargeMonitorThread(this);
        this.cmdQuitThread = new CommandLineThread(this, rechargeThread);
        this.pingMasterThread = new PingMasterThread(this);
    }

    public void start() throws JSONException, InterruptedException {

        if(!RESTservice.start()) {
            Utils.printDetail(" \n[REST INIT] ops, qualcosa è andato storto", 1);
            System.exit(0);
        }else{
            // Il servizio REST se restituisce una coda vuota, setta il drone this a master = true
            Drone dronethis = new Drone(this.id, this.getIp(), this.getPort());
            dronethis.setMaster(this.isMaster);
            dronesList.add(dronethis);
        }

        grpcServer.start();
        cmdQuitThread.start();

        if(dronesList.size() != 1) {
            Utils.printDetail("[INITIALIZATION COMPLETED]", 1);
            sayHello();
        }else{
            Utils.printDetail("[INITIALIZATION COMPLETED] è il master\n", 1);
            becomeMaster();
        }

        pollutionSensor.start();
        pingMasterThread.start();

        Timer timer = new Timer();
        timer.schedule(printInfo,0,Config.SLEEPTIME_PRINT_INFO);
    }

    public void sayHello() throws InterruptedException {

        ArrayList<InitPresentationThread> threads = new ArrayList<>();

        for(Drone drone: getDronesListCopy()) {
            if(drone.id != this.id) {
                InitPresentationThread t = new InitPresentationThread(this, drone);
                threads.add(t);
                t.start();
            }
        }

        for(InitPresentationThread t: threads){
            t.join();
        }

        // entra ma tutti gli altri droni sono caduti, diventa master
        if(dronesList.size() == 1){
            setMasterDrone(id);
            Utils.printDetail(" - il drone è da solo, diventa master ", 1);
            becomeMaster();
        }

        try {
            Utils.printDetail(" - Il drone master è: " + getMasterDrone().id + "\n", 1);
        } catch (NullPointerException e) {
            if(!isMaster){
                Utils.printDetail(" - Il drone master non c'è, staranno eleggendo\n", 1);
            }
        }

    }

    public void becomeMaster(){
        setMaster(true);
        Utils.printDetail("[MASTER DRONE] becomeMaster() \n", 3);

        this.masterThread = new MasterThread(this);
        masterThread.start();
    }

    public void setTimerStart(){
        synchronized (timerDelivery){
            timerDelivery.setStart(System.currentTimeMillis());
        }
    }
    public boolean isTimerExpired(){
        synchronized (timerDelivery){
            return timerDelivery.isExpired();
        }
    }

    public ArrayList<Measurement> getPollutionMeasure(){
        return pollutionSensor.getDeliveryPollution();
    }

    public void addDrone(Drone drone){
        synchronized (dronesList){
            // un drone quitta e avvisa il server REST, il server REST fa entrare un nuovo drone con quell'ID
            // un deliveryDrone non sa che il drone è caduto e riceve una richiesta di add da un drone con un ID uguale
            for(Drone d: dronesList){
                if(d.id == drone.id){
                    Utils.printDetail(" - il drone entrato ha lo stesso ID di un altro più vecchio", 1);
                    dronesList.remove(d);
                    break;
                }
            }
            Utils.printDetail("[ADD] drone: " + drone.id + "\n", 1);
            dronesList.add(drone);
            Collections.sort(dronesList);
        }
    }
    public void removeDrone(Drone drone){
        synchronized (dronesList){
            dronesList.removeIf(d -> d.id == drone.id);
            Utils.printDetail("[REMOVE] drone " + drone.id +"\n", 1);
        }
    }

    public Drone getMasterDrone(){
        synchronized (dronesList){
            for(Drone d: dronesList){
                if(d.isMaster){
                    return d;
                }
            }
        }
        return null;
    }
    public void setMasterDrone(int id){
        synchronized (dronesList){
            for(Drone d: dronesList){
                if(d.id == id) {
                    d.setMaster(true);
                    break;
                }

            }
        }
    }
    public void resetMasterDrone(){
        synchronized (dronesList){
            for(Drone drone: dronesList){
                drone.setMaster(false);
            }
        }

    }

    public String getIp() {
        return ip;
    }
    public int getPort() {
        return port;
    }

    public void setPosition(int[] position) {
        synchronized (position) {
            this.position = position;
        }

    }
    public int[] getPosition() {
        int[] pos;
        synchronized (position){
            pos = position;
        }
        return pos;
    }
    public int getPosition(int index){
        int value = 0;
        synchronized (position){
            value = position[index];
        }
        return value;
    }

    public ArrayList<Drone> getDronesListCopy() {
        // Usare solo in lettura, return una copia
        synchronized (dronesList) {
            return new ArrayList<Drone>(dronesList);
        }
    }
    public void setDronesList(ArrayList<Drone> dronesList) {
        synchronized (dronesList){
            this.dronesList = dronesList;
        }
    }

    public boolean isMaster() {
        synchronized (masterLock) {
            return isMaster;
        }
    }
    public void setMaster(boolean master) {
        synchronized (masterLock){
            isMaster = master;
        }
    }

    public boolean isAvailable() {
        synchronized (availableLock) {
            return isAvailable;
        }
    }
    public void setAvailable(boolean bool) {
        synchronized (availableLock){
            isAvailable = bool;
        }
    }
    public void setAvailableNotify(){
        // Consegna Impl - consegna finita il drone torna available notifica
        synchronized (availableLock){
            isAvailable = true;
            availableLock.notifyAll();
        }
    }
    public void waitAvailable(String s) throws InterruptedException {
        // Quittin Thread e RechargeMonitor - aspettano se il drone non è available
        synchronized (availableLock){
            while(!isAvailable){
                Utils.printDetail(s, 1);
                availableLock.wait();
            }
        }
    }

    // Elezione
    public void forwardElectionMessage(String command, int id, int battery) throws InterruptedException {

       /* while(isInCharge()){
            synchronized (chargeLock){
                Utils.printDetail(" - vorrei indire elezione ma sono in carica, aspetto di finire l'elezione", 1);
                chargeLock.wait();
            }
        }*/

        ElectionThread electionThread = new ElectionThread(this, command, id, battery);
        electionThread.start();
    }
    public void setPartecipant(boolean bool){
        synchronized (partecipantLock){
            isPartecipant = bool;
            partecipantLock.notifyAll();
        }
    }
    public void waitNoPartecipant(String s) throws InterruptedException {
        // chiamato da QuittinThread, RicaricaImpl, PingMaster
        synchronized (partecipantLock){
            while(isPartecipant){
                Utils.printDetail(s, 1);
                partecipantLock.wait();
            }
        }
    }
    public boolean isPartecipant(){
        synchronized (partecipantLock){
            return isPartecipant;
        }
    }
    public Drone findNext(){
        // ElectionThraed trova il successore nell'anello al quale mandare il messaggio di elezione
        ArrayList<Drone> drones = getDronesListCopy();
        for(int i=0; i<drones.size();i++){
            if(this.id == drones.get(i).id && i == drones.size()-1){
                return drones.get(0);
            }else if(this.id == drones.get(i).id){
                return drones.get(i+1);
            }
        }
        return null;
    }

    //Ricarica
    public boolean isInCharge(){
        synchronized (chargeLock){
            return inCharge;
        }

    }
    public boolean wannaCharge(){
        synchronized (wannaChargeLock){
            return wannaCharge;
        }

    }
    public void setInCharge(boolean bool){
        synchronized (chargeLock){
            inCharge = bool;
        }
    }
    public void setWannaCharge(boolean bool){
        synchronized (wannaChargeLock){
            wannaCharge = bool;
            if(bool){
                wannaChargeLock.notify();
            }
        }
    }
    public Object getWannaChargeLock(){
        return wannaChargeLock;
    }
    public void addRequest(RechargeRequest req){
        synchronized (rechargeReqQueue){
            rechargeReqQueue.put(req);
        }
    }
    public ArrayList<RechargeRequest> getAllRechargeRequest(){
        synchronized (rechargeReqQueue){
            return rechargeReqQueue.readAllAndClean();
        }
    }
    public long getRechargeTimestamp(){
        return rechargeTimestamp;
    }
    public void setRechargeTimestamp(long time){
        rechargeTimestamp = time;
    }
    public void removePendingOkDrone(int id){
        rechargeThread.removePendingDrone(id);
    }
    public void updateFinishChargeDrone(int id){
        synchronized (dronesList){
            for(Drone d: dronesList){
                if(d.id == id){
                    d.setInCharge(false);
                    d.setAvailable(true);
                    d.setPosition(new int[] {0,0});
                    d.setBattery(100);
                }
            }
        }
    }

    // Quit services
    public void quit() throws InterruptedException {
        QuittingThread quitThread = new QuittingThread(this);
        quitThread.start();
    }
    public void quitGrpcServer() throws InterruptedException {
        this.grpcServer.quit();
    }
    public void quitCmdQuit() {
        cmdQuitThread.quit();
    }
    public void quitMasterThread() throws InterruptedException {
        // sa di essere il master (da election) ma non ha ancora avviato becomemaster (ultimo elected)
        if(masterThread != null){
            masterThread.quit();
        }
    }
    public void quitPingThread(){
        pingMasterThread.quit();
    }
    public void quitRechargeMonitor(){
        rechargeThread.quit();
    }
    public void quitOnlyMaster(){
        masterThread.quitOnlyMaster();
    }

    public int getBattery() {
        synchronized (batteryLock) {
            return battery;
        }
    }
    public int getUpdateBattery() {
        // se il drone è in consegna, prendo batteria - 10, se in carica 100
        if(!isAvailable){
            Utils.printDetail("Drone getUpdateBattery: il drone è in consegna, prendo batteria alla fine", 3);
            return this.battery - Config.DECREASE_BATTERY;
        }else if(inCharge){
            Utils.printDetail("Drone getUpdateBattery: il drone è in carica, prendo batteria alla fine", 3);
            return 100;
        }else{
            return getBattery();
        }

    }
    public void setBattery(int battery) {
        synchronized (batteryLock){
            this.battery = battery;
        }
    }
    public void decreaseBattery(int newbattery){
        synchronized (batteryLock){
            this.battery = newbattery;
        }
    }

    public int getNumDelivery(){
        synchronized (numDeliveryLock){
            return numDelivery;
        }
    }
    public void incrementNumDelivery(){
        synchronized (numDeliveryLock){
            numDelivery++;
        }
    }

    public double getNumKilo(){
        synchronized (numKiloLock){
            return numKilo;
        }
    }
    public void addKilo(double kilo){
        synchronized (numKiloLock){
            numKilo += kilo;
        }
    }

    public boolean isQuitting(){
        synchronized (isQuittingLock){
            return isQuitting;
        }
    }
    public void setQuitting(boolean bool) {
        synchronized (isQuittingLock){
            isQuitting = bool;
        }
    }

    public Object getUpdateDroneLock() {
        return updateDroneLock;
    }

    @Override
    public int compareTo(Drone other) {
        if(this.id - other.id > 0 ) return 1;
        else return -1;
    }

    @Override
    public String toString() {
        return  "id: " + id +  "  - pos: " + this.getPosition(0) + ", " + this.getPosition(1)
                + " - master: " + this.isMaster()
                + " - available: " + this.isAvailable()
                + " - battery: " + this.getBattery()
                + " - num delivery: " + this.getNumDelivery()
                + " - charge: " + this.isInCharge()
                + "\n";
    }

    public static void main(String[] args) throws JSONException, InterruptedException {
        int port = 2000 + (int)(Math.random() * 1000);
        Drone drone = new Drone(6, "localhost", port);
        drone.start();
    }

}
