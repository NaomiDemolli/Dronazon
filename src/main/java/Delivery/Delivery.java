package Delivery;

import Drone.Utilities.Config;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Arrays;
import java.util.Random;

public class Delivery {

    private int id;
    private int[] fromPosition;
    private int[] toPosition;


    public Delivery(int id){
        this.id = id;
        Random r = new Random();
        this.fromPosition = new int[] {r.nextInt(Config.SMARTCITY_DIMENSION),r.nextInt(Config.SMARTCITY_DIMENSION)};
        this.toPosition = new int[] {r.nextInt(Config.SMARTCITY_DIMENSION), r.nextInt(Config.SMARTCITY_DIMENSION)};
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[] getFromPosition() {
        return fromPosition;
    }
    public int getFromPosition(int index){
        return this.fromPosition[index];
    }

    public String getStringFromPos(){
        return "[" + fromPosition[0] + "," + fromPosition[1] + "]";
    }

    public int[] getToPosition() {
        return toPosition;
    }
    public int getToPosition(int index){
        return getToPosition()[index];
    }

    public void setFromPosition(int x, int y){
        fromPosition[0] = x;
        fromPosition[1] = y;
    }

    public void setToPosition(int x, int y){
        toPosition[0] = x;
        toPosition[1] = y;
    }

    @Override
    public String toString() {
        return "id " + id +
                // " - fromPosition=" + Arrays.toString(fromPosition) +
                " in " + Arrays.toString(toPosition);
    }

    public String getStringJSON() throws JSONException {
        JSONObject del = new JSONObject();
        del.put("id", this.id);

        JSONArray fromPos = new JSONArray();
        fromPos.put(this.fromPosition[0]);
        fromPos.put(this.fromPosition[1]);
        del.put("fromPosition", fromPos);

        JSONArray toPos = new JSONArray();
        toPos.put(this.toPosition[0]);
        toPos.put(this.toPosition[1]);
        del.put("toPosition", toPos);

        return del.toString();
    }
}
