package timecapsule.model;

/**
 * Represents a time capsule with its metadata and encrypted content.
 * This is the main data model used throughout the application.
 */
public class Capsule {
    
    // Unique identifier assigned by the server
    private String id;
    
    // Email of the capsule owner
    private String owner;
    
    // Optional headline/title for the capsule
    private String headline;
    
    // Unix epoch timestamp (milliseconds) when capsule can be opened
    private long unlockTimeEpoch;
    
    // Unix epoch timestamp (milliseconds) when capsule was created
    private long createdAtEpoch;
    
    // Current state: "sealed" or "opened"
    private String state;
    
    // Base64-encoded encrypted message content
    private String ciphertextBase64;
    
    // Base64-encoded initialization vector for AES-GCM
    private String ivBase64;
    
    // Base64-encoded salt used for PBKDF2 key derivation
    private String saltBase64;

    // ========================
    // Constructors
    // ========================
    
    public Capsule() {
        // Default constructor for JSON deserialization
    }

    public Capsule(String id, String owner, String headline, long unlockTimeEpoch, String state) {
        this.id = id;
        this.owner = owner;
        this.headline = headline;
        this.unlockTimeEpoch = unlockTimeEpoch;
        this.state = state;
    }

    // ========================
    // Getters and Setters
    // ========================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public long getUnlockTimeEpoch() {
        return unlockTimeEpoch;
    }

    public void setUnlockTimeEpoch(long unlockTimeEpoch) {
        this.unlockTimeEpoch = unlockTimeEpoch;
    }

    public long getCreatedAtEpoch() {
        return createdAtEpoch;
    }

    public void setCreatedAtEpoch(long createdAtEpoch) {
        this.createdAtEpoch = createdAtEpoch;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCiphertextBase64() {
        return ciphertextBase64;
    }

    public void setCiphertextBase64(String ciphertextBase64) {
        this.ciphertextBase64 = ciphertextBase64;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public void setIvBase64(String ivBase64) {
        this.ivBase64 = ivBase64;
    }

    public String getSaltBase64() {
        return saltBase64;
    }

    public void setSaltBase64(String saltBase64) {
        this.saltBase64 = saltBase64;
    }

    @Override
    public String toString() {
        return "Capsule{" +
                "id='" + id + '\'' +
                ", headline='" + headline + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
