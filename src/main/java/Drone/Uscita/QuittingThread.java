package Drone.Uscita;

import Drone.Drone;
import Drone.Utilities.Utils;

public class QuittingThread extends Thread{

    private Drone drone;

    public QuittingThread(Drone drone){
        this.drone = drone;
    }

    @Override
    public void run() {

        Utils.printDetail("[QUIT] \nIl drone vuole uscire...", 1);

        // aspetta di finire la consegna
        try { drone.waitAvailable(" - il drone sta consegnando, appena terminato inizierà ad uscire"); }
        catch (InterruptedException e) {e.printStackTrace();}

        if(drone.isMaster()){
                Utils.printDetail("il master può uscire perché non sta più consegnando", 1);
                try {
                    drone.waitNoPartecipant( " - il master drone è ancora in elezione ... \n");
                    drone.quitMasterThread();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }else{
                try {
                    drone.waitNoPartecipant( " - il drone è in elezione ... \n");
                    Utils.printDetail("il drone può uscire perché non sta consegnando né è in elezione", 1);
                    drone.quitGrpcServer();
                    Utils.notifyDeleteREST(drone);
                    drone.quitCmdQuit();
                    drone.quitRechargeMonitor();
                    drone.quitPingThread();
                    System.exit(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }

    }

}
