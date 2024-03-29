# Vocabs Registry installation

Please also refer to `README.md` for information about the Registry.

The following instructions are a work in progress.
Although correct, and mostly complete, there should be more added
about integration with the related services.

(For example, you will need to install Sesame server and SISSVoc.)

(For example, TBD: add a reference to
https://github.com/au-research/ANDS-ResearchVocabularies-LDA
for LDA custom configuration.)

# Prerequisites

* JDK for Java 8
* Ant
* gawk version 4
* MySQL (at least version 5.6)
* Solr (at least version 8.1 is required)
  * Solr must be run in SolrCloud mode. You may use either the
    embedded ZooKeeper, or use an external ZooKeeper.
* Tomcat (at least version 7)
* npm (for packing the generated JavaScript Client API)

# Locale setting for JDK

The Java source code has characters encoded in UTF-8. You must have a
locale setting in your environment that means that UTF-8 will be
understood by the JDK.

In the ARDC environment, the command `echo $LANG` prints:
en_AU.UTF-8

If, for you, the command `echo $LANG` prints an empty response, or a
response that does not include `UTF-8` in the variant (i.e., in what
is printed after the "."), set the `LANG` variable to include it in
the variant. For example (for Bourne shell/bash):

```
export LANG=en_AU.UTF-8
```

or

```
export LANG=C.UTF-8
```

# gawk 4.2

Gawk (i.e., GNU Awk) version 4.2 or later is required by the script
`src/CrawlerDetect/generate-matcher.sh` to generate the Java source
file
`src/main/java/au/org/ands/vocabs/registry/utils/BotDetector.java`.
If the output of the command `awk --version` does _not_ indicate that
the `awk` command is gawk 4.2 or later, you may need to download and
install a recent release of gawk. You _don't_ need to install it as
the default for `awk`; you only need to have it installed _somewhere_
in the file system.

Once you have done that, configure the `GAWK` environment variable to
point to the gawk executable. For example, for bash, if the gawk
executable is `/opt/gawk-4.2.1/bin`, add this to `.bashrc`:

```
# gawk 4.2 is needed to run generate-matcher.sh
export GAWK=/opt/gawk-4.2.1/bin
```

# Configuration files

Configuration files for toolkit, registry, roles are in the directory
conf.


## Create Toolkit configuration

If you will be migrating content from an existing Toolkit
installation, you need to define at least the database properties for
that installation.

Create `conf/toolkit.properties`. You may base it on
`conf/toolkit.properties.sample`.

### Elda/SISSVoc Config template

The Toolkit property `SISSVoc.specTemplate` must point to a file that
contains a template which will be used in the generation of
configuration ('spec') files as used by the Elda library.  Please
refer to `conf/ANDS-ELDAConfig-template.ttl.sample` for an example.

### Elda/SISSVoc XSL transform

The Toolkit property `SISSVoc.variable.HTML_STYLESHEET` must point to
an XSL transform used by Elda/SISSVoc to generate HTML pages.  Either,
copy `lda/resources/default/transform/ands-ashtml-sissvoc.xsl` into
the `resources/default/transform` directory of your deployed instance
of SISSVoc, or make use of the file(s) provided in the repository
https://github.com/au-research/ANDS-ResearchVocabularies-LDA.  (There
is a version of `ands-ashtml-sissvoc.xsl` contained in the directory
`common/resources/default/transform`.)


## Create Registry configuration

### Web application properties

Create `conf/registry.properties`. You may base it on
`conf/registry.properties.sample`.

You decide on the Registry database JDBC URL, username, and password
here, and you will need them in subsequent steps. They are referred to
in subsequent steps as `registry-url`, `registry-user`, and
`registry-password`.

The setting `Registry.storagePath` points to a directory in the file
system where the Registry stores all of the vocabulary data. This
directory must exist, and be writable by the user that runs
Tomcat. For example, if you are using
`/var/vocab-files/registry-data`, you might do something like this to
create it:
```
sudo sh
mkdir -p /var/vocab-files/registry-data
chmod 755 /var/vocab-files/registry-data
chown tomcat.tomcat /var/vocab-files/registry-data
```

