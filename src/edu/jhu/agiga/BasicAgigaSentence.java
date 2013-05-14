package edu.jhu.agiga;

import static edu.jhu.agiga.AgigaSentenceReader.require;

import java.io.IOException;
import java.io.Writer;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.agiga.AgigaConstants.DependencyForm;

/**
 * Implements the basic functionality of the AgigaSentence interface.
 * 
 * @author mgormley
 *
 */
public class BasicAgigaSentence implements Serializable {

    private static Logger log = Logger.getLogger(BasicAgigaSentence.class);

	public static final long serialVersionUID = 1;

    protected AgigaPrefs prefs = new AgigaPrefs();
    
    private int sentIdx;

    // Contains the word, lemma, token offsets, POS tag, NER tag, and
    // normalized NER
    private List<AgigaToken> tokens;

    // This only contains the text form of the constituency parse. For an
    // object representation, use StanfordAgigaSetence.getStanfordContituencyParse().
    private String parseText;
    
    // Working with the Stanford structures is awkward and slow because the
    // TreeGraphNode converts the WordLemmaTag to a CyclicCoreLabel which
    // doesn't contain the POS tag, so instead we use our own typed dependency
    // data structure and lazily construct the stanford versions.
    //
    // More importantly this allows us to leave our dependence on the Stanford
    // NLP toolkit as purely optional.
    private List<AgigaTypedDependency> basicDeps;
    private List<AgigaTypedDependency> colDeps;
    private List<AgigaTypedDependency> colCcprocDeps;

    public BasicAgigaSentence(AgigaPrefs prefs) {
        this.prefs = prefs;
    }
    
