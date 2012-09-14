package thredds.server.wms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.Assert;
import org.junit.BeforeClass;
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
 * Base class for Thredds WMS tests.
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public abstract class WmsTestSupport {

    /**
     * Singleton WMS controller used for this test fixture.
     */
    protected static ThreddsWmsController wmsController;

    /**
     * Set up the singleton test fixture. As this is a read-only test, this can
     * be shared between test methods and even test classes. The first test
     * class to be run will configure the shared fixture.
     */
    @BeforeClass
    public static synchronized void oneTimeSetUp() throws Exception {
        if (wmsController == null) {
            ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
            ServletContext servletContext = new MockServletContext(
                    WmsTestSupport.class.getResource("test-data/webapp")
                            .toString(), new FileSystemResourceLoader());
            // Required because Thredds implementation examines its own context.
            servletContext
                    .setAttribute(
                            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                            context);
            context.setServletContext(servletContext);
            context.refresh();
            (new TdsConfigContextListener())
                    .contextInitialized(new ServletContextEvent(servletContext));
            wmsController = (ThreddsWmsController) context
                    .getBean("wmsController");
            // Do not know why this is needed. Spring does not call this method.
            wmsController.init();
        }
    }

    /**
     * Return true if and only if two images have the same size and
     * corresponding pixels have the same RGBA integer pixel values. Even if two
     * images have different colour models, they are equivalent if corresponding
     * pixels have the same colour.
     * 
     * @param a
     *            first image
     * @param b
     *            second image
     * @return true if and only if the images are equivalent
     */
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

    /**
     * Assert that two images are equivalent. See
     * {@link #isEquivalent(BufferedImage, BufferedImage)}.
     * 
     * @param a
     *            first image
     * @param b
     *            second image
     */
    protected static void assertEquivalent(BufferedImage a, BufferedImage b) {
        Assert.assertTrue(isEquivalent(a, b));
    }

    /**
     * Assert that two images are not equivalent. See
     * {@link #isEquivalent(BufferedImage, BufferedImage)}.
     * 
     * @param a
     *            first image
     * @param b
     *            second image
     */
    protected static void assertNotEquivalent(BufferedImage a, BufferedImage b) {
        Assert.assertFalse(isEquivalent(a, b));
    }

    /**
     * Read an expected response image from the test data set.
     * 
     * @param imageFilename
     *            base filename of image (without directory part)
     * @return expected image.
     */
    protected BufferedImage readExpected(String imageFilename) {
        try {
            return ImageIO.read(LaamuWmsTest.class
                    .getResource(testDataExpectedDirectory + File.separator
                            + imageFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * servlet request URI path component.
     */
    private final String servletRequestUri;

    /**
     * Servlet request URI path component with the servlet context removed.
     */
    private final String servletRequestPathInfo;

    /**
     * Relative path to the classpath directory containing expected response
     * images.
     */
    private final String testDataExpectedDirectory;

    /**
     * Filesystem path to the location where response images should be saved, or
     * null if none. Usefule for bootstrapping or debugging
     */
    private final String responseSaveDirectory;

    /**
     * Constructor.
     * 
     * @param servletRequestUri
     *            servlet request URI path component
     * @param servletRequestPathInfo
     *            servlet request URI path component with the servlet context
     *            removed
     * @param testDataExpectedDirectory
     *            relative path to the classpath directory containing expected
     *            response images
     * @param responseSaveDirectory
     *            filesystem path to the location where response images should
     *            be saved, or null if none
     */
    public WmsTestSupport(String servletRequestUri,
            String servletRequestPathInfo, String testDataExpectedDirectory,
            String responseSaveDirectory) {
        this.servletRequestUri = servletRequestUri;
        this.servletRequestPathInfo = servletRequestPathInfo;
        this.testDataExpectedDirectory = testDataExpectedDirectory;
        this.responseSaveDirectory = responseSaveDirectory;
    }

    /**
     * Build a GetCapabilities servlet request.
     * 
     * @return servlet request
     */
    protected MockHttpServletRequest buildGetCapabilitiesRequest() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "GET", servletRequestUri);
        servletRequest.setPathInfo(servletRequestPathInfo);
        servletRequest.addParameter("service", "WMS");
        servletRequest.addParameter("version", "1.3.0");
        servletRequest.addParameter("request", "GetCapabilities");
        return servletRequest;
    }

    /**
     * Build a GetMap servlet request.
     * 
     * @param layers
     *            request layers
     * @param styles
     *            request styles
     * @param colorScaleRange
     *            request colorscalerange
     * @param bbox
     *            request bbox
     * @param expectedImageFilename
     *            filename of expected response image (without directory part)
     * @return servlet request
     */
    private MockHttpServletRequest buildGetMapRequest(String layers,
            String styles, String colorScaleRange, String bbox) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "GET", servletRequestUri);
        servletRequest.setPathInfo(servletRequestPathInfo);
        servletRequest.addParameter("layers", layers);
        servletRequest.addParameter("elevation", "0");
        servletRequest.addParameter("time", "1970-01-01T00:00:00");
        servletRequest.addParameter("transparent", "true");
        servletRequest.addParameter("styles", styles);
        servletRequest.addParameter("crs", "EPSG:4326");
        servletRequest.addParameter("colorscalerange", colorScaleRange);
        servletRequest.addParameter("numcolorbands", "252");
        servletRequest.addParameter("logscale", "false");
        servletRequest.addParameter("service", "WMS");
        servletRequest.addParameter("version", "1.3.0");
        servletRequest.addParameter("request", "GetMap");
        servletRequest.addParameter("exceptions", "XML");
        servletRequest.addParameter("format", "image/png");
        servletRequest.addParameter("bbox", bbox);
        servletRequest.addParameter("width", "512");
        servletRequest.addParameter("height", "512");
        return servletRequest;
    }

    /**
     * Run a WMS GetMap test.
     * 
     * @param layers
     *            request layers
     * @param styles
     *            request styles
     * @param colorScaleRange
     *            request colorscalerange
     * @param bbox
     *            request bbox
     * @param expectedImageFilename
     *            filename of expected response image (without directory part)
     * @param responseSaveDirectory
     *            directory in which to save the response image, or null to not
     *            save
     */
    protected void runGetMapTest(String layers, String styles,
            String colorScaleRange, String bbox, String expectedImageFilename) {
        String request = "GetMap";
        MockHttpServletRequest servletRequest = buildGetMapRequest(layers,
                styles, colorScaleRange, bbox);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        BufferedImage responseImage;
        try {
            wmsController.dispatchWmsRequest(request, new RequestParams(
                    servletRequest.getParameterMap()), servletRequest,
                    servletResponse, new UsageLogEntry(servletRequest));
            responseImage = ImageIO.read(new ByteArrayInputStream(
                    servletResponse.getContentAsByteArray()));
            if (responseSaveDirectory != null) {
                OutputStream out = new FileOutputStream(responseSaveDirectory
                        + File.separator + expectedImageFilename);
                out.write(servletResponse.getContentAsByteArray());
                out.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BufferedImage expectedImage = readExpected(expectedImageFilename);
        assertEquivalent(expectedImage, responseImage);
    }

}