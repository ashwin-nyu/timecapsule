package timecapsule.model;

public class Friend {
    private String requesterUserId;
    private String addresseeUserId;
    private FriendStatus status;
    private long createdAtUtc;
    private long updatedAtUtc;
    
    private String friendEmail;
    private String friendDisplayName;

    public Friend() {}

    public Friend(String requesterUserId, String addresseeUserId, FriendStatus status, 
                  long createdAtUtc, long updatedAtUtc) {
        this.requesterUserId = requesterUserId;
        this.addresseeUserId = addresseeUserId;
        this.status = status;
        this.createdAtUtc = createdAtUtc;
        this.updatedAtUtc = updatedAtUtc;
    }

    public String getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(String requesterUserId) { this.requesterUserId = requesterUserId; }

    public String getAddresseeUserId() { return addresseeUserId; }
    public void setAddresseeUserId(String addresseeUserId) { this.addresseeUserId = addresseeUserId; }

    public FriendStatus getStatus() { return status; }
    public void setStatus(FriendStatus status) { this.status = status; }

    public long getCreatedAtUtc() { return createdAtUtc; }
    public void setCreatedAtUtc(long createdAtUtc) { this.createdAtUtc = createdAtUtc; }

    public long getUpdatedAtUtc() { return updatedAtUtc; }
    public void setUpdatedAtUtc(long updatedAtUtc) { this.updatedAtUtc = updatedAtUtc; }

    public String getFriendEmail() { return friendEmail; }
    public void setFriendEmail(String friendEmail) { this.friendEmail = friendEmail; }

    public String getFriendDisplayName() { return friendDisplayName; }
    public void setFriendDisplayName(String friendDisplayName) { this.friendDisplayName = friendDisplayName; }

    public String getFriendUserId(String currentUserId) {
        if (requesterUserId.equals(currentUserId)) {
            return addresseeUserId;
        } else {
            return requesterUserId;
        }
    }

    public boolean isRequester(String currentUserId) {
        return requesterUserId.equals(currentUserId);
    }
}
