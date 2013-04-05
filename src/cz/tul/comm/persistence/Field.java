package cz.tul.comm.persistence;

/**
 *
 * @author Petr Ječmen
 */
public class Field {

    private final String name;
    private final String value;

    public Field(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
    
}
