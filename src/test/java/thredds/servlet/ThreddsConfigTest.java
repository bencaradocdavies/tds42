package thredds.servlet;

import org.junit.Test;
import thredds.TestAll;
import thredds.catalog.InvCatalogRef;
import thredds.filesystem.FileSystemProto;
import thredds.server.config.CrDsPluginConfigManager;

import java.io.File;

import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class ThreddsConfigTest
{
  @Test
  public void checkReadingCrDsPluginConfigs()
  {
    File testFile = new File( TestAll.tdsLocalTestDataDir,
                              "thredds/servlet/ThreddsConfigTest_readingCrDsPluginConfigs.xml" );
    assertTrue( "Test file [" + testFile.getAbsolutePath() + "] does not exist.", testFile.exists() );
    ThreddsConfig.init( testFile.getPath() );

    CrDsPluginConfigManager crDsPCM = ThreddsConfig.getCrDsPluginConfigManager();
    assertEquals( 1, crDsPCM.size() );
    assertTrue( crDsPCM.containsPathMapping( "joe/foe" ));
    assertEquals( "thredds.crawlabledataset.CrawlableDatasetFile", crDsPCM.getClassName( "joe/foe" ) );
  }
}
