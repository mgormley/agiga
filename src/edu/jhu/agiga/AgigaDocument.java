package edu.jhu.agiga;

import static edu.jhu.agiga.AgigaSentenceReader.require;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

/**
 * AgigaDocument provides access to the AgigaSentence and AgigaCoref objects,
 * the document id, and the document type. This class also provides a method for
 * writing out the coreference resolution annotations in a MUC style SGML output
 * format.
 * 
 * @author mgormley
 * 
 */
public class AgigaDocument {

    private static Logger log = Logger.getLogger(AgigaDocument.class);

    private String docId;
    private String type;
    private List<AgigaSentence> sents;
    private List<AgigaCoref> corefs;
    private AgigaPrefs prefs;

    public AgigaDocument(AgigaPrefs prefs) {
        this.prefs = prefs;
        sents = new ArrayList<AgigaSentence>();
        if (prefs.readCoref) {
            corefs = new ArrayList<AgigaCoref>();
        }
    }

    public void add(AgigaSentence agigaSent) {
        sents.add(agigaSent);
    }

    public void setCorefs(List<AgigaCoref> corefs) {
        this.corefs = corefs;
    }

    public List<AgigaSentence> getSents() {
        return sents;
    }

    public List<AgigaCoref> getCorefs() {
        return corefs;
    }

    public AgigaPrefs getPrefs() {
        return prefs;
    }
    
    public String getDocId() {
        return docId;
    }

    public void setDocId(String id) {
        this.docId = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    // TODO: should this move to an external class? it might be tricky to do so
    public void writeMucStyleCoref(Writer writer) throws IOException {
        require(prefs.readWord && prefs.readCoref, 
                "AgigaPrefs.{readWord,readCoref} must be true for writeMucStyleCoref()");
                
        assignMucStyleIdsAndRefsToMentions();
        
        AgigaMention[] mentionArray = getAllMentions().toArray(new AgigaMention[0]);
        Arrays.sort(mentionArray, new StartMentionComparator());
        LinkedList<AgigaMention> mentionStarts = new LinkedList<AgigaMention>(Arrays.asList(mentionArray));
        log.trace("Total number of mentions: " + mentionStarts.size());
        PriorityQueue<AgigaMention> mentionEnds = new PriorityQueue<AgigaMention>(11, new EndMentionComparator());
        
        log.trace("Number of sentences: " + sents.size());
        for (int s=0; s<sents.size(); s++) {
            AgigaSentence sent = sents.get(s);
            List<AgigaToken> tokens = sent.getTokens();
            log.trace("Number of tokens: " + tokens.size());
            for (int i=0; i<tokens.size()+1; i++) {
                while (mentionEnds.size() > 0 && mentionEnds.peek().getSentenceIdx() == s && mentionEnds.peek().getEndTokenIdx() == i) {
                    mentionEnds.remove();
                    writer.write("</COREF>");
                }
                if (i > 0 && i < tokens.size()) {
                    writer.write(" ");
                }
                if (mentionEnds.size() > 0 && (mentionEnds.peek().getSentenceIdx() < s 
                        || (mentionEnds.peek().getSentenceIdx() == s && mentionEnds.peek().getEndTokenIdx() < i))) {
                    writer.flush();
                    log.error(mentionEnds);
                    throw new RuntimeException(String.format("Overlapping coref elements. s=%d i=%d", s, i));
                }
                while (mentionStarts.size() > 0 && mentionStarts.peek().getSentenceIdx() == s && mentionStarts.peek().getStartTokenIdx() == i) {
                    AgigaMention head = mentionStarts.pop();
                    if (head.isRepresentative()) {
                        writer.write(String.format("<COREF ID=%d>", head.getMucId()));
                    } else {
                        writer.write(String.format("<COREF ID=%d REF=%d>", head.getMucId(), head.getMucRef()));
                    }
                    mentionEnds.add(head);
                } 
                if (i >= tokens.size()) {
                    break;
                }
                AgigaToken tok = tokens.get(i);
                writer.write(tok.getWord());
            }
            require(mentionEnds.size() == 0);
            writer.write("\n");
        }
        writer.write("\n");
    }

    private static class StartMentionComparator implements Comparator<AgigaMention> {
        
        @Override
        public int compare(AgigaMention m1, AgigaMention m2) {
            int val = m1.getSentenceIdx() - m2.getSentenceIdx();
            if (val != 0) {
                return val;
            }
            val = m1.getStartTokenIdx() - m2.getStartTokenIdx();
            if (val != 0) {
                return val;
            }
            // For overlapping mentions starting at the same token, we want the
            // later one to start on the left
            val = m2.getEndTokenIdx() - m1.getEndTokenIdx();
            return val;
        }
        
    }
    
    private static class EndMentionComparator implements Comparator<AgigaMention> {
        
        @Override
        public int compare(AgigaMention m1, AgigaMention m2) {
            int val = m1.getSentenceIdx() - m2.getSentenceIdx();
            if (val != 0) {
                return val;
            }
            return m1.getEndTokenIdx() - m2.getEndTokenIdx();
        }
        
    }
    
    private List<AgigaMention> getAllMentions() {
        List<AgigaMention> allMentions = new ArrayList<AgigaMention>();
        for (AgigaCoref coref : corefs) {
            allMentions.addAll(coref.getMentions());
        }
        return allMentions;
    }

    private void assignMucStyleIdsAndRefsToMentions() {
        // Create IDs and REFs as in MUC-7
        int id = 0;
        for (AgigaCoref coref : corefs) {
            int representativeId = -1;
            for (AgigaMention mention : coref.getMentions()) {
                mention.setMucId(id++);
                if (mention.isRepresentative()) {
                    representativeId = mention.getMucId();
                }
            }
            for (AgigaMention mention : coref.getMentions()) {
                if (!mention.isRepresentative()) {
                    mention.setMucRef(representativeId);
                } else {
                    mention.setMucRef(AgigaMention.UNASSIGNED);
                }
            }
        }
    }
        
}
