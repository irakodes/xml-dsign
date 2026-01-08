package online.erakodes.xmldsig.model;

public record ContactDetails(String name, String mobileNumber, String email, String address) {

    public static ContactDetails fromArray(String[] args) {
        return new ContactDetails(args[0], args[1], args[2], args[3]);
    }
}