package Drone.Master;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.getinfo.GetInfoGrpc;
import com.example.getinfo.GetInfoOuterClass.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class GettingInfoThread extends Thread{

    private Drone masterDrone;
    private Drone deliveryDrone;
    private MasterThread masterThread;
    private ManagedChannel channel;

    public GettingInfoThread(Drone masterDrone, MasterThread masterThread, Drone deliveryDrone){
        this.masterDrone = masterDrone;
        this.deliveryDrone = deliveryDrone;
        this.masterThread = masterThread;
    }

    @Override
    public void run() {

        String address = String.format("%s:%s", deliveryDrone.getIp(), deliveryDrone.getPort());
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

        GetInfoGrpc.GetInfoStub stub = GetInfoGrpc.newStub(channel);

        RequestMessage request = RequestMessage.newBuilder().setIdMaster(masterDrone.id).build();

        stub.gettingInfo(request, new StreamObserver<InfoMessage>() {

            @Override
            public void onNext(InfoMessage value) {
                int x = value.getPosition(0);
                int y = value.getPosition(1);
                int battery = value.getBattery();
                Utils.printDetail("[GET INFO THREAD] delivery drone " + value.getId() + " ha risposto, posizione " + x + ", " + y +"\n", 2);
                int[] pos = new int[]{x, y};
                deliveryDrone.setPosition(pos);
                deliveryDrone.setBattery(battery);
                Utils.printDetail("[GET INFO THREAD] lista dopo l'update\n" + masterDrone.getDronesListCopy().toString() +"\n", 3);
                channel.shutdown();
            }

            @Override
            public void onError(Throwable t) {

                if(t.getMessage().equalsIgnoreCase( "FAILED_PRECONDITION: ErrorMasterID")) {
                    // incremento un contatore, la ri-elezione viene riavviata una sola volta
                    masterThread.increaseReElectionCounter();

                    Utils.printDetail("[masterDrone GETINFO] Il drone " + deliveryDrone.id + " non concorda sull'id del master \n", 1);

                    if(masterThread.getReElectionCounter() == 1) {
                        try {
                            masterDrone.forwardElectionMessage("elected", masterDrone.id, masterDrone.getUpdateBattery());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    channel.shutdown();
                }else{
                    Utils.printDetail("[GET INFO THREAD] errore di comunicazione con drone " + deliveryDrone.id, 1);
                    masterDrone.removeDrone(deliveryDrone);
                    Utils.notifyDeleteREST(deliveryDrone);
                    channel.shutdown();
                }
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });


    }
}
