package edu.jhu.agiga;

/**
 * Each AgigaTypedDependency represents an arc in a dependency tree. It provides
 * access to the type of dependency arc. It also provides the indices of the
 * governor and dependent. These are zero-indexed and correspond to the indices
 * of the AgigaToken objects in the AgigaSentence object.
 * 
 * @author mgormley
 * 
 */
public class AgigaTypedDependency {

    private String type;
    private int gov;
    private int dep;

    public AgigaTypedDependency(String type, int gov, int dep) {
        this.type = type;
        this.gov = gov;
        this.dep = dep;
    }

    public String getType() {
        return type;
    }

    public int getGovIdx() {
        return gov;
    }

    public int getDepIdx() {
        return dep;
    }

}
