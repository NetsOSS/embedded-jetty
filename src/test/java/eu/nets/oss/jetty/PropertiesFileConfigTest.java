package eu.nets.oss.jetty;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Kristian Rosenvold
 */
public class PropertiesFileConfigTest {

    private PropertiesFileConfig propertiesFileConfig;

    @Before
    public void setUp() throws Exception {
        propertiesFileConfig = new PropertiesFileConfig();
    }

    @Test
    public void testGetContextPath() throws Exception {
        String contextPath = propertiesFileConfig.getContextPath();
        assertEquals("abc", contextPath);
    }

    @Test
    public void whiteListSplitter() throws Exception {
        System.setProperty("ipWhiteList", "192.168.1.1,10.10.10.10");
        Iterable<String> ipWhiteList = propertiesFileConfig.getIpWhiteList();
        Assert.assertThat(ipWhiteList, hasItems("192.168.1.1", "10.10.10.10"));
    }

    @Test
    public void thatEmptyWhiteListGivesEmptyList() throws Exception {
        System.clearProperty("ipWhiteList");
        Iterable<String> ipWhiteList = propertiesFileConfig.getIpWhiteList();
        assertThat(ipWhiteList, notNullValue());
        assertThat(ipWhiteList.iterator().hasNext(), is(false));
    }
}
