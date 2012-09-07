package edu.jhu.agiga;

import java.util.ArrayList;
import java.util.List;

/**
 * Each AgigaCoref object provides access to all the mentions of a single entity
 * in a document. These coreference resolution annotations are represented as a
 * list of coref mentions, or AgigaMention objects.
 * 
 * @author mgormley
 * 
 */
public class AgigaCoref {

    private List<AgigaMention> mentions;
    
    public AgigaCoref() {
        this.mentions = new ArrayList<AgigaMention>();
    }

    public List<AgigaMention> getMentions() {
        return mentions;
    }

    public void add(AgigaMention mention) {
        mentions.add(mention);
    }
        
}
