package edu.jhu.agiga;

import static edu.jhu.agiga.AgigaSentenceReader.require;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.PilotException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

/**
 * Provides an iterator over AgigaDocument objects given an Annotated Gigaword
 * file. This class should usually not be used directly since VTD-XML will load
 * the entire XML file into memory, and requires that the file be unzipped.
 * Instead, StreamingDocumentReader should be used which provides a fast,
 * memory-efficient version of this iterator.
 * 
 * This implementation using VTD-XML should handle XML files up to 2GB in size.
 * For larger files, we can switch to extended VTD-XML as described here:
 * <url>http://vtd-xml.sourceforge.net/codeSample/cs12.html</url>
 * 
 * @author mgormley
 *
 */
class AgigaDocumentReader implements Iterable<AgigaDocument>, Iterator<AgigaDocument> {

    private static Logger log = Logger.getLogger(AgigaDocumentReader.class.getName());

    private boolean hasNext;
    private int numDocs;

    private VTDNav vn;
    private AutoPilot docAp;

    private AgigaPrefs prefs = new AgigaPrefs();
    
    public AgigaDocumentReader(String inputFile, AgigaPrefs prefs) {
        try {
            this.prefs = prefs;
            // Read the file into a byte array
            log.fine("Reading file into byte array");
            File f = new File(inputFile);
            InputStream fis = new FileInputStream(f);
            log.fine("File size: " + f.length());
            byte[] b = new byte[(int)f.length()];
            fis.read(b);
            fis.close();
            
            init(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public AgigaDocumentReader(byte[] b, AgigaPrefs prefs) {
        this.prefs = prefs;
        init(b);
    }

    private void init(byte[] b) {
        try {            
            // Index the xml with VTD-XML
            log.fine("Building VTD index");
            VTDGen vg = new VTDGen();
            vg.setDoc(b);
            vg.parse(false);
            vn = vg.getNav();

            numDocs = 0;
            vn.toElement(VTDNav.ROOT);

            // Initialize auto pilot
            init();
        } catch (NavException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() {
        try {
            docAp = new AutoPilot(this.vn);
            docAp.selectElement(AgigaConstants.DOC);
            hasNext = docAp.iterate();
        } catch (NavException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<AgigaDocument> iterator() {
        return this;
    }
    
    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public AgigaDocument next() {
        try {
            String docId = vn.toString(vn.getAttrVal(AgigaConstants.DOC_ID));
            String docType = vn.toString(vn.getAttrVal(AgigaConstants.DOC_TYPE));
            log.finer("doc id=" + docId);
            log.finer("doc type=" + docType);
    
            AgigaDocument agigaDoc = new AgigaDocument(prefs);
            agigaDoc.setDocId(docId);
            agigaDoc.setType(docType);
            
            // Read the sentences
            log.finer("Reading sents");
            if (vn.toElement(VTDNav.FIRST_CHILD, AgigaConstants.SENTENCES)) {
                AgigaSentenceReader sentReader = new AgigaSentenceReader(vn.cloneNav(), prefs);
                for (AgigaSentence agigaSent : sentReader) {
                    agigaDoc.add(agigaSent);
                }
                vn.toElement(VTDNav.PARENT);
            }
            
            // Read the coreference resolution annotations
            if (prefs.readCoref) {
                log.finer("Reading corefs");
                List<AgigaCoref> agigaCorefs = parseCorefs(vn.cloneNav());
                agigaDoc.setCorefs(agigaCorefs);
            }
            
            numDocs++;
            
            hasNext = docAp.iterate();
            
            return agigaDoc;
        } catch(NavException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented");        
    }


    public int getNumDocs() {
        return numDocs;
    }
    
    /**
     * Assumes the position of vn is at a "DOC" tag
     */
    private List<AgigaCoref> parseCorefs(VTDNav vn) throws PilotException, NavException {
        require (vn.matchElement(AgigaConstants.DOC));
        
        List<AgigaCoref> agigaCorefs = new ArrayList<AgigaCoref>();
        if (!vn.toElement(VTDNav.FIRST_CHILD, AgigaConstants.COREFERENCES)) {
            // If there is no coref annotation return the empty list
            log.finer("No corefs found");
            return agigaCorefs;
        }

        // Loop through each token
        AutoPilot corefAp = new AutoPilot(vn);
        corefAp.selectElement(AgigaConstants.COREFERENCE);
        while (corefAp.iterate()) {
            AgigaCoref coref = parseCoref(vn.cloneNav());
            agigaCorefs.add(coref);
        }
        return agigaCorefs;
    }
    
    private AgigaCoref parseCoref(VTDNav vn) throws NavException {
        require (vn.matchElement(AgigaConstants.COREFERENCE));
        AgigaCoref coref = new AgigaCoref();
        
        AutoPilot mentionAp = new AutoPilot(vn);
        mentionAp.selectElement(AgigaConstants.MENTION);
        while (mentionAp.iterate()) {
            int repInt = vn.getAttrVal(AgigaConstants.MENTION_REPRESENTATIVE);
            boolean isRepresentative = false;
            if (repInt != -1) {
                isRepresentative = Boolean.parseBoolean(vn.toString(repInt));
            }

            // Subtract one, since the sentences and tokens are one-indexed in
            // the XML but zero-indexed in this API
            require(vn.toElement(VTDNav.FC, AgigaConstants.M_SENTENCE));
            int sentenceId = vn.parseInt(vn.getText()) - 1;

            require(vn.toElement(VTDNav.NS, AgigaConstants.START));
            int startTokenId = vn.parseInt(vn.getText()) - 1;

            require(vn.toElement(VTDNav.NS, AgigaConstants.END));
            int endTokenId = vn.parseInt(vn.getText()) - 1;

            require(vn.toElement(VTDNav.NS, AgigaConstants.HEAD));
            int headTokenId = vn.parseInt(vn.getText()) - 1;

            AgigaMention agigaMention = new AgigaMention(isRepresentative, sentenceId, startTokenId, endTokenId, headTokenId);
            coref.add(agigaMention);
        }
        return coref;
    }
    
    public static void main(String args[]) throws Exception {
        // Must be Level.FINER for debug logging
        Util.initializeLogging(Level.FINE);

        // Parse each file provided on the command line.
        for (int i = 0; i < args.length; i++) {
            AgigaDocumentReader reader = new AgigaDocumentReader(args[i], new AgigaPrefs());
            log.fine("Parsing XML");
            for (AgigaDocument agigaDoc : reader) { 
                // Do nothing
            }
            log.info("Number of documents: " + reader.getNumDocs());
        }
    }

}
