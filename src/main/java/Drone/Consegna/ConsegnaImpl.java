package Drone.Consegna;

import Drone.Drone;
import Drone.Utilities.Exceptions.InChargeDroneException;
import Drone.Utilities.Exceptions.LowBatteryDroneException;
import Drone.Utilities.Exceptions.NotAvailableDroneException;
import Drone.Utilities.Utils;
import com.example.consegna.ConsegnaGrpc.*;
import com.example.consegna.ConsegnaOuterClass.*;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ConsegnaImpl extends ConsegnaImplBase {

    private Drone deliveryDrone;
    private Statistics stats;
    private int newBattery;
    private double distance;
    private InfoDelivery infoDelivery;

    public ConsegnaImpl(Drone deliveryDrone){
        this.deliveryDrone = deliveryDrone;
    }

     @Override
    public StreamObserver<InfoDelivery> assignConsegna(StreamObserver<Statistics> responseObserver) {

        // streamObserver che viene creato dal master per mandare infodelivery
        return new StreamObserver<InfoDelivery>() {

            @Override
            public void onNext(InfoDelivery id) {

                if (deliveryDrone.getMasterDrone() != null && deliveryDrone.getMasterDrone().id != deliveryDrone.id) {
                    Utils.printDetail("[CONSEGNA] richiesta per delivery " + id.getId() + "\n", 1);
                }else if(deliveryDrone.getMasterDrone() == null) {
                    // il drone si è perso l'elected, ma riceve un ordine dal drone master
                    deliveryDrone.setMasterDrone(infoDelivery.getMasterId());
                }

                infoDelivery = id;

                if (deliveryDrone.getBattery() < 15) {
                    Status status = Status.FAILED_PRECONDITION.withDescription("LowBattery");
                    responseObserver.onError(new LowBatteryDroneException(status));
                } else if (!deliveryDrone.isAvailable()) {
                    Status status = Status.FAILED_PRECONDITION.withDescription("NotAvailable");
                    responseObserver.onError(new NotAvailableDroneException(status));
                }else if(deliveryDrone.isInCharge()){
                    Utils.printDetail(" - il drone non accetta la consegna perchè è in carica", 1);
                    Status status = Status.FAILED_PRECONDITION.withDescription("InCharge");
                    responseObserver.onError(new InChargeDroneException(status));
                }else {
                    deliveryDrone.setAvailable(false);
                    deliveryDrone.setTimerStart();
                    ConsegnaThread consegnaThread = new ConsegnaThread(deliveryDrone, infoDelivery, stats);
                    consegnaThread.start();

                    try {
                        consegnaThread.join();

                        stats = consegnaThread.getStat();
                        distance = consegnaThread.getDistance();
                        newBattery = consegnaThread.getNewBattery();
                        int[] newpos = new int[2];
                        newpos[0] = consegnaThread.getPosition(0);
                        newpos[1] = consegnaThread.getPosition(1);

                        if(newBattery < 15 && !deliveryDrone.isInCharge() && !deliveryDrone.wannaCharge()) {
                            Utils.printDetail("[CONSEGNA THREAD] drone con batteria a " + newBattery + ", inizio procedura d'uscita \n", 1);
                            deliveryDrone.setQuitting(true);
                            deliveryDrone.quit();
                        }

                        responseObserver.onNext(stats);
                        responseObserver.onCompleted();

                        synchronized (deliveryDrone.getUpdateDroneLock()) {
                            deliveryDrone.incrementNumDelivery();
                            deliveryDrone.setPosition(newpos);
                            deliveryDrone.addKilo(distance);
                            deliveryDrone.setAvailableNotify();
                            // lasciare dopo setavailable = true per getUpdateBattery
                            deliveryDrone.setBattery(newBattery);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                Utils.printDetail("[CONSEGNA] errore di comunicazione con il master " + deliveryDrone.getMasterDrone().id
                        + " problemi per la consegna "  + infoDelivery.getId() + ", indire elezione", 1);
                deliveryDrone.removeDrone(deliveryDrone.getMasterDrone());
                deliveryDrone.setAvailableNotify();
                deliveryDrone.resetMasterDrone();

                Context ctx = Context.current().fork();
                ctx.run(() -> {
                    try {
                        deliveryDrone.setPartecipant(true);
                        deliveryDrone.forwardElectionMessage("election", deliveryDrone.id, deliveryDrone.getUpdateBattery());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                // responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

}
