package eu.nets.oss.jetty.embedded;

import org.junit.Test;

import eu.nets.oss.jetty.embedded.PropertiesFileConfig;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class PropertiesFileConfigTest {
    @Test
    public void testGetContextPath() throws Exception {
        PropertiesFileConfig propertiesFileConfig = new PropertiesFileConfig();
        String contextPath = propertiesFileConfig.getContextPath();
        assertEquals("abc", contextPath);
    }
}
