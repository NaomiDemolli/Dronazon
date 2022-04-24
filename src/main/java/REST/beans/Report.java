package REST.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Report {

    private int id;
    private int distance;
    private int pollution;
    private int battery;
    private int timestamp;
    private int delivery;

    public Report(){}

    @Override
    public String toString() {
        return "REPORT: " + "id: " + id + " distance: " + distance + " pollution: " + pollution +
                " battery: " + battery + " timestamp: " + timestamp + " delivery: " + delivery;
    }

    public double getDistance() {
        return distance;
    }

    public double getPollution() {
        return pollution;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setPollution(int pollution) {
        this.pollution = pollution;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDelivery() {
        return delivery;
    }

    public void setDelivery(int delivery) {
        this.delivery = delivery;
    }
}
