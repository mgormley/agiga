package edu.jhu.agiga;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StreamingDocumentReader is an iterator over AgigaDocument objects. The
 * AgigaDocument class gives access to the coreference resolution (via
 * AgigaCoref objects) annotations and the sentences (via AgigaSentence
 * objects).
 * 
 * @author mgormley
 * 
 */
public class StreamingDocumentReader extends StreamingVtdXmlReader<AgigaDocument> {
    
    private static Logger log = Logger.getLogger(StreamingDocumentReader.class.getName());
    private AgigaPrefs prefs;

    public StreamingDocumentReader(String inputFile, AgigaPrefs prefs) {
        super(inputFile);
        this.prefs = prefs;
    }

    @Override
    protected Iterator<AgigaDocument> getIteratorInstance(byte[] b) {
        return new AgigaDocumentReader(b, prefs);
    }

    public static void main(String args[]) throws Exception {
        // Must be Level.FINER for debug logging
        Util.initializeLogging(Level.FINE);

        // Parse each file provided on the command line.
        for (int i = 0; i < args.length; i++) {
            StreamingDocumentReader reader = new StreamingDocumentReader(args[i], new AgigaPrefs());
            log.info("Parsing XML");
            for (AgigaDocument doc : reader) { 
                // Do nothing
            }
            log.info("Number of docs: " + reader.getNumDocs());
        }
    }

    @Override
    protected int getNumSents(AgigaDocument doc) {
        return doc.getSents().size();
    }

}
