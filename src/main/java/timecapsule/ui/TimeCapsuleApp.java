package timecapsule.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import timecapsule.api.ApiClient;
import timecapsule.model.User;

public class TimeCapsuleApp extends Application {
    
    private Stage primaryStage;
    private BorderPane rootPane;
    private ApiClient apiClient;
    private User currentUser;
    
    private VBox navBar;
    private Button navFriends;
    private Button navSent;
    private Button navReceived;
    private Button navCompose;
    
    private FriendsScreen friendsScreen;
    private SentCapsulesScreen sentCapsulesScreen;
    private ReceivedCapsulesScreen receivedCapsulesScreen;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.apiClient = new ApiClient();
        
        showLoginScreen();
        
        stage.setTitle("TimeCapsule");
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }
    
    private void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen(apiClient, this::onLoginSuccess);
        Scene scene = new Scene(loginScreen);
        scene.setFill(Color.web("#1C1C1E"));
        primaryStage.setScene(scene);
    }
    
    private void onLoginSuccess(User user) {
        this.currentUser = user;
        showMainApp();
    }
    
    private void showMainApp() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #1C1C1E;");
        
        navBar = createNavBar();
        rootPane.setLeft(navBar);
        
        friendsScreen = new FriendsScreen(apiClient);
        sentCapsulesScreen = new SentCapsulesScreen(apiClient, v -> showComposeScreen());
        receivedCapsulesScreen = new ReceivedCapsulesScreen(apiClient);
        
        showScreen("received");
        
        Scene scene = new Scene(rootPane);
        scene.setFill(Color.web("#1C1C1E"));
        primaryStage.setScene(scene);
    }
    
    private VBox createNavBar() {
        VBox nav = new VBox(10);
        nav.setPadding(new Insets(20));
        nav.setStyle("-fx-background-color: #2C2C2E;");
        nav.setPrefWidth(200);
        nav.setAlignment(Pos.TOP_CENTER);
        
        Label logo = new Label("⏳");
        logo.setFont(Font.font(48));
        
        Label appName = new Label("TimeCapsule");
        appName.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 18));
        appName.setTextFill(Color.WHITE);
        
        VBox userBox = new VBox(3);
        userBox.setAlignment(Pos.CENTER);
        userBox.setPadding(new Insets(10, 0, 20, 0));
        
        Label userName = new Label(currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty() 
            ? currentUser.getDisplayName() : currentUser.getEmail());
        userName.setFont(Font.font("SF Pro Display", FontWeight.BOLD, 14));
        userName.setTextFill(Color.WHITE);
        
        Label userEmail = new Label(currentUser.getEmail());
        userEmail.setFont(Font.font("SF Pro Display", 11));
        userEmail.setTextFill(Color.web("#8E8E93"));
        
        userBox.getChildren().addAll(userName, userEmail);
        
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #3A3A3C;");
        
        navReceived = createNavButton("📬 Inbox", "received");
        navSent = createNavButton("📤 My Capsules", "sent");
        navFriends = createNavButton("👥 Friends", "friends");
        navCompose = createNavButton("✉️ New Capsule", "compose");
        navCompose.setStyle(
            "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 15; " +
            "-fx-cursor: hand;"
        );
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle(
            "-fx-background-color: #3A3A3C; " +
            "-fx-text-fill: #FF3B30; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 15; " +
            "-fx-cursor: hand;"
        );
        logoutBtn.setOnAction(e -> logout());
        
        nav.getChildren().addAll(
            logo, appName, userBox, sep1,
            navReceived, navSent, navFriends,
            new Separator(),
            navCompose,
            spacer, logoutBtn
        );
        
        return nav;
    }
    
    private Button createNavButton(String text, String screenId) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 15; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyleClass().contains("selected")) {
                btn.setStyle(btn.getStyle().replace("transparent", "#3A3A3C"));
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyleClass().contains("selected")) {
                btn.setStyle(btn.getStyle().replace("#3A3A3C", "transparent"));
            }
        });
        
        btn.setOnAction(e -> showScreen(screenId));
        
        return btn;
    }
    
    private void showScreen(String screenId) {
        resetNavButtons();
        
        switch (screenId) {
            case "friends":
                rootPane.setCenter(friendsScreen);
                friendsScreen.refreshAll();
                setNavSelected(navFriends);
                break;
                
            case "sent":
                rootPane.setCenter(sentCapsulesScreen);
                sentCapsulesScreen.refresh();
                setNavSelected(navSent);
                break;
                
            case "received":
                rootPane.setCenter(receivedCapsulesScreen);
                receivedCapsulesScreen.refresh();
                setNavSelected(navReceived);
                break;
                
            case "compose":
                showComposeScreen();
                break;
        }
    }
    
    private void showComposeScreen() {
        ComposeCapsuleScreen composeScreen = new ComposeCapsuleScreen(apiClient, success -> {
            if (success) {
                showScreen("sent");
            } else {
                showScreen("received");
            }
        });
        rootPane.setCenter(composeScreen);
        setNavSelected(navCompose);
    }
    
    private void resetNavButtons() {
        for (Button btn : new Button[]{navReceived, navSent, navFriends}) {
            btn.getStyleClass().remove("selected");
            btn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 10 15; " +
                "-fx-cursor: hand;"
            );
        }
    }
    
    private void setNavSelected(Button btn) {
        if (btn == navCompose) return;
        
        btn.getStyleClass().add("selected");
        btn.setStyle(
            "-fx-background-color: #667eea; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 15; " +
            "-fx-cursor: hand;"
        );
    }
    
    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You'll need to login again to access your capsules.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                currentUser = null;
                apiClient.setCurrentUser(null, null);
                showLoginScreen();
            }
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
