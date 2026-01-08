package online.erakodes.xml_dsign.model.pacs;

public class StatusReason {
    public String code;
    public String proprietary;
    public String description;

    public StatusReason(String code, String description) {
        this.code = code;
        this.description = description;
    }
}