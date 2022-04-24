package Drone.Altro;

// THREAD CHE OGNI DIECI SECONDI STAMPA LE INFORMAZIONI SUL DRONE

import Drone.Drone;
import Drone.Utilities.Utils;

import java.util.TimerTask;

public class PrintInfoThread extends TimerTask{

    private Drone drone;

    public PrintInfoThread(Drone drone){
        this.drone = drone;
    }

    @Override
    public void run() {

        if (!drone.isQuitting()) {

            synchronized (drone.getUpdateDroneLock()) {
                Utils.printDetail("[INFO DRONE] "
                        + "totale consegne effettuate: " + drone.getNumDelivery()
                        + " - kilometri percorsi: " + Math.round(drone.getNumKilo() * 1000) / 1000
                        + " - batteria residua: " + drone.getBattery()
                        + " - posizione: [" + drone.getPosition(0) +"," +drone.getPosition(1) + "]"
                        + "\n", 1);
                        /* + " - available: " + drone.isAvailable()
                        + " - election: " + drone.isPartecipant()
                        + " - master: " + drone.isMaster() */

            }
        }

    }


}
