package Drone.Ricarica;

import Drone.Drone;
import Drone.Utilities.Exceptions.ErrorMasterIDException;
import Drone.Utilities.Utils;
import com.example.getinfo.GetInfoOuterClass;
import com.example.ricarica.RicaricaGrpc.*;
import com.example.ricarica.RicaricaOuterClass.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class RicaricaImpl extends RicaricaImplBase {

    private Drone drone;

    public RicaricaImpl(Drone drone){
        this.drone = drone;
    }

    @Override
    public StreamObserver<RicaricaRequest> wannaRecharge(StreamObserver<OkRicarica> responseObserver) {
        return new StreamObserver<RicaricaRequest>() {

            @Override
            public void onNext(RicaricaRequest rechargeRequest) {

                int reqDroneID = rechargeRequest.getId();
                int reqPort = rechargeRequest.getPort();
                String reqIp = rechargeRequest.getIp();
                long reqTimestamp = rechargeRequest.getTimestamp();

                Utils.printContext("[RECHARGE SERVICE] \n - il drone " + reqDroneID + " chiede di potersi ricaricare" , 'r');

                /* Se il drone riceve una richiesta di recharge, ma è in elezione aspetta
                try {drone.waitNoPartecipant(" - sono in elezione, gestisco richiesta di recharge ad elezione terminata");}
                catch (InterruptedException e) {e.printStackTrace();}*/

                // RICART AND AGRAWALA
                if(!drone.isInCharge() && !drone.wannaCharge()){

                    Utils.printContext(" - mando OK al drone " + reqDroneID, 'r');
                    responseObserver.onNext(OkRicarica.newBuilder().setId(drone.id).build());

                }else if(!drone.isInCharge() && drone.wannaCharge()){

                    if(reqTimestamp < drone.getRechargeTimestamp()){
                        Utils.printContext(" - sono in wanna ma il drone " + reqDroneID + " l'ha chiesto prima, mando OK", 'r');
                        responseObserver.onNext(OkRicarica.newBuilder().setId(drone.id).build());

                    }else{
                        Utils.printContext(" - sono in wanna, sono arrivato prima, accodo la richiesta del drone " + reqDroneID +"\n", 'r');
                        RechargeRequest request = new RechargeRequest(reqDroneID, reqIp, reqPort, reqTimestamp);
                        drone.addRequest(request);

                    }
                }else if(drone.isInCharge()){
                    Utils.printContext(" - sono in ricarica, accodo la richiesta del drone " + reqDroneID + "\n", 'r');

                    RechargeRequest request = new RechargeRequest(reqDroneID, reqIp,reqPort,reqTimestamp);
                    drone.addRequest(request);
                }

                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                Utils.printContext("[RECHARGE networkError] errore di comunicazione col drone che richiede ricarica", 'r');
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }

        };
    }

    @Override
    public StreamObserver<OkRicarica> sendPendingOk(StreamObserver<OkRicarica> responseObserver) {
        return new StreamObserver<OkRicarica>() {
            @Override
            public void onNext(OkRicarica value) {
                drone.removePendingOkDrone(value.getId());
                Utils.printContext(" - arrivato OK dal drone " + value.getId(), 'r');
                onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void sayRechargeTerminated(OkRicarica request, StreamObserver<OkRicarica> responseObserver) {

        int finishDroneID = request.getId();
        Utils.printDetail("[RECHARGE SERVICE] il drone " + finishDroneID + " ha finito di ricaricarsi\n", 1);
        drone.updateFinishChargeDrone(finishDroneID);
        responseObserver.onCompleted();

    }
}
