package thredds.server.wms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.Assert;
import org.junit.BeforeClass;
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
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class WmsTest {

    private static final String TEST_DATA_EXPECTED = "test-data/expected/";

    private static ThreddsWmsController wmsController;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
        ServletContext servletContext = new MockServletContext(WmsTest.class
                .getResource("test-data/webapp").toString(),
                new FileSystemResourceLoader());
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                context);
        context.setServletContext(servletContext);
        context.refresh();
        (new TdsConfigContextListener())
                .contextInitialized(new ServletContextEvent(servletContext));
        wmsController = (ThreddsWmsController) context.getBean("wmsController");
        wmsController.init();

    }

    @Test
    public void saveFalse741() throws Exception {

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
        OutputStream out = new FileOutputStream("/tmp/False741.png");
        out.write(servletResponse.getContentAsByteArray());
        out.close();
    }

    @Test
    public void compareFalse741() {

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
        BufferedImage responseImage;
        try {
            wmsController.dispatchWmsRequest(request, new RequestParams(
                    servletRequest.getParameterMap()), servletRequest,
                    servletResponse, new UsageLogEntry(servletRequest));
            responseImage = ImageIO.read(new ByteArrayInputStream(
                    servletResponse.getContentAsByteArray()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BufferedImage expectedImage = readExpected("False741.png");
        BufferedImage falseColourImage = readExpected("FalseColour741.png");
        assertEquivalent(expectedImage, responseImage);
    }

    @Test
    public void selfTest() {
        BufferedImage b7 = readExpected("Band7.png");
        BufferedImage b4 = readExpected("Band4.png");
        BufferedImage b1 = readExpected("Band1.png");
        BufferedImage f741 = readExpected("False741.png");
        BufferedImage f741rgba = readExpected("False741-RGBA.png");
        BufferedImage fc741 = readExpected("FalseColour741.png");
        // every image must be equivalent to itself
        assertEquivalent(b7, b7);
        assertEquivalent(b4, b4);
        assertEquivalent(b1, b1);
        assertEquivalent(f741, f741);
        assertEquivalent(f741rgba, f741rgba);
        assertEquivalent(fc741, fc741);
        // palette and rgba reduced-depth images must be equivalent
        assertEquivalent(f741, f741rgba);
        assertEquivalent(f741rgba, f741);
        // greyscale bands must be different
        assertNotEquivalent(b7, b4);
        assertNotEquivalent(b4, b1);
        assertNotEquivalent(b1, b7);
        // greyscale must be different to reduced-depth false colour
        assertNotEquivalent(b7, f741);
        // palette and rgba reduced-depth images must be different to full-depth
        assertNotEquivalent(f741, fc741);
        assertNotEquivalent(fc741, f741);
        assertNotEquivalent(f741rgba, fc741);
        assertNotEquivalent(fc741, f741rgba);
    }

    private static BufferedImage readExpected(String imageFileName) {
        try {
            return ImageIO.read(WmsTest.class.getResource(TEST_DATA_EXPECTED
                    + imageFileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertEquivalent(BufferedImage a, BufferedImage b) {
        Assert.assertTrue(isEquivalent(a, b));
    }

    private static void assertNotEquivalent(BufferedImage a, BufferedImage b) {
        Assert.assertFalse(isEquivalent(a, b));
    }

    private static boolean isEquivalent(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        int width = a.getWidth();
        int height = a.getHeight();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (a.getRGB(i, j) != b.getRGB(i, j)) {
                    return false;
                }
            }
        }
        return true;
    }

}
