package timecapsule.model;

import java.util.ArrayList;
import java.util.List;

public class Capsule {
    private String capsuleId;
    private String ownerUserId;
    private String ownerEmail;
    private String ownerDisplayName;
    private String headline;
    private long unlockAtUtc;
    private CapsuleState state;
    private long createdAtUtc;
    private long updatedAtUtc;
    
    private String ciphertextBase64;
    private String ivBase64;
    private String saltBase64;
    
    private List<CapsuleRecipient> recipients;
    private transient String plaintextMessage;

    public Capsule() {
        this.recipients = new ArrayList<>();
        this.state = CapsuleState.SEALED;
    }

    public Capsule(String capsuleId, String ownerUserId, String headline, 
                   long unlockAtUtc, long createdAtUtc) {
        this();
        this.capsuleId = capsuleId;
        this.ownerUserId = ownerUserId;
        this.headline = headline;
        this.unlockAtUtc = unlockAtUtc;
        this.createdAtUtc = createdAtUtc;
        this.updatedAtUtc = createdAtUtc;
    }

    public String getCapsuleId() { return capsuleId; }
    public void setCapsuleId(String capsuleId) { this.capsuleId = capsuleId; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getOwnerDisplayName() { return ownerDisplayName; }
    public void setOwnerDisplayName(String ownerDisplayName) { this.ownerDisplayName = ownerDisplayName; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public long getUnlockAtUtc() { return unlockAtUtc; }
    public void setUnlockAtUtc(long unlockAtUtc) { this.unlockAtUtc = unlockAtUtc; }

    public CapsuleState getState() { return state; }
    public void setState(CapsuleState state) { this.state = state; }

    public long getCreatedAtUtc() { return createdAtUtc; }
    public void setCreatedAtUtc(long createdAtUtc) { this.createdAtUtc = createdAtUtc; }

    public long getUpdatedAtUtc() { return updatedAtUtc; }
    public void setUpdatedAtUtc(long updatedAtUtc) { this.updatedAtUtc = updatedAtUtc; }

    public String getCiphertextBase64() { return ciphertextBase64; }
    public void setCiphertextBase64(String ciphertextBase64) { this.ciphertextBase64 = ciphertextBase64; }

    public String getIvBase64() { return ivBase64; }
    public void setIvBase64(String ivBase64) { this.ivBase64 = ivBase64; }

    public String getSaltBase64() { return saltBase64; }
    public void setSaltBase64(String saltBase64) { this.saltBase64 = saltBase64; }

    public List<CapsuleRecipient> getRecipients() { return recipients; }
    public void setRecipients(List<CapsuleRecipient> recipients) { this.recipients = recipients; }

    public String getPlaintextMessage() { return plaintextMessage; }
    public void setPlaintextMessage(String plaintextMessage) { this.plaintextMessage = plaintextMessage; }

    public void addRecipient(CapsuleRecipient recipient) {
        if (recipients == null) {
            recipients = new ArrayList<>();
        }
        recipients.add(recipient);
    }

    public boolean isOpenable() {
        return state == CapsuleState.OPENED || System.currentTimeMillis() >= unlockAtUtc;
    }

    public String getSenderDisplay() {
        if (ownerDisplayName != null && !ownerDisplayName.isEmpty()) {
            return ownerDisplayName;
        }
        return ownerEmail;
    }

    public String getTimeRemaining() {
        long now = System.currentTimeMillis();
        if (now >= unlockAtUtc) {
            return "✓ Ready!";
        }
        
        long diff = unlockAtUtc - now;
        long days = diff / (24 * 60 * 60 * 1000);
        long hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
