package thredds.server.wms;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.Ranges;

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
     * Test that scaling of individual band data with
     * {@link ThreddsFalseColorLayer#scale(List, Range, Float)} has the expected
     * output.
     */
    @Test
    public void scale() {
        @SuppressWarnings("serial")
        List<Float> data = new ArrayList<Float>() {
            {
                add(-1.0f);
                add(0.0f);
                add(1.0f);
                add(1000.0f);
                add(4000.0f);
                add(5000.0f);
                add(6000.0f);
                add(9000.0f);
                add(9999.0f);
                add(10000.0f);
                add(10001.0f);
            }
        };
        Range<Float> range = Ranges.newRange(0.0f, 10000.0f);
        Float gamma = 0.5f;
        ThreddsFalseColorLayer.scale(data, range, gamma);
        Assert.assertEquals((Float) null, data.get(0));
        Assert.assertEquals((Float) 0.0f, data.get(1));
        Assert.assertEquals((Float) 3.0f, data.get(2));
        Assert.assertEquals((Float) 81.0f, data.get(3));
        Assert.assertEquals((Float) 161.0f, data.get(4));
        Assert.assertEquals((Float) 180.0f, data.get(5));
        Assert.assertEquals((Float) 198.0f, data.get(6));
        Assert.assertEquals((Float) 242.0f, data.get(7));
        Assert.assertEquals((Float) 255.0f, data.get(8));
        Assert.assertEquals((Float) 255.0f, data.get(9));
        Assert.assertEquals((Float) null, data.get(10));
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