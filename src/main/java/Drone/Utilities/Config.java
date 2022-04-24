package Drone.Utilities;

public class Config {

    // DEBUGGING
    public static final int ERROR_DETAIL = 1;
    public static final char ERROR_CONTEXT = 'r';

    //smart city
    public static final int SMARTCITY_DIMENSION = 10;

    // server amministratore
    public static final String ADMINSERVER_ADDRESS = "http://localhost:1337";

    public static final String BROKER_MQTT = "tcp://localhost:1883";
    public static final String TOPIC = "dronazon/smartcity/orders";

    // master thread
    public static final double MAXIMUM_DISTANCE = SMARTCITY_DIMENSION * Math.sqrt(2);

    public static final int DECREASE_BATTERY = 15;

    // timer
    public static final int SLEEPTIME_MONITOR_REPORT = 10000;
    public static final int SLEEPTIME_PING_INIT = 20000;
    public static final int SLEEPTIME_PING_DELIVERY = 10000;
    public static final int SLEEPTIME_PRINT_INFO = 20000;


}
