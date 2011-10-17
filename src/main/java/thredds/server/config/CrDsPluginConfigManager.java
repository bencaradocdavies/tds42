package thredds.server.config;

import thredds.crawlabledataset.CrawlableDataset;

import java.util.HashMap;
import java.util.Map;

/**
 * Manage configuration information that maps between TDS data root paths and particular
 * CrawlableDataset implementations that should be used to locate datasets under that
 * data root.
 *
 * @author edavis
 * @since 4.2.9
 */
public class CrDsPluginConfigManager
{
  private Map<String,String> configs;

  public CrDsPluginConfigManager(){
    this.configs = new HashMap<String,String>();
  }

  public boolean containsPathMapping( String path) {
    return this.configs.containsKey( path );
  }

  public String getClassName( String path) {
    return this.configs.get( path );
  }

  public int size() {
    return this.configs.size();
  }

  /**
   * Add configuration information for a particular mapping between a data root path and a
   * CrawlableDataset implementation.
   *
   * @param path - a data root path, may not be null or empty
   * @param className - the name of a class that implements thredds.crawlabledataset.CrawlableDataset, may not be null or empty
   * @return true if the given mapping is added, false if the given mapping was not added because the data root path was already contained.
   * @throws ClassNotFoundException if the class given by className can't be found
   * @throws IllegalArgumentException if path or className are null or empty or if the class given by className does not implement thredds.crawlabledataset.CrawlableDataset
   */
  public boolean addPluginConfig( String path, String className)
          throws ClassNotFoundException
  {
    if ( path == null || path.equals( "" ))
      throw new IllegalArgumentException( "path may not be null or empty" );
    if ( className == null || className.equals( "" ))
      throw new IllegalArgumentException( "className may not be null or emtpy" );

    Class crDsClass = Class.forName( className );

    if ( ! CrawlableDataset.class.isAssignableFrom( crDsClass ) )
      throw new IllegalArgumentException( "requested class not an implementation of thredds.crawlabledataset.CrawlableDataset" );

    if ( ! this.configs.containsKey( path )) {
      this.configs.put( path, className );
      return true;
    } else {
      return false;
    }
  }
}
