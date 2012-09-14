package thredds.server.wms;

import org.junit.Test;

/**
 * Test Thredds / ncWMS against Adelaide River Mouth subset netCDF data with
 * 16-bit bands.
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class AdelaideWmsTest extends WmsTestSupport {

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
     * Request colorscalerange for greyscale bands.
     */
    private static final String GREYSCALE_COLORSCALERANGE = "1,65535";

    /**
     * Standard request bbox.
     */
    private static final String STANDARD_BBOX = "131.19,-12.27,131.26,-12.20";

    /**
     * Path of HTTP request.
     */
    private static final String SERVLET_REQUEST_URI = "/thredds/wms/adelaide/adelaide.nc";

    /**
     * Path of HTTP request with the servlet context removed.
     */
    private static final String SERVLET_REQUEST_PATH_INFO = "adelaide/adelaide.nc";

    /**
     * Relative path to directory containing expected WMS response images.
     */
    private static final String TEST_DATA_EXPECTED_DIRECTORY = "test-data/expected/adelaide";

    public AdelaideWmsTest() {
        super(SERVLET_REQUEST_URI, SERVLET_REQUEST_PATH_INFO,
                TEST_DATA_EXPECTED_DIRECTORY, RESPONSE_SAVE_DIRECTORY);
    }

    /**
     * Test WMS GetMap request for FalseColour741. The styles and
     * colourscalerange parameters will be ignored.
     */
    @Test
    public void getMapFalseColour741() {
        runGetMapTest("FalseColour741", GREYSCALE_STYLES,
                GREYSCALE_COLORSCALERANGE, STANDARD_BBOX, "FalseColour741.png");
    }

}