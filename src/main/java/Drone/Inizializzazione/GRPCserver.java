package Drone.Inizializzazione;

import Drone.Altro.PingImpl;
import Drone.Consegna.ConsegnaImpl;
import Drone.Drone;
import Drone.Elezione.ElectionImpl;
import Drone.Master.GetInfoImpl;
// import Drone.Ricarica.RechargeImpl;
import Drone.Ricarica.RicaricaImpl;
import Drone.Utilities.Utils;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GRPCserver extends Thread {

    private Drone receiverDrone;
    private boolean quitting;

    public GRPCserver(Drone drone) {
        this.receiverDrone = drone;
    }

    @Override
    public synchronized void run() {

        while(!quitting) {
            Server server = ServerBuilder.forPort(receiverDrone.getPort())
                    .addService(new InitPresentationImpl(receiverDrone))
                    .addService(new GetInfoImpl(receiverDrone))
                    .addService(new ConsegnaImpl(receiverDrone))
                    .addService(new ElectionImpl(receiverDrone))
                    .addService(new PingImpl(receiverDrone))
                    .addService(new RicaricaImpl(receiverDrone)).build();
            try {
                server.start();
                Utils.printDetail("[GRPC] il server è partito\n", 2);
            } catch (IOException e) {
                Utils.printDetail("[GRPC] il server non è partito", 1);
                e.printStackTrace();
            }

            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                Utils.printDetail("- chiusura server GRPC", 1);
                server.shutdown();
            }
        }
    }

    public void quit(){
        Utils.printDetail("Il server GRPC si sta disconnettendo ", 1);
        quitting = true;
    }
}
