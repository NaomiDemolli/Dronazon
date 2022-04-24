package Drone.Ricarica;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.ricarica.RicaricaGrpc;
import com.example.ricarica.RicaricaOuterClass;
import com.example.ricarica.RicaricaOuterClass.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class SendingPendingOkThread extends Thread{

    private Drone fromDrone;
    private int toID;
    private int toPort;
    private String toIp;
    private ManagedChannel channel;

    public SendingPendingOkThread(Drone fromDrone, int id, String ip, int port){
        this.fromDrone = fromDrone;
        this.toID = id;
        this.toIp = ip;
        this.toPort = port;
    }

    @Override
    public void run() {

        Utils.printContext("[SENDING PENDING OK] sto mandando OK al drone: " + toID, 'r');
        String address = String.format("%s:%s", toIp, toPort);
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        RicaricaGrpc.RicaricaStub stub = RicaricaGrpc.newStub(channel);

        OkRicarica okRicarica = OkRicarica.newBuilder().setId(fromDrone.id).build();

        StreamObserver<OkRicarica> streamDrone = stub.sendPendingOk(new StreamObserver<OkRicarica>() {

            @Override
            public void onNext(RicaricaOuterClass.OkRicarica value) {
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

        streamDrone.onNext(okRicarica);
    }

}
