package thredds.server.wms;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * Test Thredds / ncWMS against Laamu Atoll subset netCDF data with 8-bit bands.
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class LaamuWmsTest extends WmsTestSupport {

    /**
     * If not null, GetMap response images are saved to this directory. Useful
     * for bootstrapping and debugging.
     */
    private static final String RESPONSE_SAVE_DIRECTORY = null;
    // private static final String RESPONSE_SAVE_DIRECTORY = "/tmp";

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
     * Standard request bbox.
     */
    private static final String STANDARD_BBOX = "73.35,1.77,73.42,1.84";

    /**
     * Offset request bbox to test transparency.
     */
    private static final String OFFSET_BBOX = "73.37,1.79,73.44,1.86";

    /**
     * Path of HTTP request.
     */
    private static final String SERVLET_REQUEST_URI = "/thredds/wms/laamu/laamu.nc";

    /**
     * Path of HTTP request with the servlet context removed.
     */
    private static final String SERVLET_REQUEST_PATH_INFO = "laamu/laamu.nc";

    /**
     * Relative path to directory containing expected WMS response images.
     */
    private static final String TEST_DATA_EXPECTED_DIRECTORY = "test-data/expected/laamu";

    public LaamuWmsTest() {
        super(SERVLET_REQUEST_URI, SERVLET_REQUEST_PATH_INFO,
                TEST_DATA_EXPECTED_DIRECTORY, RESPONSE_SAVE_DIRECTORY);
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
     * Test WMS GetCapabilities request.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getCapabilities() {
        String request = "GetCapabilities";
        MockHttpServletRequest servletRequest = buildGetCapabilitiesRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> models;
        try {
            models = wmsController.dispatchWmsRequest(request,
                    new RequestParams(servletRequest.getParameterMap()),
                    servletRequest, servletResponse,
                    new UsageLogEntry(servletRequest)).getModelMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Set<Layer> layers = ((List<ThreddsDataset>) models.get("datasets"))
                .get(0).getLayers();
        Map<String, Layer> namedLayers = new HashMap<String, Layer>();
        for (Layer layer : layers) {
            namedLayers.put(layer.getName(), layer);
        }
        Assert.assertTrue(namedLayers.containsKey("Band1"));
        Assert.assertTrue(namedLayers.containsKey("Band4"));
        Assert.assertTrue(namedLayers.containsKey("Band7"));
        Assert.assertTrue(namedLayers.containsKey("False741"));
        Assert.assertTrue(namedLayers.containsKey("FalseColour741"));
        Assert.assertTrue(Arrays.asList(
                (String[]) models.get("supportedCrsCodes")).contains(
                "EPSG:4283"));
    }

    /**
     * Test WMS GetMap request for Band1.
     */
    @Test
    public void getMapBand1() {
        runGetMapTest("Band1", GREYSCALE_STYLES, GREYSCALE_COLORSCALERANGE,
                STANDARD_BBOX, "Band1.png");
    }

    /**
     * Test WMS GetMap request for Band4.
     */
    @Test
    public void getMapBand4() {
        runGetMapTest("Band4", GREYSCALE_STYLES, GREYSCALE_COLORSCALERANGE,
                STANDARD_BBOX, "Band4.png");
    }

    /**
     * Test WMS GetMap request for Band7.
     */
    @Test
    public void getMapBand7() {
        runGetMapTest("Band7", GREYSCALE_STYLES, GREYSCALE_COLORSCALERANGE,
                STANDARD_BBOX, "Band7.png");
    }

    /**
     * Test WMS GetMap request for False741.
     */
    @Test
    public void getMapFalse741() {
        runGetMapTest("False741", FALSE_STYLES, FALSE_COLORSCALERANGE,
                STANDARD_BBOX, "False741.png");
    }

    /**
     * Test WMS GetMap request for FalseColour741. The styles and
     * colourscalerange parameters will be ignored.
     */
    @Test
    public void getMapFalseColour741() {
        runGetMapTest("FalseColour741", FALSE_STYLES, FALSE_COLORSCALERANGE,
                STANDARD_BBOX, "FalseColour741.png");
    }

    /**
     * Test WMS GetMap request for FalseColour741 with offset bbox so part of
     * the image is transparent.
     */
    @Test
    public void getMapFalseColour741OffsetBbox() {
        runGetMapTest("FalseColour741", FALSE_STYLES, FALSE_COLORSCALERANGE,
                OFFSET_BBOX, "FalseColour741OffsetBbox.png");
    }

    /**
     * Test WMS GetFeatureInfo request for FalseColour741.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getFeatureInfoFalseColour741() {
        String request = "GetFeatureInfo";
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "GET", SERVLET_REQUEST_URI);
        servletRequest.setPathInfo(SERVLET_REQUEST_PATH_INFO);
        servletRequest.addParameter("layers", "FalseColour741");
        servletRequest.addParameter("query_layers", "FalseColour741");
        servletRequest.addParameter("elevation", "0");
        servletRequest.addParameter("time", "1970-01-01T00:00:00");
        servletRequest.addParameter("transparent", "true");
        servletRequest.addParameter("styles", FALSE_STYLES);
        servletRequest.addParameter("crs", "EPSG:4326");
        servletRequest.addParameter("colorscalerange", FALSE_COLORSCALERANGE);
        servletRequest.addParameter("numcolorbands", "252");
        servletRequest.addParameter("logscale", "false");
        servletRequest.addParameter("service", "WMS");
        servletRequest.addParameter("version", "1.3.0");
        servletRequest.addParameter("request", "GetFeatureInfo");
        servletRequest.addParameter("exceptions", "XML");
        servletRequest.addParameter("info_format", "text/xml");
        servletRequest.addParameter("bbox", STANDARD_BBOX);
        servletRequest.addParameter("width", "512");
        servletRequest.addParameter("height", "512");
        servletRequest.addParameter("i", "256");
        servletRequest.addParameter("j", "256");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> models;
        try {
            models = wmsController.dispatchWmsRequest(request,
                    new RequestParams(servletRequest.getParameterMap()),
                    servletRequest, servletResponse,
                    new UsageLogEntry(servletRequest)).getModelMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(models.containsKey("data"));
    }

}
