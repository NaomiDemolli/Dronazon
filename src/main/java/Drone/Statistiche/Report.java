package Drone.Statistiche;

public class Report implements Comparable<Report>{

    private int id; // id del delivery drone
    private double distance;
    private double pollution;
    private int battery;
    private long timestamp;

    public Report(int id, int battery, double distance, double pollution, long timestamp){
        this.id  = id;
        this.battery = battery;
        this.distance = distance;
        this.pollution = pollution;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getPollution() {
        return pollution;
    }

    public void setPollution(double pollution) {
        this.pollution = pollution;
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

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return  "droneId= " + id +
                ", distance= " + distance +
                ", pollution= " + pollution +
                ", battery= " + battery +
                ", timestamp= " + timestamp;
    }

    @Override
    public int compareTo(Report other) {
        if(this.getTimestamp() - other.getTimestamp() > 0) return -1; // il report in oggetto è più recente del report other
        else return 1;
    }
}
