package Drone.Ricarica;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.ricarica.RicaricaGrpc;
import com.example.ricarica.RicaricaOuterClass;
import com.example.ricarica.RicaricaOuterClass.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class WannaRechargeThread extends Thread{

    private Drone fromDrone;
    private Drone toDrone;
    private ManagedChannel channel;
    private RechargeMonitorThread rechargeMonitor;


    public WannaRechargeThread(Drone fromDrone, Drone toDrone, RechargeMonitorThread rechargeMonitor){
        this.fromDrone = fromDrone;
        this.toDrone = toDrone;
        this.rechargeMonitor = rechargeMonitor;
    }

    @Override
    public void run() {

        String address = String.format("%s:%s", toDrone.getIp(), toDrone.getPort());
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

        RicaricaGrpc.RicaricaStub stub = RicaricaGrpc.newStub(channel);

        RicaricaOuterClass.RicaricaRequest rechargeReq = RicaricaOuterClass.RicaricaRequest.newBuilder()
                .setResource("recharge")
                .setId(fromDrone.id)
                .setIp(fromDrone.getIp())
                .setPort(fromDrone.getPort())
                .setTimestamp(System.currentTimeMillis())
                .build();

        StreamObserver<RicaricaRequest> streamDrone = stub.wannaRecharge(new StreamObserver<OkRicarica>() {

            @Override
            public void onNext(OkRicarica value) {
                rechargeMonitor.removePendingDrone(value.getId());
                Utils.printContext(" - arrivato OK dal drone " + value.getId(), 'r');
                //Utils.printContext("[WANNA RECHARGE] arrivato OK dal drone " + value.getId() + " incremento okCounter " + fromDrone.getOKcounter(), 'r');
                channel.shutdown();
            }

            @Override
            public void onError(Throwable t) {
                rechargeMonitor.removePendingDrone(toDrone.id);
                Utils.printContext(" - il drone " + toDrone.id + " non è più tra noi", 'r');
                fromDrone.removeDrone(toDrone);
                channel.shutdown();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }

        });

        streamDrone.onNext(rechargeReq);
        try {
            channel.awaitTermination(3000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
