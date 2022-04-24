package Drone.Elezione;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.elezione.ElectionGrpc;
import com.example.elezione.ElectionOuterClass.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class ElectionThread extends Thread{

    private Drone drone;
    private int messID;
    private int messBattery;
    private String command;
    private ManagedChannel channel;


    public ElectionThread(Drone drone, String command, int messageID, int messageBattery){
        this.drone = drone;
        this.messID = messageID;
        this.messBattery = messageBattery;
        this.command = command;
    }


    @Override
    public void run() {

        Drone successor = drone.findNext();
        String address = String.format("%s:%s", successor.getIp(), successor.getPort());
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

        ElectionGrpc.ElectionStub stub = ElectionGrpc.newStub(channel);

        ElectionMessage electionMessage = ElectionMessage.newBuilder()
                .setBattery(messBattery)
                .setId(messID)
                .setCommand(command)
                .build();

        Utils.printDetail(" - inoltrato messaggio {command: " + command
                + ", id: "+ messID +
                ", battery: " + messBattery
                + "} al drone " + successor.id +"\n" , 1);

        StreamObserver<ElectionMessage> streamSuccessorDrone = stub.makingElection(new StreamObserver<OkElection>() {

            @Override
            public void onNext(OkElection value) {
                channel.shutdown();
            }

            @Override
            public void onError(Throwable t) {
                Utils.printDetail("[ELEZIONE THREAD] errore di comunicazione con il successore: " + successor.id, 1);
                drone.removeDrone(successor);
                try {
                    drone.forwardElectionMessage(command, messID, messBattery);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                channel.shutdown();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }

        });

        streamSuccessorDrone.onNext(electionMessage);
    }
}
