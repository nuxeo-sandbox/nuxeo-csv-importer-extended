# Description

This repository contains An extension of the Nuxeo Platform CSV importer with support for zip archives containing both a metadata csv file and the binary files
# How to build

```
git clone https://github.com/nuxeo-sandbox/nuxeo-csv-importer-extended
cd nuxeo-csv-importer-extended
mvn clean install
```

To build the plugin without building the Docker image, use:

```
mvn -DskipDocker=true clean install
```


# How to use

This plugin extends the default CSV import plugin. All the [documentation of the CSV importer](https://doc.nuxeo.com/nxdoc/nuxeo-csv/) remains applicable. In addition to this, the plugin brings the ability to upload a zip file instead of a simple .csv file.
The expected structure of the archive is the following:
- a metadata.csv file which follows the [format](https://doc.nuxeo.com/nxdoc/nuxeo-csv/#csv-file-definition) of the Nuxeo CSV importer
- binary files

To link a binary file to a document, the binary path in the zip archive must be set in the name column of the csv file

An sample zip archive is available [here](https://github.com/nuxeo-sandbox/nuxeo-csv-importer-extended/blob/master/nuxeo-csv-importer-extended-core/src/test/resources/files/meta-data.csv)

# Known limitations
This plugin is a work in progress.

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# Nuxeo Marketplace
This plugin is published on the [marketplace]((https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-csv-importer-extended))

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com)
