package eu.nets.oss.jetty;

import org.junit.Test;

import eu.nets.oss.jetty.PropertiesFileConfig;

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
