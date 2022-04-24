package Drone.Inizializzazione;
import Drone.Drone;
import com.example.initpres.InitPresentationOuterClass.*;
import com.example.initpres.InitPresentationGrpc.*;
import io.grpc.stub.StreamObserver;

public class InitPresentationImpl extends InitPresentationImplBase {

    private Drone receiverDrone;

    public InitPresentationImpl(Drone drone) {
        this.receiverDrone = drone;
    }

    @Override
    public void hello(HelloMessage hello, StreamObserver<OkMessage> responseObserver) {

        Drone d = new Drone(hello.getId(), hello.getIp(), hello.getPort());
        if(receiverDrone.isMaster()) {
            int[] pos = new int[] {hello.getPosition(0), hello.getPosition(1)};
            d.setPosition(pos);
        }
        receiverDrone.addDrone(d);

        OkMessage response = OkMessage.newBuilder()
                .setIsMaster(receiverDrone.isMaster())
                .setIsPartecipant(receiverDrone.isPartecipant()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
