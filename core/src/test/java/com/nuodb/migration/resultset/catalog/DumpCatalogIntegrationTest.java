package com.nuodb.migration.resultset.catalog;


import com.nuodb.migration.TestUtils;
import com.nuodb.migration.resultset.format.csv.CsvAttributes;
import com.nuodb.migration.spec.ResourceSpec;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class DumpCatalogIntegrationTest extends TestUtils {

    private static final String TEST_DIR = "/tmp/migration-tool";
    private static final String TEST_PATH = TEST_DIR + "/migration-tool-test";

    private FileCatalog catalogFile;
    private CatalogWriter writer;
    private ResourceSpec outputSpec;

    @Before
    public void setUp() throws Exception {
        ResourceSpec outputSpec = new ResourceSpec();
        outputSpec.setPath(TEST_PATH);
        outputSpec.setType(CsvAttributes.FORMAT);
        this.outputSpec = outputSpec;

        FileCatalog catalogFile = new FileCatalog(outputSpec.getType());
        Assert.assertEquals(catalogFile.getPath(), TEST_PATH);
    }

    @Test
    public void testOpen() throws Exception {
        try {
            writer = catalogFile.getCatalogWriter();
        } catch (CatalogException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = CatalogException.class)
    public void testOpenError() throws Exception {
        new FileCatalog("").getCatalogWriter();
    }

    @After
    public void tearDown() throws Exception {
        writer.close();
        final File file = catalogFile.getCatalogDir();
        if (file != null && file.exists()) {
            FileUtils.forceDelete(file);
        }
    }
}
