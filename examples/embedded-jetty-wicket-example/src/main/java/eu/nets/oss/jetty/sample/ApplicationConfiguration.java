package eu.nets.oss.jetty.sample;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;

/**
 * A spring javaconfig sample context
 * @author Kristian Rosenvold
 */
@Configuration
public class ApplicationConfiguration {

    @Lazy
    @Bean
    public SampleWicketApplication getWicketApplication(ApplicationContext applicationContext) {
        return new SampleWicketApplication(applicationContext);
    }

    @Bean
    public SimpleWsdl11Definition helloServiceWsdl() {
        return new SimpleWsdl11Definition(new ClassPathResource("myService.wsdl"));
    }

    @Bean
    public HelloService helloService(){
        return new DefaultHelloService();
    }
}
