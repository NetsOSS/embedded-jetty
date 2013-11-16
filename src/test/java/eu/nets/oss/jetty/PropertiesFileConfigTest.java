package eu.nets.oss.jetty;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
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

    @Test
    public void whiteListSplitter() throws Exception {
        System.setProperty("ipWhiteList", "192.168.1.1,10.10.10.10");
        PropertiesFileConfig propertiesFileConfig = new PropertiesFileConfig();
        Iterable<String> ipWhiteList = propertiesFileConfig.getIpWhiteList();
        Assert.assertThat(ipWhiteList, hasItems("192.168.1.1", "10.10.10.10"));
    }
}
