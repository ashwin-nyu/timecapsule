package timecapsule.model;

import java.util.List;

/**
 * Generic response wrapper for API calls to the backend.
 * Parsed from JSON responses returned by the Google Apps Script web app.
 */
public class ApiResponse {
    
    // Status of the request: "ok", "error", "notYet"
    private String status;
    
    // Error message (if status is "error")
    private String error;
    
    // Capsule ID (returned after successful create)
    private String id;
    
    // List of capsules (returned by list action)
    private List<Capsule> capsules;
    
    // Single capsule data (returned by open action)
    private Capsule capsule;
    
    // Message from server (optional)
    private String message;

    // ========================
    // Getters and Setters
    // ========================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Capsule> getCapsules() {
        return capsules;
    }

    public void setCapsules(List<Capsule> capsules) {
        this.capsules = capsules;
    }

    public Capsule getCapsule() {
        return capsule;
    }

    public void setCapsule(Capsule capsule) {
        this.capsule = capsule;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Check if the response indicates success.
     */
    public boolean isSuccess() {
        return "ok".equalsIgnoreCase(status);
    }

    /**
     * Check if the capsule is not yet ready to open.
     */
    public boolean isNotYet() {
        return "notYet".equalsIgnoreCase(status);
    }
}
