# ANDS Vocabulary Services Registry

## What the Registry does

The Registry implements a vocabulary metadata catalogue and
repository, and provides an API interface for a vocabulary portal and
for machine-to-machine clients.

## Installation

Please refer to `INSTALL.md` for detailed installation instructions.

### Tomcat deployment

The Registry should be deployed in Tomcat. (Deployment to other
containers may be possible; it has not been tested.)

## Registry functionality

The Registry combines the functions of a metadata catalogue and a
repository. The repository functions were previously undertaken by a
project known as the Vocabulary Toolkit; those functions are now fully
integrated into this Registry.

## Repository functionality

This is an overview of the repository functions:

* Get project metadata from PoolParty
* Harvest
  * PoolParty
  * Sesame
  * SPARQL endpoint
  * File
* Transform
  * Create a tree of SKOS concepts in JSON format
  * Create a list of SKOS concepts in JSON format
* Import
  * Upload data into a Sesame repository
* Publish
  * Create a configuration file for use with the Elda library
* Unpublish
* Unimport
* Unharvest
* Backup
  * Download project exports from PoolParty

## Registry API

The API is accessed via HTTP(S).

## Technology

This section provides some background information on the technology
used to implement the Registry.

### Java

The Registry has been developed using Java 8, and a Java 8 Runtime
Environment is required. Specifically, the code uses lambda
expressions.

### JPA and Hibernate

The Registry uses JPA for database access.  Hibernate is used as the
implementation provider for JPA.  The JPA layer provides database
independence. Nevertheless, because ANDS software has for a long time
used MySQL, the MySQL JDBC driver is included, and the software has
only been tested using MySQL.

### JAX-RS and Jersey

The HTTP-based API is implemented as a set of restlets using
JAX-RS. Strictly speaking, the interface is not RESTful.
Jersey is used as the implementation provider for JAX-RS.

### Sesame

The OpenRDF Sesame libraries are used extensively to implement the
Registry features.

### Apache Commons

The Registry uses Apache Commons components. Currently, Configuration,
IO, and Lang (both versions 2 and 3) are used. (Commons IO is required
by Sesame, but it is also invoked directly from the Registry code.)

## Licence

The Vocabulary Registry is licensed under the Apache License,
version 2.0. See `LICENSE` for details.
