package timecapsule.model;

public class Invite {
    private String inviteId;
    private String inviterUserId;
    private String inviteeEmail;
    private InviteStatus status;
    private long createdAtUtc;
    private long expiresAtUtc;
    private Long acceptedAtUtc;
    private String inviterDisplayName;
    private String inviterEmail;

    public Invite() {}

    public Invite(String inviteId, String inviterUserId, String inviteeEmail, 
                  InviteStatus status, long createdAtUtc, long expiresAtUtc) {
        this.inviteId = inviteId;
        this.inviterUserId = inviterUserId;
        this.inviteeEmail = inviteeEmail;
        this.status = status;
        this.createdAtUtc = createdAtUtc;
        this.expiresAtUtc = expiresAtUtc;
    }

    public String getInviteId() { return inviteId; }
    public void setInviteId(String inviteId) { this.inviteId = inviteId; }

    public String getInviterUserId() { return inviterUserId; }
    public void setInviterUserId(String inviterUserId) { this.inviterUserId = inviterUserId; }

    public String getInviteeEmail() { return inviteeEmail; }
    public void setInviteeEmail(String inviteeEmail) { this.inviteeEmail = inviteeEmail; }

    public InviteStatus getStatus() { return status; }
    public void setStatus(InviteStatus status) { this.status = status; }

    public long getCreatedAtUtc() { return createdAtUtc; }
    public void setCreatedAtUtc(long createdAtUtc) { this.createdAtUtc = createdAtUtc; }

    public long getExpiresAtUtc() { return expiresAtUtc; }
    public void setExpiresAtUtc(long expiresAtUtc) { this.expiresAtUtc = expiresAtUtc; }

    public Long getAcceptedAtUtc() { return acceptedAtUtc; }
    public void setAcceptedAtUtc(Long acceptedAtUtc) { this.acceptedAtUtc = acceptedAtUtc; }

    public String getInviterDisplayName() { return inviterDisplayName; }
    public void setInviterDisplayName(String inviterDisplayName) { this.inviterDisplayName = inviterDisplayName; }

    public String getInviterEmail() { return inviterEmail; }
    public void setInviterEmail(String inviterEmail) { this.inviterEmail = inviterEmail; }

    public boolean isValid() {
        return status == InviteStatus.SENT && System.currentTimeMillis() < expiresAtUtc;
    }
}
