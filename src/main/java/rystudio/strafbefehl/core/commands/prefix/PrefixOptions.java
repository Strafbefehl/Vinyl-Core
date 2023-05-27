package rystudio.strafbefehl.core.commands.prefix;


public class PrefixOptions {

    private String name;
    private String description;
    private String stringValue;

    public PrefixOptions(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }
}
