package timecapsule.api;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import timecapsule.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ApiClient {
    
    private static final String BACKEND_URL = "https://script.google.com/macros/s/AKfycbwcwtuPmCSls1nHClxsLEkNMU3noKebQ-xshBFnQ5jAJGEPIXglYTOiOCGx2gcjFMpDyg/exec";
    
    private final HttpClient httpClient;
    private final Gson gson;
    private String currentUserId;
    private String currentUserEmail;
    
    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Long.class, new LenientLongAdapter())
                .registerTypeAdapter(long.class, new LenientLongAdapter())
                .create();
    }
    
    public void setCurrentUser(String userId, String email) {
        this.currentUserId = userId;
        this.currentUserEmail = email;
    }
    
    public String getCurrentUserId() { return currentUserId; }
    public String getCurrentUserEmail() { return currentUserEmail; }
    
    public CompletableFuture<ApiResponse> registerOrLogin(String email, String displayName, String passwordHash) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "registerOrLogin");
        request.put("email", email);
        request.put("displayName", displayName);
        request.put("passwordHash", passwordHash);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> searchUsers(String query) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "searchUsers");
        request.put("userId", currentUserId);
        request.put("query", query);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> getUserByEmail(String email) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "getUserByEmail");
        request.put("email", email);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> sendFriendRequest(String addresseeUserId) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "friendRequest");
        request.put("requesterUserId", currentUserId);
        request.put("addresseeUserId", addresseeUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> acceptFriendRequest(String requesterUserId) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "friendAccept");
        request.put("requesterUserId", requesterUserId);
        request.put("addresseeUserId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> declineFriendRequest(String requesterUserId) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "friendDecline");
        request.put("requesterUserId", requesterUserId);
        request.put("addresseeUserId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> blockUser(String userIdToBlock) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "friendBlock");
        request.put("userId", currentUserId);
        request.put("blockUserId", userIdToBlock);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> listFriends() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "friendsList");
        request.put("userId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> listFriendRequests() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "friendRequests");
        request.put("userId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> sendInvite(String inviteeEmail, String optionalMessage) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "inviteSend");
        request.put("inviterUserId", currentUserId);
        request.put("inviteeEmail", inviteeEmail);
        request.put("message", optionalMessage);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> acceptInvite(String token, String email, String displayName, String passwordHash) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "inviteAccept");
        request.put("token", token);
        request.put("email", email);
        request.put("displayName", displayName);
        request.put("passwordHash", passwordHash);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> resendInvite(String inviteId) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "inviteResend");
        request.put("inviteId", inviteId);
        request.put("inviterUserId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> listSentInvites() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "invitesList");
        request.put("userId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> createCapsule(String headline, long unlockTimeEpoch,
            String ciphertextBase64, String ivBase64, String saltBase64,
            List<CapsuleRecipient> recipients, boolean surpriseMode) {
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "create");
        request.put("owner", currentUserEmail);
        request.put("ownerUserId", currentUserId);
        request.put("headline", headline);
        request.put("unlockTimeEpoch", unlockTimeEpoch);
        request.put("ciphertextBase64", ciphertextBase64);
        request.put("ivBase64", ivBase64);
        request.put("saltBase64", saltBase64);
        
        if (recipients != null && !recipients.isEmpty()) {
            for (CapsuleRecipient r : recipients) {
                r.setNotifyOnCreate(!surpriseMode);
                r.setNotifyOnUnlock(true);
            }
            request.put("recipients", recipients);
        }
        
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> listSentCapsules() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "listSent");
        request.put("owner", currentUserEmail);
        request.put("userId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> listReceivedCapsules() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "listReceived");
        request.put("userId", currentUserId);
        request.put("email", currentUserEmail);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> listAllCapsules() {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "list");
        request.put("owner", currentUserEmail);
        request.put("userId", currentUserId);
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> openCapsule(String capsuleId) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "open");
        request.put("id", capsuleId);
        request.put("owner", currentUserEmail);
        request.put("userId", currentUserId);
        request.put("requestId", generateRequestId());
        return sendRequest(request);
    }
    
    public CompletableFuture<ApiResponse> markRecipientOpened(String capsuleId) {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "markRecipientOpened");
        request.put("capsuleId", capsuleId);
        request.put("userId", currentUserId);
        request.put("email", currentUserEmail);
        return sendRequest(request);
    }
    
    private CompletableFuture<ApiResponse> sendRequest(Map<String, Object> requestData) {
        String json = gson.toJson(requestData);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        String body = response.body();
                        System.out.println("[ApiClient] Response: " + body);
                        return gson.fromJson(body, ApiResponse.class);
                    } catch (Exception e) {
                        ApiResponse errorResponse = new ApiResponse();
                        errorResponse.setStatus("error");
                        errorResponse.setError("Failed to parse response: " + e.getMessage());
                        System.err.println("[ApiClient] Parse error: " + e.getMessage());
                        return errorResponse;
                    }
                })
                .exceptionally(e -> {
                    ApiResponse errorResponse = new ApiResponse();
                    errorResponse.setStatus("error");
                    errorResponse.setError("Network error: " + e.getMessage());
                    return errorResponse;
                });
    }
    
    private String generateRequestId() {
        return "REQ" + System.currentTimeMillis() + "_" + 
               Long.toHexString(Double.doubleToLongBits(Math.random()));
    }
    
    private static class LenientLongAdapter extends TypeAdapter<Long> {
        @Override
        public void write(JsonWriter out, Long value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value);
            }
        }
        
        @Override
        public Long read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return 0L;
            } else if (token == JsonToken.STRING) {
                String str = in.nextString();
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException e) {
                    return 0L;
                }
            } else if (token == JsonToken.NUMBER) {
                return in.nextLong();
            }
            return 0L;
        }
    }
}
