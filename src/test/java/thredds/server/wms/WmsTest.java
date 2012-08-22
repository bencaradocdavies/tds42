package thredds.server.wms;

import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.Test;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import thredds.server.config.TdsConfigContextListener;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;

/**
 * @author car605
 */
public class WmsTest {

    @Test
    public void wms() throws Exception {
        ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
        // context.setConfigLocation(WmsTestSupport.class.getResource(
        // "applicationContext.xml").toString());
        ServletContext servletContext = new MockServletContext(WmsTest.class
                .getResource("test-data/webapp").toString(),
                new FileSystemResourceLoader());
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                context);
        context.setServletContext(servletContext);
        context.refresh();
        // ((TdsContext) context.getBean("tdsContext")).init(servletContext);
        // ((DataRootHandler) context.getBean("tdsDRH")).init();
        // System.err.println(context.getBean("serverInfo"));
        // System.err.println(((TdsContext) context.getBean("tdsContext"))
        // .getWmsConfig().isAllow());
        // System.err.println(((TdsContext) context.getBean("tdsContext"))
        // .getWmsConfig().getMaxImageHeight());
        // System.err.println(((TdsContext) context.getBean("tdsContext"))
        // .getTdsConfigFileName());
        // System.err.println(context.getBean("wmsController"));

        (new TdsConfigContextListener())
                .contextInitialized(new ServletContextEvent(servletContext));

        ThreddsWmsController wmsController = (ThreddsWmsController) context
                .getBean("wmsController");

        wmsController.init();

        String request = "GetMap";
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "GET", "/thredds/wms/laamu/laamu.nc");
        servletRequest.setPathInfo("laamu/laamu.nc");
        // servletRequest.addParameter("layers", "Band1");
        servletRequest.addParameter("layers", "False741");
        servletRequest.addParameter("elevation", "0");
        servletRequest.addParameter("time", "1970-01-01T00:00:00");
        servletRequest.addParameter("transparent", "true");
        // servletRequest.addParameter("styles", "boxfill/greyscale");
        servletRequest.addParameter("styles", "boxfill/rgb252");
        servletRequest.addParameter("crs", "EPSG:4326");
        // servletRequest.addParameter("colorscalerange", "1,255");
        servletRequest.addParameter("colorscalerange", "0.5,252.5");
        servletRequest.addParameter("numcolorbands", "252");
        servletRequest.addParameter("logscale", "false");
        servletRequest.addParameter("service", "WMS");
        servletRequest.addParameter("version", "1.3.0");
        servletRequest.addParameter("request", "GetMap");
        servletRequest.addParameter("exceptions", "XML");
        servletRequest.addParameter("format", "image/png");
        servletRequest.addParameter("bbox", "73.35,1.77,73.42,1.84");
        servletRequest.addParameter("width", "512");
        servletRequest.addParameter("height", "512");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        wmsController.dispatchWmsRequest(request, new RequestParams(
                servletRequest.getParameterMap()), servletRequest,
                servletResponse, new UsageLogEntry(servletRequest));
        OutputStream out = new FileOutputStream("/tmp/ncwms.png");
        out.write(servletResponse.getContentAsByteArray());
        out.close();
    }

}
