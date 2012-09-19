package thredds.server.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;

import thredds.server.wms.config.LayerSettings;
import thredds.server.wms.config.LayerSettings.FalseColorComponentSettings;
import uk.ac.rdg.resc.ncwms.coords.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.FalseColorLayer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;

/**
 * A scalar layer that consists of three colour components that can be combined
 * to form a false colour layer.
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class ThreddsFalseColorLayer extends ThreddsScalarLayer implements
        FalseColorLayer {

    private static final int COLOR_CHANNEL_MINIMUM = 0;

    private static final int COLOR_CHANNEL_MAXIMUM = 255;

    /**
     * Scale raw data in place, replacing source values with colour channel
     * values. Source data outside the range is replaced with null. Gamma
     * correction is applied to the range-fraction.
     * 
     * @param data
     *            raw variable data to be converted to colour channel values.
     * @param range
     *            range of valid raw data
     * @param gamma
     *            gamma to be applied to range-fraction
     */
    public static void scale(List<Float> data, Range<Float> range, Float gamma) {
        double min = range.getMinimum();
        double max = range.getMaximum();
        double g = gamma;
        int n = data.size();
        for (int i = 0; i < n; i++) {
            if (data.get(i) != null) {
                double d = data.get(i);
                if (d < min || d > max) {
                    data.set(i, null);
                } else {
                    double c = Math
                            .floor((COLOR_CHANNEL_MAXIMUM - COLOR_CHANNEL_MINIMUM)
                                    * Math.pow((d - min) / (max - min), g)
                                    + COLOR_CHANNEL_MINIMUM + 0.5);
                    if (c < COLOR_CHANNEL_MINIMUM || c > COLOR_CHANNEL_MAXIMUM) {
                        data.set(i, null);
                    } else {
                        data.set(i, (float) c);
                    }
                }
            }
        }
    }

    /**
     * The red component of the false colour layer.
     */
    private final ComponentLayer red;

    /**
     * The green component of the false colour layer.
     */
    private final ComponentLayer green;

    /**
     * The blue component of the false colour layer.
     */
    private final ComponentLayer blue;

    /**
     * Constructor.
     * 
     * @param id
     *            the name of the false colour layer
     * @param redSourceLayer
     *            the source layer of the red component
     * @param greenSourceLayer
     *            the source layer of the green component
     * @param blueSourceLayer
     *            the source layer of the blue component
     */
    public ThreddsFalseColorLayer(String id, LayerSettings layerSettings,
            ThreddsScalarLayer redSourceLayer,
            ThreddsScalarLayer greenSourceLayer,
            ThreddsScalarLayer blueSourceLayer) {
        super(id);
        red = new ComponentLayer(redSourceLayer, layerSettings.getRed());
        green = new ComponentLayer(greenSourceLayer, layerSettings.getGreen());
        blue = new ComponentLayer(blueSourceLayer, layerSettings.getBlue());
        if (layerSettings.getTitle() == null) {
            setTitle(String.format("False Colour (Red=%s, Green=%s, Blue=%s)",
                    redSourceLayer.getId(), greenSourceLayer.getId(),
                    blueSourceLayer.getId()));
        } else {
            setTitle(layerSettings.getTitle());
        }
        if (layerSettings.getAbstract() == null) {
            setLayerAbstract(getTitle());
        } else {
            setLayerAbstract(layerSettings.getAbstract());
        }
        setHorizontalCoordSys(redSourceLayer.getHorizontalCoordSys());
        setGeographicBoundingBox(redSourceLayer.getGeographicBoundingBox());
        setGridDatatype(redSourceLayer.getGridDatatype());
        setTimeValues(redSourceLayer.getTimeValues());
    }

    /**
     * @see uk.ac.rdg.resc.ncwms.wms.FalseColorLayer#getRed()
     */
    public ScalarLayer getRed() {
        return red;
    }

    /**
     * @see uk.ac.rdg.resc.ncwms.wms.FalseColorLayer#getGreen()
     */
    public ScalarLayer getGreen() {
        return green;
    }

    /**
     * @see uk.ac.rdg.resc.ncwms.wms.FalseColorLayer#getBlue()
     */
    public ScalarLayer getBlue() {
        return blue;
    }

    /**
     * Return null for reading a single point.
     * 
     * @see thredds.server.wms.ThreddsScalarLayer#readSinglePoint(org.joda.time.DateTime,
     *      double, uk.ac.rdg.resc.ncwms.coords.HorizontalPosition)
     */
    @Override
    public Float readSinglePoint(DateTime time, double elevation,
            HorizontalPosition xy) throws InvalidDimensionValueException,
            IOException {
        return null;
    }

    /**
     * Return a list of nulls for reading a timeseries.
     * 
     * @see uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer#readTimeseries(java.util
     *      .List, double, uk.ac.rdg.resc.ncwms.coords.HorizontalPosition)
     */
    @SuppressWarnings("unused")
    @Override
    public List<Float> readTimeseries(List<DateTime> times, double elevation,
            HorizontalPosition xy) throws InvalidDimensionValueException,
            IOException {
        List<Float> values = new ArrayList<Float>(times.size());
        for (DateTime st : times) {
            values.add(null);
        }
        return values;
    }

    /**
     * A scalar layer for the component of a false colour layer. Almost all
     * methods are delegated to the source layer, except for transformation of
     * the data, which is performed according to the settings for this
     * component.
     */
    public class ComponentLayer implements ScalarLayer {

        private final ScalarLayer sourceLayer;

        private final FalseColorComponentSettings componentSettings;

        /**
         * @param sourceLayer
         *            layer that is the source for this colour component
         * @param componentSettings
         *            settings used to transform this data into a colour channel
         */
        public ComponentLayer(ScalarLayer sourceLayer,
                FalseColorComponentSettings componentSettings) {
            this.sourceLayer = sourceLayer;
            this.componentSettings = componentSettings;
        }

        public List<Float> readPointList(DateTime time, double elevation,
                PointList pointList) throws InvalidDimensionValueException,
                IOException {
            List<Float> data = sourceLayer.readPointList(time, elevation,
                    pointList);
            scale(data, componentSettings.getRange(),
                    componentSettings.getGamma());
            return data;
        }

        /* Only automatically generated delegates below this point. */

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.ScalarLayer#readSinglePoint(org.joda.time.DateTime,
         *      double, uk.ac.rdg.resc.ncwms.coords.HorizontalPosition)
         */
        public Float readSinglePoint(DateTime time, double elevation,
                HorizontalPosition xy) throws InvalidDimensionValueException,
                IOException {
            return sourceLayer.readSinglePoint(time, elevation, xy);
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getDataset()
         */
        public Dataset getDataset() {
            return sourceLayer.getDataset();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getId()
         */
        public String getId() {
            return sourceLayer.getId();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getTitle()
         */
        public String getTitle() {
            return sourceLayer.getTitle();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getLayerAbstract()
         */
        public String getLayerAbstract() {
            return sourceLayer.getLayerAbstract();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getName()
         */
        public String getName() {
            return sourceLayer.getName();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getUnits()
         */
        public String getUnits() {
            return sourceLayer.getUnits();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#isQueryable()
         */
        public boolean isQueryable() {
            return sourceLayer.isQueryable();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getGeographicBoundingBox()
         */
        public GeographicBoundingBox getGeographicBoundingBox() {
            return sourceLayer.getGeographicBoundingBox();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getHorizontalCoordSys()
         */
        public HorizontalCoordSys getHorizontalCoordSys() {
            return sourceLayer.getHorizontalCoordSys();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getChronology()
         */
        public Chronology getChronology() {
            return sourceLayer.getChronology();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getTimeValues()
         */
        public List<DateTime> getTimeValues() {
            return sourceLayer.getTimeValues();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getCurrentTimeValue()
         */
        public DateTime getCurrentTimeValue() {
            return sourceLayer.getCurrentTimeValue();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getDefaultTimeValue()
         */
        public DateTime getDefaultTimeValue() {
            return sourceLayer.getDefaultTimeValue();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getElevationValues()
         */
        public List<Double> getElevationValues() {
            return sourceLayer.getElevationValues();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.ScalarLayer#readTimeseries(java.util.List,
         *      double, uk.ac.rdg.resc.ncwms.coords.HorizontalPosition)
         */
        public List<Float> readTimeseries(List<DateTime> times,
                double elevation, HorizontalPosition xy)
                throws InvalidDimensionValueException, IOException {
            return sourceLayer.readTimeseries(times, elevation, xy);
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getDefaultElevationValue()
         */
        public double getDefaultElevationValue() {
            return sourceLayer.getDefaultElevationValue();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getElevationUnits()
         */
        public String getElevationUnits() {
            return sourceLayer.getElevationUnits();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#isElevationPositive()
         */
        public boolean isElevationPositive() {
            return sourceLayer.isElevationPositive();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getApproxValueRange()
         */
        public Range<Float> getApproxValueRange() {
            return sourceLayer.getApproxValueRange();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#isLogScaling()
         */
        public boolean isLogScaling() {
            return sourceLayer.isLogScaling();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getDefaultColorPalette()
         */
        public ColorPalette getDefaultColorPalette() {
            return sourceLayer.getDefaultColorPalette();
        }

        /**
         * @see uk.ac.rdg.resc.ncwms.wms.Layer#getDefaultNumColorBands()
         */
        public int getDefaultNumColorBands() {
            return sourceLayer.getDefaultNumColorBands();
        }

    }

}
