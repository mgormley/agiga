package edu.jhu.agiga;

import java.util.Iterator;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

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
    
    private static Logger log = Logger.getLogger(StreamingDocumentReader.class);
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
        ConsoleAppender cAppender = new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%t] %p %c %x - %m%n"));
        BasicConfigurator.configure(cAppender);
        // Must be Level.TRACE for debug logging
        Logger.getRootLogger().setLevel(Level.DEBUG);

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
