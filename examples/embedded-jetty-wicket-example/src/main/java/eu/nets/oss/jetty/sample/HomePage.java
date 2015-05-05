package eu.nets.oss.jetty.sample;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * @author Kristian Rosenvold
 */
public class HomePage extends WebPage {

    @SpringBean
    private HelloService helloService;

    public HomePage() {
        helloService.sayHello("Til deg");
    }
}
