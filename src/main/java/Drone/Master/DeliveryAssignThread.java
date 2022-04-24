package Drone.Master;

import Delivery.Delivery;
import Drone.Drone;
import Drone.Statistiche.Report;
import Drone.Utilities.Utils;
import com.example.consegna.ConsegnaGrpc.*;
import com.example.consegna.ConsegnaGrpc;
import com.example.consegna.ConsegnaOuterClass.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import Delivery.DeliveryQueue;

import java.util.concurrent.TimeUnit;

public class DeliveryAssignThread extends Thread{

    private Delivery delivery;
    private Drone deliveryDrone;
    private MasterThread masterThread;
    private ManagedChannel channel;
    private DeliveryQueue deliveryQueue;
    private Drone masterDrone;

    public DeliveryAssignThread(Delivery delivery, Drone masterDrone, Drone deliveryDrone, MasterThread masterThread, DeliveryQueue deliveryQueue) {
        this.delivery = delivery;
        this.deliveryDrone = deliveryDrone;
        this.masterThread = masterThread;
        this.deliveryQueue = deliveryQueue;
        this.masterDrone = masterDrone;
    }

    @Override
    public void run() {

        String address = String.format("%s:%s", deliveryDrone.getIp(), deliveryDrone.getPort());
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();

        ConsegnaStub stub = ConsegnaGrpc.newStub(channel);

        InfoDelivery infoDelivery = InfoDelivery.newBuilder()
                .setId(delivery.getId())
                .setMasterId(masterDrone.id)
                .addFromPosition(delivery.getFromPosition(0))
                .addFromPosition(delivery.getFromPosition(1))
                .addToPosition(delivery.getToPosition(0))
                .addToPosition(delivery.getToPosition(1))
                .build();

        Utils.printDetail("[ASSIGN THREAD grpc] presa a carico della consegna " + delivery
                + " da parte del drone " + deliveryDrone.id +"\n" , 2);

        StreamObserver<InfoDelivery> streamDeliveryDrone = stub.assignConsegna(new StreamObserver<Statistics>() {

            @Override
            public void onNext(Statistics value) {
                int x =  value.getNewPosition(0);
                int y =  value.getNewPosition(1);
                Utils.printDetail("[DELIVERY DONE] \nrisposta dal drone " + deliveryDrone.id + " per consegna " + infoDelivery.getId() + "\n"
                        + "battery: " + value.getBattery() + "\n"
                        + "nuova posizione:  [" + x + "," + y +"]\n"
                        + "distanza: " + Math.round(value.getDistance()*100/100) + "\n",1);
                int[] newPos = {x,y};
                masterThread.increaseNotifyResponseCounter();
                masterThread.setAvailableUpdateDrone(deliveryDrone, newPos, value.getBattery());
                // se battery < 15 available rimane false

                Report report = new Report(deliveryDrone.id, deliveryDrone.getBattery(), value.getDistance(),
                        value.getAvgPollution(), value.getTimestamp());

                Utils.printDetail("[DELIVERY ASSIGN] generato nuovo report: " + report, 2);
                masterThread.addReport(report);
                onCompleted();
            }

            @Override
            public void onError(Throwable t) {

                if (t.getMessage().equalsIgnoreCase("FAILED_PRECONDITION: LowBattery")) {
                    Utils.printDetail("[ASSIGN THREAD] Il drone è scarico ", 1);
                } else if (t.getMessage().equalsIgnoreCase("FAILED_PRECONDITION: NotAvailable")) {
                    Utils.printDetail("[ASSIGN THREAD] Il drone non è disponibile ", 1);
                } else if (t.getMessage().equalsIgnoreCase("FAILED_PRECONDITION: InCharge")){
                    Utils.printDetail("[ASSIGN THREAD] Il drone " + deliveryDrone.id + " è in ricarica, non accetta la consegna\n", 1);
                    deliveryDrone.setInCharge(true);
                }else {
                    Utils.printDetail("[ASSIGN THREAD] errore di comunicazione con il drone " + deliveryDrone.id
                            + " problemi per la consegna " + infoDelivery.getId(), 1);
                    masterThread.removeDrone(deliveryDrone);
                }

                deliveryQueue.put(delivery);
                masterThread.increaseDeliveryCounter();
                masterThread.increaseNotifyResponseCounter();
                masterThread.increaseReportCounter(1);
                Utils.notifyDeleteREST(deliveryDrone);
                onCompleted();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }

        });

        streamDeliveryDrone.onNext(infoDelivery);

        try {
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
