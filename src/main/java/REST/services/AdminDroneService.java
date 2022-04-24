package REST.services;

import REST.beans.Drone;
import REST.beans.Drones;
import REST.beans.InitResponse;
import Drone.Utilities.Utils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/droneadmin")
public class AdminDroneService {

    @GET
    @Produces("application/json")
    public Response getList() {
        return Response.ok(Drones.getInstance()).build();
    }

    @GET
    @Path("{id}")
    @Produces("application/json")
    public Response getDroneById(@PathParam("id") int id) {
        Drone drone = Drones.getInstance().getDroneByID(id);
        if(drone != null) return Response.ok(drone).build();
        else return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("add")
    @Consumes("application/json")
    @Produces("application/json")
    public Response addDrone(Drone drone) {;
        InitResponse ir = Drones.getInstance().add(drone);
        if(ir != null) {
            return Response.ok(ir).build();
        } else {
            Utils.printDetail("[DRONESERVICE] drone " + drone.getId() + " non aggiunto alla lista", 3);
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @DELETE
    @Path("delete/{id}")
    @Consumes("application/json")
    public Response deleteDrone(@PathParam("id") int id) {
        Utils.printDetail("Richiesta per cancellare il drone " + id, 1);
        Drones.getInstance().deleteById(id);

        return Response.ok().build();
    }

}
