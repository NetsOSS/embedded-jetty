package eu.nets.oss.jetty.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/rest")
public class RootResource {

    @GET
    public Response get() {
        return Response.ok().build();
    }

}
