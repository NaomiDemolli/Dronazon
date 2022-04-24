package Drone.Master;

import Drone.Drone;
import Drone.Utilities.Exceptions.ErrorMasterIDException;
import Drone.Utilities.Utils;
import com.example.getinfo.GetInfoGrpc.*;
import com.example.getinfo.GetInfoOuterClass.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class GetInfoImpl extends GetInfoImplBase {

    Drone deliveryDrone;

    public GetInfoImpl(Drone deliveryDrone){
        this.deliveryDrone = deliveryDrone;
    }

    @Override
    public void gettingInfo(RequestMessage request, StreamObserver<InfoMessage> responseObserver) {

        int idMaster = request.getIdMaster();

        if(deliveryDrone.getMasterDrone() != null && deliveryDrone.getMasterDrone().id == idMaster) {
            InfoMessage response = InfoMessage.newBuilder()
                    .setId(deliveryDrone.id)
                    .setIp(deliveryDrone.getIp())
                    .setPort(deliveryDrone.getPort())
                    .addPosition(deliveryDrone.getPosition(0))
                    .addPosition(deliveryDrone.getPosition(1))
                    .setBattery(deliveryDrone.getUpdateBattery())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        // un drone con master = null si è perso l'elezione, quando riceve consegna setta il master
        }else if(deliveryDrone.getMasterDrone() == null)   {
            Utils.printDetail("[GET INFO] il drone non aveva riconosciuto il master, ora lo salva", 1);
            deliveryDrone.setMasterDrone(idMaster);
            InfoMessage response = InfoMessage.newBuilder()
                    .setId(deliveryDrone.id)
                    .setIp(deliveryDrone.getIp())
                    .setPort(deliveryDrone.getPort())
                    .addPosition(deliveryDrone.getPosition(0))
                    .addPosition(deliveryDrone.getPosition(1))
                    .setBattery(deliveryDrone.getUpdateBattery())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }else {
            Utils.printDetail("[deliveryDrone GETINFO] il drone non riconosce l'ID del master\n", 1);
            Status status = Status.FAILED_PRECONDITION.withDescription("ErrorMasterID");
            responseObserver.onError(new ErrorMasterIDException(status));
            responseObserver.onCompleted();
        }
    }
}
