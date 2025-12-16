package timecapsule.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import timecapsule.api.ApiClient;
import timecapsule.model.User;

import java.security.MessageDigest;
import java.util.function.Consumer;

public class LoginScreen extends VBox {
    
    private final ApiClient apiClient;
    private final Consumer<User> onLoginSuccess;
    
    private TextField emailField;
    private TextField displayNameField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Button actionButton;
    private Label statusLabel;
    private Hyperlink toggleLink;
    private boolean isLoginMode = true;
    
    private String inviteToken = null;
    
    public LoginScreen(ApiClient apiClient, Consumer<User> onLoginSuccess) {
        this.apiClient = apiClient;
        this.onLoginSuccess = onLoginSuccess;
        
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(50));
        setStyle("-fx-background-color: #1C1C1E;");
        
        Label logoLabel = new Label("⏳");
        logoLabel.setFont(Font.font(72));
        
        Label titleLabel = new Label("TimeCapsule");
        titleLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        
        Label subtitleLabel = new Label("Seal your memories in time");
        subtitleLabel.setTextFill(Color.web("#8E8E93"));
        subtitleLabel.setFont(Font.font("SF Pro Display", 14));
        
        VBox formBox = createForm();
        formBox.setMaxWidth(350);
        
        statusLabel = new Label();
        statusLabel.setTextFill(Color.web("#FF3B30"));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(350);
        
        toggleLink = new Hyperlink("Don't have an account? Sign up");
        toggleLink.setTextFill(Color.web("#667eea"));
        toggleLink.setOnAction(e -> toggleMode());
        
        getChildren().addAll(logoLabel, titleLabel, subtitleLabel, formBox, statusLabel, toggleLink);
    }
    
    private VBox createForm() {
        VBox form = new VBox(15);
        form.setStyle("-fx-background-color: #2C2C2E; -fx-background-radius: 12;");
        form.setPadding(new Insets(25));
        form.setAlignment(Pos.CENTER);
        
        emailField = new TextField();
        emailField.setPromptText("Email address");
        styleTextField(emailField);
        
        displayNameField = new TextField();
        displayNameField.setPromptText("Display name");
        styleTextField(displayNameField);
        displayNameField.setVisible(false);
        displayNameField.setManaged(false);
        
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleTextField(passwordField);
        
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm password");
        styleTextField(confirmPasswordField);
        confirmPasswordField.setVisible(false);
        confirmPasswordField.setManaged(false);
        
        actionButton = new Button("Login");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 12 30;"
        );
        actionButton.setOnAction(e -> handleAction());
        
        passwordField.setOnAction(e -> {
            if (isLoginMode) handleAction();
        });
        confirmPasswordField.setOnAction(e -> handleAction());
        
        form.getChildren().addAll(emailField, displayNameField, passwordField, confirmPasswordField, actionButton);
        
        return form;
    }
    
    private void toggleMode() {
        isLoginMode = !isLoginMode;
        
        if (isLoginMode) {
            actionButton.setText("Login");
            toggleLink.setText("Don't have an account? Sign up");
            displayNameField.setVisible(false);
            displayNameField.setManaged(false);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
        } else {
            actionButton.setText("Sign Up");
            toggleLink.setText("Already have an account? Login");
            displayNameField.setVisible(true);
            displayNameField.setManaged(true);
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
        }
        
        statusLabel.setText("");
    }
    
    private void handleAction() {
        String email = emailField.getText();
        String password = passwordField.getText();
        
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            setStatus("Please enter a valid email address");
            return;
        }
        
        if (password == null || password.isEmpty()) {
            setStatus("Please enter a password");
            return;
        }
        
        if (!isLoginMode) {
            String confirmPassword = confirmPasswordField.getText();
            if (!password.equals(confirmPassword)) {
                setStatus("Passwords do not match");
                return;
            }
            
            if (password.length() < 6) {
                setStatus("Password must be at least 6 characters");
                return;
            }
        }
        
        actionButton.setDisable(true);
        setStatus(isLoginMode ? "Logging in..." : "Creating account...");
        
        String displayName = displayNameField.getText();
        String passwordHash = hashPassword(password);
        
        apiClient.registerOrLogin(email.trim(), displayName, passwordHash)
            .thenAccept(response -> Platform.runLater(() -> {
                actionButton.setDisable(false);
                
                if (response.isOk() && response.getUser() != null) {
                    User user = response.getUser();
                    apiClient.setCurrentUser(user.getUserId(), user.getEmail());
                    onLoginSuccess.accept(user);
                } else {
                    setStatus("Failed: " + (response.getError() != null ? response.getError() : "Unknown error"));
                }
            }));
    }
    
    public void setInviteToken(String token) {
        this.inviteToken = token;
        if (token != null && !token.isEmpty()) {
            toggleMode();
            setStatus("Complete signup to accept your invite!");
        }
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password; // Fallback
        }
    }
    
    private void styleTextField(TextField field) {
        field.setStyle(
            "-fx-background-color: #3A3A3C; " +
            "-fx-text-fill: white; " +
            "-fx-prompt-text-fill: #8E8E93; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 12;"
        );
        field.setMaxWidth(Double.MAX_VALUE);
    }
    
    private void setStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            if (message.startsWith("Failed") || message.contains("must") || message.contains("match")) {
                statusLabel.setTextFill(Color.web("#FF3B30"));
            } else {
                statusLabel.setTextFill(Color.web("#8E8E93"));
            }
        });
    }
}
