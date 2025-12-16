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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ComposeCapsuleScreen extends VBox {
    
    private final ApiClient apiClient;
    private final Consumer<Boolean> onComplete;
    
    private TextField headlineField;
    private TextArea messageArea;
    private DatePicker datePicker;
    private Spinner<Integer> hourSpinner;
    private Spinner<Integer> minuteSpinner;
    private ListView<RecipientEntry> recipientsListView;
    private ToggleButton surpriseToggle;
    private PasswordField passphraseField;
    private PasswordField confirmPassphraseField;
    private Label statusLabel;
    private Button sendButton;
    
    private List<Friend> availableFriends = new ArrayList<>();
    
    public ComposeCapsuleScreen(ApiClient apiClient, Consumer<Boolean> onComplete) {
        this.apiClient = apiClient;
        this.onComplete = onComplete;
        
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: #1C1C1E;");
        setAlignment(Pos.TOP_CENTER);
        
        Label headerLabel = new Label("✉️ New Time Capsule");
        headerLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);
        
        VBox formBox = createForm();
        formBox.setMaxWidth(600);
        VBox.setVgrow(formBox, Priority.ALWAYS);
        
        HBox buttonBox = createButtonBar();
        
        statusLabel = new Label();
        statusLabel.setTextFill(Color.web("#8E8E93"));
        
        getChildren().addAll(headerLabel, formBox, buttonBox, statusLabel);
        
        loadFriends();
    }
    
    private VBox createForm() {
        VBox form = new VBox(20);
        form.setStyle("-fx-background-color: #2C2C2E; -fx-background-radius: 12;");
        form.setPadding(new Insets(25));
        
        VBox headlineBox = new VBox(5);
        Label headlineLabel = new Label("Headline");
        headlineLabel.setTextFill(Color.WHITE);
        headlineLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
        
        headlineField = new TextField();
        headlineField.setPromptText("A title for your capsule...");
        styleTextField(headlineField);
        
        headlineBox.getChildren().addAll(headlineLabel, headlineField);
        
        VBox messageBox = new VBox(5);
        Label messageLabel = new Label("Message");
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
        
        messageArea = new TextArea();
        messageArea.setPromptText("Write your message here. It will be encrypted and time-locked...");
        messageArea.setPrefRowCount(6);
        messageArea.setWrapText(true);
        styleTextArea(messageArea);
        
        messageBox.getChildren().addAll(messageLabel, messageArea);
        
        VBox dateTimeBox = createDateTimeSection();
        
        VBox recipientsBox = createRecipientsSection();
        
        VBox surpriseBox = createSurpriseSection();
        
        VBox passphraseBox = createPassphraseSection();
        
        form.getChildren().addAll(headlineBox, messageBox, dateTimeBox, 
                                   recipientsBox, surpriseBox, passphraseBox);
        
        return form;
    }
    
    private VBox createDateTimeSection() {
        VBox box = new VBox(5);
        
        Label label = new Label("Unlock Date & Time (UTC)");
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
        
        HBox dateTimeRow = new HBox(15);
        dateTimeRow.setAlignment(Pos.CENTER_LEFT);
        
        datePicker = new DatePicker(LocalDate.now().plusDays(1));
        datePicker.setStyle("-fx-background-color: #3A3A3C; -fx-control-inner-background: #3A3A3C;");
        
        hourSpinner = new Spinner<>(0, 23, 12);
        hourSpinner.setPrefWidth(80);
        hourSpinner.setEditable(true);
        
        Label colonLabel = new Label(":");
        colonLabel.setTextFill(Color.WHITE);
        colonLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 16));
        
        minuteSpinner = new Spinner<>(0, 59, 0);
        minuteSpinner.setPrefWidth(80);
        minuteSpinner.setEditable(true);
        
        Label utcLabel = new Label("UTC");
        utcLabel.setTextFill(Color.web("#8E8E93"));
        
        dateTimeRow.getChildren().addAll(datePicker, hourSpinner, colonLabel, minuteSpinner, utcLabel);
        box.getChildren().addAll(label, dateTimeRow);
        
        return box;
    }
    
    private VBox createRecipientsSection() {
        VBox box = new VBox(10);
        
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("Recipients (optional)");
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
        HBox.setHgrow(label, Priority.ALWAYS);
        
        Button addFriendBtn = createSmallButton("+ Add Friend", "#667eea");
        addFriendBtn.setOnAction(e -> showAddFriendDialog());
        
        Button addEmailBtn = createSmallButton("+ Add Email", "#5856D6");
        addEmailBtn.setOnAction(e -> showAddEmailDialog());
        
        headerRow.getChildren().addAll(label, addFriendBtn, addEmailBtn);
        
        recipientsListView = new ListView<>();
        recipientsListView.setPrefHeight(120);
        recipientsListView.setStyle("-fx-background-color: #3A3A3C; -fx-control-inner-background: #3A3A3C;");
        recipientsListView.setCellFactory(lv -> new RecipientCell());
        recipientsListView.setPlaceholder(new Label("No recipients added (capsule will be for yourself only)"));
        
        Label hintLabel = new Label("Recipients will receive the encrypted capsule and need the passphrase to decrypt.");
        hintLabel.setTextFill(Color.web("#8E8E93"));
        hintLabel.setWrapText(true);
        hintLabel.setFont(Font.font("SF Pro Display", 11));
        
        box.getChildren().addAll(headerRow, recipientsListView, hintLabel);
        return box;
    }
    
    private VBox createSurpriseSection() {
        VBox box = new VBox(8);
        
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("🎁 Surprise Mode");
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
        
        surpriseToggle = new ToggleButton("ON");
        surpriseToggle.setSelected(true);
        surpriseToggle.setStyle(
            "-fx-background-color: #34C759; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 15; " +
            "-fx-padding: 5 15;"
        );
        
        surpriseToggle.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                surpriseToggle.setText("ON");
                surpriseToggle.setStyle(surpriseToggle.getStyle().replace("#8E8E93", "#34C759").replace("#FF9500", "#34C759"));
                if (!surpriseToggle.getStyle().contains("#34C759")) {
                    surpriseToggle.setStyle("-fx-background-color: #34C759; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 15;");
                }
            } else {
                surpriseToggle.setText("OFF");
                surpriseToggle.setStyle("-fx-background-color: #8E8E93; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 15;");
            }
        });
        
        row.getChildren().addAll(label, surpriseToggle);
        
        Label descLabel = new Label(
            "ON: Recipients are notified only when the capsule becomes openable.\n" +
            "OFF: Recipients are notified immediately when you send, and again at unlock time."
        );
        descLabel.setTextFill(Color.web("#8E8E93"));
        descLabel.setWrapText(true);
        descLabel.setFont(Font.font("SF Pro Display", 11));
        
        box.getChildren().addAll(row, descLabel);
        return box;
    }
    
    private VBox createPassphraseSection() {
        VBox box = new VBox(10);
        
        Label label = new Label("🔐 Passphrase");
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
        
        Label hintLabel = new Label("Choose a strong passphrase. Share it with recipients separately (out-of-band).");
        hintLabel.setTextFill(Color.web("#FF9500"));
        hintLabel.setWrapText(true);
        hintLabel.setFont(Font.font("SF Pro Display", 11));
        
        passphraseField = new PasswordField();
        passphraseField.setPromptText("Enter passphrase...");
        styleTextField(passphraseField);
        
        confirmPassphraseField = new PasswordField();
        confirmPassphraseField.setPromptText("Confirm passphrase...");
        styleTextField(confirmPassphraseField);
        
        box.getChildren().addAll(label, hintLabel, passphraseField, confirmPassphraseField);
        return box;
    }
    
    private HBox createButtonBar() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: #3A3A3C; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 12 30;"
        );
        cancelBtn.setOnAction(e -> onComplete.accept(false));
        
        sendButton = new Button("🚀 Seal & Send Capsule");
        sendButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 12 30;"
        );
        sendButton.setOnAction(e -> sendCapsule());
        
        box.getChildren().addAll(cancelBtn, sendButton);
        return box;
    }
    
    private void loadFriends() {
        apiClient.listFriends()
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk() && response.getFriends() != null) {
                    availableFriends.clear();
                    availableFriends.addAll(response.getFriends());
                }
            }));
    }
    
    private void showAddFriendDialog() {
        if (availableFriends.isEmpty()) {
            setStatus("No friends available. Add friends first!");
            return;
        }
        
        Dialog<Friend> dialog = new Dialog<>();
        dialog.setTitle("Add Friend as Recipient");
        dialog.setHeaderText("Select a friend to receive this capsule:");
        
        ListView<Friend> friendsList = new ListView<>();
        friendsList.getItems().addAll(availableFriends);
        friendsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Friend friend, boolean empty) {
                super.updateItem(friend, empty);
                if (empty || friend == null) {
                    setText(null);
                } else {
                    String name = friend.getFriendDisplayName() != null ? 
                        friend.getFriendDisplayName() : friend.getFriendEmail();
                    setText(name + " (" + friend.getFriendEmail() + ")");
                }
            }
        });
        friendsList.setPrefHeight(200);
        
        dialog.getDialogPane().setContent(friendsList);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return friendsList.getSelectionModel().getSelectedItem();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(friend -> {
            for (RecipientEntry entry : recipientsListView.getItems()) {
                if (entry.email.equals(friend.getFriendEmail())) {
                    setStatus("This friend is already added");
                    return;
                }
            }
            
            RecipientEntry entry = new RecipientEntry();
            entry.email = friend.getFriendEmail();
            entry.displayName = friend.getFriendDisplayName();
            entry.userId = friend.getFriendUserId(apiClient.getCurrentUserId());
            entry.isFriend = true;
            
            recipientsListView.getItems().add(entry);
        });
    }
    
    private void showAddEmailDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Email Recipient");
        dialog.setHeaderText("Enter the email address of the recipient:");
        dialog.setContentText("Email:");
        
        dialog.showAndWait().ifPresent(email -> {
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                setStatus("Please enter a valid email address");
                return;
            }
            
            for (RecipientEntry entry : recipientsListView.getItems()) {
                if (entry.email.equals(email.trim())) {
                    setStatus("This email is already added");
                    return;
                }
            }
            
            RecipientEntry entry = new RecipientEntry();
            entry.email = email.trim();
            entry.isFriend = false;
            
            recipientsListView.getItems().add(entry);
        });
    }
    
    private void sendCapsule() {
        String message = messageArea.getText();
        if (message == null || message.trim().isEmpty()) {
            setStatus("Please enter a message");
            return;
        }
        
        String passphrase = passphraseField.getText();
        String confirmPassphrase = confirmPassphraseField.getText();
        
        if (passphrase == null || passphrase.isEmpty()) {
            setStatus("Please enter a passphrase");
            return;
        }
        
        if (!passphrase.equals(confirmPassphrase)) {
            setStatus("Passphrases do not match");
            return;
        }
        
        if (passphrase.length() < 6) {
            setStatus("Passphrase must be at least 6 characters");
            return;
        }
        
        LocalDate date = datePicker.getValue();
        if (date == null) {
            setStatus("Please select an unlock date");
            return;
        }
        
        LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
        LocalDateTime unlockDateTime = LocalDateTime.of(date, time);
        long unlockTimeEpoch = unlockDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        
        if (unlockTimeEpoch <= System.currentTimeMillis()) {
            setStatus("Unlock time must be in the future");
            return;
        }
        
        sendButton.setDisable(true);
        setStatus("Encrypting...");
        
        try {
            String headline = headlineField.getText();
            String associatedData = apiClient.getCurrentUserEmail() + "|" + unlockTimeEpoch;
            
            CryptoUtils.EncryptionResult encrypted = CryptoUtils.encrypt(
                message.trim(), passphrase, associatedData);
            
            List<CapsuleRecipient> recipients = new ArrayList<>();
            for (RecipientEntry entry : recipientsListView.getItems()) {
                CapsuleRecipient r = new CapsuleRecipient();
                r.setRecipientEmail(entry.email);
                r.setRecipientUserId(entry.userId);
                recipients.add(r);
            }
            
            boolean surpriseMode = surpriseToggle.isSelected();
            
            setStatus("Sending to server...");
            
            apiClient.createCapsule(
                headline, unlockTimeEpoch,
                encrypted.ciphertextBase64, encrypted.ivBase64, encrypted.saltBase64,
                recipients, surpriseMode
            ).thenAccept(response -> Platform.runLater(() -> {
                sendButton.setDisable(false);
                
                if (response.isOk()) {
                    showSuccessDialog(headline, recipients.size());
                    onComplete.accept(true);
                } else {
                    setStatus("Failed: " + response.getError());
                }
            }));
            
        } catch (Exception e) {
            sendButton.setDisable(false);
            setStatus("Encryption failed: " + e.getMessage());
        }
    }
    
    private void showSuccessDialog(String headline, int recipientCount) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Capsule Sealed!");
        alert.setHeaderText("🎉 Your time capsule has been created!");
        
        String content = "\"" + (headline != null && !headline.isEmpty() ? headline : "Untitled") + "\"";
        if (recipientCount > 0) {
            content += "\n\nSent to " + recipientCount + " recipient(s).\n" +
                       "Remember to share the passphrase with them separately!";
        }
        
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void styleTextField(TextField field) {
        field.setStyle(
            "-fx-background-color: #3A3A3C; " +
            "-fx-text-fill: white; " +
            "-fx-prompt-text-fill: #8E8E93; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10;"
        );
    }
    
    private void styleTextArea(TextArea area) {
        area.setStyle(
            "-fx-background-color: #3A3A3C; " +
            "-fx-text-fill: white; " +
            "-fx-prompt-text-fill: #8E8E93; " +
            "-fx-background-radius: 8; " +
            "-fx-control-inner-background: #3A3A3C;"
        );
    }
    
    private Button createSmallButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 11; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 5 10;"
        );
        return btn;
    }
    
    private void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    private static class RecipientEntry {
        String email;
        String displayName;
        String userId;
        boolean isFriend;
    }
    
    private class RecipientCell extends ListCell<RecipientEntry> {
        @Override
        protected void updateItem(RecipientEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            
            if (empty || entry == null) {
                setGraphic(null);
                return;
            }
            
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5));
            
            Label icon = new Label(entry.isFriend ? "👤" : "✉️");
            
            Label nameLabel = new Label(
                entry.displayName != null ? entry.displayName + " (" + entry.email + ")" : entry.email
            );
            nameLabel.setTextFill(Color.WHITE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            
            Button removeBtn = new Button("✗");
            removeBtn.setStyle(
                "-fx-background-color: #FF3B30; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-min-width: 20; -fx-min-height: 20; " +
                "-fx-max-width: 20; -fx-max-height: 20;"
            );
            removeBtn.setOnAction(e -> recipientsListView.getItems().remove(entry));
            
            row.getChildren().addAll(icon, nameLabel, removeBtn);
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
}
