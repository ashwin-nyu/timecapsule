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
import timecapsule.model.*;

import java.util.List;

public class FriendsScreen extends VBox {
    
    private final ApiClient apiClient;
    private final TabPane tabPane;
    private final ListView<Friend> friendsListView;
    private final ListView<Friend> requestsListView;
    private final ListView<Invite> invitesListView;
    private final Label statusLabel;
    
    public FriendsScreen(ApiClient apiClient) {
        this.apiClient = apiClient;
        
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #1C1C1E;");
        
        Label headerLabel = new Label("👥 Friends");
        headerLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 28));
        headerLabel.setTextFill(Color.WHITE);
        
        HBox searchBox = createSearchSection();
        
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #2C2C2E;");
        
        friendsListView = new ListView<>();
        friendsListView.setStyle("-fx-background-color: #2C2C2E; -fx-control-inner-background: #2C2C2E;");
        friendsListView.setCellFactory(lv -> new FriendCell());
        friendsListView.setPlaceholder(new Label("No friends yet. Add some!"));
        
        Tab friendsTab = new Tab("Friends", friendsListView);
        friendsTab.setStyle("-fx-background-color: #667eea;");
        
        requestsListView = new ListView<>();
        requestsListView.setStyle("-fx-background-color: #2C2C2E; -fx-control-inner-background: #2C2C2E;");
        requestsListView.setCellFactory(lv -> new RequestCell());
        requestsListView.setPlaceholder(new Label("No pending requests"));
        
        Tab requestsTab = new Tab("Requests", requestsListView);
        
        invitesListView = new ListView<>();
        invitesListView.setStyle("-fx-background-color: #2C2C2E; -fx-control-inner-background: #2C2C2E;");
        invitesListView.setCellFactory(lv -> new InviteCell());
        invitesListView.setPlaceholder(new Label("No invites sent"));
        
        Tab invitesTab = new Tab("Invites Sent", invitesListView);
        
        tabPane.getTabs().addAll(friendsTab, requestsTab, invitesTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        statusLabel = new Label();
        statusLabel.setTextFill(Color.web("#8E8E93"));
        
        getChildren().addAll(headerLabel, searchBox, tabPane, statusLabel);
        
        refreshAll();
    }
    
    private HBox createSearchSection() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search by email or username...");
        searchField.setPrefWidth(250);
        searchField.setStyle(
            "-fx-background-color: #3A3A3C; " +
            "-fx-text-fill: white; " +
            "-fx-prompt-text-fill: #8E8E93; " +
            "-fx-background-radius: 8;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        Button searchBtn = createStyledButton("🔍 Search", "#5856D6");
        searchBtn.setOnAction(e -> searchUsers(searchField.getText()));
        
        Button inviteBtn = createStyledButton("✉️ Invite by Email", "#667eea");
        inviteBtn.setOnAction(e -> showInviteDialog());
        
        Button refreshBtn = createStyledButton("🔄 Refresh", "#34C759");
        refreshBtn.setOnAction(e -> refreshAll());
        
        box.getChildren().addAll(searchField, searchBtn, inviteBtn, refreshBtn);
        return box;
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
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(color, adjustColor(color, 0.9))));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace(adjustColor(color, 0.9), color)));
        return btn;
    }
    
    private String adjustColor(String hex, double factor) {
        return hex;
    }
    
    private void searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            setStatus("Enter a search term");
            return;
        }
        
        setStatus("Searching...");
        
        apiClient.searchUsers(query.trim())
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk() && response.getUsers() != null) {
                    showSearchResults(response.getUsers());
                } else {
                    setStatus("Search failed: " + response.getError());
                }
            }));
    }
    
    private void showSearchResults(List<User> users) {
        if (users.isEmpty()) {
            setStatus("No users found. Try inviting by email!");
            return;
        }
        
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Search Results");
        dialog.setHeaderText("Select a user to add as friend:");
        
        ListView<User> resultsList = new ListView<>();
        resultsList.getItems().addAll(users);
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getDisplayName() + " (" + user.getEmail() + ")");
                }
            }
        });
        resultsList.setPrefHeight(200);
        
        dialog.getDialogPane().setContent(resultsList);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return resultsList.getSelectionModel().getSelectedItem();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(user -> sendFriendRequest(user.getUserId()));
    }
    
    private void sendFriendRequest(String addresseeUserId) {
        setStatus("Sending friend request...");
        
        apiClient.sendFriendRequest(addresseeUserId)
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk()) {
                    setStatus("Friend request sent!");
                    refreshRequests();
                } else {
                    setStatus("Failed: " + response.getError());
                }
            }));
    }
    
    private void showInviteDialog() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Invite by Email");
        dialog.setHeaderText("Invite someone who isn't on TimeCapsule yet");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField emailField = new TextField();
        emailField.setPromptText("friend@example.com");
        emailField.setPrefWidth(300);
        
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Optional personal message...");
        messageArea.setPrefRowCount(3);
        
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Message:"), 0, 1);
        grid.add(messageArea, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new String[]{emailField.getText(), messageArea.getText()};
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> sendInvite(result[0], result[1]));
    }
    
    private void sendInvite(String email, String message) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            setStatus("Please enter a valid email address");
            return;
        }
        
        setStatus("Sending invite...");
        
        apiClient.sendInvite(email.trim(), message)
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk()) {
                    setStatus("Invite sent to " + email + "!");
                    refreshInvites();
                } else {
                    setStatus("Failed: " + response.getError());
                }
            }));
    }
    
    public void refreshAll() {
        refreshFriends();
        refreshRequests();
        refreshInvites();
    }
    
    private void refreshFriends() {
        apiClient.listFriends()
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk() && response.getFriends() != null) {
                    friendsListView.getItems().clear();
                    friendsListView.getItems().addAll(response.getFriends());
                }
            }));
    }
    
    private void refreshRequests() {
        apiClient.listFriendRequests()
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk() && response.getRequests() != null) {
                    requestsListView.getItems().clear();
                    requestsListView.getItems().addAll(response.getRequests());
                    
                    int count = response.getRequests().size();
                    tabPane.getTabs().get(1).setText("Requests" + (count > 0 ? " (" + count + ")" : ""));
                }
            }));
    }
    
    private void refreshInvites() {
        apiClient.listSentInvites()
            .thenAccept(response -> Platform.runLater(() -> {
                if (response.isOk() && response.getInvites() != null) {
                    invitesListView.getItems().clear();
                    invitesListView.getItems().addAll(response.getInvites());
                }
            }));
    }
    
    private void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    private class FriendCell extends ListCell<Friend> {
        @Override
        protected void updateItem(Friend friend, boolean empty) {
            super.updateItem(friend, empty);
            
            if (empty || friend == null) {
                setGraphic(null);
                return;
            }
            
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: #3A3A3C; -fx-background-radius: 8;");
            
            VBox info = new VBox(3);
            Label nameLabel = new Label(friend.getFriendDisplayName() != null ? 
                friend.getFriendDisplayName() : friend.getFriendEmail());
            nameLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
            nameLabel.setTextFill(Color.WHITE);
            
            Label emailLabel = new Label(friend.getFriendEmail());
            emailLabel.setTextFill(Color.web("#8E8E93"));
            
            info.getChildren().addAll(nameLabel, emailLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            
            Button sendCapsuleBtn = createStyledButton("📨 Send Capsule", "#667eea");
            sendCapsuleBtn.setOnAction(e -> {
            });
            
            row.getChildren().addAll(info, sendCapsuleBtn);
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
    
    private class RequestCell extends ListCell<Friend> {
        @Override
        protected void updateItem(Friend request, boolean empty) {
            super.updateItem(request, empty);
            
            if (empty || request == null) {
                setGraphic(null);
                return;
            }
            
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: #3A3A3C; -fx-background-radius: 8;");
            
            boolean isIncoming = !request.isRequester(apiClient.getCurrentUserId());
            
            VBox info = new VBox(3);
            Label nameLabel = new Label(request.getFriendDisplayName() != null ? 
                request.getFriendDisplayName() : request.getFriendEmail());
            nameLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
            nameLabel.setTextFill(Color.WHITE);
            
            Label typeLabel = new Label(isIncoming ? "Incoming request" : "Outgoing request");
            typeLabel.setTextFill(Color.web(isIncoming ? "#FF9500" : "#8E8E93"));
            
            info.getChildren().addAll(nameLabel, typeLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            
            HBox buttons = new HBox(8);
            
            if (isIncoming) {
                Button acceptBtn = createStyledButton("✓ Accept", "#34C759");
                acceptBtn.setOnAction(e -> {
                    apiClient.acceptFriendRequest(request.getRequesterUserId())
                        .thenAccept(response -> Platform.runLater(() -> {
                            if (response.isOk()) {
                                setStatus("Friend added!");
                                refreshAll();
                            } else {
                                setStatus("Failed: " + response.getError());
                            }
                        }));
                });
                
                Button declineBtn = createStyledButton("✗ Decline", "#FF3B30");
                declineBtn.setOnAction(e -> {
                    apiClient.declineFriendRequest(request.getRequesterUserId())
                        .thenAccept(response -> Platform.runLater(() -> refreshRequests()));
                });
                
                buttons.getChildren().addAll(acceptBtn, declineBtn);
            } else {
                Label pendingLabel = new Label("Pending...");
                pendingLabel.setTextFill(Color.web("#8E8E93"));
                buttons.getChildren().add(pendingLabel);
            }
            
            row.getChildren().addAll(info, buttons);
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
    
    private class InviteCell extends ListCell<Invite> {
        @Override
        protected void updateItem(Invite invite, boolean empty) {
            super.updateItem(invite, empty);
            
            if (empty || invite == null) {
                setGraphic(null);
                return;
            }
            
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: #3A3A3C; -fx-background-radius: 8;");
            
            VBox info = new VBox(3);
            Label emailLabel = new Label(invite.getInviteeEmail());
            emailLabel.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
            emailLabel.setTextFill(Color.WHITE);
            
            String statusText;
            Color statusColor;
            switch (invite.getStatus()) {
                case ACCEPTED:
                    statusText = "✓ Accepted";
                    statusColor = Color.web("#34C759");
                    break;
                case EXPIRED:
                    statusText = "✗ Expired";
                    statusColor = Color.web("#FF3B30");
                    break;
                default:
                    long daysLeft = (invite.getExpiresAtUtc() - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);
                    statusText = "Pending • Expires in " + Math.max(0, daysLeft) + " days";
                    statusColor = Color.web("#FF9500");
            }
            
            Label statusLabel = new Label(statusText);
            statusLabel.setTextFill(statusColor);
            
            info.getChildren().addAll(emailLabel, statusLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            
            HBox buttons = new HBox(8);
            
            if (invite.getStatus() == InviteStatus.SENT && invite.isValid()) {
                Button resendBtn = createStyledButton("🔄 Resend", "#5856D6");
                resendBtn.setOnAction(e -> {
                    apiClient.resendInvite(invite.getInviteId())
                        .thenAccept(response -> Platform.runLater(() -> {
                            if (response.isOk()) {
                                setStatus("Invite resent!");
                            } else {
                                setStatus("Failed: " + response.getError());
                            }
                        }));
                });
                buttons.getChildren().add(resendBtn);
            }
            
            row.getChildren().addAll(info, buttons);
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
}
