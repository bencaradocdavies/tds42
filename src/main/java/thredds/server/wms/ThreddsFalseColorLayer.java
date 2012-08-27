package thredds.server.wms;

import uk.ac.rdg.resc.ncwms.wms.FalseColorLayer;

public class ThreddsFalseColorLayer extends ThreddsScalarLayer implements FalseColorLayer {

    private final ThreddsScalarLayer redLayer;

    private final ThreddsScalarLayer greenLayer;

    private final ThreddsScalarLayer blueLayer;

    public ThreddsFalseColorLayer(String id, ThreddsScalarLayer redLayer,
            ThreddsScalarLayer greenLayer, ThreddsScalarLayer blueLayer) {
        super(id);
        this.redLayer = redLayer;
        this.greenLayer = greenLayer;
        this.blueLayer = blueLayer;
        setTitle(String.format("False Colour (Red=%s, Green=%s, Blue=%s)",
                redLayer.getId(), greenLayer.getId(), blueLayer.getId()));
        setTimeValues(redLayer.getTimeValues());
    }

    public ThreddsScalarLayer getRedLayer() {
        return redLayer;
    }

    public ThreddsScalarLayer getGreenLayer() {
        return greenLayer;
    }

    public ThreddsScalarLayer getBlueLayer() {
        return blueLayer;
    }

}
