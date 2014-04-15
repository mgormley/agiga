package edu.jhu.agiga;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * Each AgigaCoref object provides access to all the mentions of a single entity
 * in a document. These coreference resolution annotations are represented as a
 * list of coref mentions, or AgigaMention objects.
 * 
 * @author mgormley
 * 
 */
public class AgigaCoref implements Serializable {

	public static final long serialVersionUID = 1;

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

    @Override
    public boolean equals(Object other) {
        if(other == null) return false;
        if(other instanceof AgigaCoref) {
            AgigaCoref o = (AgigaCoref) other;
            return Util.safeEquals(mentions, o.mentions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.safeHashCode(mentions);
    }
    
}
