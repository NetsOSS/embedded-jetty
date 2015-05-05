package eu.nets.oss.jetty.sample;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointExceptionResolver;
import org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.server.endpoint.SimpleSoapExceptionResolver;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings({"SpringJavaAutowiringInspection", "UnusedDeclaration"})
@Configuration
public class WsServletConfiguration {

    @Bean
    public PayloadRootAnnotationMethodEndpointMapping payloadRootAnnotationMethodEndpointMapping() {
        return new PayloadRootAnnotationMethodEndpointMapping();
    }

    @Bean
    public HelloEndpoint helloEndpoint(){
       return new HelloEndpoint();
    }

    @Bean
    public EndpointExceptionResolver exceptionResolver(){
        return new SimpleSoapExceptionResolver(){
            @Override
            protected void customizeFault(MessageContext messageContext, Object endpoint, Exception ex, SoapFault fault) {
                if (!(ex instanceof IllegalArgumentException)){
                    messageContext.clearResponse();
                    SoapMessage response = (SoapMessage) messageContext.getResponse();
                    String faultString = StringUtils.hasLength(ex.getMessage()) ? ex.getMessage() : ex.toString();
                    SoapBody body = response.getSoapBody();
                    body.addServerOrReceiverFault("General server failure", getLocale());
                }
            }
        };
    }
}
