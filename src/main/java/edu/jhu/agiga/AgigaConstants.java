package edu.jhu.agiga;

/**
 * This class contains the names of the XML tags and attributes used in
 * Annotated Gigaword .xml.gz files.
 * 
 * @author mgormley
 * 
 */
public class AgigaConstants {
    
    // XML Dependency Parse Tag names
    public enum DependencyForm {
        BASIC_DEPS("basic-dependencies"), 
        COL_DEPS("collapsed-dependencies"), 
        COL_CCPROC_DEPS("collapsed-ccprocessed-dependencies");

        private String xmlTag;

        private DependencyForm(String xmlTag) {
            this.xmlTag = xmlTag;
        }

        public String getXmlTag() {
            return xmlTag;
        }
    }
    
    // XML Tag names
    public static final String FILE = "FILE";    
    public static final String FILE_ID = "id";    
    
    public static final String DOC = "DOC";

    public static final String SENTENCES = "sentences";
    public static final String SENTENCE = "sentence";
    public static final String TOKEN = "token";
    public static final String TOKEN_ID = "id";
    public static final String WORD = "word";
    public static final String LEMMA = "lemma";
    public static final String POS = "POS";
    public static final String NER = "NER";
    public static final String NORM_NER = "NormNER";
    public static final String PARSE = "parse";
    public static final String DEP = "dep";
    public static final String DEP_TYPE = "type";
    public static final String GOVERNOR = "governor";
    public static final String DEPENDENT = "dependent";
    
    public static final String COREFERENCES = "coreferences";
    public static final String COREFERENCE = "coreference";
    public static final String MENTION = "mention";
    public static final String M_SENTENCE = "sentence";
    public static final String START = "start";
    public static final String END = "end";
    public static final String HEAD = "head";
    
    public static final String HEADLINE = "HEADLINE";
    public static final String DATELINE = "DATELINE";

    // XML Attribute names
    public static final String DOC_ID = "id";
    public static final String DOC_TYPE = "type";

    public static final String CHARACTER_OFFSET_BEGIN = "CharacterOffsetBegin";
    public static final String CHARACTER_OFFSET_END = "CharacterOffsetEnd";

    public static final String MENTION_REPRESENTATIVE = "representative";
    
    private AgigaConstants() {
        // private constructor
    }
    
}
