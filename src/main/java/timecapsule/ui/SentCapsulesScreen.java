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
import timecapsule.crypto.CryptoUtils;
import timecapsule.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class SentCapsulesScreen extends VBox {
    
    private final ApiClient apiClient;
    private final Consumer<Void> onNewCapsule;
    private final ListView<Capsule> capsulesListView;
    private final Label statusLabel;
    
    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm").withZone(ZoneId.systemDefault());
    
    public SentCapsulesScreen(ApiClient apiClient, Consumer<Void> onNewCapsule) {
        this.apiClient = apiClient;
        this.onNewCapsule = onNewCapsule;
        
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #1C1C1E;");
        
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label headerLabel = new Label("📤 Your Capsules");
        headerLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);
        HBox.setHgrow(headerLabel, Priority.ALWAYS);
        
        Button newBtn = createStyledButton("+ New Capsule", "#667eea");
        newBtn.setOnAction(e -> onNewCapsule.accept(null));
        
        Button refreshBtn = createStyledButton("🔄 Refresh", "#34C759");
        refreshBtn.setOnAction(e -> refresh());
        
        headerRow.getChildren().addAll(headerLabel, newBtn, refreshBtn);
        
        capsulesListView = new ListView<>();
        capsulesListView.setStyle("-fx-background-color: #2C2C2E; -fx-control-inner-background: #2C2C2E;");
        capsulesListView.setCellFactory(lv -> new SentCapsuleCell());
        capsulesListView.setPlaceholder(new Label("No capsules created yet.\nClick '+ New Capsule' to create one!"));
        VBox.setVgrow(capsulesListView, Priority.ALWAYS);
        
        statusLabel = new Label();
        statusLabel.setTextFill(Color.web("#8E8E93"));
        
        getChildren().addAll(headerRow, capsulesListView, statusLabel);
        
        refresh();
    }
    
    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand;"
        );
        return btn;
    }
    
    public void refresh() {
        setStatus("Loading...");
        
        apiClient.listSentCapsules()
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk() && response.getCapsules() != null) {
                    capsulesListView.getItems().clear();
                    capsulesListView.getItems().addAll(response.getCapsules());
                    setStatus("Loaded " + response.getCapsules().size() + " capsule(s)");
                } else {
                    setStatus("Failed to load: " + (response.getError() != null ? response.getError() : "Unknown error"));
                }
            }));
    }
    
    private void openCapsule(Capsule capsule) {
        long now = System.currentTimeMillis();
        if (now < capsule.getUnlockAtUtc()) {
            showNotYetDialog(capsule);
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Open Your Capsule");
        dialog.setHeaderText("🔓 Enter Passphrase");
        dialog.setContentText("Enter the passphrase you used when creating this capsule:");
        
        dialog.showAndWait().ifPresent(passphrase -> {
            if (passphrase == null || passphrase.isEmpty()) {
                return;
            }
            
            setStatus("Opening capsule...");
            
            apiClient.openCapsule(capsule.getCapsuleId())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.isNotYet()) {
                        showNotYetDialog(capsule);
                        return;
                    }
                    
                    if (!response.isOk() || response.getCapsule() == null) {
                        setStatus("Failed to open: " + response.getError());
                        return;
                    }
                    
                    Capsule openedCapsule = response.getCapsule();
                    
                    try {
                        String associatedData = openedCapsule.getOwnerEmail() + "|" + openedCapsule.getUnlockAtUtc();
                        
                        String plaintext = CryptoUtils.decrypt(
                            openedCapsule.getCiphertextBase64(),
                            openedCapsule.getIvBase64(),
                            openedCapsule.getSaltBase64(),
                            passphrase,
                            associatedData
                        );
                        
                        showMessageDialog(openedCapsule, plaintext);
                        refresh();
                        
                    } catch (Exception e) {
                        showDecryptionError();
                    }
                }));
        });
    }
    
    private void showNotYetDialog(Capsule capsule) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Not Yet!");
        alert.setHeaderText("⏰ This capsule is still time-locked");
        
        String timeRemaining = capsule.getTimeRemaining();
        String unlockDate = DATE_FORMAT.format(Instant.ofEpochMilli(capsule.getUnlockAtUtc()));
        
        alert.setContentText(
            "This capsule will unlock on:\n" + unlockDate + "\n\n" +
            "Time remaining: " + timeRemaining + "\n\n" +
            "Even you, the creator, cannot open it early!\n" +
            "The SERVER controls when capsules can be opened."
        );
        
        alert.showAndWait();
    }
    
    private void showMessageDialog(Capsule capsule, String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Your Time Capsule");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #2C2C2E;");
        content.setPrefWidth(500);
        
        Label headerLabel = new Label("📖 " + (capsule.getHeadline() != null && !capsule.getHeadline().isEmpty() 
            ? capsule.getHeadline() : "Your Time Capsule"));
        headerLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 24));
        headerLabel.setTextFill(Color.WHITE);
        
        String sentDate = DATE_FORMAT.format(Instant.ofEpochMilli(capsule.getCreatedAtUtc()));
        Label dateLabel = new Label("Created: " + sentDate);
        dateLabel.setTextFill(Color.web("#8E8E93"));
        
        if (capsule.getRecipients() != null && !capsule.getRecipients().isEmpty()) {
            Label recipientsLabel = new Label("Sent to " + capsule.getRecipients().size() + " recipient(s)");
            recipientsLabel.setTextFill(Color.web("#667eea"));
            content.getChildren().add(recipientsLabel);
        }
        
        TextArea messageArea = new TextArea(message);
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(10);
        messageArea.setStyle(
            "-fx-background-color: #3A3A3C; " +
            "-fx-text-fill: white; " +
            "-fx-control-inner-background: #3A3A3C; " +
            "-fx-font-size: 14;"
        );
        
        content.getChildren().addAll(headerLabel, dateLabel, messageArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #2C2C2E;");
        
        dialog.showAndWait();
    }
    
    private void showDecryptionError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Decryption Failed");
        alert.setHeaderText("❌ Could not decrypt the capsule");
        alert.setContentText(
            "The passphrase you entered is incorrect.\n\n" +
            "If you've forgotten the passphrase, unfortunately\n" +
            "the message cannot be recovered."
        );
        alert.showAndWait();
    }
    
    private void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    private class SentCapsuleCell extends ListCell<Capsule> {
        @Override
        protected void updateItem(Capsule capsule, boolean empty) {
            super.updateItem(capsule, empty);
            
            if (empty || capsule == null) {
                setGraphic(null);
                return;
            }
            
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(15));
            row.setStyle("-fx-background-color: #3A3A3C; -fx-background-radius: 10;");
            
            long now = System.currentTimeMillis();
            boolean isOpenable = now >= capsule.getUnlockAtUtc();
            boolean isOpened = capsule.getState() == CapsuleState.OPENED;
            
            String icon;
            if (isOpened) {
                icon = "📖";
            } else if (isOpenable) {
                icon = "🔓";
            } else {
                icon = "🔒";
            }
            
            Label iconLabel = new Label(icon);
            iconLabel.setFont(Font.font(24));
            
            VBox info = new VBox(5);
            
            Label headlineLabel = new Label(
                capsule.getHeadline() != null && !capsule.getHeadline().isEmpty() 
                    ? capsule.getHeadline() : "(No title)"
            );
            headlineLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 16));
            headlineLabel.setTextFill(Color.WHITE);
            
            String unlockDate = DATE_FORMAT.format(Instant.ofEpochMilli(capsule.getUnlockAtUtc()));
            Label dateLabel = new Label("Unlocks: " + unlockDate);
            dateLabel.setTextFill(Color.web("#8E8E93"));
            
            int recipientCount = capsule.getRecipients() != null ? capsule.getRecipients().size() : 0;
            String recipientText = recipientCount > 0 
                ? "Sent to " + recipientCount + " recipient(s)" 
                : "Personal capsule";
            Label recipientsLabel = new Label(recipientText);
            recipientsLabel.setTextFill(Color.web("#667eea"));
            recipientsLabel.setFont(Font.font("SF Pro Display", 11));
            
            info.getChildren().addAll(headlineLabel, dateLabel, recipientsLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            
            VBox actionBox = new VBox(5);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            
            String statusText;
            Color statusColor;
            if (isOpened) {
                statusText = "Opened";
                statusColor = Color.web("#34C759");
            } else if (isOpenable) {
                statusText = "✓ Ready!";
                statusColor = Color.web("#34C759");
            } else {
                statusText = capsule.getTimeRemaining();
                statusColor = Color.web("#FF9500");
            }
            
            Label statusLabel = new Label(statusText);
            statusLabel.setTextFill(statusColor);
            statusLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 12));
            
            actionBox.getChildren().add(statusLabel);
            
            if (isOpenable) {
                Button openBtn = createStyledButton("🔓 Open", "#34C759");
                openBtn.setOnAction(e -> openCapsule(capsule));
                actionBox.getChildren().add(openBtn);
            }
            
            row.getChildren().addAll(iconLabel, info, actionBox);
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
}
