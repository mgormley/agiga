package edu.jhu.agiga;

/**
 * Each AgigaMention object represents a single mention of an entity for
 * coreference resolution within a document. Annotated Gigaword does not
 * directly provide a unique identifier for each entity in a document. However,
 * AgigaDocument.assignMucStyleIdsAndRefsToMentions() can be used if such ids
 * are needed -- this method will set the mucId and mucRef fields as if they
 * were the ID and REF attributes of a MUC-7 mention.
 * 
 * @author mgormley
 * 
 */
public class AgigaMention {

    public static final int UNASSIGNED = -1;
    // Agiga annotations
    private boolean isRepresentative;
    private int sentenceIdx;
    private int startTokenIdx;
    private int endTokenIdx;
    private int headTokenIdx;

    // Additional annotations for MUC style printout
    private int mucId;
    private int mucRef;

    public AgigaMention(boolean isRepresentative, int sentenceIdx, int startTokenIdx, int endTokenIdx, int headTokenIdx) {
        this.isRepresentative = isRepresentative;
        this.sentenceIdx = sentenceIdx;
        this.startTokenIdx = startTokenIdx;
        this.endTokenIdx = endTokenIdx;
        this.headTokenIdx = headTokenIdx;
        this.mucId = UNASSIGNED;
        this.mucRef = UNASSIGNED;
    }

    public boolean isRepresentative() {
        return isRepresentative;
    }

    public int getSentenceIdx() {
        return sentenceIdx;
    }

    public int getStartTokenIdx() {
        return startTokenIdx;
    }

    public int getEndTokenIdx() {
        return endTokenIdx;
    }

    public int getHeadTokenIdx() {
        return headTokenIdx;
    }

    public int getMucId() {
        return mucId;
    }

    public int getMucRef() {
        return mucRef;
    }

    public void setMucId(int id) {
        this.mucId = id;
    }

    public void setMucRef(int ref) {
        this.mucRef = ref;
    }

    @Override
    public String toString() {
        return "AgigaMention [endTokenId=" + endTokenIdx + ", headTokenId=" + headTokenIdx + ", id=" + mucId
                + ", isRepresentative=" + isRepresentative + ", ref=" + mucRef + ", sentenceId=" + sentenceIdx
                + ", startTokenId=" + startTokenIdx + "]";
    }

}
