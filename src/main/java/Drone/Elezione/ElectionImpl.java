package Drone.Elezione;

import Drone.Drone;
import Drone.Utilities.Utils;
import com.example.elezione.ElectionGrpc.*;
import com.example.elezione.ElectionOuterClass;
import com.example.elezione.ElectionOuterClass.*;
import io.grpc.stub.StreamObserver;

public class ElectionImpl extends ElectionImplBase {

    private Drone drone;

    public ElectionImpl(Drone drone){
        this.drone = drone;
    }

    @Override
    public StreamObserver<ElectionMessage> makingElection(StreamObserver<OkElection> responseObserver) {

        return new StreamObserver<ElectionMessage>() {

            @Override
            public void onNext(ElectionOuterClass.ElectionMessage electionMessage) {

                Utils.printDetail("[ELEZIONE] \nRicevuto messaggio " + electionMessage.getCommand() +
                        " , ID: " + electionMessage.getId() + ", batteria: "  + electionMessage.getBattery(),1);

                String cmd = electionMessage.getCommand();
                int reqID = electionMessage.getId();
                int reqBattery = electionMessage.getBattery();
                int thisBattery = drone.getUpdateBattery();

                Utils.printDetail(" - drone ricevente, ID: " + drone.id + ", batteria: " + thisBattery, 1);


                if(cmd.equals("election")){

                    /* un nuovo drone entra in elezione, il prossimo master non ha ancora ricevuto election per cui
                    non sa di essere master, se il master manda prima l'elected rispetto ad inserire il nuovo drone,
                    il nuovo drone potrebbe non sapere mai chi è il master -> indice elezione perchè master = null
                    se un drone già master riceve un messaggio election, fa un giro di elected di controllo */

                    if(drone.isMaster()){
                        try {
                            Utils.printDetail(" [!] un drone si è perso l'elezione, giro di elected di sicurezza", 1);
                            drone.forwardElectionMessage("elected", drone.id, drone.getUpdateBattery());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else if(reqID == drone.id){

                        Utils.printDetail(" - questo drone è master -> manda elected agli altri droni ", 1);
                        drone.setMaster(true);
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            drone.forwardElectionMessage("elected", drone.id, drone.getUpdateBattery());
                            // si setterà partecipant = false quando riceverà il suo elected e diventerà master
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }else if(!drone.isMaster() && (reqBattery > thisBattery || (reqBattery == thisBattery && reqID > drone.id))){
                        // il request è un candidato migliore, se il drone è già master non deve continuare l'elezione
                        drone.setPartecipant(true);
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            drone.forwardElectionMessage("election", reqID, reqBattery);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }else if(!drone.isMaster() && (reqBattery < thisBattery || (reqBattery == thisBattery && reqID < drone.id))){
                        // il drone ricevente è un candidato migliore
                        if (!drone.isPartecipant()) {

                            drone.setPartecipant(true);
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            try {
                                drone.forwardElectionMessage("election", drone.id, thisBattery);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }else{
                            Utils.printDetail(" - messaggio scartato, elezioni multiple\n", 1);

                        }

                    }

                }else if(cmd.equals("elected")) {

                    if (electionMessage.getId() == drone.id) {
                        Utils.printDetail("\n[ELEZIONE CONCLUSA] questo drone è master + sleep\n", 1);

                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        drone.resetMasterDrone();
                        drone.setMasterDrone(electionMessage.getId());
                        drone.becomeMaster();
                        drone.setPartecipant(false);

                    }else if(drone.isMaster() && (reqBattery < thisBattery || (reqBattery == thisBattery && reqID < drone.id))) {
                        // wrong election --> un drone già master riceve un elected da un altro drone
                        // il drone ricevente è un candidato migliore, stoppa e inoltra il suo elected
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            drone.forwardElectionMessage(" elected", drone.id, drone.getUpdateBattery());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else if(drone.isMaster() && (reqBattery > thisBattery || (reqBattery == thisBattery && reqID > drone.id))){
                        // wrong election --> il master Request è un drone migliore

                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        drone.setMaster(false);
                        drone.quitOnlyMaster();
                        try {
                            drone.forwardElectionMessage("elected", electionMessage.getId(), electionMessage.getBattery());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        // ricevo un elected, il drone che ha mandato il messaggio è il master
                        drone.resetMasterDrone();
                        drone.setMasterDrone(electionMessage.getId());
                        Utils.printDetail("\n[ELEZIONE CONCLUSA] il drone master è " + reqID +"\n", 1);
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            drone.forwardElectionMessage("elected", electionMessage.getId(), electionMessage.getBattery());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // prima re-inoltra il msg elected! se deve uscire quitThread è in wait su partecipant,
                        // se si setta partecipant = false c'è il rischio che esca prima di re-inoltrare il mex elected
                        drone.setPartecipant(false);
                    }
                }

                responseObserver.onCompleted();

            }

            @Override
            public void onError(Throwable t) {
                Utils.printDetail("BROKEN RING",1);
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed");
            }
        };
    }
}
