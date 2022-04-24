package REST.services;

import REST.beans.Report;
import REST.beans.Reports;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("/reportsadmin")
public class ReportsService {

    @GET
    @Produces("application/json")
    public Response getReport(){
        return Response.ok(Reports.getInstance()).build();
    }

    @POST
    @Path("add")
    @Consumes("application/json")
    @Produces("application/json")
    public Response addDrone(Report report) {;
        Reports.getInstance().add(report);
        return Response.ok().build();
    }

    @GET
    @Path("rep/{n}")
    @Produces("application/json")
    public Response getNReport(@PathParam("n") int n){
        ArrayList<Report> rep = Reports.getInstance().getFirstN(n);
        return Response.ok(rep).build();
    }

    @GET
    @Path("delivery/{t1}-{t2}")
    @Produces("application/json")
    public Response getDelivery(@PathParam("t1") int t1, @PathParam("t2") int t2){
        return Response.ok(Reports.getInstance().numDelivery(t1, t2)).build();
    }

    @GET
    @Path("kilometers/{t1}-{t2}")
    @Produces("application/json")
    public Response getKilo(@PathParam("t1") int t1, @PathParam("t2") int t2){
        return Response.ok(Reports.getInstance().numKilo(t1, t2)).build();
    }


}
