package REST;


import Drone.Utilities.Config;
import Drone.Utilities.Utils;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;

public class ServerAmministratore {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServerFactory.create(Config.ADMINSERVER_ADDRESS +"/");
        server.start();

        Utils.printDetail("[SERVER AMMINISTRATORE] " + "Server running on "+ Config.ADMINSERVER_ADDRESS, 1);
    }

}
