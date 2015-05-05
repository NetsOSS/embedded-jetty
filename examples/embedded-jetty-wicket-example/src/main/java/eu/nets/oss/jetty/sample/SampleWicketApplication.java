package eu.nets.oss.jetty.sample;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebResponse;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.settings.IApplicationSettings;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.time.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

@Component
public class SampleWicketApplication extends WebApplication {
    private final ApplicationContext applicationContext;

    protected WebResponse newWebResponse(final WebRequest webRequest, final HttpServletResponse httpServletResponse){
        return new ServletWebResponse((ServletWebRequest)webRequest, httpServletResponse) {
            @Override
            public String encodeURL(CharSequence url) {
                return url.toString();
            }

            @Override
            public String encodeRedirectURL(CharSequence url) {
                return url.toString();
            }
        };
    }

    @Inject
    public SampleWicketApplication(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }

    @Override
    public void init() {
        getComponentInstantiationListeners().add(new SpringComponentInjector(this, applicationContext));
        getMarkupSettings().setStripWicketTags(true);
        getDebugSettings().setAjaxDebugModeEnabled(false);
        mountPage("homePage", HomePage.class);
        IApplicationSettings settings = getApplicationSettings();
        settings.setPageExpiredErrorPage(HomePage.class);
        IResourceSettings iResourceSettings = getResourceSettings();
        iResourceSettings.setResourcePollFrequency( Duration.ONE_SECOND );
    }
}
