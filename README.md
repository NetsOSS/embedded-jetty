Embedded Jetty
==============
![travis-ci status](https://travis-ci.org/NetsOSS/embedded-jetty.svg?branch=master)

Provides a simple way of building embedded Jetty instances that allows for web.xml-free startup. The resulting application is identical within your IDE and your actual application deployment environment.

How to use
----------

The project consists of the following modules:

| Module                    | Description |
| --------------------------|-------------|
| embedded-jetty_9.3        | The core module - this is what you need to deliver plain Servlet or JSP webapps |
| embedded-jetty-spring_9.3 | Extends the core with Spring (4.1) bootstrappers |
| embedded-jetty-wicket_9.3 | Extends the Spring module with Wicket (6) bootstrappers |
| examples                  | Examples |

Documentation
-------------

The project Wiki serves as an additional source of information. See https://github.com/NetsOSS/embedded-jetty/wiki.