    public void setTokens(List<AgigaToken> tokens) {
        this.tokens = tokens;
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#getAgigaDeps(edu.jhu.hltcoe.sp.data.depparse.DependencyForm)
     */
    public List<AgigaTypedDependency> getAgigaDeps(DependencyForm form) {
        List<AgigaTypedDependency> agigaDeps;
        if (form == DependencyForm.BASIC_DEPS) {
            agigaDeps = basicDeps;
        } else if (form == DependencyForm.COL_DEPS) {
            agigaDeps = colDeps;
        } else if (form == DependencyForm.COL_CCPROC_DEPS) {
            agigaDeps = colCcprocDeps;
        } else {
            throw new IllegalStateException("Unsupported DependencyForm: " + form);
        }
        return agigaDeps;
    }
    

    private void requireDeps(DependencyForm form, String message) {
        if (form == DependencyForm.BASIC_DEPS) {
            require(prefs.readBasicDeps, message);
        } else if (form == DependencyForm.COL_DEPS) {
            require(prefs.readColDeps, message);
        } else if (form == DependencyForm.COL_CCPROC_DEPS) {
            require(prefs.readColCcprocDeps, message);
        } else {
            throw new IllegalStateException("Unsupported DependencyForm: " + form);
        }
    }
    
    // -------------- Printing functions ----------------- 
    
    public void writeWords(Writer writer) throws IOException {
        writeTokens(writer, false, false);
    }
    
    public void writeLemmas(Writer writer) throws IOException {
        writeTokens(writer, true, false);
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#writeTags(java.io.Writer, boolean, boolean, boolean)
     */
    public void writeTokens(Writer writer, boolean useLemmas, boolean useNormNer) throws IOException {
        for (int i=0; i<tokens.size(); i++) {
            AgigaToken tok = tokens.get(i);
            if (useNormNer && tok.getNormNer() != null) {
                require(prefs.readNormNer, 
                        "AgigaPrefs.readNormNer must be true if useNormNer=true for writeTokens()");
                writer.write(tok.getNormNer());
            } else if (useLemmas) {
                require(prefs.readLemma, 
                        "AgigaPrefs.readLemma must be true if useLemmas=true for writeTokens()");
                writer.write(tok.getLemma());
            } else {
                require(prefs.readWord, 
                        "AgigaPrefs.readWord must be true if useLemmas and useNormNer are false for writeTokens()");
                writer.write(tok.getWord());                
            }
            if (i < tokens.size()-1) {
                writer.write(" ");
            }
        }
        writer.write("\n");
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#writePosTags(java.io.Writer)
     */
    public void writePosTags(Writer writer) throws IOException {
        writeTags(writer, false, false, false);
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#writeNerTags(java.io.Writer)
     */
    public void writeNerTags(Writer writer) throws IOException {
        writeTags(writer, false, false, true);
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#writeTags(java.io.Writer, boolean, boolean, boolean)
     */
    public void writeTags(Writer writer, boolean useLemmas, boolean useNormNer, boolean useNerTags) throws IOException {
        for (int i=0; i<tokens.size(); i++) {
            AgigaToken tok = tokens.get(i);
            if (useNormNer && tok.getNormNer() != null) {
                require(prefs.readNormNer, 
                        "AgigaPrefs.readNormNer must be true if useNormNer=true for writeTags()");
                writer.write(tok.getNormNer());
            } else if (useLemmas) {
                require(prefs.readLemma, 
                        "AgigaPrefs.readLemma must be true if useLemmas=true for writeTags()");
                writer.write(tok.getLemma());
            } else {
                require(prefs.readWord, 
                        "AgigaPrefs.readWord must be true if useLemmas and useNormNer are false for writeTags()");
                writer.write(tok.getWord());                
            }
            writer.write("/");
            if (useNerTags) {
                require(prefs.readNer,
                        "AgigaPrefs.readNer must be true if useNerTags=true for writeTags()");
                if (prefs.strict) { 
                    writer.write(tok.getNerTag());
                } else {
                    if (tok.getNerTag() != null) {
                        writer.write(tok.getNerTag());
                    } else {
                        log.warn("Missing NER annotation written as '__MISSING_NER_ANNOTATION__'");
                        writer.write("__MISSING_NER_ANNOTATION__");
                    }
                }
            } else {
                require(prefs.readPos,
                        "AgigaPrefs.readPos must be true if useNerTags=false for writeTags()");
                writer.write(tok.getPosTag());
            }
            if (i < tokens.size()-1) {
                writer.write(" ");
            }
        }
        writer.write("\n");
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#writeConnlStyleDeps(java.io.Writer, edu.jhu.hltcoe.sp.data.depparse.DependencyForm)
     */
    public void writeConnlStyleDeps(Writer writer, DependencyForm form) throws IOException {
        requireDeps(form,
            "AgigaPrefs.{readWord,readLemma,readPos} and the dependency form in AgigaPrefs must be true for writeConnlStyleDeps()");
        require(prefs.readWord && prefs.readLemma && prefs.readPos,
            "AgigaPrefs.{readWord,readLemma,readPos} and the dependency form in AgigaPrefs must be true for writeConnlStyleDeps()");
        List<AgigaTypedDependency> agigaDeps = getAgigaDeps(form);
        
        int[] parents = new int[tokens.size()];
        Arrays.fill(parents, -2);
        String[] types = new String[tokens.size()];
        for (AgigaTypedDependency dep : agigaDeps) {
            parents[dep.getDepIdx()] = dep.getGovIdx();
            types[dep.getDepIdx()] = dep.getType();
        }
        
        for (int i=0; i<tokens.size(); i++) {
            AgigaToken tok = tokens.get(i);
            
            // Numbered comments contain the description of fields from http://ilk.uvt.nl/conll/
            // Field number:     Field name:     Description:
            //1    ID  Token counter, starting at 1 for each new sentence.
            // Note: add one since the CONNL-X format is one-indexed
            writer.write(String.valueOf(i+1));
            writer.write("\t");
            //2    FORM    Word form or punctuation symbol.
            writer.write(tok.getWord());
            writer.write("\t");
            //3    LEMMA   Lemma or stem (depending on particular data set) of word form, or an underscore if not available.
            writer.write(tok.getLemma());
            writer.write("\t");
            //4    CPOSTAG  Coarse-grained part-of-speech tag, where tagset depends on the language.
            writer.write(tok.getPosTag());
            writer.write("\t");
            //5    POSTAG  Fine-grained part-of-speech tag, where the tagset depends on the language, or identical to the coarse-grained part-of-speech tag if not available.
            writer.write(tok.getPosTag());
            writer.write("\t");
            //6    FEATS   Unordered set of syntactic and/or morphological features (depending on the particular language), separated by a vertical bar (|), or an underscore if not available.
            writer.write("_\t");
            if (types[i] != null) {
                //7    HEAD    Head of the current token, which is either a value of ID or zero ('0'). Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
                // Note: add one since the CONNL-X format is one-indexed
                writer.write(String.valueOf(parents[i] + 1));
                writer.write("\t");
                //8    DEPREL  Dependency relation to the HEAD. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
                writer.write(types[i]);
                writer.write("\t");
            } else {
                writer.write("_\t_\t");
            }
            
            //9    PHEAD   Projective head of current token, which is either a value of ID or zero ('0'), or an underscore if not available. Note that depending on the original treebank annotation, there may be multiple tokens an with ID of zero. The dependency structure resulting from the PHEAD column is guaranteed to be projective (but is not available for all languages), whereas the structures resulting from the HEAD column will be non-projective for some sentences of some languages (but is always available).
            writer.write("_\t");
            //10   PDEPREL     Dependency relation to the PHEAD, or an underscore if not available. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
            writer.write("_\t");
            writer.write("\n");
        }
        // Sentences are separated by a blank line
        writer.write("\n");
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#writeParseText(java.io.Writer)
     */
    public void writeParseText(Writer writer) throws IOException {
        require(prefs.readParse, "AgigaPrefs.readParse must be true for writeParseText()");
        writer.write(parseText);
        writer.write("\n");
    }
    
    // -------------- Auto generated getters/setters below ----------------- 

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#getSentId()
     */
    public int getSentIdx() {
        return sentIdx;
    }
    
    public void setSentIdx(int sentIdx) {
        this.sentIdx = sentIdx;
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#getTokens()
     */
    public List<AgigaToken> getTokens() {
        return tokens;
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#getParseText()
     */
    public String getParseText() {
        return parseText;
    }

    public void setParseText(String parseText) {
        this.parseText = parseText;
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#getBasicDeps()
     */
    public List<AgigaTypedDependency> getBasicDeps() {
        return basicDeps;
    }

    public void setBasicDeps(List<AgigaTypedDependency> basicDeps) {
        this.basicDeps = basicDeps;
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#getColDeps()
     */
    public List<AgigaTypedDependency> getColDeps() {
        return colDeps;
    }

    public void setColDeps(List<AgigaTypedDependency> colDeps) {
        this.colDeps = colDeps;
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.sp.data.depparse.AgigaSentence#getColCcprocDeps()
     */
    public List<AgigaTypedDependency> getColCcprocDeps() {
        return colCcprocDeps;
    }

    public void setColCcprocDeps(List<AgigaTypedDependency> colCcprocDeps) {
        this.colCcprocDeps = colCcprocDeps;
    }

    @Override
    public boolean equals(Object other) {
        if(other == null) return false;
        if(other instanceof BasicAgigaSentence) {
            BasicAgigaSentence o = (BasicAgigaSentence) other;
            return Util.safeEquals(prefs, o.prefs)
                && sentIdx == o.sentIdx
                && Util.safeEquals(tokens, o.tokens)
                && Util.safeEquals(parseText, o.parseText)
                && Util.safeEquals(basicDeps, o.basicDeps)
                && Util.safeEquals(colDeps, o.colDeps)
                && Util.safeEquals(colCcprocDeps, o.colCcprocDeps);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.safeHashCode(prefs,
            sentIdx, tokens, parseText, basicDeps, colDeps, colCcprocDeps);
    }

}
