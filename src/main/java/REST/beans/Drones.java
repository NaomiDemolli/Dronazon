package REST.beans;

import Drone.Utilities.Config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Random;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class Drones {

    @XmlElement(name="drones_list")
    private ArrayList<Drone> drones;
    private static Drones instance;

    private Drones(){
        drones = new ArrayList<Drone>();
    }

    public synchronized static Drones getInstance(){
        if(instance==null)
            instance = new Drones();
        return instance;
    }

    public synchronized ArrayList<Drone> getDronesList() {
        return new ArrayList<>(this.drones);
    }

    public synchronized InitResponse add(Drone d) {
        for(Drone x: this.drones) {
            if(x.getId() == d.getId()) return null;
        }

        int[] position = RandomPosition();
        this.drones.add(d);

        return new InitResponse(this, position);
    }

    private int[] RandomPosition(){
        Random random = new Random();
        int x = random.nextInt(Config.SMARTCITY_DIMENSION);
        int y = random.nextInt(Config.SMARTCITY_DIMENSION);
        return new int[]{x,y};
    }

    public synchronized Drone getDroneByID(int id) {
        for(Drone drone: this.drones)
            if(drone.getId() == id) return drone;
        return null;
    }

    public synchronized void deleteById(int id) {
        this.drones.removeIf(drone -> drone.getId() == id);
    }

    public int size(){
        return drones.size();
    }

    public Drone get(int index){
        return drones.get(index);
    }

}
