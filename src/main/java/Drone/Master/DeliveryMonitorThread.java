package Drone.Master;

import Delivery.Delivery;
import Delivery.DeliveryQueue;
import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.*;

public class DeliveryMonitorThread extends Thread{

    private MasterThread masterThread;
    private DeliveryQueue deliveryQueue;
    private MqttClient client;

    public DeliveryMonitorThread(MasterThread mt, DeliveryQueue dq ){
        this.masterThread = mt;
        this.deliveryQueue = dq;
    }

    @Override
    public void run() {

        String clientId = MqttClient.generateClientId();
        int qos = 2;

        try {
            client = new MqttClient(Config.BROKER_MQTT, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.connect(connOpts);

            client.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) throws JSONException {
                    String receivedMessage = new String(message.getPayload());
                    Delivery delivery = fromJSONtoDel(receivedMessage);
                    masterThread.increaseDeliveryCounter();
                    deliveryQueue.put(delivery);
                }

                public void connectionLost(Throwable cause) {
                    System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.subscribe(Config.TOPIC,qos);

        } catch (MqttException me) {
            me.printStackTrace();
        }
    }

    private Delivery fromJSONtoDel(String message) throws JSONException {

        JSONObject jsonDel = new JSONObject(message);
        JSONArray fromPos = jsonDel.getJSONArray("fromPosition");
        JSONArray toPos = jsonDel.getJSONArray("toPosition");

        Delivery delivery = new Delivery((int)jsonDel.get("id"));
        delivery.setFromPosition((int) fromPos.get(0), (int) fromPos.get(1));
        delivery.setToPosition((int) toPos.get(0), (int) toPos.get(1));

        return delivery;
    }

    public void quit(){
        try {
            Utils.printDetail("Il client mqtt si sta disconnettendo ", 1);
            client.disconnect();
        } catch (MqttException e) {
            Utils.printDetail("Errore nella disconnessione Mqtt", 2);
        }
    }

}
