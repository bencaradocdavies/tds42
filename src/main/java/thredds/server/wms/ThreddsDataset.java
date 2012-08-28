/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.wms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thredds.server.wms.config.DatasetPathSettings;
import thredds.server.wms.config.LayerSettings;
import thredds.server.wms.config.WmsDetailedConfig;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.cdm.AbstractScalarLayerBuilder;
import uk.ac.rdg.resc.ncwms.cdm.CdmUtils;
import uk.ac.rdg.resc.ncwms.cdm.DataReadingStrategy;
import uk.ac.rdg.resc.ncwms.cdm.LayerBuilder;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * A {@link uk.ac.rdg.resc.ncwms.wms.Dataset} that provides access to layers read from
 * {@link ucar.nc2.dataset.NetcdfDataset} objects.
 *
 * @author Jon
 */
public class ThreddsDataset implements Dataset
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreddsDataset.class);

  private final String urlPath;
  private final String title;
  private final Map<String, ThreddsScalarLayer> scalarLayers =
          new LinkedHashMap<String, ThreddsScalarLayer>();
  private final Map<String, ThreddsVectorLayer> vectorLayers =
          new LinkedHashMap<String, ThreddsVectorLayer>();

  /**
   * LayerBuilder used to create ThreddsLayers in CdmUtils.findAndUpdateLayers
   */
  private static final LayerBuilder<ThreddsScalarLayer> THREDDS_LAYER_BUILDER = new AbstractScalarLayerBuilder<ThreddsScalarLayer>()
  {
    @Override
    public ThreddsScalarLayer newLayer( String id )
    {
      return new ThreddsScalarLayer( id );
    }

    @Override
    public void setTimeValues( ThreddsScalarLayer layer, List<DateTime> times )
    {
      layer.setTimeValues( times );
    }

    @Override
    public void setGridDatatype( ThreddsScalarLayer layer, GridDatatype grid )
    {
      layer.setGridDatatype( grid );
    }
  };

  /**
   * Creates a new ThreddsDataset with the given id from the given NetcdfDataset
   */
  public ThreddsDataset( String urlPath, GridDataset gridDataset, WmsDetailedConfig wmsConfig )
  {
    this.urlPath = urlPath;
    this.title = gridDataset.getTitle();
    
    NetcdfDataset ncDataset = (NetcdfDataset) gridDataset.getNetcdfFile();

    // Get the most appropriate data-reading strategy for this dataset
    DataReadingStrategy drStrategy = CdmUtils.getOptimumDataReadingStrategy( ncDataset );

    // Now load the scalar layers
    CdmUtils.findAndUpdateLayers( gridDataset, THREDDS_LAYER_BUILDER, this.scalarLayers );

    // Add the false colour layers
    for (ThreddsFalseColorLayer layer : findFalseColorLayers(wmsConfig)) {
        this.scalarLayers.put(layer.getId(), layer);
    }

    // Set the extra properties of each layer
    for ( ThreddsScalarLayer layer : this.scalarLayers.values() )
    {
      layer.setDataReadingStrategy( drStrategy );
      layer.setDataset( this );
      layer.setLayerSettings(wmsConfig.getSettings(layer));
    }

    // Find the vector quantities
    Collection<VectorLayer> vectorLayersColl = WmsUtils.findVectorLayers( this.scalarLayers.values() );
    // Add the vector quantities to the map of layers
    for ( VectorLayer vecLayer : vectorLayersColl )
    {
      // We must wrap these vector layers as ThreddsVectorLayers to ensure that
      // the name of each layer matches its id.
      ThreddsVectorLayer tdsVecLayer = new ThreddsVectorLayer(vecLayer);
      tdsVecLayer.setLayerSettings(wmsConfig.getSettings(tdsVecLayer));
      this.vectorLayers.put( vecLayer.getId(), tdsVecLayer );
    }
  }

    /**
     * Examine the WMS config for variable settings that define false colour
     * layers and create them.
     * 
     * @param wmsConfig
     *            the WMS settings to be examined
     * @return list of false colour layers
     */
    private List<ThreddsFalseColorLayer> findFalseColorLayers(
            WmsDetailedConfig wmsConfig) {
        List<ThreddsFalseColorLayer> falseColorLayers = new ArrayList<ThreddsFalseColorLayer>();
        DatasetPathSettings settings = wmsConfig
                .getBestDatasetPathMatch(urlPath);
        if (settings != null) {
            Map<String, LayerSettings> settingsPerVariable = settings
                    .getSettingsPerVariable();
            for (String layer : settingsPerVariable.keySet()) {
                LayerSettings layerSettings = settingsPerVariable.get(layer);
                if (layerSettings.isFalseColor()) {
                    ThreddsScalarLayer redComponent = scalarLayers
                            .get(layerSettings.getRedComponent());
                    ThreddsScalarLayer greenComponent = scalarLayers
                            .get(layerSettings.getGreenComponent());
                    ThreddsScalarLayer blueComponent = scalarLayers
                            .get(layerSettings.getBlueComponent());
                    if (redComponent == null || greenComponent == null
                            || blueComponent == null) {
                        StringBuilder missingComponents = new StringBuilder();
                        if (redComponent == null) {
                            missingComponents.append(" ");
                            missingComponents.append(layerSettings
                                    .getRedComponent());
                        }
                        if (greenComponent == null) {
                            missingComponents.append(" ");
                            missingComponents.append(layerSettings
                                    .getGreenComponent());
                        }
                        if (blueComponent == null) {
                            missingComponents.append(" ");
                            missingComponents.append(layerSettings
                                    .getBlueComponent());
                        }
                        LOGGER.warn(String.format("Skipped false colour layer"
                                + " %s (Red=%s, Green=%s, Blue=%s)"
                                + " with missing component:%s", layer,
                                layerSettings.getRedComponent(),
                                layerSettings.getGreenComponent(),
                                layerSettings.getBlueComponent(),
                                missingComponents.toString()));
                    } else {
                        falseColorLayers.add(new ThreddsFalseColorLayer(layer,
                                redComponent, greenComponent, blueComponent));
                    }
                }
            }
        }
        return falseColorLayers;
    }

  /**
   * Uses the {@link #getDatasetPath() url path} as the unique id.
   */
  @Override
  public String getId()
  {
    return this.urlPath;
  }

  @Override
  public String getTitle()
  {
    return this.title;
  }

  /** Gets the path that was specified on the incoming URL */
  public String getDatasetPath()
  {
      return this.urlPath;
  }

  /**
   * Returns the current time, since datasets could change at any time without
   * our knowledge.
   *
   * @see ThreddsServerConfig#getLastUpdateTime()
   */
  @Override
  public DateTime getLastUpdateTime()
  {
    return new DateTime();
  }

  /**
   * Gets the {@link uk.ac.rdg.resc.ncwms.wms.Layer} with the given {@link uk.ac.rdg.resc.ncwms.wms.Layer#getId() id}.  The id
   * is unique within the dataset, not necessarily on the whole server.
   *
   * @return The layer with the given id, or null if there is no layer with
   *         the given id.
   * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
   */
  @Override
  public ThreddsLayer getLayerById( String layerId )
  {
    ThreddsLayer layer = this.scalarLayers.get( layerId );
    if ( layer == null )
      layer = this.vectorLayers.get( layerId );

    return layer;
  }

  /**
   * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
   */
  @Override
  public Set<Layer> getLayers()
  {
    Set<Layer> layerSet = new LinkedHashSet<Layer>();
    layerSet.addAll( this.scalarLayers.values() );
    layerSet.addAll( this.vectorLayers.values() );
    return layerSet;
  }

  /**
   * Returns an empty string
   */
  @Override
  public String getCopyrightStatement()
  {
    return "";
  }

  /**
   * Returns an empty string
   */
  @Override
  public String getMoreInfoUrl()
  {
    return "";
  }

  @Override
  public boolean isReady()
  {
    return true;
  }

  @Override
  public boolean isLoading()
  {
    return false;
  }

  @Override
  public boolean isError()
  {
    return false;
  }

  @Override
  public Exception getException()
  {
    return null;
  }

  @Override
  public boolean isDisabled()
  {
    return false;
  }

}