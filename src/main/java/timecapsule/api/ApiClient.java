package timecapsule.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import timecapsule.model.ApiResponse;
import timecapsule.model.Capsule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP client for communicating with the Google Apps Script backend.
 * All network operations run on a background thread to keep the UI responsive.
 */
public class ApiClient {

    // The URL of the deployed Google Apps Script Web App
    private final String backendUrl;
    
    // JSON serializer/deserializer
    private final Gson gson;
    
    // Thread pool for background HTTP operations
    private final ExecutorService executor;

    /**
     * Create an API client for the given backend URL.
     * 
     * @param backendUrl The URL of the Google Apps Script Web App
     */
    public ApiClient(String backendUrl) {
        this.backendUrl = backendUrl;
        this.gson = new GsonBuilder().create();
        // Single-threaded executor to serialize requests (simple and safe)
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ApiClient-Worker");
            t.setDaemon(true);  // Don't prevent app shutdown
            return t;
        });
    }

    /**
     * Create a new time capsule on the server.
     * 
     * @param owner Owner's email address
     * @param headline Optional headline/title
     * @param unlockTimeEpoch Unix timestamp (milliseconds) when capsule can be opened
     * @param ciphertextBase64 Base64-encoded encrypted message
     * @param ivBase64 Base64-encoded IV
     * @param saltBase64 Base64-encoded salt
     * @return CompletableFuture with the API response
     */
    public CompletableFuture<ApiResponse> createCapsule(String owner, String headline,
            long unlockTimeEpoch, String ciphertextBase64, String ivBase64, String saltBase64) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build the request payload
                Map<String, Object> payload = new HashMap<>();
                payload.put("action", "create");
                payload.put("owner", owner);
                payload.put("headline", headline != null ? headline : "");
                payload.put("unlockTimeEpoch", unlockTimeEpoch);
                payload.put("ciphertextBase64", ciphertextBase64);
                payload.put("ivBase64", ivBase64);
                payload.put("saltBase64", saltBase64);

                String responseJson = sendPost(payload);
                return gson.fromJson(responseJson, ApiResponse.class);
                
            } catch (Exception e) {
                // Return error response instead of throwing
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setStatus("error");
                errorResponse.setError(e.getMessage());
                return errorResponse;
            }
        }, executor);
    }

    /**
     * List all capsules for a given owner.
     * 
     * @param owner Owner's email address
     * @return CompletableFuture with the API response containing capsule list
     */
    public CompletableFuture<ApiResponse> listCapsules(String owner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("action", "list");
                payload.put("owner", owner);

                String responseJson = sendPost(payload);
                return gson.fromJson(responseJson, ApiResponse.class);
                
            } catch (Exception e) {
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setStatus("error");
                errorResponse.setError(e.getMessage());
                return errorResponse;
            }
        }, executor);
    }

    /**
     * Attempt to open a capsule.
     * 
     * @param capsuleId The ID of the capsule to open
     * @param owner Owner's email (for verification)
     * @return CompletableFuture with the API response
     */
    public CompletableFuture<ApiResponse> openCapsule(String capsuleId, String owner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("action", "open");
                payload.put("id", capsuleId);
                payload.put("owner", owner);
                // Generate a unique request ID for audit logging
                payload.put("requestId", java.util.UUID.randomUUID().toString());

                String responseJson = sendPost(payload);
                return gson.fromJson(responseJson, ApiResponse.class);
                
            } catch (Exception e) {
                ApiResponse errorResponse = new ApiResponse();
                errorResponse.setStatus("error");
                errorResponse.setError(e.getMessage());
                return errorResponse;
            }
        }, executor);
    }

    /**
     * Send a POST request to the backend with JSON payload.
     * 
     * @param payload Map to be serialized as JSON
     * @return The response body as a string
     */
    private String sendPost(Map<String, Object> payload) throws Exception {
        // Convert payload to JSON
        String jsonPayload = gson.toJson(payload);
        
        // Open connection
        URL url = new URL(backendUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure the request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);  // 30 second timeout
            conn.setReadTimeout(30000);
            
            // Handle redirects (Apps Script may redirect)
            conn.setInstanceFollowRedirects(true);
            
            // Send the JSON payload
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }
            
            // Read the response
            int responseCode = conn.getResponseCode();
            
            // Handle redirect manually if needed (for exec URLs)
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == 303) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null) {
                    conn.disconnect();
                    return sendGetRedirect(newUrl);
                }
            }
            
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return response.toString();
            
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Follow a redirect with a GET request.
     * Google Apps Script sometimes redirects POST to GET.
     */
    private String sendGetRedirect(String redirectUrl) throws Exception {
        URL url = new URL(redirectUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return response.toString();
            
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Shutdown the executor service gracefully.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
