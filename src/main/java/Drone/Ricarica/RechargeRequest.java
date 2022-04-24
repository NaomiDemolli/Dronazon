package Drone.Ricarica;

public class RechargeRequest{

    private int id;
    private String ip;
    private int port;
    private long timestamp;

    public RechargeRequest(int id, String ip, int port, long timestamp){
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.timestamp = timestamp;
    }

    public long getTimestamp(){
        return timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
