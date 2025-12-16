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

public class ReceivedCapsulesScreen extends VBox {
    
    private final ApiClient apiClient;
    private final ListView<Capsule> capsulesListView;
    private final Label statusLabel;
    private final ComboBox<String> filterCombo;
    
    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm").withZone(ZoneId.systemDefault());
    
    public ReceivedCapsulesScreen(ApiClient apiClient) {
        this.apiClient = apiClient;
        
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #1C1C1E;");
        
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label headerLabel = new Label("📬 Received Capsules");
        headerLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);
        HBox.setHgrow(headerLabel, Priority.ALWAYS);
        
        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All", "Ready to Open", "Locked", "Opened");
        filterCombo.setValue("All");
        filterCombo.setStyle("-fx-background-color: #3A3A3C;");
        filterCombo.setOnAction(e -> applyFilter());
        
        Button refreshBtn = createStyledButton("🔄 Refresh", "#34C759");
        refreshBtn.setOnAction(e -> refresh());
        
        headerRow.getChildren().addAll(headerLabel, filterCombo, refreshBtn);
        
        capsulesListView = new ListView<>();
        capsulesListView.setStyle("-fx-background-color: #2C2C2E; -fx-control-inner-background: #2C2C2E;");
        capsulesListView.setCellFactory(lv -> new ReceivedCapsuleCell());
        capsulesListView.setPlaceholder(new Label("No capsules received yet.\nAsk your friends to send you one!"));
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
        
        apiClient.listReceivedCapsules()
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk() && response.getCapsules() != null) {
                    capsulesListView.getItems().clear();
                    capsulesListView.getItems().addAll(response.getCapsules());
                    setStatus("Loaded " + response.getCapsules().size() + " capsule(s)");
                    applyFilter();
                } else {
                    setStatus("Failed to load: " + (response.getError() != null ? response.getError() : "Unknown error"));
                }
            }));
    }
    
    private void applyFilter() {
        String filter = filterCombo.getValue();
    }
    
    private void openCapsule(Capsule capsule) {
        long now = System.currentTimeMillis();
        if (now < capsule.getUnlockAtUtc()) {
            showNotYetDialog(capsule);
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Open Capsule");
        dialog.setHeaderText("🔓 Enter Passphrase");
        dialog.setContentText("The sender should have shared the passphrase with you:");
        
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
                        
                        apiClient.markRecipientOpened(capsule.getCapsuleId());
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
            "The SERVER checks the time, not your computer.\n" +
            "Please wait until the unlock time passes."
        );
        
        alert.showAndWait();
    }
    
    private void showMessageDialog(Capsule capsule, String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Time Capsule Opened!");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #2C2C2E;");
        content.setPrefWidth(500);
        
        Label headerLabel = new Label("🎉 " + (capsule.getHeadline() != null && !capsule.getHeadline().isEmpty() 
            ? capsule.getHeadline() : "Time Capsule"));
        headerLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 24));
        headerLabel.setTextFill(Color.WHITE);
        
        Label senderLabel = new Label("From: " + capsule.getSenderDisplay());
        senderLabel.setTextFill(Color.web("#8E8E93"));
        
        String sentDate = DATE_FORMAT.format(Instant.ofEpochMilli(capsule.getCreatedAtUtc()));
        Label dateLabel = new Label("Sent: " + sentDate);
        dateLabel.setTextFill(Color.web("#8E8E93"));
        
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
        
        content.getChildren().addAll(headerLabel, senderLabel, dateLabel, messageArea);
        
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
            "Please check with the sender for the correct passphrase."
        );
        alert.showAndWait();
    }
    
    private void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    private class ReceivedCapsuleCell extends ListCell<Capsule> {
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
            boolean isOpened = capsule.getRecipients() != null && !capsule.getRecipients().isEmpty() 
                && capsule.getRecipients().get(0).isOpened();
            
            String icon;
            Color iconColor;
            if (isOpened) {
                icon = "📖";
                iconColor = Color.web("#8E8E93");
            } else if (isOpenable) {
                icon = "🔓";
                iconColor = Color.web("#34C759");
            } else {
                icon = "🔒";
                iconColor = Color.web("#FF9500");
            }
            
            Label iconLabel = new Label(icon);
            iconLabel.setFont(Font.font(24));
            
            VBox info = new VBox(5);
            
            HBox titleRow = new HBox(10);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            
            Label headlineLabel = new Label(
                capsule.getHeadline() != null && !capsule.getHeadline().isEmpty() 
                    ? capsule.getHeadline() : "(No title)"
            );
            headlineLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 16));
            headlineLabel.setTextFill(Color.WHITE);
            
            titleRow.getChildren().add(headlineLabel);
            
            Label senderLabel = new Label("From: " + capsule.getSenderDisplay());
            senderLabel.setTextFill(Color.web("#8E8E93"));
            
            String unlockDate = DATE_FORMAT.format(Instant.ofEpochMilli(capsule.getUnlockAtUtc()));
            Label dateLabel = new Label("Unlocks: " + unlockDate);
            dateLabel.setTextFill(Color.web("#8E8E93"));
            
            info.getChildren().addAll(titleRow, senderLabel, dateLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            
            VBox actionBox = new VBox(5);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            
            String statusText;
            Color statusColor;
            if (isOpened) {
                statusText = "Opened";
                statusColor = Color.web("#8E8E93");
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
            
            if (isOpenable && !isOpened) {
                Button openBtn = createStyledButton("🔓 Open", "#34C759");
                openBtn.setOnAction(e -> openCapsule(capsule));
                actionBox.getChildren().add(openBtn);
            } else if (isOpened) {
                Button readBtn = createStyledButton("📖 Read Again", "#5856D6");
                readBtn.setOnAction(e -> openCapsule(capsule));
                actionBox.getChildren().add(readBtn);
            }
            
            row.getChildren().addAll(iconLabel, info, actionBox);
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
}
