package Drone.Altro;

import Drone.Drone;
import Drone.Ricarica.RechargeMonitorThread;
import Drone.Utilities.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandLineThread extends Thread {

    private Drone drone;
    private BufferedReader inUser;
    private boolean quitting;
    private RechargeMonitorThread rechargeThread;

    public CommandLineThread(Drone drone, RechargeMonitorThread rechargeThread) {
        this.drone = drone;
        this.inUser = new BufferedReader(new InputStreamReader(System.in));
        this.rechargeThread = rechargeThread;
    }

    @Override
    public void run() {

        while(!quitting) {
            String message = null;
            try {
                message = inUser.readLine();

                if(message != null && message.equalsIgnoreCase("Q")) {

                    if(drone.isInCharge()){
                        Utils.printDetail(" - il drone si sta ricaricando, non uscire ora", 1);
                    }else if(drone.wannaCharge()){
                        Utils.printDetail(" - il drone vuole ricaricarsi, non uscire ora", 1);
                    }else {
                        drone.quit();
                    }

                }else if(message != null && message.equalsIgnoreCase("R")){

                    if(drone.isMaster()){
                        Utils.printDetail(" - il drone master non può ricaricarsi! \n", 1);
                    }else if(drone.isInCharge()) {
                        Utils.printDetail(" - il drone è già in carica \n", 1);
                    }else if(drone.wannaCharge()){
                        Utils.printDetail(" - il drone ha già richiesto di potersi ricaricare ", 1);
                    }else{
                        Utils.printDetail("[RECHARGE] inizializzazione procedura di ricarica ", 1);
                        drone.setWannaCharge(true);
                        if(!rechargeThread.isAlive()){
                            rechargeThread.start();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Utils.printDetail("interrupt thread di quitting", 3);
            }catch (NullPointerException ne){
                Utils.printDetail("interrupt thread di quitting", 3);
            }
        }

        Utils.printDetail("[CMD QUIT THREAD] uscendo dal servizio di quit", 1);
    }

    public void quit(){
        quitting = true;
    }


}
