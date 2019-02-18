/** See the file "LICENSE" for the full license governing this code. */

/* The contents of this file are adapted from the file
 * solr/solrj/src/java/org/apache/solr/common/util/SolrJSONWriter.java
 * contained in the Apache Solr 7.7.0 source distribution.
 */

package au.org.ands.vocabs.registry.solr;

import java.io.IOException;
import java.io.Writer;

import org.apache.solr.common.util.FastWriter;

/** Customized SolrJSONWriter that produces results that closely match
 * what comes back from Solr server.
 */
public class ServerSolrJSONWriter implements ServerJsonTextWriter {

    // ARDC: this code is modified from third-party code,
    // so we allow the Checkstyle visibility violations for these fields.

    /** The style to use for NamedLists. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected final String namedListStyle;

    /** The FastWriter wrapperput around the client's Wrapper. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    final FastWriter writer;

    /** Indentation level. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected int level;

    /** Whether or not indenting is used. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected boolean doIndent;

    /** Constructor.
     * @param aWriter The Writer into which the result will be written.
     */
    public ServerSolrJSONWriter(final Writer aWriter) {
        this(aWriter, JSON_NL_MAP);
    }

    /** Constructor.
     * @param aWriter The Writer into which the result will be written.
     * @param aNamedListStyle The style to use for NamedLists.
     */
    public ServerSolrJSONWriter(final Writer aWriter,
            final String aNamedListStyle) {
        if (aWriter == null) {
            this.writer = null;
        } else {
            this.writer = FastWriter.wrap(aWriter);
        }
        this.namedListStyle = aNamedListStyle;
    }

    /** Write an object to the writer.
     * @param o The Object to be written.
     * @return this, so that this method can be used in fluent style.
     * @throws IOException If there is a problem writing the object.
     */
    public ServerSolrJSONWriter writeObj(final Object o) throws IOException {
        writeVal(null, o);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.flushBuffer();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getNamedListStyle() {
        return namedListStyle;
    }

    /** {@inheritDoc} */
    @Override
    public void _writeChar(final char c) throws IOException {
        writer.write(c);
    }

    /** {@inheritDoc} */
    @Override
    public void _writeStr(final String s) throws IOException {
        writer.write(s);
    }

    /** {@inheritDoc} */
    @Override
    public int level() {
        return level;
    }

    /** {@inheritDoc} */
    @Override
    public int incLevel() {
        return ++level;
    }

    /** {@inheritDoc} */
    @Override
    public int decLevel() {
        return --level;
    }

    /** {@inheritDoc} */
    @Override
    public ServerSolrJSONWriter setIndent(final boolean aDoIndent) {
        this.doIndent = aDoIndent;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean doIndent() {
        return doIndent;
    }

    /** {@inheritDoc} */
    @Override
    public Writer getWriter() {
        return writer;
    }

}
