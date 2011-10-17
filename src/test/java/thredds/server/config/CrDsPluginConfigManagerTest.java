package thredds.server.config;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class CrDsPluginConfigManagerTest
{
  private String crDsFileClassName = "thredds.crawlabledataset.CrawlableDatasetFile";
  private String crDsOdapClassName = "thredds.crawlabledataset.CrawlableDatasetDods";

  @Test
  public void checkLooksGood() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    assertTrue( crDsPCM.addPluginConfig( "joe/shmoe", crDsFileClassName ) );
    assertTrue( crDsPCM.addPluginConfig( "joe/floe", crDsOdapClassName ) );
    assertTrue( crDsPCM.addPluginConfig( "joe/no", crDsOdapClassName ) );
    assertEquals( 3, crDsPCM.size() );
    assertTrue( crDsPCM.containsPathMapping( "joe/shmoe" ) );
    assertTrue( crDsPCM.containsPathMapping( "joe/floe" ) );
    assertTrue( crDsPCM.containsPathMapping( "joe/no" ));
    assertEquals( crDsFileClassName, crDsPCM.getClassName( "joe/shmoe" ) );
    assertEquals( crDsOdapClassName, crDsPCM.getClassName( "joe/floe" ) );
    assertEquals( crDsOdapClassName, crDsPCM.getClassName( "joe/no" ) );
  }

  @Test
  public void checkDropsDuplicates() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    assertTrue( crDsPCM.addPluginConfig( "joe/shmoe", crDsFileClassName ) );
    assertFalse( crDsPCM.addPluginConfig( "joe/shmoe", crDsOdapClassName ) );
    assertEquals( 1, crDsPCM.size() );
    assertTrue( crDsPCM.containsPathMapping( "joe/shmoe" ));
    assertEquals( crDsFileClassName, crDsPCM.getClassName( "joe/shmoe" ) );
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkNullPathNotAccepted() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    crDsPCM.addPluginConfig( null, crDsFileClassName );
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkNullClassNameNotAccepted() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    crDsPCM.addPluginConfig( "joe/go", null );
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkEmptyPathNotAccepted() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    crDsPCM.addPluginConfig( "", "thredds.crawlabledataset.CrawlableDatasetFile" );
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkEmptyClassNameNotAccepted() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    crDsPCM.addPluginConfig( "joe/go", "" );
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkClassNamedNotCrDs() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    crDsPCM.addPluginConfig( "joe/go", "java.io.File" );
  }

  @Test(expected = ClassNotFoundException.class)
  public void checkClassNamedNotFound() throws ClassNotFoundException
  {
    CrDsPluginConfigManager crDsPCM = new CrDsPluginConfigManager();
    crDsPCM.addPluginConfig( "joe/go", "treds.crds.CrDsFun" );
  }
}
