package Drone.Ricarica;

import Drone.Utilities.Utils;

import java.util.ArrayList;

public class RechargeQueue {

    public ArrayList<RechargeRequest> buffer = new ArrayList<>();

    public synchronized void put(RechargeRequest req) {

        boolean isAlready = false;
        for(RechargeRequest rechargeRequest: buffer){
            if(rechargeRequest.getId() == req.getId()){
                isAlready = true;
                break;
            }
        }

        if(!isAlready){
            buffer.add(req);
        }else{
            Utils.printDetail("[RECHARGE QUEUE] ricevuta richiesta duplicata dal drone " + req.getId(), 1);
        }
    }

    public synchronized ArrayList<RechargeRequest> readAllAndClean() {
        ArrayList<RechargeRequest> l = new ArrayList<>(buffer);
        buffer.clear();
        return l;
    }

}
