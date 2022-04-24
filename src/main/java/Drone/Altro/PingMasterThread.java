package Drone.Altro;

import Drone.Drone;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import com.example.ping.PingGrpc;
import com.example.ping.PingGrpc.*;
import com.example.ping.PingOuterClass.*;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;


public class PingMasterThread extends Thread {

    private Drone drone;
    private ManagedChannel channel;
    private boolean quit = false;

    public PingMasterThread(Drone drone){
        this.drone = drone;
    }

    @Override
    public void run() {

        while(!quit) {

            if(!drone.isMaster()) {

                try {waitInitTime();}
                catch (InterruptedException e) {e.printStackTrace();}

                // Timer aggiornato da ConsegnaImpl nel momento in cui è affidata una consegna al drone
                while(!drone.isTimerExpired()){
                    try{waitTime();}
                    catch (InterruptedException e) {e.printStackTrace();}
                }

                // non faccio partire un'altra elezione, ma se il master non fa in tempo a mandare "elected"
                // nessuno sa chi è il master e tutti sono partecipant = true --> stallo
                // l'unica soluzione è aspettare che entri un nuovo drone, quando pinga non trova il master e fa partire
                // un'altra elezione, ma dev'essere il candidato migliore se no si stoppa (ASSUNZIONE ID CRESCENTI)

                while(drone.isPartecipant()){
                    try {waitTime();}
                    catch (InterruptedException e) {e.printStackTrace();}
                }

                if(drone.getMasterDrone() == null && !drone.isMaster() && !drone.isPartecipant()){
                    Utils.printDetail("[PING] Il drone non ha riconosciuto nessun master, indice elezione per capire", 1);
                    // il drone è entrato in elezione e si è perso l'elected, inizia un'elezione se un master è già presente
                    // ignora il messaggio e invia un "elected" all'anello
                    drone.setPartecipant(true);
                    try {drone.forwardElectionMessage("election", drone.id, drone.getUpdateBattery());}
                    catch (InterruptedException e) {e.printStackTrace();}

                }else if(drone.getMasterDrone() != null && drone.getMasterDrone().id != drone.id){
                    Drone master = drone.getMasterDrone();
                    String address = String.format("%s:%s", master.getIp(), master.getPort());
                    channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

                    PingStub stub = PingGrpc.newStub(channel);
                    isThereMessage request = isThereMessage.newBuilder().setIdDrone(drone.id).build();

                    stub.pingMaster(request, new StreamObserver<masterMessage>() {
                        @Override
                        public void onNext(masterMessage value) {
                            Utils.printDetail("[PING] il drone master c'è \n", 1);
                            channel.shutdown();
                        }

                        @Override
                        public void onError(Throwable t) {
                            Utils.printDetail("[PING] il drone master non risponde, indico elezione!", 1);

                            Context ctx = Context.current().fork();
                            ctx.run(() -> {
                                try {
                                    drone.setPartecipant(true);
                                    drone.forwardElectionMessage("election", drone.id, drone.getUpdateBattery());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });
                            channel.shutdown();
                        }

                        @Override
                        public void onCompleted() {
                            channel.shutdown();
                        }
                    });

                    try {
                        channel.awaitTermination(3, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public synchronized void waitInitTime() throws InterruptedException {
        wait(Config.SLEEPTIME_PING_INIT);
    }

    public synchronized void waitTime() throws InterruptedException {
        wait(Config.SLEEPTIME_PING_DELIVERY + (int)(Math.random() * 5000));
    }

    public void quit(){
        quit = true;
    }

}
