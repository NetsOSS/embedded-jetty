package eu.nets.oss.jetty.sample;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;


/**
 * @author Kristian Rosenvold
 */
@Endpoint
public class HelloEndpoint {
    private static final String NAMESPACE_URI = "http://nets.eu/helloService";


    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "HelloRequest")
    @ResponsePayload
    public String registerCoupon(@RequestPayload String helloRequest)
            throws Exception {
        return "Yo";
    }
}
