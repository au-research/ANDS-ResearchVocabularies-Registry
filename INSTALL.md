# Vocabs Registry installation

These instructions are a work in progress!

Please also refer to `README.md` for the time being, for some of the
missing bits.

(e.g., TBD: reference to
https://github.com/au-research/ANDS-ResearchVocabularies-LDA
for LDA custom configuration.)

# Prerequisites

* JDK for Java 8
* Ant
* gawk version 4
* MySQL
* Solr
* Tomcat

# Locale setting for JDK

The Java source code has characters encoded in UTF-8. You must have a
locale setting in your environment that means that UTF-8 will be
understood by the JDK.

In the ANDS environment, the command `echo $LANG` prints:
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

# gawk 4

Gawk (i.e., GNU Awk) version 4 is required by the script
`src/CrawlerDetect/generate-matcher.sh` to generate the Java source
file
`src/main/java/au/org/ands/vocabs/registry/utils/BotDetector.java`.
If the output of the command `awk --version` does _not_ indicate that
the `awk` command is gawk 4 or later, you may need to download and
install gawk 4. You _don't_ need to install it as the default for
`awk`; you only need to have it installed _somewhere_ in the file
system.

Once you have done that, configure the `GAWK` environment variable to
point to the gawk 4 executable. For example, for bash, if the gawk 4
executable is `/opt/gawk-4.2.1/bin`, add this to `.bashrc`:

```
# gawk 4 is needed to run generate-matcher.sh
export GAWK=/opt/gawk-4.2.1/bin
```

# Configuration files

Configuration files for toolkit, registry, roles are in the directory
conf.


## Create Toolkit configuration

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
chown 755 /var/vocab-files/registry-data
chown tomcat.tomcat /var/vocab-files/registry-data
```

### Solr configuration

Set Registry.Solr.collection to your choice of name for the Solr
collection. In the following, we have chosen `vocabs-registry`.

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
INSERT INTO subject_resolver_sources(source,iri) VALUES ('anzsrc-for', 'http://vocabs.ands.org.au/repository/api/sparql/anzsrc-for');
INSERT INTO subject_resolver_sources(source,iri) VALUES ('anzsrc-seo', 'http://vocabs.ands.org.au/repository/api/sparql/anzsrc-seo');
INSERT INTO subject_resolver_sources(source,iri) VALUES ('gcmd', 'http://vocabs.ands.org.au/repository/api/sparql/gcmd-sci');
```

Now run:

```
wget -O - http://localhost:8080/registry-context/adminApi/database/populateSubjectResolver
```

# Create Solr collection and install schema

## Create the collection

Create the Solr collection. Here, `vocabs-registry` matches the name
chosen for the collection and assigned in `registry.properties` above.

```
/path-to-Solr-installation/bin/solr create -c vocabs-registry
```

## Install the schema

```
ant create-solr-schema
```

## Force indexing of the vocabularies into Solr

Run:

```
wget -O - http://localhost:8080/registry-context/adminApi/solr/index
```

Once again, it is crucial to check the log file of the Registry to see
if there are any log messages related to indexing. If Solr is not
responding, you will see an error message of priority "SEVERE".
In this case, restart Solr and try again.

# Create PHP Client API

`ant php-client`

# Create JavaScript Client API

`ant js-client`

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

For example, to roll back to the tag `version_0001`, run:

```
tools/dist/liquibase-3.5.3/liquibase --defaultsFile=registry-liquibase-superuser.properties --changeLogFile=src/main/db/changelog/registry-master.xml rollback version_0001
```