### Language Subtag Registry

The Registry uses the IANA Language Subtag Registry (LSR) to resolve
BCP 47 language codes. A sample instance of this file is contained in
this repository as `conf/language-subtag-registry`. You may use
this, or download and use the latest version from
[https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry].

The setting `Registry.lsr` must be configured to point to the file as
installed. You may choose to keep the example setting, which is
`${Registry.storagePath}/conf/language-subtag-registry`.

For example, if you are using
`/var/vocab-files/registry-data` as your storage path, you might do
something like this:
```
sudo sh
mkdir -p /var/vocab-files/registry-data/conf/
cp conf/language-subtag-registry /var/vocab-files/registry-data/conf/
chmod -R 755 /var/vocab-files/registry-data/conf
chown -R tomcat.tomcat /var/vocab-files/registry-data/conf
```

(The `chmod` and `chown` commands make the ownership and permissions
consistent with the rest of the Registry's storage.)

### Solr configuration

Set `Registry.Solr.baseURL` to the base URL of your Solr installation,
and `Registry.Solr.zkHost` to the zkZost setting of your ZooKeeper.

Set `Registry.Solr.collection` to your choice of name for the Solr
collection used for registry entities. In the following, we have
chosen `vocabs-registry`.

Set `Registry.Solr.resources.collection` to your choice of name for
the Solr collection for vocabulary resources (SKOS concepts, etc.). In
the following, we have chosen `vocabs-resources`.

### SMTP configuration

The Registry can send email notifications for updates to vocabularies.
This functionality requires a correctly-configured SMTP server.

The file `scripts/send_email_notifications.sh.sample` is a sample bash
script that can be invoked by a cron task to send weekly
notifications. If you wish to use the script, please take note of its
dependency on GNU date. The script uses the logging configuration in
the file `scripts/logback-email_notifications.xml`.

## Create Roles configuration

Create `conf/roles.properties`. You may base it on
`conf/roles.properties.sample`.

## Configure logging

The file `conf/logback.xml` is the logging configuration for the
Registry. You may modify it to suit your needs.

## Liquibase properties

Create `conf/registry-liquibase.properties`. You may base it on
`registry-liquibase.properties.sample`.

In order to create the tables in the registry database, the database
user must have the "CREATE" privilege. If the database user used by
the Registry web app does not have that privilege, create another
`registry-liquibase-superuser.properties` configuration file that
specifies the username and password of a database user that has the
necessary privileges. Then, specify that configuration file in the
Liquibase command line.

# Create the Registry database

## Initialize the Registry database

Connect to MySQL. For the following steps, you may need to connect as
a database administrator user.

Create the `registry-user` database user, if it does not already exist.

```
CREATE DATABASE `vocabs_registry` DEFAULT CHARACTER SET utf8mb4;
GRANT ALL PRIVILEGES ON `vocabs_registry`.* TO 'registry-user'@'localhost';
```

## Create Registry database tables

```
tools/dist/liquibase-3.5.3/liquibase --defaultsFile=conf/registry-liquibase.properties dropAll; ant -Dregistrydb-properties=conf/registry-liquibase.properties registry-database-update
```

Or, if you have created a `registry-liquibase-superuser.properties`
configuration file as mentioned above, use it as follows:

```
tools/dist/liquibase-3.5.3/liquibase --defaultsFile=registry-liquibase-superuser.properties dropAll; ant -Dregistrydb-properties=registry-liquibase-superuser.properties registry-database-update
```

# Build the Toolkit/Registry web application

`ant war`

# Deploy and start the Toolkit/Registry web application

Deploy the resulting `vocabs-registry.war` into Tomcat.

You may choose to deploy it using a Tomcat context file in order to
select a convenient context path.

Whatever you decide, in the following, the context path is taken to be 
`http://localhost:8080/registry-context`.

Check the (Tomcat) log files to confirm that the web application is
running.

# Migrate any existing content from an "old-style" vocabs database

To proceed, the web application must have been successfully deployed.

Access to the migration process is locked-down to the loopback
address; in the following, you must specify "localhost" or its IP
address "127.0.0.1".

Migrate the content from the "old-style" database into the new, blank
Registry database:

```
wget -O - http://localhost:8080/registry-context/adminApi/database/migrateToolkitToRegistry
```

It is crucial to check the log file of the Registry to see if there
are any log messages related to migration that have priority
"ERROR". If there are, an error has occurred during migration that
needs to be addressed. Note that the migration may have continued
anyway, and reported completion, in order to migrate as much data as
possible; nevertheless, the errors need to be addressed. When that has
been done, you will need to clear out the database (see "Create
Registry database tables" above) and run the migration again.

# Define PoolParty server for the Registry

Connect to the MySQL Registry database as the `registry-user` user.

Define the main PoolParty server as follows.
Replace the strings `hostname`, `username`, `password` with the
hostname, username, and password you specified in `conf/toolkit.properties`.

```
INSERT INTO poolparty_servers(id,api_url,username,password) VALUES (1,"hostname","username","password");
```

The Registry software does not currently make use of any additional
PoolParty servers you define in this way.

# Define subject resolver sources, and populate subject resolution

Connect to the MySQL Registry database as the `registry-user` user.

```
INSERT INTO subject_resolver_sources(source,iri) VALUES ('anzsrc-for', 'http://vocabs.ardc.edu.au/repository/api/sparql/anzsrc-for');
INSERT INTO subject_resolver_sources(source,iri) VALUES ('anzsrc-seo', 'http://vocabs.ardc.edu.au/repository/api/sparql/anzsrc-seo');
INSERT INTO subject_resolver_sources(source,iri) VALUES ('gcmd', 'http://vocabs.ardc.edu.au/repository/api/sparql/gcmd-sci');
```

Access to the populate process is locked-down to the loopback address;
in the following, you must specify "localhost" or its IP address
"127.0.0.1".

Now run:

```
wget -O - http://localhost:8080/registry-context/adminApi/database/populateSubjectResolver
```

# Create Solr collections and install schemas

## Add custom query plugin

The Registry's search function uses a custom Solr query plugin.

You must install the plugin into _every_ Solr instance that is part of
your SolrCloud setup.

In the top level of each Solr instance, make a directory `ardc`.
(For example, if your Solr instance is installed in `/opt/solr`,
create a directory `/opt/solr/ardc`.)

Copy the JAR file from `lib/ifpress-solr-plugin-*/*.jar` (currently,
that is `lib/ifpress-solr-plugin-1.6.2/ifpress-solr-plugin-1.6.2.jar`)
into the `ardc` directory in your Solr installation.

Note: there must be only one version of the plugin in the `ardc`
directory of your Solr installation. If you ever upgrade the plugin,
make sure to remove any old version.

## Create the Solr collections

Create the two Solr collections. Here, `vocabs-registry` and
`vocabs-resources` match the names chosen for the collections and
assigned in `registry.properties` above.  The output of the create
command includes a recommendation to run another command to disable
auto-creation of fields. You don't need to run that command, as the
installation of the Solr schemas in the next step makes that setting
for you.

```
/path-to-Solr-installation/bin/solr create -c vocabs-registry
/path-to-Solr-installation/bin/solr create -c vocabs-resources
```

If your setting of `Registry.Solr.baseURL` includes a port that isn't
Solr's default, you'll need to specify that for these commands. E.g.,
if you set `Registry.Solr.baseURL = http://localhost:8988/solr`, the
collection creation commands must be given as:

```
/path-to-Solr-installation/bin/solr create -p 8988 -c vocabs-registry
/path-to-Solr-installation/bin/solr create -p 8988 -c vocabs-resources
```

## Install the two Solr schemas

Use this build target to upload a custom `solrconfig.xml` (from the
file `conf/solrconfig.xml`) and to create the Solr schemas.

```
ant create-solr-schema
ant create-solr-schema-resources
```

Very important: if you need to re-install the Solr schemas (either
because something went wrong, or you're upgrading the schemas), use
the command line to delete the collections and start again:

```
/path-to-Solr-installation/bin/solr delete -c vocabs-registry
/path-to-Solr-installation/bin/solr delete -c vocabs-resources
```

or, specifing a non-default port:

```
/path-to-Solr-installation/bin/solr delete -p 8988 -c vocabs-registry
/path-to-Solr-installation/bin/solr delete -p 8988 -c vocabs-resources
```

This way, you'll also delete each collection's corresponding "config
set", which contains the value of the "schema version".  (See
`src/main/java/au/org/ands/vocabs/registry/solr/admin/SolrSchemaBase.java`
for details of how this is set/read.)  If instead, you use Solr's web
interface to delete the collections, the config sets won't be deleted,
and recreating the collections will create them with the existing
config sets, including any previous setting of the schema version, so
that any subsequent attempt to do schema installation stops
immediately.

## Force indexing of the vocabularies into Solr

If vocabulary metadata has been migrated from a Toolkit database or
from another Registry installation, it's necessary to force indexing
of the migrated vocabularies' metadata (and concept data) within Solr.

This API method is authenticated, and requires a user with the
`VOCABS_REGISTRY_SUPERUSER` functional role. In the following, supply
the appropriate values in place of `username` and `pass`:

Run:

```
wget --user=username --password=pass -O - http://localhost:8080/registry-context/adminApi/solr/index
```

Once again, it is crucial to check the log file of the Registry to see
if there are any log messages related to indexing. If Solr is not
responding, you will see an error message of priority "SEVERE".
In this case, restart Solr and try again.

# Create PHP Client API

The PHP code of the Portal accesses the Registry using a client
library API generated from the Registry code itself. To generate this
PHP library:

`ant php-client`

# Create JavaScript Client API

The browser-side JavaScript code of the Portal also accesses the
Registry using a client library API generated from the Registry code
itself. To generate this JavaScript library and make it available to
the Portal's own build process in the expected format (a `.tgz` file):

```
ant js-client
( cd js-client; npm pack )
```

# Optional: use automated webapp deploy/start/stop

If you want to be able to script the deployment of the Registry webapp
to Tomcat, and/or to script starting/stopping the webapp, please see
the section in `build.xml` that begins with the comment
"Support deployment to Tomcat".

It's easiest if you have a local installation of Tomcat, so that you
have the file `catalina-tasks.xml` that is part of the Tomcat
distribution. Otherwise, you will need, at least, to download a copy
of the Tomcat distribution and unpack it. Then, edit `build.xml` and
change the file attribute of the `include` statement:
```
<include optional="true" file="/usr/share/tomcat/bin/catalina-tasks.xml"/>
```
so that it points to the correct location of `catalina-tasks.xml`.

Here is a summary of the steps involved to configure deployment:

* In the Tomcat installation to which you will be deploying, ensure
  that in Tomcat's `tomcat-users.xml`, you have a suitable user
  defined that has the `manager-script` role. For example:
  ```
  <user name="deploy" password="my-password" roles="manager-script" />
  ```
* If you add a username/password in this way, you need to restart
  Tomcat so that it loads `tomcat-users.xml` afresh.
* In the top-level of your checkout of the Registry code, create
  a file `deployer.properties` containing the URL of the target
  Tomcat, the username/password you specified, and the context
  path. For example (following the running example used in these
  instructions), the file might look like this:
  ```
  tomcat.url=http://localhost:8080/manager/text
  tomcat.username=deploy
  tomcat.password=my-password
  tomcat.context=/registry-context
  ```

Now you can run `ant deploy`, `ant stop`, and `ant start`.

# Subsequent updating of the database schema

When updating the registry to a later version, there may be changes
to the database schema. To apply them, run:

```
ant -Dregistrydb-properties=registry-liquibase-superuser.properties registry-database-update
```

## Rollback

If necessary, it is possible to roll back the database schema to an
earlier form. There are tags defined at the end of each stage of the
schema definition; see the files in `src/main/db/changelog`.

For example, to roll back to the tag `version_0003`, run:

```
tools/dist/liquibase-3.5.3/liquibase --defaultsFile=registry-liquibase-superuser.properties --changeLogFile=src/main/db/changelog/registry-master.xml rollback version_0003
```
