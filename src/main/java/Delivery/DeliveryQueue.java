package Delivery;

import java.util.ArrayList;

public class DeliveryQueue {

    public ArrayList<Delivery> buffer = new ArrayList<Delivery>();

    public synchronized void put(Delivery delivery) {
        buffer.add(delivery);
        notify();
    }

    public synchronized Delivery take() {

        Delivery delivery = null;

        while(buffer.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(buffer.size()>0){
            delivery = buffer.get(0);
            buffer.remove(0);
        }


        return delivery;
    }

    public synchronized int size(){
        return buffer.size();
    }

    public synchronized ArrayList<Delivery> getQueueCopy(){
        return new ArrayList<Delivery>(buffer);
    }

    public void uniqueInsert(Delivery delivery){
        ArrayList<Delivery> queueCopy = getQueueCopy();
        if(!queueCopy.contains(delivery)){
            this.put(delivery);
        }
    }

    @Override
    public String toString() {
        return "DeliveryQueue{" +
                "buffer=" + buffer.toString() +
                '}';
    }
}
