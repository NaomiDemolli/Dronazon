package Delivery;

import Drone.Utilities.Config;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Dronazon {

    private static int numDelivery = 1;

    public static void main(String[] args) {

        MqttClient client;
        String clientId = MqttClient.generateClientId();
        int qos = 2;

        try {
            client = new MqttClient(Config.BROKER_MQTT, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.connect(connOpts);

            while (true) {
                Delivery delivery = new Delivery(numDelivery);
                MqttMessage message = new MqttMessage(delivery.getStringJSON().getBytes());
                message.setQos(qos);

                System.out.println("[DRONAZON] Publishing message: " + delivery+ " ...");
                client.publish(Config.TOPIC, message);
                numDelivery++;
                Thread.sleep(5000);
            }


        } catch (MqttException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
