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
import java.util.List;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class CSVImporterExtendedWork extends CSVImporterWork {

    private static final String MARKER = "metadata.csv";

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
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setEscape(options.getEscapeCharacter())
                .setCommentMarker(options.getCommentMarker())
                .setIgnoreSurroundingSpaces(true).build();
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
    protected Blob createBlobFromFilePath(String fileRelativePath) {
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

    @Override
    protected Serializable convertValue(CompositeType compositeType, String fieldName, String headerValue,
                                        String stringValue, long lineNumber) {
        if (compositeType.hasField(fieldName)) {
            Field field = compositeType.getField(fieldName);
            if (field != null) {
                try {
                    Serializable fieldValue = null;
                    Type fieldType = field.getType();
                    if (fieldType.isComplexType()) {
                        if (fieldType instanceof ComplexType complex) {
                            if (complex.getFieldsCount() == 6 && complex.hasField("digest")) {
                                fieldValue = (Serializable) createBlobFromFilePath(stringValue);
                                if (fieldValue == null) {
                                    logError(lineNumber, "The file '%s' does not exist",
                                            LABEL_CSV_IMPORTER_NOT_EXISTING_FILE, stringValue);
                                    return null;
                                }
                            }
                        } else {
                            fieldValue = (Serializable) ComplexTypeJSONDecoder.decode((ComplexType) fieldType,
                                    stringValue);
                            replaceBlobs((Map<String, Object>) fieldValue);
                        }
                    } else {
                        if (fieldType.isListType()) {
                            Type listFieldType = ((ListType) fieldType).getFieldType();
                            if (listFieldType.isSimpleType()) {
                                /*
                                 * Array.
                                 */
                                fieldValue = stringValue.split(options.getListSeparatorRegex());
                            } else {
                                /*
                                 * Complex list.
                                 */
                                fieldValue = (Serializable) ComplexTypeJSONDecoder.decodeList((ListType) fieldType,
                                        stringValue);
                                replaceBlobs((List<Object>) fieldValue);
                            }
                        } else {
                            /*
                             * Primitive type.
                             */
                            Type type = field.getType();
                            if (type instanceof SimpleTypeImpl) {
                                type = type.getSuperType();
                            }
                            if (type.isSimpleType()) {
                                if (type instanceof StringType) {
                                    fieldValue = stringValue;
                                } else if (type instanceof IntegerType) {
                                    fieldValue = Integer.valueOf(stringValue);
                                } else if (type instanceof LongType) {
                                    fieldValue = Long.valueOf(stringValue);
                                } else if (type instanceof DoubleType) {
                                    fieldValue = Double.valueOf(stringValue);
                                } else if (type instanceof BooleanType) {
                                    fieldValue = Boolean.valueOf(stringValue);
                                } else if (type instanceof DateType) {
                                    DateFormat dateFormat = options.getDateFormat();
                                    fieldValue = dateFormat != null ? dateFormat.parse(stringValue) : stringValue;
                                }
                            }
                        }
                    }
                    return fieldValue;
                } catch (ParseException | NumberFormatException | IOException e) {
                    logError(lineNumber, "Unable to convert field '%s' with value '%s'",
                            LABEL_CSV_IMPORTER_CANNOT_CONVERT_FIELD_VALUE, headerValue, stringValue);
                    log.debug(e, e);
                }
            }
        } else {
            logError(lineNumber, "Field '%s' does not exist on type '%s'", LABEL_CSV_IMPORTER_NOT_EXISTING_FIELD,
                    headerValue, compositeType.getName());
        }
        return null;
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
