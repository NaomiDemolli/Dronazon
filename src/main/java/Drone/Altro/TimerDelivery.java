package Drone.Altro;

public class TimerDelivery {

    private long start;
    private long delay;

    public TimerDelivery(long delay, long start){
        this.delay = delay;
        this.start = System.currentTimeMillis();
    }

    public void setStart(long startUpdate) {
        start = startUpdate;
    }

    public boolean isExpired(){
        return (System.currentTimeMillis() - this.start) > this.delay;
    }
}
