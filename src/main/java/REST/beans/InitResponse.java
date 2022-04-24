package REST.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// La POST(add) mi deve restituire la lista dei droni e la posizione di partenza del drone decisa dal server
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class InitResponse {

    Drones drones;
    int[] position;

    public InitResponse() {}

    public InitResponse(Drones drones, int[] position) {
        this.drones = drones;
        this.position = position;
    }

    //GETTER and SETTER

    public Drones getDrones() {
        return drones;
    }

    public void setDrones(Drones drones) {
        this.drones = drones;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }
}
