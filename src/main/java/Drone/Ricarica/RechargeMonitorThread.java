package Drone.Ricarica;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.ricarica.RicaricaGrpc;
import com.example.ricarica.RicaricaOuterClass.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;

public class RechargeMonitorThread extends Thread{

    private Drone drone;
    private ArrayList<Drone> pendingDrones;
    private boolean quit = false;

    public RechargeMonitorThread(Drone drone){
        this.drone = drone;
        this.pendingDrones = new ArrayList<>();
    }

    @Override
    public void run() {

        while (!quit) {

            // fino a che il drone non vuole ricaricarsi, il monitor è in wait
            while (!drone.wannaCharge()) {
                synchronized (drone.getWannaChargeLock()) {
                    try {
                        drone.getWannaChargeLock().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                drone.waitNoPartecipant(" - sono in elezione, finisce la scelta del master e mi ricarico\n");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(!drone.isMaster()){

                drone.setRechargeTimestamp(System.currentTimeMillis());
                pendingDrones = drone.getDronesListCopy();

                try {
                    sayToAllWannaRecharge();
                    waitAllOk();
                    // il drone ha ottenuto tutti gli OK, se sta consegnando, termina la consegna e si ricarica
                    drone.waitAvailable(" - sto consegnando, finisco la consegna e mi ricarico");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                drone.setInCharge(true);
                Utils.printDetail("\n[RECHARGE MONITOR] ricarica in corso! \n", 1);

                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                drone.setPosition(new int[]{0,0});
                drone.setBattery(100);
                drone.setWannaCharge(false);
                drone.setInCharge(false);

                Utils.printDetail("[RECHARGE MONITOR] ricarica terminata \n", 1);

                drone.setWannaCharge(false);
                sendToAllPendingOk();
                notifyMaster();
            }else{
                Utils.printDetail("[RECHARGE MONITOR] il drone è diventato master, ricarica non consentita\n", 1);
            }



            /* System.out.println("INTERROMPI ORA, il drone in ricarica non manda sendToAllPending");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } */

        }
    }

    public void sayToAllWannaRecharge() throws InterruptedException {

        ArrayList<WannaRechargeThread> threads = new ArrayList<>();
        ArrayList<Drone> dronesCopy = new ArrayList<>(pendingDrones);

        for(Drone otherDrone: dronesCopy) {
            if(drone.id != otherDrone.id) {
                WannaRechargeThread t = new  WannaRechargeThread(drone, otherDrone, this);
                threads.add(t);
                t.start();
            }
        }

        for(WannaRechargeThread t: threads){
            t.join();
        }
    }

    public void waitAllOk() throws InterruptedException {
        while(pendingDrones.size() > 1){

            synchronized (pendingDrones){
                pendingDrones.wait(10000);
                ArrayList<PingThread> threads = new ArrayList<>();

                for(Drone toDrone: pendingDrones) {
                    if(toDrone.id != drone.id) {
                        PingThread t = new PingThread(drone, toDrone);
                        threads.add(t);
                        t.start();
                    }
                }
            }
        }
    }

    public void sendToAllPendingOk(){

        ArrayList<SendingPendingOkThread> threads = new ArrayList<>();
        ArrayList<RechargeRequest> reqs = drone.getAllRechargeRequest();

        if(reqs.size() != 0) {
            for(RechargeRequest req: reqs) {
                SendingPendingOkThread t = new SendingPendingOkThread(drone, req.getId(), req.getIp(), req.getPort());
                threads.add(t);
                t.start();
            }
        }

    }

    public void removePendingDrone(int id){
        synchronized (pendingDrones){
            pendingDrones.removeIf(d -> d.id == id);
            pendingDrones.notify();
        }
    }

    public void notifyMaster() {

        Drone master = drone.getMasterDrone();

        // sono in ricarica, cade il master inizio elezione. L'elezione comincia quando la ricarica è terminata, a
        // carica terminata bisogna notificare il master, ma è null
        if(drone.getMasterDrone() != null) {
            String address = String.format("%s:%s", master.getIp(), master.getPort());
            ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

            RicaricaGrpc.RicaricaStub stub = RicaricaGrpc.newStub(channel);
            OkRicarica okRicarica = OkRicarica.newBuilder().setId(drone.id).build();

            stub.sayRechargeTerminated(okRicarica, new StreamObserver<OkRicarica>() {
                @Override
                public void onNext(OkRicarica value) {
                    channel.shutdown();
                }

                @Override
                public void onError(Throwable t) {
                    channel.shutdown();
                }

                @Override
                public void onCompleted() {
                    channel.shutdown();
                }
            });
        }

    }

    public void quit(){
        quit = true;
    }

}
