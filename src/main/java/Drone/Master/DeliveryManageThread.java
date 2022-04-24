package Drone.Master;

import Delivery.Delivery;
import Delivery.DeliveryQueue;
import Drone.Drone;
import Drone.Utilities.Utils;

public class DeliveryManageThread extends Thread{

    private DeliveryQueue deliveryQueue;
    private MasterThread masterThread;
    private Drone masterDrone;
    private boolean quit;

    public DeliveryManageThread(Drone masterDrone, MasterThread masterThread, DeliveryQueue delivery) {
        this.masterThread = masterThread;
        this.deliveryQueue = delivery;
        this.masterDrone = masterDrone;
        this.quit = false;
    }

    @Override
    public void run() {

        while(!quit) {

            Delivery delivery = deliveryQueue.take();
            Utils.printDetail("[DELIVERY MGM] consegna da effettuare ... delivery " + delivery.toString(), 1);

            // il drone è settato non available nella lista del master
            Drone deliveryDrone = masterThread.chooseDeliveryDrone(delivery);

            if(deliveryDrone != null) {
                Utils.printDetail("[DELIVERY MGN] lista dopo aver affidato una delivery \n" + masterThread.getDroneListCopy().toString(), 3);
                DeliveryAssignThread deliveryAssign = new DeliveryAssignThread(delivery, masterDrone, deliveryDrone, masterThread, deliveryQueue);
                deliveryAssign.start();
                Utils.printDetail("[DELIVERY MGM] la consegna " + delivery.getId() + " è assegnata al drone " + deliveryDrone.id + "\n", 1);
            }else{

                Utils.printDetail("[DELIVERY MGM] non ci sono droni disponibili \n", 1);
                deliveryQueue.uniqueInsert(delivery);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }


    }

    public void quit(){
        quit = true;
    }

}
