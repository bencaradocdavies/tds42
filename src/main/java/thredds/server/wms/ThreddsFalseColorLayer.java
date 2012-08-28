package thredds.server.wms;

import uk.ac.rdg.resc.ncwms.wms.FalseColorLayer;

/**
 * A scalar layer that consists of three colour components that can be combined
 * to form a false colour layer.
 * 
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class ThreddsFalseColorLayer extends ThreddsScalarLayer implements
        FalseColorLayer {

    /**
     * The red component of the false colour layer.
     */
    private final ThreddsScalarLayer redComponent;

    /**
     * The green component of the false colour layer.
     */
    private final ThreddsScalarLayer greenComponent;

    /**
     * The blue component of the false colour layer.
     */
    private final ThreddsScalarLayer blueComponent;

    /**
     * Constructor.
     * 
     * @param id
     *            the name of the false colour layer
     * @param redComponent
     *            the red component of the false colour layer
     * @param greenComponent
     *            the green component of the false colour layer
     * @param blueComponent
     *            the blue component of the false colour layer
     */
    public ThreddsFalseColorLayer(String id, ThreddsScalarLayer redComponent,
            ThreddsScalarLayer greenComponent, ThreddsScalarLayer blueComponent) {
        super(id);
        this.redComponent = redComponent;
        this.greenComponent = greenComponent;
        this.blueComponent = blueComponent;
        setTitle(String.format("False Colour (Red=%s, Green=%s, Blue=%s)",
                redComponent.getId(), greenComponent.getId(),
                blueComponent.getId()));
        setLayerAbstract(getTitle());
        setGeographicBoundingBox(redComponent.getGeographicBoundingBox());
        setTimeValues(redComponent.getTimeValues());
    }

    /**
     * @see uk.ac.rdg.resc.ncwms.wms.FalseColorLayer#getRedComponent()
     */
    public ThreddsScalarLayer getRedComponent() {
        return redComponent;
    }

    /**
     * @see uk.ac.rdg.resc.ncwms.wms.FalseColorLayer#getGreenComponent()
     */
    public ThreddsScalarLayer getGreenComponent() {
        return greenComponent;
    }

    /**
     * @see uk.ac.rdg.resc.ncwms.wms.FalseColorLayer#getBlueComponent()
     */
    public ThreddsScalarLayer getBlueComponent() {
        return blueComponent;
    }

}
