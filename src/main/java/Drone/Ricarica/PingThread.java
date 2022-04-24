package Drone.Ricarica;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.ping.PingGrpc.*;
import com.example.ping.PingGrpc;
import com.example.ping.PingOuterClass.*;
import com.example.ping.PingOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class PingThread extends Thread{

    private Drone toDrone;
    private Drone fromDrone;
    private ManagedChannel channel;
    private boolean quit = false;

    public PingThread(Drone fromDrone, Drone toDrone){
        this.toDrone = toDrone;
        this.fromDrone = fromDrone;
    }

    @Override
    public void run() {
        Utils.printDetail(" - ping verso il drone " + toDrone.id + " per avere l'OK", 1 );
        String address = String.format("%s:%s", toDrone.getIp(), toDrone.getPort());
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

        PingStub stub = PingGrpc.newStub(channel);
        PingOuterClass.isThereMessage request = PingOuterClass.isThereMessage.newBuilder().setIdDrone(fromDrone.id).build();

        stub.pingMaster(request, new StreamObserver<masterMessage>() {
            @Override
            public void onNext(masterMessage value) {
                Utils.printDetail(" - il drone " + toDrone.id + " risponde, devo aspettare", 'r');
                channel.shutdown();
            }

            @Override
            public void onError(Throwable t) {
                Utils.printDetail(" - il drone " + toDrone.id + " non risponde, non mi serve il suo OK!", 1);
                fromDrone.removePendingOkDrone(toDrone.id);
                channel.shutdown();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });
    }


}
