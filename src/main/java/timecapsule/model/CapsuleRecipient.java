package timecapsule.model;

public class CapsuleRecipient {
    private String capsuleId;
    private String recipientEmail;
    private String recipientUserId;
    private boolean notifyOnCreate;
    private boolean notifyOnUnlock;
    private DeliveryStatus deliveryStatus;
    private Long openedAtUtc;
    private long createdAtUtc;
    private String recipientDisplayName;

    public CapsuleRecipient() {
        this.deliveryStatus = DeliveryStatus.NONE;
        this.notifyOnUnlock = true;
    }

    public CapsuleRecipient(String capsuleId, String recipientEmail, String recipientUserId,
                            boolean notifyOnCreate, boolean notifyOnUnlock, long createdAtUtc) {
        this.capsuleId = capsuleId;
        this.recipientEmail = recipientEmail;
        this.recipientUserId = recipientUserId;
        this.notifyOnCreate = notifyOnCreate;
        this.notifyOnUnlock = notifyOnUnlock;
        this.deliveryStatus = DeliveryStatus.NONE;
        this.createdAtUtc = createdAtUtc;
    }

    public String getCapsuleId() { return capsuleId; }
    public void setCapsuleId(String capsuleId) { this.capsuleId = capsuleId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(String recipientUserId) { this.recipientUserId = recipientUserId; }

    public boolean isNotifyOnCreate() { return notifyOnCreate; }
    public void setNotifyOnCreate(boolean notifyOnCreate) { this.notifyOnCreate = notifyOnCreate; }

    public boolean isNotifyOnUnlock() { return notifyOnUnlock; }
    public void setNotifyOnUnlock(boolean notifyOnUnlock) { this.notifyOnUnlock = notifyOnUnlock; }

    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public Long getOpenedAtUtc() { return openedAtUtc; }
    public void setOpenedAtUtc(Long openedAtUtc) { this.openedAtUtc = openedAtUtc; }

    public long getCreatedAtUtc() { return createdAtUtc; }
    public void setCreatedAtUtc(long createdAtUtc) { this.createdAtUtc = createdAtUtc; }

    public String getRecipientDisplayName() { return recipientDisplayName; }
    public void setRecipientDisplayName(String recipientDisplayName) { this.recipientDisplayName = recipientDisplayName; }

    public boolean isOpened() {
        return openedAtUtc != null;
    }

    public String getDisplayIdentifier() {
        if (recipientDisplayName != null && !recipientDisplayName.isEmpty()) {
            return recipientDisplayName;
        }
        return recipientEmail;
    }
}
