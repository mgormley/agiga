package edu.jhu.agiga;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.PilotException;
import com.ximpleware.VTDException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

import edu.jhu.agiga.AgigaConstants.DependencyForm;

/**
 * Provides an iterator over AgigaSentence objects given an Annotated Gigaword
 * file. This class should usually not be used directly since VTD-XML will load
 * the entire XML file into memory, and requires that the file be unzipped.
 * Instead, StreamingSentenceReader should be used which provides a fast,
 * memory-efficient version of this iterator.
 * 
 * This implementation using VTD-XML should handle XML files up to 2GB in size.
 * For larger files, we can switch to extended VTD-XML as described here:
 * <url>http://vtd-xml.sourceforge.net/codeSample/cs12.html</url>
 * 
 * @author mgormley
 * 
 */
class AgigaSentenceReader implements Iterable<AgigaSentence>, Iterator<AgigaSentence> {

    private static final String NULL_NER_TAG = "0";

    private static Logger log = Logger.getLogger(AgigaSentenceReader.class.getName());

    private int numSentences;

    private VTDNav vn;
    private AutoPilot sentAp;

    private AgigaPrefs prefs;

    private int nextIdx = -1;
    
    public AgigaSentenceReader(String inputFile, AgigaPrefs prefs) {
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
    
    public AgigaSentenceReader(byte[] b, AgigaPrefs prefs) {
        this.prefs = prefs;
        init(b);
    }
    
    public AgigaSentenceReader(VTDNav vn, AgigaPrefs prefs) {
        this.prefs = prefs;
        this.vn = vn;
        init();
    }

    private void init(byte[] b) {
        try {            
            // Index the xml with VTD-XML
            log.fine("Building VTD index");
            VTDGen vg = new VTDGen();
            vg.setDoc(b);
            vg.parse(false);
            vn = vg.getNav();

            numSentences = 0;
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
            sentAp = new AutoPilot(this.vn);
            //sentAp.selectXPath(String.format("//%s[@id]", AgigaConstants.SENTENCE));
            sentAp.selectXPath(String.format("//%s/%s", AgigaConstants.SENTENCES, AgigaConstants.SENTENCE));
            nextIdx  = sentAp.evalXPath();
        } catch (VTDException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<AgigaSentence> iterator() {
        return this;
    }
    
    @Override
    public boolean hasNext() {
        return nextIdx != -1;
    }

    @Override
    public AgigaSentence next() {
        try {
            int sentId = vn.parseInt(vn.getAttrVal(AgigaConstants.TOKEN_ID));
            log.finer("sentence id=" + sentId);
    
            StanfordAgigaSentence agigaSent = getSentenceInstance(prefs);
            // Subtract one, since the sentences are one-indexed in the XML but
            // zero-indexed in this API
            agigaSent.setSentIdx(sentId - 1);
            
            // Below we use clone nav to avoid having to find the "sent" tag
            // again
            if (prefs.readWord || prefs.readLemma || prefs.readOffsets || prefs.readPos || prefs.readNer || prefs.readNormNer) {
                List<AgigaToken> agigaTokens = parseTokens(vn.cloneNav());
                agigaSent.setTokens(agigaTokens);
            }
            if (prefs.readParse) {
                String parseText = parseParse(vn.cloneNav());
                agigaSent.setParseText(parseText);
            }
            if (prefs.readBasicDeps) {
                List<AgigaTypedDependency> basicDeps = parseDependencies(vn.cloneNav(), DependencyForm.BASIC_DEPS);
                agigaSent.setBasicDeps(basicDeps);
            }
            if (prefs.readColDeps) {
                List<AgigaTypedDependency> colDeps = parseDependencies(vn.cloneNav(), DependencyForm.COL_DEPS);
                agigaSent.setColDeps(colDeps);
            }
            if (prefs.readColCcprocDeps) {
                List<AgigaTypedDependency> colCcprocDeps = parseDependencies(vn.cloneNav(), DependencyForm.COL_CCPROC_DEPS);
                agigaSent.setColCcprocDeps(colCcprocDeps);
            }

            // Note that if we instead wanted to find the sent element using 
            // XPath evaluation, we could do the following. 
            //            basicDepRelAp.selectXPath(String.format("//sent[@id=%d]", sentId));
            //            require(basicDepRelAp.evalXPath() != -1);
            //
            // We could alternatively move up through the tree to find it, after calling parseTokens().
            //            // Move back up to the <sent> tag
            //            while (!vn.matchElement(AgigaConstants.SENTENCE)) {
            //                vn.toElement(VTDNav.PARENT);
            //            }
                
    
            numSentences++;
            
            nextIdx  = sentAp.evalXPath();
            
            return agigaSent;
        } catch(VTDException e) {
            throw new RuntimeException(e);
        }
    }

    protected StanfordAgigaSentence getSentenceInstance(AgigaPrefs prefs) {
        return new StanfordAgigaSentence(prefs);
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented");        
    }

    public int getNumSentences() {
        return numSentences;
    }
    
    /**
     * Assumes the position of vn is at a AgigaConstants.SENTENCE tag
     * @param tree 
     * @return 
     */
    private List<AgigaToken> parseTokens(VTDNav vn) throws PilotException, NavException {
        require (vn.matchElement(AgigaConstants.SENTENCE));

        int tokId = -1;
        
        List<AgigaToken> agigaTokens = new ArrayList<AgigaToken>();
        
        // Loop through each token
        AutoPilot tokAp = new AutoPilot(vn);
        tokAp.selectElement(AgigaConstants.TOKEN);
        while (tokAp.iterate()) {
            // Just double check that the tokens are in order
            if (tokId < 0) {
                tokId = vn.parseInt(vn.getAttrVal(AgigaConstants.TOKEN_ID));
            }
            require (vn.parseInt(vn.getAttrVal(AgigaConstants.TOKEN_ID)) == tokId);

            AgigaToken agigaToken = new AgigaToken();
            // Subtract one, since the tokens are one-indexed in the XML but
            // zero-indexed in this API
            agigaToken.setTokIdx(tokId - 1);
            
            // Read the word, lemma, token offsets, POS tag, NER tag, and
            // normalized NER
            
            // We have to move to the word (first child) so that the next
            // sibling moves succeed.
            require(vn.toElement(VTDNav.FC, AgigaConstants.WORD));
            if (prefs.readWord) {
                String word = vn.toString(vn.getText());
                agigaToken.setWord(word);
            }

            if (prefs.readLemma) {
                require(vn.toElement(VTDNav.NS, AgigaConstants.LEMMA));
                String lemma = vn.toString(vn.getText());
                agigaToken.setLemma(lemma);
            }

            if (prefs.readOffsets) {
                require(vn.toElement(VTDNav.NS, AgigaConstants.CHARACTER_OFFSET_BEGIN));
                int charOffBegin;
                if (prefs.strict) {
                    charOffBegin = Integer.parseInt(vn.toString(vn.getText()));
                } else {
                    // Remove unexpected whitespace surrounding the integer.
                    charOffBegin = Integer.parseInt(vn.toString(vn.getText()).trim());
                }
                agigaToken.setCharOffBegin(charOffBegin);
                require(vn.toElement(VTDNav.NS, AgigaConstants.CHARACTER_OFFSET_END));
                int charOffEnd;
                if (prefs.strict) {
                    charOffEnd = Integer.parseInt(vn.toString(vn.getText()));
                } else {
                    // Remove unexpected whitespace surrounding the integer.
                    charOffEnd = Integer.parseInt(vn.toString(vn.getText()).trim());
                }
                agigaToken.setCharOffEnd(charOffEnd);
            }

            if (prefs.readPos) {
                require(vn.toElement(VTDNav.NS, AgigaConstants.POS));
                String posTag = vn.toString(vn.getText());
                agigaToken.setPosTag(posTag);
            }

            if (prefs.readNer) {
                if (prefs.strict) {
                    require(vn.toElement(VTDNav.NS, AgigaConstants.NER));
                    String nerTag = vn.toString(vn.getText());
                    agigaToken.setNerTag(nerTag);
                } else {
                    String nerTag = null;
                    if (vn.toElement(VTDNav.NS, AgigaConstants.NER)) {
                        nerTag = vn.toString(vn.getText());
                    }
                    agigaToken.setNerTag(nerTag);
                }
            }
            if (prefs.readNormNer) {
                // NormNER only applies to some tokens
                String normNer = null;
                if (vn.toElement(VTDNav.NS, AgigaConstants.NORM_NER)) {
                    normNer = vn.toString(vn.getText());
                }
                agigaToken.setNormNer(normNer);
            }

            agigaTokens.add(agigaToken);
            
            tokId++;
        }
        
        return agigaTokens;
    }
    
    /**
     * Assumes the position of vn is at a AgigaConstants.SENTENCE tag
     * @return 
     */
    private String parseParse(VTDNav vn) throws NavException,
            PilotException {
        require (vn.matchElement(AgigaConstants.SENTENCE));

        // Move to the <parse> tag
        require (vn.toElement(VTDNav.FC, AgigaConstants.PARSE));
        String parseText = vn.toString(vn.getText());
        
        return parseText;
    }

    /**
     * Assumes the position of vn is at a "sent" tag
     * @return 
     */
    private List<AgigaTypedDependency> parseDependencies(VTDNav vn, DependencyForm form) throws NavException,
            PilotException {
        require (vn.matchElement(AgigaConstants.SENTENCE));

        // Move to the <basic-deps> tag
        require (vn.toElement(VTDNav.FC, form.getXmlTag()));

        List<AgigaTypedDependency> agigaDeps = new ArrayList<AgigaTypedDependency>();
        
        // Loop through the dep tags
        AutoPilot basicDepRelAp = new AutoPilot(vn);
        basicDepRelAp.selectElement(AgigaConstants.DEP);
        while (basicDepRelAp.iterate()) {
            // Read the type, governor, and dependent
            String type = vn.toString(vn.getAttrVal(AgigaConstants.DEP_TYPE));
            require (vn.toElement(VTDNav.FC, AgigaConstants.GOVERNOR));
            int governorId = vn.parseInt(vn.getText());
            require (vn.toElement(VTDNav.NS, AgigaConstants.DEPENDENT));
            int dependentId = vn.parseInt(vn.getText());

            log.finer(String.format("\tdep type=%s\t%d-->%d", type, governorId, dependentId));

            // Subtract one, since the tokens are one-indexed in the XML but
            // zero-indexed in this API
            AgigaTypedDependency agigaDep = new AgigaTypedDependency(type, governorId - 1, dependentId - 1);
            agigaDeps.add(agigaDep);
        }
        return agigaDeps;
    }

    /**
     * This method will print out the XML from the current position of
     * <code>vn</code>. Very useful for debugging.
     */
    public static String getElementFragmentAsString(byte[] b, VTDNav vn) throws NavException {
        long l = vn.getElementFragment();
        int offset = (int) l;
        int len = (int) (l >> 32);
        String elementFragment = new String(Arrays.copyOfRange(b, offset, offset + len));
        return elementFragment;
    }
    
    public static void main(String args[]) throws Exception {
        // Must be Level.FINER for debug logging
        Util.initializeLogging(Level.FINE);

        // Parse each file provided on the command line.
        for (int i = 0; i < args.length; i++) {
            AgigaSentenceReader reader = new AgigaSentenceReader(args[i], new AgigaPrefs());
            log.fine("Parsing XML");
            for (AgigaSentence agigaSent : reader) { 
                // Do nothing
            }
            log.info("Number of sentences: " + reader.getNumSentences());
        }
    }
    
    // TODO: any call to require should also have a message explaining what condition wasn't met
    public static void require(boolean truth) {
        if (!truth) {
            throw new IllegalStateException();
        }
    }
    
    public static void require(boolean truth, String message) {
        if (!truth) {
            throw new IllegalStateException(message);
        }
    }

}
