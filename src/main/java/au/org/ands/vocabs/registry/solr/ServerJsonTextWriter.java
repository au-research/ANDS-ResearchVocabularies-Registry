/** See the file "LICENSE" for the full license governing this code. */

/* The contents of this file are adapted from the file
 * solr/solrj/src/java/org/apache/solr/common/util/JsonTextWriter.java
 * contained in the Apache Solr 7.7.0 source distribution.
 */

package au.org.ands.vocabs.registry.solr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.solr.common.EnumFieldValue;
import org.apache.solr.common.IteratorWriter;
import org.apache.solr.common.MapSerializable;
import org.apache.solr.common.MapWriter;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.JsonTextWriter;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.WriteableValue;
import org.apache.solr.search.ReturnFields;

/** Custom JSON Writer that gives a result closely matching
 * what comes back from Solr server. */
public interface ServerJsonTextWriter extends JsonTextWriter {

    // ARDC: this method is copied from
    // org.apache.solr.common.util.TextWriter.writeVal(), and has
    // been modified to include a case for SolrDocumentList,
    // based on org.apache.solr.response.TextResponseWriter.writeVal().
    // ARDC: this code is modified from third-party code,
    // so we allow the raw types.
    @Override
    @SuppressWarnings("rawtypes")
    default void writeVal(final String name, final Object val)
            throws IOException {
        // if there get to be enough types, perhaps hashing on the type
        // to get a handler might be faster (but types must be
        // exact to do that...)
        //    (see a patch on LUCENE-3041 for inspiration)

        // go in order of most common to least common, however
        // some of the more general types like Map belong towards the end
        if (val == null) {
          writeNull(name);
        } else if (val instanceof CharSequence) {
          writeStr(name, val.toString(), true);
          // micro-optimization... using toString() avoids a cast first
        } else if (val instanceof Number) {
          writeNumber(name, (Number) val);
        } else if (val instanceof Boolean) {
          writeBool(name, (Boolean) val);
        } else if (val instanceof AtomicBoolean)  {
          writeBool(name, ((AtomicBoolean) val).get());
        } else if (val instanceof Date) {
          writeDate(name, (Date) val);
        } else if (val instanceof NamedList) {
          writeNamedList(name, (NamedList) val);
        } else if (val instanceof Path) {
          writeStr(name, ((Path) val).toAbsolutePath().toString(), true);
        } else if (val instanceof IteratorWriter) {
          writeIterator((IteratorWriter) val);
        } else if (val instanceof MapWriter) {
          writeMap((MapWriter) val);
        } else if (val instanceof MapSerializable) {
          //todo find a better way to reuse the map more efficiently
          writeMap(name, ((MapSerializable) val).toMap(new LinkedHashMap<>()),
                  false, true);
        } else if (val instanceof Map) {
          writeMap(name, (Map) val, false, true);
        } else if (val instanceof Iterator) {
            // very generic; keep towards the end
          writeArray(name, (Iterator) val);
        } else if (val instanceof SolrDocumentList) {
            // ARDC: This is the
            writeSolrDocumentList(name, (SolrDocumentList) val, null);
        } else if (val instanceof Iterable) {
            // very generic; keep towards the end
          writeArray(name, ((Iterable) val).iterator());
        } else if (val instanceof Object[]) {
          writeArray(name, (Object[]) val);
        } else if (val instanceof byte[]) {
          byte[] arr = (byte[]) val;
          writeByteArr(name, arr, 0, arr.length);
        } else if (val instanceof EnumFieldValue) {
          writeStr(name, val.toString(), true);
        } else if (val instanceof WriteableValue) {
          ((WriteableValue) val).write(name, this);
        } else {
          // default... for debugging only.  Would be nice to "assert false" ?
          writeStr(name, val.getClass().getName() + ':' + val.toString(), true);
        }
      }

    // This method is copied from
    // org.apache.solr.response.writeSolrDocumentList().
    /** Write a SolrDocumentList.
     * @param name The "name" of the list. But this value is not used.
     * @param docs The SolrDocumentList to be written.
     * @param fields A list of fields to be included. But this value
     *      is not used, and can be null.
     * @throws IOException If there is an exception when writing the
     *      SolrDocumentList.
     */
    default void writeSolrDocumentList(final String name,
            final SolrDocumentList docs, final ReturnFields fields)
                    throws IOException {
      writeStartDocumentList(name, docs.getStart(), docs.size(),
              docs.getNumFound(), docs.getMaxScore());
      for (int i = 0; i < docs.size(); i++) {
        writeSolrDocument(null, docs.get(i), fields, i);
      }
      writeEndDocumentList();
    }

    /** Write the start of a SolrDocumentList.
     * @param name The "name" of the list. But this value is not used.
     * @param start The "start" value of the SolrDocumentList.
     * @param size The "size" value of the SolrDocumentList.
     * @param numFound The "numFound" value of the SolrDocumentList.
     * @param maxScore The "maxScore" value of the SolrDocumentList.
     * @throws IOException If there is an exception when writing the
     *      end of the SolrDocumentList.
     */
    void writeStartDocumentList(String name, long start,
            int size, long numFound, Float maxScore) throws IOException;

    /** Write one SolrDocument.
     * @param name The "name" of the SolrDocument. But this value is not used.
     * @param doc The SolrDocument to be written.
     * @param fields A list of fields to be included. But this value
     *      is not used, and can be null.
     * @param idx The zero-based index of this document in the list. Used to
     *      determine when to write a list separator.
     * @throws IOException If there is an exception when writing the
     *      SolrDocument.
     */
    void writeSolrDocument(String name, SolrDocument doc,
            ReturnFields fields, int idx) throws IOException;

    /** Write the end of a SolrDocumentList.
     * @throws IOException If there is an exception when writing the
     *      end of the SolrDocumentList.
     */
    void writeEndDocumentList() throws IOException;

}
