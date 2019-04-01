/** See the file "LICENSE" for the full license governing this code. */

/* The contents of this file are adapted from the file
 * solr/solrj/src/java/org/apache/solr/common/util/JsonTextWriter.java
 * contained in the Apache Solr 7.7.0 source distribution.
 */

package au.org.ands.vocabs.registry.solr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.MapWriter;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.JsonTextWriter;

/** Custom JSON Writer that gives a result closely matching
 * what comes back from Solr server. */
public interface ServerJsonTextWriter extends JsonTextWriter {

     @Override
     default void writeMap(final MapWriter val) throws IOException {
         writeMapOpener(-1);
         incLevel();

         val.writeMap(new MapWriter.EntryWriter() {
             // ARDC: this code is modified from third-party code,
             // so we allow the Checkstyle visibility violation.
             @SuppressWarnings("checkstyle:VisibilityModifier")
             boolean isFirst = true;

             @Override
             public MapWriter.EntryWriter put(final CharSequence k,
                     final Object v) throws IOException {
                 if (isFirst) {
                     isFirst = false;
                 } else {
                     ServerJsonTextWriter.this.writeMapSeparator();
                 }
                 ServerJsonTextWriter.this.indent();
                 ServerJsonTextWriter.this.writeKey(k.toString(), true);
                 // Special treatment for the "response" key, where
                 // the value is a SolrDocumentList.
                 if (k.toString().equals("response")
                         && v instanceof SolrDocumentList) {
                     SolrDocumentList vSDL = (SolrDocumentList) v;
                     // Make a map, whose structure matches that
                     // of what comes back from Solr server.
                     Map<String, Object> sdlMap = new HashMap<>();
                     sdlMap.put("docs", v);
                     sdlMap.put("numFound", vSDL.getNumFound());
                     sdlMap.put("start", vSDL.getStart());
                     writeVal(k.toString(), sdlMap);
                 } else {
                     writeVal(k.toString(), v);
                 }
                 return this;
             }
         });
         decLevel();
         writeMapCloser();
     }

}
