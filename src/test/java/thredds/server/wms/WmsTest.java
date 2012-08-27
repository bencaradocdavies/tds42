package thredds.server.wms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

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
 * Test Thredds / ncWMS against Laamu Atoll subset netCDF data.
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class WmsTest {

    /**
     * Request styles for greyscale bands.
     */
    private static final String GREYSCALE_STYLES = "boxfill/greyscale";

    /**
     * Request styles for False741.
     */
    private static final String FALSE_STYLES = "boxfill/rgb252";

    /**
     * Request colorscalerange for greyscale bands.
     */
    private static final String GREYSCALE_COLORSCALERANGE = "1,255";

    /**
     * Request colorscalerange for False741.
     */
    private static final String FALSE_COLORSCALERANGE = "0.5,252.5";

    /**
     * Request bbox.
     */
    private static final String BBOX = "73.35,1.77,73.42,1.84";

    /**
     * Relative path to directory containing expected WMS response images.
     */
    private static final String TEST_DATA_EXPECTED = "test-data/expected/";

    /**
     * Singleton WMS controller used for this test fixture.
     */
    private static ThreddsWmsController wmsController;

    /**
     * Set up the singleton test fixture. As this is a read-only test, this can
     * be shared between test methods.
     */
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
        ServletContext servletContext = new MockServletContext(WmsTest.class
                .getResource("test-data/webapp").toString(),
                new FileSystemResourceLoader());
        // Required because Thredds implementation examines its own context.
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                context);
        context.setServletContext(servletContext);
        context.refresh();
        (new TdsConfigContextListener())
                .contextInitialized(new ServletContextEvent(servletContext));
        wmsController = (ThreddsWmsController) context.getBean("wmsController");
        // Do not know why this is needed. Spring does not call this method.
        wmsController.init();
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
    private static void assertEquivalent(BufferedImage a, BufferedImage b) {
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
    private static void assertNotEquivalent(BufferedImage a, BufferedImage b) {
        Assert.assertFalse(isEquivalent(a, b));
    }

    /**
     * Read an expected response image from the test data set.
     * 
     * @param imageFileName
     *            base file name of image (without directory part)
     * @return expected image.
     */
    private static BufferedImage readExpected(String imageFileName) {
        try {
            return ImageIO.read(WmsTest.class.getResource(TEST_DATA_EXPECTED
                    + imageFileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MockHttpServletRequest buildGetMapRequest(String layers,
            String styles, String colorScaleRange, String bbox) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "GET", "/thredds/wms/laamu/laamu.nc");
        servletRequest.setPathInfo("laamu/laamu.nc");
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

    private void runTest(String layers, String styles, String colorScaleRange,
            String bbox, String expectedImageFileName) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BufferedImage expectedImage = readExpected(expectedImageFileName);
        assertEquivalent(expectedImage, responseImage);

    }

    /**
     * Test image loading and comparison with the expected response images from
     * the test data set.
     */
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

    /**
     * Test WMS GetMap request for Band1.
     */
    @Test
    public void getMapBand1() {
        runTest("Band1", GREYSCALE_STYLES, GREYSCALE_COLORSCALERANGE, BBOX,
                "Band1.png");
    }

    /**
     * Test WMS GetMap request for Band4.
     */
    @Test
    public void getMapBand4() {
        runTest("Band4", GREYSCALE_STYLES, GREYSCALE_COLORSCALERANGE, BBOX,
                "Band4.png");
    }

    /**
     * Test WMS GetMap request for Band7.
     */
    @Test
    public void getMapBand7() {
        runTest("Band7", GREYSCALE_STYLES, GREYSCALE_COLORSCALERANGE, BBOX,
                "Band7.png");
    }

    /**
     * Test WMS GetMap request for False741.
     */
    @Test
    public void getMapFalse741() {
        runTest("False741", FALSE_STYLES, FALSE_COLORSCALERANGE, BBOX,
                "False741.png");
    }

    /**
     * Test WMS GetMap request for FalseColour741. The styles and
     * colourscalerange parameters will be ignored.
     */
    @Test
    public void getMapFalseColour741() {
        runTest("FalseColour741", FALSE_STYLES, FALSE_COLORSCALERANGE, BBOX,
                "FalseColour741.png");
    }

}
