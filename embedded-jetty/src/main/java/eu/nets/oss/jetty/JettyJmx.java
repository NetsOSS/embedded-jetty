package eu.nets.oss.jetty;

import java.lang.management.ManagementFactory;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;

/**
 * Utility class to export Jetty's MBeans to the platform MBean server.
 *
 * This is in a separate class to prevent the builder from loading and thus requiring the classes.
 */
class JettyJmx {
    public static void exportMBeans(Server server) {
        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addEventListener(mbContainer);
        server.addBean(mbContainer);
    }
}
