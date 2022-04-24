package Drone.Altro;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.ping.PingGrpc.*;
import com.example.ping.PingOuterClass.*;
import io.grpc.stub.StreamObserver;

public class PingImpl extends PingImplBase {

    private Drone drone;

    public PingImpl(Drone drone){
        this.drone = drone;
    }

    @Override
    public void pingMaster(isThereMessage request, StreamObserver<masterMessage> responseObserver) {

        int idPingDrone = request.getIdDrone();
        int idMaster = drone.id;

        Utils.printDetail("[PING] il drone " + idPingDrone + " ha pingato\n", 2);
        masterMessage response = masterMessage.newBuilder().setIdMaster(idMaster).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
