package eu.nets.oss.jetty.sample;

/**
 * @author Kristian Rosenvold
 */
public class DefaultHelloService implements HelloService {

    public String sayHello(String name) {
        if (name == null) {
            return "Hello, Stranger!";
        }
        return "Hello, " + name;
    }
}
