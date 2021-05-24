/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.ecm.csv.core;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class CSVImporterExtendedWork extends CSVImporterWork {

    private static final String MARKER = "meta-data.csv";

    private static final Logger log = LogManager.getLogger(CSVImporterExtendedWork.class);

    protected ZipFile zip = null;

    public CSVImporterExtendedWork(String repositoryName, String parentPath, String username, Blob csvBlob, CSVImporterOptions options) {
        super(repositoryName, parentPath, username, csvBlob, options);
    }

    @Override
    public void work() {
        TransientStore store = getStore();
        setStatus("Importing");
        openUserSession();
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader()
                .withEscape(options.getEscapeCharacter())
                .withCommentMarker(options.getCommentMarker())
                .withIgnoreSurroundingSpaces();
        try {
            getArchiveFileIfValid(getBlob().getFile());
        } catch (IOException e) {
            log.error(e);
        }
        if (zip == null) {
            return;
        }
        ZipEntry index = zip.getEntry(MARKER);

        try (Reader in = new InputStreamReader(zip.getInputStream(index)); CSVParser parser = csvFormat.parse(in)) {
            doImport(parser);
        } catch (IOException e) {
            logError(0, "Error while doing the import: %s", LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT, e.getMessage());
            log.debug(e, e);
        }
        store.putParameter(id, "logs", importLogs);
        if (options.sendEmail()) {
            setStatus("Sending email");
            sendMail();
        }
        setStatus(null);
    }

    @Override
    protected Blob createBlobFromFilePath(String fileRelativePath) throws IOException {
        ZipEntry blobIndex = zip.getEntry(fileRelativePath);
        if (blobIndex != null) {
            Blob blob;
            try {
                blob = Blobs.createBlob(zip.getInputStream(blobIndex));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            blob.setFilename(fileRelativePath);
            return blob;
        } else {
            return null;
        }
    }

    public ZipFile getArchiveFileIfValid(File file) throws IOException {
        try {
            zip = new ZipFile(file);
        } catch (ZipException e) {
            log.debug("file is not a zipfile ! ", e);
            return null;
        } catch (IOException e) {
            log.debug("can not open zipfile ! ", e);
            return null;
        }

        ZipEntry marker = zip.getEntry(MARKER);

        if (marker == null) {
            zip.close();
            return null;
        } else {
            return zip;
        }
    }

    public Blob getBlob() {
        return getStore().getBlobs(id).get(0);
    }

}
