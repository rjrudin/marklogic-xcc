package com.marklogic.xcc.types;

import java.math.BigDecimal;

/**
 * Interface for JSON number node.
 * 
 * @author jchen
 *
 */
public interface NumberNode extends XdmNode, JsonItem {
    /**
     * @return The value of this item as a double.
     */
    public double asDouble();
    
    /**
     * @return The value of this item as a BigDecimal.
     */
    public BigDecimal asBigDecimal();
}
