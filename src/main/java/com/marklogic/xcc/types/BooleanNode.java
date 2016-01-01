package com.marklogic.xcc.types;

/**
 * Interface for JSON boolean node.
 * 
 * @author jchen
 *
 */
public interface BooleanNode extends XdmNode, JsonItem {

    /**
     * @return The value of this item as a boolean.
     */
    public Boolean asBoolean();

}
