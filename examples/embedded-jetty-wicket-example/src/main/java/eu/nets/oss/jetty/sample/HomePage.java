package eu.nets.oss.jetty.sample;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * @author Kristian Rosenvold
 */
public class HomePage extends WebPage {

    @SpringBean
    private HelloService helloService;

    private String message = "Initial Hello";

    public HomePage() {

        add(new Link("helloButton") {

            @Override
            public void onClick() {
                HomePage.this.message = helloService.sayHello("Til deg");
            }
        });

        add(new Label("label", new Model<String>() {
            public String getObject() {
                return message;
            }
        }));
    }
}
