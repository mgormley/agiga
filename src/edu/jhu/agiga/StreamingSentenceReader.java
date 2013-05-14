package edu.jhu.agiga;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StreamingSentenceReader is an iterator over AgigaSentence objects. This
 * bypasses the document level annotations such as coref and the document ids
 * and provides direct access to the sentence annotations only.
 * 
 * @author mgormley
 * 
 */
public class StreamingSentenceReader extends StreamingVtdXmlReader<AgigaSentence> {
    
    private static Logger log = Logger.getLogger(StreamingSentenceReader.class.getName());
    private AgigaPrefs prefs;

    public StreamingSentenceReader(String inputFile, AgigaPrefs prefs) {
        super(inputFile);
        this.prefs = prefs;
    }

    @Override
    protected Iterator<AgigaSentence> getIteratorInstance(byte[] b) {
        return new AgigaSentenceReader(b, prefs);
    }

    public static void main(String args[]) throws Exception {
        // Must be Level.FINER for debug logging
        Util.initializeLogging(Level.FINE);

        // Parse each file provided on the command line.
        for (int i = 0; i < args.length; i++) {
            StreamingSentenceReader reader = new StreamingSentenceReader(args[i], new AgigaPrefs());
            log.info("Parsing XML");
            for (AgigaSentence sent : reader) { 
                // Do nothing
            }
            log.info("Number of sentences: " + reader.getNumSents());
        }
    }

    @Override
    protected int getNumSents(AgigaSentence sent) {
        return 1;
    }

}
