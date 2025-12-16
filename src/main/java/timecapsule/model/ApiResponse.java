package timecapsule.model;

import java.util.List;

public class ApiResponse {
    private String status;
    private String error;
    private String message;
    
    private String id;
    private Capsule capsule;
    private List<Capsule> capsules;
    
    private List<Friend> friends;
    private List<Friend> requests;
    
    private String inviteId;
    private String token;
    private List<Invite> invites;
    
    private User user;
    private List<User> users;
    
    private long serverTimeEpoch;
    private long unlockTimeEpoch;

    public ApiResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Capsule getCapsule() { return capsule; }
    public void setCapsule(Capsule capsule) { this.capsule = capsule; }

    public List<Capsule> getCapsules() { return capsules; }
    public void setCapsules(List<Capsule> capsules) { this.capsules = capsules; }

    public List<Friend> getFriends() { return friends; }
    public void setFriends(List<Friend> friends) { this.friends = friends; }

    public List<Friend> getRequests() { return requests; }
    public void setRequests(List<Friend> requests) { this.requests = requests; }

    public String getInviteId() { return inviteId; }
    public void setInviteId(String inviteId) { this.inviteId = inviteId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public List<Invite> getInvites() { return invites; }
    public void setInvites(List<Invite> invites) { this.invites = invites; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public long getServerTimeEpoch() { return serverTimeEpoch; }
    public void setServerTimeEpoch(long serverTimeEpoch) { this.serverTimeEpoch = serverTimeEpoch; }

    public long getUnlockTimeEpoch() { return unlockTimeEpoch; }
    public void setUnlockTimeEpoch(long unlockTimeEpoch) { this.unlockTimeEpoch = unlockTimeEpoch; }

    public boolean isOk() {
        return "ok".equalsIgnoreCase(status);
    }

    public boolean isNotYet() {
        return "notYet".equalsIgnoreCase(status);
    }
}
