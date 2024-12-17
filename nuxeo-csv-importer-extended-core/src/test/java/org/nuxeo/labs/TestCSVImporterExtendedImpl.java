package org.nuxeo.labs;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.csv.core.CSVImporter;
import org.nuxeo.ecm.csv.core.CSVImporterExtendedImpl;
import org.nuxeo.ecm.csv.core.CSVImporterOptions;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import jakarta.inject.Inject;
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.csv.core.CSVImporterExtendedImpl.ZIP_MIMETYPE;

@RunWith(FeaturesRunner.class)
@Features({PlatformFeature.class, TransactionalFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({
        "nuxeo-csv-importer-extended-core",
        "org.nuxeo.ecm.platform.types",
        "org.nuxeo.ecm.platform.webapp.types",
        "org.nuxeo.ecm.csv.core"
})
public class TestCSVImporterExtendedImpl {

    @Inject
    public CoreSession coreSession;

    @Inject
    public CSVImporter csvImporter;

    @Inject
    public TransactionalFeature txFeature;

    @Test
    public void shouldBeCustomService() {
        CSVImporter importer = Framework.getService(CSVImporter.class);
        assertTrue(importer instanceof CSVImporterExtendedImpl);
    }

    @Test
    public void shouldImportCsvZip() {
        DocumentModel testFolder = coreSession.createDocumentModel("/", "testFolder", "Folder");
        testFolder = coreSession.createDocument(testFolder);

        txFeature.nextTransaction();

        File zipFile = FileUtils.getResourceFileFromContext("files/test-csv-importer.zip");
        Blob zipBlob = new FileBlob(zipFile, ZIP_MIMETYPE);

        CSVImporterOptions options = new CSVImporterOptions.Builder().sendEmail(false)
                .importMode(CSVImporterOptions.ImportMode.CREATE)
                .build();

        csvImporter.launchImport(coreSession, testFolder.getPathAsString(), zipBlob, options);

        txFeature.nextTransaction();

        DocumentModelList children = coreSession.query(String.format("Select * From Document Where ecm:ancestorId = '%s'", testFolder.getId()));
        Assert.assertEquals(6, children.size());

        List<DocumentModel> folders = children.stream().filter(child -> "Folder".equals(child.getType())).toList();
        Assert.assertEquals(2, folders.size());

        List<DocumentModel> files = children.stream().filter(child -> !"Folder".equals(child.getType())).toList();
        Assert.assertEquals(4, files.size());
    }

    @Test
    public void shouldImportCsvFile() {
        DocumentModel testFolder = coreSession.createDocumentModel("/", "testFolder", "Folder");
        testFolder = coreSession.createDocument(testFolder);

        txFeature.nextTransaction();

        File zipFile = FileUtils.getResourceFileFromContext("files/meta-data.csv");
        Blob zipBlob = new FileBlob(zipFile);

        CSVImporterOptions options = new CSVImporterOptions.Builder().sendEmail(false)
                .importMode(CSVImporterOptions.ImportMode.CREATE)
                .build();

        csvImporter.launchImport(coreSession, testFolder.getPathAsString(), zipBlob, options);

        txFeature.nextTransaction();

        DocumentModelList children = coreSession.query(String.format("Select * From Document Where ecm:ancestorId = '%s'", testFolder.getId()));
        assertTrue(children.size() > 0);
    }

}
