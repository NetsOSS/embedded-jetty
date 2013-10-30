package eu.nets.oss.jetty;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

/**
 * Mounts a given folder as resource content
 *
 * @author Kristian Rosenvold
 */
public class ClasspathResourceHandler extends ResourceHandler {

    private final boolean cacheContent;
    private final String classPathFolder;

    public ClasspathResourceHandler(String resourceFolder, boolean useCaches) {
        this.cacheContent = useCaches;
        if (resourceFolder == null) throw new IllegalArgumentException("resourceFolder cannot be null");
        if (!resourceFolder.startsWith("/")) throw new IllegalArgumentException("resourceFolder must start with /");
        if (resourceFolder.length() < 2) {
            throw new IllegalArgumentException("resourceFolder must point to a subdirectory, or you will expose your entire classpath as http resources");
        }
        this.classPathFolder = resourceFolder;
        if (!useCaches){
            setMaxContentLength();
        }
    }

	private void setMaxContentLength() {
		try {
			Method setMinMemoryMappedContentLength;
			setMinMemoryMappedContentLength = ResourceHandler.class.getMethod("setMinMemoryMappedContentLength", int.class);
			if (setMinMemoryMappedContentLength != null) {
				setMinMemoryMappedContentLength.invoke(this, Integer.MAX_VALUE);
			}
		} catch (NoSuchMethodException ignore) {
		} catch (InvocationTargetException ignored) {
		} catch (IllegalAccessException ignored) {
		}
	}

    @Override
    public Resource getResource(String path) throws MalformedURLException {
        return Resource.newClassPathResource(classPathFolder + path, cacheContent, false);
    }
}
