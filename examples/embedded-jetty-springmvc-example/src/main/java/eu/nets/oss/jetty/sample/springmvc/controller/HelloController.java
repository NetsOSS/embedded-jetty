package eu.nets.oss.jetty.sample.springmvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HelloController {

    @RequestMapping("/")
        public ModelAndView index() {
            return new ModelAndView("index");
        }

    @RequestMapping("/hello.htm")
    public ModelAndView welcomeHandler() {
        return new ModelAndView("hello");
    }

}
