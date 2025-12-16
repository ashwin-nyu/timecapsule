package timecapsule.model;

public class User {
    private String userId;
    private String email;
    private String displayName;
    private long createdAtUtc;

    public User() {}

    public User(String userId, String email, String displayName, long createdAtUtc) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.createdAtUtc = createdAtUtc;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public long getCreatedAtUtc() { return createdAtUtc; }
    public void setCreatedAtUtc(long createdAtUtc) { this.createdAtUtc = createdAtUtc; }

    @Override
    public String toString() {
        return displayName != null && !displayName.isEmpty() ? displayName : email;
    }
}
