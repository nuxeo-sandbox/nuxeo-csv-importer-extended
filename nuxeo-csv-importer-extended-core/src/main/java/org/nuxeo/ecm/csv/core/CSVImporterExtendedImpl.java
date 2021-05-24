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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

public class CSVImporterExtendedImpl extends CSVImporterImpl {

    public static final String ZIP_MIMETYPE = "application/zip";

    @Override
    public String launchImport(CoreSession session, String parentPath, Blob blob, CSVImporterOptions options) {
        MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
        String mimetype = registry.getMimetypeFromFilenameAndBlobWithDefault(blob.getFilename(), blob, null);

        if (ZIP_MIMETYPE.equals(mimetype)) {
            return new CSVImporterExtendedWork(session.getRepositoryName(), parentPath, session.getPrincipal().getName(), blob,
                    options).launch();
        } else {
            return new CSVImporterWork(session.getRepositoryName(), parentPath, session.getPrincipal().getName(), blob,
                    options).launch();
        }
    }

}
