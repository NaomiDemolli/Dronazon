package Drone.Inizializzazione;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.initpres.InitPresentationGrpc.*;
import com.example.initpres.InitPresentationGrpc;
import com.example.initpres.InitPresentationOuterClass.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class InitPresentationThread extends Thread{

    private Drone sender;
    private Drone receveir;
    private ManagedChannel channel;

    public InitPresentationThread(Drone sender, Drone receiver){
        this.sender = sender;
        this.receveir = receiver;
    }

    @Override
    public synchronized void run() {

        String address = String.format("%s:%s", receveir.getIp(), receveir.getPort());
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

        InitPresentationStub stub = InitPresentationGrpc.newStub(channel);

        HelloMessage request = HelloMessage.newBuilder()
                .setId(sender.id)
                .setIp(sender.getIp())
                .setPort(sender.getPort())
                .addPosition(sender.getPosition(0))
                .addPosition(sender.getPosition(1))
                .build();

        stub.hello(request, new StreamObserver<OkMessage>() {

            public void onNext(OkMessage okMessage) {

                if (okMessage.getIsMaster()) {
                    receveir.setMaster(true);
                }

            }

            public void onError(Throwable throwable) {
                Utils.printDetail("[ERRORE di COMUNICAZIONE] con il drone: " + receveir.id ,1);
                sender.removeDrone(receveir);
                channel.shutdown();
            }

            public void onCompleted() {
                channel.shutdownNow();
            }

        });

        try {
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
