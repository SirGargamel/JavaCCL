package cz.tul.javaccl.persistence;

/**
 *
 * @author Lenam s.r.o.
 */
public enum XmlNodes {
    
    /**
     * name of the root settings node
     */
    ROOT("JavaCCL"),

    /**
     * server's IP and port
     */
    SERVER("server"),

    /**
     * IP and port of a single client
     */
    CLIENT("client"),;
    
    private final String xmlNode;

    private XmlNodes(final String xmlNode) {
        this.xmlNode = xmlNode;
    }
    
    @Override
    public String toString() {
        return xmlNode;
    }
    
}
