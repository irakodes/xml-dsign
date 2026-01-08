package online.erakodes.xmldsig.model;

public class AccountOpeningResponse {
    private String messageId;
    private String accountId;
    private boolean success;

    public AccountOpeningResponse() {
    }

    public AccountOpeningResponse(String messageId, String accountId, boolean success) {
        this.messageId = messageId;
        this.accountId = accountId;
        this.success = success;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
