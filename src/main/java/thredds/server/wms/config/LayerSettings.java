/*
 * Copyright (c) 2010 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package thredds.server.wms.config;

import org.jdom.Element;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.Ranges;

/**
 * Simple Java bean encapsulating the settings (allowFeatureInfo,
 * defaultColorScaleRange, defaultPaletteName and logScaling) at a particular
 * part of the config XML document.  A Null value for a field implies that
 * that field has not been set in the document and a default should be used.
 * @author Jon
 */
public class LayerSettings
{

    /**
     * Default source variable range for false colour layers.
     */
    private static final Range<Float> DEFAULT_RANGE = Ranges.newRange(0.0f, 255.0f);

    /**
     * Default gamma for false colour layers.
     */
    private static final Float DEFAULT_GAMMA = 1.0f;

    private Boolean allowFeatureInfo = null;
    private Range<Float> defaultColorScaleRange = null;
    private String defaultPaletteName =  null;
    private Boolean logScaling = null;
    private Integer defaultNumColorBands = null;
    private String title = null;
    private String abstract_ = null;    
    private FalseColorComponentSettings red = null;
    private FalseColorComponentSettings green = null;
    private FalseColorComponentSettings blue = null;

    LayerSettings(Element parentElement) throws WmsConfigException
    {
        if (parentElement == null) return; // Create a set of layer settings with all-null fields
        this.allowFeatureInfo = getBoolean(parentElement, "allowFeatureInfo");
        this.defaultColorScaleRange = getRange(parentElement, "defaultColorScaleRange");
        this.defaultPaletteName = parentElement.getChildTextTrim("defaultPaletteName");
        // If the default palette name tag is used, it must be populated
        // TODO: can we check this against the installed palettes?
        if (this.defaultPaletteName != null && this.defaultPaletteName.isEmpty())
        {
            throw new WmsConfigException("defaultPaletteName must contain a value");
        }
        this.defaultNumColorBands = getInteger(parentElement, "defaultNumColorBands",
                Ranges.newRange(5, ColorPalette.MAX_NUM_COLOURS));
        this.logScaling = getBoolean(parentElement, "logScaling");
        // false colour layers
        this.title = parentElement.getChildTextTrim("title");
        this.abstract_ = parentElement.getChildTextTrim("abstract");
        Range<Float> defaultRange = getRange(parentElement, "range");
        if (defaultRange == null) {
            defaultRange = DEFAULT_RANGE;
        }
        Float defaultGamma = getFloat(parentElement, "gamma");
        if (defaultGamma == null) {
            defaultGamma = DEFAULT_GAMMA;
        }
        this.red = getFalseColorComponentSettings(
                parentElement.getChild("red"), defaultRange, defaultGamma);
        this.green = getFalseColorComponentSettings(
                parentElement.getChild("green"), defaultRange, defaultGamma);
        this.blue = getFalseColorComponentSettings(
                parentElement.getChild("blue"), defaultRange, defaultGamma);
        if (!isFalseColor()
                && (this.red != null || this.green != null || this.blue != null)) {
            throw new WmsConfigException(
                    "Either all or none of red, green, and blue must be given");
        }
    }

    /** Package-private constructor, sets all fields to null */
    LayerSettings() {}

    private static Boolean getBoolean(Element parentElement, String childName)
            throws WmsConfigException
    {
        String str = parentElement.getChildTextTrim(childName);
        if (str == null) return null;
        if (str.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (str.equalsIgnoreCase("false")) return Boolean.FALSE;
        throw new WmsConfigException("Value of " + childName + " must be true or false");
    }

    private static Integer getInteger(Element parentElement, String childName, Range<Integer> validRange)
            throws WmsConfigException
    {
        String str = parentElement.getChildTextTrim(childName);
        if (str == null) return null;
        int val;
        try
        {
            val = Integer.parseInt(str);
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsConfigException(nfe);
        }
        if (val < validRange.getMinimum()) return validRange.getMinimum();
        else if (val > validRange.getMaximum()) return validRange.getMaximum();
        else return val;
    }

    private static Float getFloat(Element parentElement, String childName)
            throws WmsConfigException {
        String s = parentElement.getChildTextTrim(childName);
        if (s == null) {
            return null;
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            throw new WmsConfigException(e);
        }
    }

    private static Range<Float> getRange(Element parentElement, String childName)
            throws WmsConfigException
    {
        String str = parentElement.getChildTextTrim(childName);
        if (str == null) return null;
        String[] els = str.split(" ");
        if (els.length != 2)
        {
            throw new WmsConfigException("Invalid range format");
        }
        try
        {
            float min = Float.parseFloat(els[0]);
            float max = Float.parseFloat(els[1]);
            return Ranges.newRange(min, max);
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsConfigException("Invalid floating-point value in range");
        }
    }

    private static FalseColorComponentSettings getFalseColorComponentSettings(
            Element element, Range<Float> defaultRange, Float defaultGamma)
            throws WmsConfigException {
        if (element == null) {
            return null;
        }
        String source = element.getChildTextTrim("source");
        if (source == null) {
            throw new WmsConfigException("False colour component "
                    + "missing mandatory <source> element");
        }
        Range<Float> range = getRange(element, "range");
        if (range == null) {
            range = defaultRange;
        }
        if (range.getMinimum().equals(range.getMaximum())) {
            throw new WmsConfigException("False colour range must have "
                    + "different minimum and maximum");
        }
        Float gamma = getFloat(element, "gamma");
        if (gamma == null) {
            gamma = defaultGamma;
        }
        if (gamma.compareTo(0.0f) <= 0) {
            throw new WmsConfigException("False colour gamma must be positive");
        }
        return new FalseColorComponentSettings(source, range, gamma);
    }

    public Boolean isAllowFeatureInfo() {
        return allowFeatureInfo;
    }

    public Range<Float> getDefaultColorScaleRange() {
        return defaultColorScaleRange;
    }

    public String getDefaultPaletteName() {
        return defaultPaletteName;
    }

    public Boolean isLogScaling() {
        return logScaling;
    }

    public Integer getDefaultNumColorBands() {
        return defaultNumColorBands;
    }

    public String getTitle() {
        return title;
    }

    public String getAbstract() {
        return abstract_;
    }

    public FalseColorComponentSettings getRed() {
        return red;
    }

    public FalseColorComponentSettings getGreen() {
        return green;
    }

    public FalseColorComponentSettings getBlue() {
        return blue;
    }

    public boolean isFalseColor() {
        return red != null && green != null && blue != null;
    }

    /**
     * Replaces all unset values in this object with values from the given
     * LayerSettings object.
     */
    void replaceNullValues(LayerSettings newSettings)
    {
        if (this.allowFeatureInfo == null) this.allowFeatureInfo = newSettings.allowFeatureInfo;
        if (this.defaultColorScaleRange == null) this.defaultColorScaleRange = newSettings.defaultColorScaleRange;
        if (this.defaultPaletteName == null) this.defaultPaletteName = newSettings.defaultPaletteName;
        if (this.logScaling == null) this.logScaling = newSettings.logScaling;
        if (this.defaultNumColorBands == null) this.defaultNumColorBands = newSettings.defaultNumColorBands;
        if (this.title == null) this.title = newSettings.title;
        if (this.abstract_ == null) this.abstract_ = newSettings.abstract_;
        if (this.red == null) this.red = newSettings.red;
        if (this.green == null) this.green = newSettings.green;
        if (this.blue == null) this.blue = newSettings.blue;
    }

    void setDefaultColorScaleRange(Range<Float> defaultColorScaleRange)
    {
        this.defaultColorScaleRange = defaultColorScaleRange;
    }

    @Override
    public String toString() {
        return String
                .format("allowFeatureInfo = %s, defaultColorScaleRange = %s, "
                        + "defaultPaletteName = %s, defaultNumColorBands = %s, "
                        + "logScaling = %s, "
                        + "title = \"%s\", abstract = \"%s\", "
                        + "red = %s, green = %s, blue = %s",
                        this.allowFeatureInfo, this.defaultColorScaleRange,
                        this.defaultPaletteName, this.defaultNumColorBands,
                        this.logScaling, this.title, this.abstract_, this.red,
                        this.green, this.blue);
    }

    /**
     * Setting for a colour component of a false colour layer.
     */
    public static class FalseColorComponentSettings {

        private final String source;

        private final Range<Float> range;

        private final Float gamma;

        /**
         * @param source
         *            name of the source variable for a false colour component
         * @param range
         *            range of values in the source variable that are mapped to
         *            a colour channel
         * @param gamma
         *            gamma applied to normalised colour value [0,1]
         */
        public FalseColorComponentSettings(String source, Range<Float> range,
                Float gamma) {
            this.source = source;
            this.range = range;
            this.gamma = gamma;
        }

        /**
         * Return name of the source variable for a false colour component.
         */
        public String getSource() {
            return source;
        }

        /**
         * Return range of values in the source variable that are mapped to a
         * colour channel.
         */
        public Range<Float> getRange() {
            return range;
        }

        /**
         * Return the gamma applied to normalised colour value [0,1].
         */
        public Float getGamma() {
            return gamma;
        }

    }

}
