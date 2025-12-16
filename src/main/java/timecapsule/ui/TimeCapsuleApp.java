package timecapsule.ui;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import timecapsule.api.ApiClient;
import timecapsule.crypto.CryptoUtils;
import timecapsule.model.ApiResponse;
import timecapsule.model.Capsule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Premium iOS-style TimeCapsule Application
 * 
 * TIME SECURITY: Changing your local clock WON'T help you open capsules early!
 * The SERVER (Google's cloud) checks the time, not your computer.
 * Server uses Date.now() which is Google's trusted clock, immune to client manipulation.
 */
public class TimeCapsuleApp extends Application {

    // ========================
    // Configuration
    // ========================
    private static final String BACKEND_URL = "https://script.google.com/macros/s/AKfycbyC0OBbnuPQl4xwEHokLDJ4P2bcqStNuMsHKMaUEK3a-EWuiyfr0U_HA1O6gPJJ5UKhlw/exec";
    private static final String OWNER_EMAIL = "sharmaashwin4000@gmail.com";

    // ========================
    // SVG Icons
    // ========================
    private static final String SVG_REFRESH = "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z";
    private static final String SVG_ADD = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_UNLOCK = "M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10z";
    private static final String SVG_CAPSULE = "M6 2v6h.01L6 8.01 10 12l-4 4 .01.01H6V22h12v-5.99h-.01L18 16l-4-4 4-3.99-.01-.01H18V2H6zm10 14.5V20H8v-3.5l4-4 4 4zm-4-5l-4-4V4h8v3.5l-4 4z";
    private static final String SVG_INFO = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
    private static final String SVG_SHIELD = "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z";

    // ========================
    // Colors
    // ========================
    private static final String PRIMARY_START = "#667eea";
    private static final String PRIMARY_END = "#764ba2";
    private static final String ACCENT = "#5856D6";
    private static final String SUCCESS = "#34C759";
    private static final String WARNING = "#FF9500";
    private static final String ERROR = "#FF3B30";
    private static final String BG_DARK = "#1C1C1E";
    private static final String BG_SECONDARY = "#2C2C2E";
    private static final String BG_TERTIARY = "#3A3A3C";
    private static final String TEXT_PRIMARY = "#FFFFFF";
    private static final String TEXT_SECONDARY = "#8E8E93";
    private static final String BORDER_COLOR = "#48484A";

    // ========================
    // UI Components
    // ========================
    private ApiClient apiClient;
    private TableView<Capsule> capsuleTable;
    private ObservableList<Capsule> capsuleList;
    private Label statusLabel;
    private VBox mainContainer;
    private StackPane loadingOverlay;
    private Stage primaryStage;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm");

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        apiClient = new ApiClient(BACKEND_URL);
        
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");
        
        mainContainer = createMainContainer();
        Rectangle gradientBg = createBackground(root);
        loadingOverlay = createLoadingOverlay();
        loadingOverlay.setVisible(false);
        
        root.getChildren().addAll(gradientBg, mainContainer, loadingOverlay);
        
        Scene scene = new Scene(root, 900, 650);
        scene.setFill(Color.web(BG_DARK));
        scene.getStylesheets().add("data:text/css," + getDarkCSS());
        
        stage.setTitle("TimeCapsule");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
        
        playEntrance();
        stage.setOnCloseRequest(e -> apiClient.shutdown());
    }

    private Rectangle createBackground(StackPane root) {
        Rectangle rect = new Rectangle();
        rect.widthProperty().bind(root.widthProperty());
        rect.heightProperty().bind(root.heightProperty());
        rect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#1a1a2e")),
            new Stop(0.5, Color.web("#16213e")),
            new Stop(1, Color.web("#0f0f23"))
        ));
        return rect;
    }

    private VBox createMainContainer() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));
        box.setAlignment(Pos.TOP_CENTER);
        
        box.getChildren().addAll(
            createHeader(),
            createTableCard(),
            createButtonBar(),
            createStatusLabel()
        );
        
        VBox.setVgrow(box.getChildren().get(1), Priority.ALWAYS);
        return box;
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Logo with glow
        SVGPath logo = createSVG(SVG_CAPSULE, 32);
        logo.setFill(Color.web(PRIMARY_START));
        DropShadow glow = new DropShadow(15, Color.web(PRIMARY_START, 0.6));
        logo.setEffect(glow);
        
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 10)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(glow.radiusProperty(), 20))
        );
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        
        VBox titleBox = new VBox(2);
        Label title = new Label("TimeCapsule");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label subtitle = new Label("Seal your memories in time");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Info button
        Button infoBtn = new Button();
        SVGPath infoIcon = createSVG(SVG_INFO, 22);
        infoIcon.setFill(Color.web(ACCENT));
        infoBtn.setGraphic(infoIcon);
        infoBtn.setStyle("-fx-background-color: " + BG_SECONDARY + "; -fx-background-radius: 20; -fx-padding: 10; -fx-cursor: hand;");
        infoBtn.setOnAction(e -> showInfoDialog());
        addHover(infoBtn);
        
        // User badge
        HBox userBadge = new HBox(8);
        userBadge.setAlignment(Pos.CENTER);
        userBadge.setPadding(new Insets(8, 15, 8, 15));
        userBadge.setStyle("-fx-background-color: " + BG_SECONDARY + "; -fx-background-radius: 20;");
        
        Circle avatar = new Circle(12, Color.web(ACCENT));
        Label initial = new Label(OWNER_EMAIL.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane avatarPane = new StackPane(avatar, initial);
        
        Label email = new Label(OWNER_EMAIL.length() > 18 ? OWNER_EMAIL.substring(0, 15) + "..." : OWNER_EMAIL);
        email.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        userBadge.getChildren().addAll(avatarPane, email);
        
        header.getChildren().addAll(logo, titleBox, spacer, infoBtn, userBadge);
        return header;
    }

    /**
     * Show info dialog explaining how time-lock works
     */
    private void showInfoDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_LEFT);
        root.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background-radius: 20; -fx-border-color: " + ACCENT + "40; -fx-border-radius: 20; -fx-border-width: 2;");
        
        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        SVGPath shield = createSVG(SVG_SHIELD, 28);
        shield.setFill(Color.web(ACCENT));
        Label titleLbl = new Label("How TimeCapsule Works");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        header.getChildren().addAll(shield, titleLbl);
        
        // Content sections
        VBox content = new VBox(15);
        
        content.getChildren().add(createInfoSection("🔐 Encryption", 
            "Your message is encrypted on YOUR device using AES-256.\n" +
            "The server NEVER sees your plaintext or passphrase."));
        
        content.getChildren().add(createInfoSection("⏰ Time Authority", 
            "The SERVER (Google Cloud) checks the unlock time.\n" +
            "It uses Date.now() - Google's trusted clock.\n" +
            "Your local computer time is IGNORED."));
        
        content.getChildren().add(createInfoSection("🛡️ Can I cheat by changing my clock?", 
            "NO! Changing your local time does nothing.\n" +
            "The server's clock is the only authority.\n" +
            "Even if you send a fake timestamp, the server\n" +
            "uses its OWN time to verify unlock eligibility."));
        
        content.getChildren().add(createInfoSection("❓ What happens if I open early?", 
            "Server returns: \"Not Yet!\"\n" +
            "You'll see how much time remains.\n" +
            "No encrypted data is returned until time passes."));
        
        // Close button
        Button closeBtn = new Button("Got it!");
        closeBtn.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 12 30; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        
        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(10, 0, 0, 0));
        
        root.getChildren().addAll(header, new Separator(), content, btnRow);
        
        Scene scene = new Scene(root, 450, 480);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private VBox createInfoSection(String title, String text) {
        VBox section = new VBox(5);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        textLbl.setWrapText(true);
        section.getChildren().addAll(titleLbl, textLbl);
        return section;
    }

    private VBox createTableCard() {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: " + BG_SECONDARY + "; -fx-background-radius: 20; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 20;");
        card.setPadding(new Insets(20));
        
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath icon = createSVG(SVG_CAPSULE, 18);
        icon.setFill(Color.web(TEXT_SECONDARY));
        Label sectionTitle = new Label("Your Capsules");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: " + TEXT_PRIMARY + ";");
        headerRow.getChildren().addAll(icon, sectionTitle);
        
        capsuleTable = createTable();
        capsuleList = FXCollections.observableArrayList();
        capsuleTable.setItems(capsuleList);
        VBox.setVgrow(capsuleTable, Priority.ALWAYS);
        
        card.getChildren().addAll(headerRow, capsuleTable);
        return card;
    }

    private TableView<Capsule> createTable() {
        TableView<Capsule> table = new TableView<>();
        table.setStyle("-fx-background-color: transparent;");
        
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        SVGPath emptyIcon = createSVG(SVG_CAPSULE, 48);
        emptyIcon.setFill(Color.web(TEXT_SECONDARY, 0.4));
        Label emptyLbl = new Label("No capsules yet");
        emptyLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        Label hintLbl = new Label("Click '+ New Capsule' to create one");
        hintLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_SECONDARY + "; -fx-opacity: 0.6;");
        placeholder.getChildren().addAll(emptyIcon, emptyLbl, hintLbl);
        table.setPlaceholder(placeholder);
        
        TableColumn<Capsule, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getHeadline() != null && !d.getValue().getHeadline().isEmpty() 
                ? d.getValue().getHeadline() : "Untitled"));
        titleCol.setPrefWidth(220);
        titleCol.setCellFactory(c -> darkCell());
        
        TableColumn<Capsule, String> dateCol = new TableColumn<>("Unlock Date");
        dateCol.setCellValueFactory(d -> {
            LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(d.getValue().getUnlockTimeEpoch()), ZoneId.systemDefault());
            return new SimpleStringProperty(dt.format(dateFormatter));
        });
        dateCol.setPrefWidth(180);
        dateCol.setCellFactory(c -> darkCell());
        
        TableColumn<Capsule, String> stateCol = new TableColumn<>("Status");
        stateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState()));
        stateCol.setPrefWidth(100);
        stateCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color: transparent;");
                if (empty || item == null) { setGraphic(null); }
                else {
                    Label badge = new Label(item.toUpperCase());
                    String col = "sealed".equals(item) ? WARNING : SUCCESS;
                    badge.setStyle("-fx-background-color: " + col + "20; -fx-text-fill: " + col + "; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
                    setGraphic(badge);
                }
            }
        });
        
        TableColumn<Capsule, String> timeCol = new TableColumn<>("Time Left");
        timeCol.setCellValueFactory(d -> {
            long unlock = d.getValue().getUnlockTimeEpoch();
            long now = System.currentTimeMillis();
            if (now >= unlock) return new SimpleStringProperty("✓ Ready!");
            long diff = unlock - now;
            long days = diff / 86400000;
            long hours = (diff % 86400000) / 3600000;
            long mins = (diff % 3600000) / 60000;
            if (days > 0) return new SimpleStringProperty(days + "d " + hours + "h");
            return new SimpleStringProperty(hours + "h " + mins + "m");
        });
        timeCol.setPrefWidth(120);
        timeCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color: transparent;");
                if (empty || item == null) { setText(null); }
                else {
                    setText(item);
                    String col = item.startsWith("✓") ? SUCCESS : TEXT_SECONDARY;
                    setStyle("-fx-text-fill: " + col + "; -fx-padding: 12 8; -fx-font-weight: 600; -fx-background-color: transparent;");
                }
            }
        });
        
        table.getColumns().addAll(titleCol, dateCol, stateCol, timeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        table.setRowFactory(tv -> {
            TableRow<Capsule> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color: " + BG_TERTIARY + "; -fx-background-radius: 8;"); });
            row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));
            row.setCursor(Cursor.HAND);
            return row;
        });
        
        return table;
    }

    private TableCell<Capsule, String> darkCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-padding: 12 8; -fx-background-color: transparent;");
            }
        };
    }

    private HBox createButtonBar() {
        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 0, 0, 0));
        
        Button refreshBtn = createSecondaryBtn("Refresh", SVG_REFRESH, "#4A90D9");
        refreshBtn.setOnAction(e -> refreshCapsules());
        
        Button newBtn = createPrimaryBtn("New Capsule", SVG_ADD);
        newBtn.setOnAction(e -> showNewCapsuleDialog());
        
        Button openBtn = createSecondaryBtn("Open Selected", SVG_UNLOCK, SUCCESS);
        openBtn.setOnAction(e -> openSelectedCapsule());
        
        bar.getChildren().addAll(refreshBtn, newBtn, openBtn);
        return bar;
    }

    private Button createSecondaryBtn(String text, String svg, String color) {
        Button btn = new Button();
        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);
        SVGPath icon = createSVG(svg, 16);
        icon.setFill(Color.web(color));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: 500;");
        content.getChildren().addAll(icon, lbl);
        btn.setGraphic(content);
        btn.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 12; -fx-padding: 12 20; -fx-cursor: hand;");
        addHover(btn);
        return btn;
    }

    private Button createPrimaryBtn(String text, String svg) {
        Button btn = new Button();
        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);
        SVGPath icon = createSVG(svg, 18);
        icon.setFill(Color.WHITE);
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;");
        content.getChildren().addAll(icon, lbl);
        btn.setGraphic(content);
        btn.setStyle("-fx-background-color: linear-gradient(to right, " + PRIMARY_START + ", " + PRIMARY_END + "); -fx-background-radius: 14; -fx-padding: 14 28; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + PRIMARY_START + "40, 10, 0, 0, 4);");
        addHover(btn);
        return btn;
    }

    private Label createStatusLabel() {
        statusLabel = new Label("Ready • Click 'Refresh' to load capsules");
        statusLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px; -fx-padding: 10 0 0 0;");
        return statusLabel;
    }

    private StackPane createLoadingOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
        
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: " + BG_SECONDARY + "; -fx-background-radius: 16; -fx-padding: 30;");
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(50, 50);
        Label lbl = new Label("Processing...");
        lbl.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 14px;");
        box.getChildren().addAll(spinner, lbl);
        overlay.getChildren().add(box);
        return overlay;
    }

    private SVGPath createSVG(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setScaleX(size / 24.0);
        svg.setScaleY(size / 24.0);
        return svg;
    }

    private void addHover(Node node) {
        node.setOnMouseEntered(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(100), node); st.setToX(1.05); st.setToY(1.05); st.play(); });
        node.setOnMouseExited(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(100), node); st.setToX(1.0); st.setToY(1.0); st.play(); });
        node.setOnMousePressed(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(50), node); st.setToX(0.95); st.setToY(0.95); st.play(); });
        node.setOnMouseReleased(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(50), node); st.setToX(1.05); st.setToY(1.05); st.play(); });
    }

    private void playEntrance() {
        mainContainer.setOpacity(0);
        mainContainer.setTranslateY(20);
        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(mainContainer.opacityProperty(), 0), new KeyValue(mainContainer.translateYProperty(), 20)),
            new KeyFrame(Duration.millis(500), new KeyValue(mainContainer.opacityProperty(), 1, Interpolator.EASE_OUT), new KeyValue(mainContainer.translateYProperty(), 0, Interpolator.EASE_OUT))
        );
        anim.play();
    }

    private String getDarkCSS() {
        return (".table-view{-fx-background-color:transparent;-fx-background:transparent;}" +
            ".table-view .column-header-background{-fx-background-color:transparent;}" +
            ".table-view .column-header{-fx-background-color:transparent;-fx-border-color:transparent;}" +
            ".table-view .column-header .label{-fx-text-fill:" + TEXT_SECONDARY + ";-fx-font-size:11px;-fx-font-weight:600;}" +
            ".table-view .table-row-cell{-fx-background-color:transparent;-fx-border-color:transparent;}" +
            ".table-view .table-cell{-fx-border-color:transparent;-fx-background-color:transparent;}" +
            ".table-view .filler{-fx-background-color:transparent;}" +
            ".scroll-bar{-fx-background-color:transparent;}" +
            ".scroll-bar .thumb{-fx-background-color:" + BORDER_COLOR + ";-fx-background-radius:5;}" +
            ".scroll-bar .track{-fx-background-color:transparent;}" +
            ".scroll-bar .increment-button,.scroll-bar .decrement-button{-fx-background-color:transparent;}" +
            ".scroll-bar .increment-arrow,.scroll-bar .decrement-arrow{-fx-background-color:transparent;}")
            .replace(";", "%3B").replace(":", "%3A").replace("#", "%23").replace(",", "%2C").replace(" ", "%20");
    }

    // ========== BUSINESS LOGIC ==========

    private void setStatus(String txt) { Platform.runLater(() -> statusLabel.setText(txt)); }
    private void showLoading(boolean show) { loadingOverlay.setVisible(show); }

    private void refreshCapsules() {
        showLoading(true);
        setStatus("Fetching capsules...");
        apiClient.listCapsules(OWNER_EMAIL).thenAccept(resp -> Platform.runLater(() -> {
            showLoading(false);
            if (resp.isSuccess() && resp.getCapsules() != null) {
                capsuleList.clear();
                capsuleList.addAll(resp.getCapsules());
                setStatus("✓ Loaded " + capsuleList.size() + " capsule(s)");
            } else {
                showAlert("Error", resp.getError() != null ? resp.getError() : "Could not connect", ERROR);
                setStatus("⚠ Error loading");
            }
        }));
    }

    private void showNewCapsuleDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(18);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background-radius: 20; -fx-border-color: " + PRIMARY_START + "40; -fx-border-radius: 20; -fx-border-width: 2;");
        
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label("✨ Create Time Capsule");
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(titleLbl, spacer, closeBtn);
        
        // Subtitle with info
        HBox subRow = new HBox(8);
        subRow.setAlignment(Pos.CENTER_LEFT);
        Label subLbl = new Label("Write a message for your future self");
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        Button infoBtn = new Button("ⓘ");
        infoBtn.setStyle("-fx-background-color: " + ACCENT + "30; -fx-text-fill: " + ACCENT + "; -fx-background-radius: 12; -fx-padding: 2 8; -fx-font-size: 12px; -fx-cursor: hand;");
        infoBtn.setOnAction(e -> showInfoDialog());
        subRow.getChildren().addAll(subLbl, infoBtn);
        
        // Form
        TextField titleField = createTextField("Title (optional)");
        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Your secret message...");
        msgArea.setPrefRowCount(4);
        msgArea.setStyle("-fx-control-inner-background: " + BG_TERTIARY + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-prompt-text-fill: " + TEXT_SECONDARY + ";");
        
        // Date/time spinners
        Label dtLabel = new Label("📅 Unlock Date & Time");
        dtLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + TEXT_PRIMARY + ";");
        
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Spinner<Integer> monthSpin = createSpinner(1, 12, tomorrow.getMonthValue());
        Spinner<Integer> daySpin = createSpinner(1, 31, tomorrow.getDayOfMonth());
        Spinner<Integer> yearSpin = createSpinner(2024, 2034, tomorrow.getYear());
        Spinner<Integer> hourSpin = createSpinner(0, 23, 12);
        Spinner<Integer> minSpin = createSpinner(0, 59, 0);
        
        HBox dtRow = new HBox(8);
        dtRow.setAlignment(Pos.CENTER_LEFT);
        dtRow.getChildren().addAll(
            wrapSpin(monthSpin, "Month"), wrapSpin(daySpin, "Day"), wrapSpin(yearSpin, "Year"),
            new Label("  "), wrapSpin(hourSpin, "Hour"), new Label(":") {{ setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 18px;"); }}, wrapSpin(minSpin, "Min")
        );
        
        PasswordField passField = createPassField("Enter passphrase");
        PasswordField confirmField = createPassField("Confirm passphrase");
        
        // Buttons
        HBox btnRow = new HBox(15);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: " + BG_TERTIARY + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-background-radius: 10; -fx-padding: 12 25; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());
        
        Button sealBtn = new Button("🔒 Seal Capsule");
        sealBtn.setStyle("-fx-background-color: linear-gradient(to right, " + PRIMARY_START + ", " + PRIMARY_END + "); -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 12 25; -fx-font-weight: bold; -fx-cursor: hand;");
        sealBtn.setDisable(true);
        
        btnRow.getChildren().addAll(cancelBtn, sealBtn);
        
        // Validation
        Runnable validate = () -> {
            boolean ok = !msgArea.getText().trim().isEmpty() && !passField.getText().isEmpty() && passField.getText().equals(confirmField.getText());
            sealBtn.setDisable(!ok);
        };
        msgArea.textProperty().addListener((o, a, b) -> validate.run());
        passField.textProperty().addListener((o, a, b) -> validate.run());
        confirmField.textProperty().addListener((o, a, b) -> validate.run());
        
        sealBtn.setOnAction(e -> {
            int month = monthSpin.getValue();
            int day = Math.min(daySpin.getValue(), LocalDate.of(yearSpin.getValue(), month, 1).lengthOfMonth());
            LocalDateTime unlock = LocalDateTime.of(yearSpin.getValue(), month, day, hourSpin.getValue(), minSpin.getValue());
            long epoch = unlock.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            if (epoch <= System.currentTimeMillis()) {
                showAlert("Invalid Time", "Unlock time must be in the future!", WARNING);
                return;
            }
            dialog.close();
            createCapsule(titleField.getText().trim(), msgArea.getText(), epoch, passField.getText());
        });
        
        root.getChildren().addAll(header, subRow, new Separator(), 
            formRow("Title", titleField), formRow("Message", msgArea), dtLabel, dtRow, 
            formRow("Passphrase", passField), formRow("Confirm", confirmField), btnRow);
        
        Scene scene = new Scene(root, 550, 580);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private Spinner<Integer> createSpinner(int min, int max, int val) {
        Spinner<Integer> spin = new Spinner<>(min, max, val);
        spin.setEditable(true);
        spin.setPrefWidth(70);
        spin.setStyle("-fx-background-color: " + BG_TERTIARY + "; -fx-background-radius: 8;");
        spin.getEditor().setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-alignment: center;");
        spin.setOnScroll(e -> { if (e.getDeltaY() > 0) spin.increment(); else spin.decrement(); });
        return spin;
    }

    private VBox wrapSpin(Spinner<?> spin, String label) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 9px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        box.getChildren().addAll(spin, lbl);
        return box;
    }

    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: " + BG_TERTIARY + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-prompt-text-fill: " + TEXT_SECONDARY + "; -fx-background-radius: 10; -fx-padding: 12;");
        return field;
    }

    private PasswordField createPassField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: " + BG_TERTIARY + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-prompt-text-fill: " + TEXT_SECONDARY + "; -fx-background-radius: 10; -fx-padding: 12;");
        return field;
    }

    private HBox formRow(String label, Control field) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setMinWidth(75);
        lbl.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");
        HBox.setHgrow(field, Priority.ALWAYS);
        row.getChildren().addAll(lbl, field);
        return row;
    }

    private void createCapsule(String headline, String message, long unlock, String pass) {
        showLoading(true);
        setStatus("Encrypting and sealing...");
        new Thread(() -> {
            try {
                String aad = OWNER_EMAIL + "|" + unlock;
                CryptoUtils.EncryptionResult enc = CryptoUtils.encrypt(message, pass, aad);
                apiClient.createCapsule(OWNER_EMAIL, headline, unlock, enc.ciphertextBase64, enc.ivBase64, enc.saltBase64)
                    .thenAccept(resp -> Platform.runLater(() -> {
                        showLoading(false);
                        if (resp.isSuccess()) {
                            showAlert("Sealed! 🔒", "Capsule ID: " + resp.getId() + "\n\nRemember your passphrase!", SUCCESS);
                            setStatus("✓ Created");
                            refreshCapsules();
                        } else { showAlert("Error", resp.getError(), ERROR); setStatus("⚠ Failed"); }
                    }));
            } catch (Exception ex) {
                Platform.runLater(() -> { showLoading(false); showAlert("Error", ex.getMessage(), ERROR); });
            }
        }).start();
    }

    private void openSelectedCapsule() {
        Capsule sel = capsuleTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("No Selection", "Please select a capsule first.", WARNING); return; }
        
        // Password dialog
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background-radius: 16; -fx-border-color: " + SUCCESS + "40; -fx-border-radius: 16; -fx-border-width: 2;");
        
        SVGPath icon = createSVG(SVG_UNLOCK, 40);
        icon.setFill(Color.web(SUCCESS));
        Label titleLbl = new Label("Open Capsule");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        Label subLbl = new Label("Enter passphrase to decrypt");
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        
        PasswordField pwField = createPassField("Passphrase");
        pwField.setPrefWidth(250);
        
        HBox btns = new HBox(15);
        btns.setAlignment(Pos.CENTER);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: " + BG_TERTIARY + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());
        Button openBtn = new Button("Decrypt");
        openBtn.setStyle("-fx-background-color: " + SUCCESS + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
        btns.getChildren().addAll(cancelBtn, openBtn);
        
        root.getChildren().addAll(icon, titleLbl, subLbl, pwField, btns);
        
        final String[] pw = {null};
        openBtn.setOnAction(e -> { pw[0] = pwField.getText(); dialog.close(); });
        pwField.setOnAction(e -> { pw[0] = pwField.getText(); dialog.close(); });
        
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
        
        if (pw[0] == null || pw[0].isEmpty()) return;
        
        showLoading(true);
        setStatus("Opening...");
        String pass = pw[0];
        
        apiClient.openCapsule(sel.getId(), OWNER_EMAIL).thenAccept(resp -> Platform.runLater(() -> {
            showLoading(false);
            if (resp.isNotYet()) {
                showTimeLockMessage();
                setStatus("⏳ Still sealed");
            } else if (resp.isSuccess() && resp.getCapsule() != null) {
                try {
                    Capsule d = resp.getCapsule();
                    String aad = OWNER_EMAIL + "|" + sel.getUnlockTimeEpoch();
                    String msg = CryptoUtils.decrypt(d.getCiphertextBase64(), d.getIvBase64(), d.getSaltBase64(), pass, aad);
                    showDecryptedMessage(sel.getHeadline(), msg);
                    setStatus("✓ Opened!");
                    refreshCapsules();
                } catch (Exception ex) {
                    showAlert("Decryption Failed", "Wrong passphrase or corrupted data.", ERROR);
                    setStatus("⚠ Failed");
                }
            } else { showAlert("Error", resp.getError(), ERROR); }
        }));
    }

    /**
     * Show explanation when user tries to open early
     */
    private void showTimeLockMessage() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background-radius: 16; -fx-border-color: " + WARNING + "60; -fx-border-radius: 16; -fx-border-width: 2;");
        root.setMaxWidth(400);
        
        Label emoji = new Label("⏰");
        emoji.setStyle("-fx-font-size: 48px;");
        
        Label titleLbl = new Label("Not Yet!");
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + WARNING + ";");
        
        Label msgLbl = new Label(
            "This capsule is still time-locked.\n\n" +
            "The SERVER checks the time, not your computer.\n" +
            "Even if you change your local clock, it won't help!\n\n" +
            "Please wait until the unlock time passes."
        );
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_SECONDARY + "; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);
        
        Button okBtn = new Button("I understand");
        okBtn.setStyle("-fx-background-color: " + WARNING + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 12 30; -fx-cursor: hand;");
        okBtn.setOnAction(e -> dialog.close());
        
        root.getChildren().addAll(emoji, titleLbl, msgLbl, okBtn);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Modern minimalist message card
     */
    private void showDecryptedMessage(String headline, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background-radius: 20;");
        root.setMaxWidth(500);
        
        // Gradient header
        StackPane headerPane = new StackPane();
        headerPane.setStyle("-fx-background-color: linear-gradient(to right, " + PRIMARY_START + ", " + PRIMARY_END + "); -fx-background-radius: 20 20 0 0;");
        headerPane.setPadding(new Insets(30));
        
        VBox headerContent = new VBox(8);
        headerContent.setAlignment(Pos.CENTER);
        
        Label successIcon = new Label("🎉");
        successIcon.setStyle("-fx-font-size: 40px;");
        
        Label titleLbl = new Label(headline != null && !headline.isEmpty() ? headline : "Your Time Capsule");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        titleLbl.setWrapText(true);
        
        Label subLbl = new Label("Message from the past");
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");
        
        headerContent.getChildren().addAll(successIcon, titleLbl, subLbl);
        headerPane.getChildren().add(headerContent);
        
        // Message body
        VBox bodyPane = new VBox(20);
        bodyPane.setPadding(new Insets(25));
        bodyPane.setStyle("-fx-background-color: " + BG_SECONDARY + "; -fx-background-radius: 0 0 20 20;");
        
        // Message card
        VBox msgCard = new VBox(10);
        msgCard.setPadding(new Insets(20));
        msgCard.setStyle("-fx-background-color: " + BG_TERTIARY + "; -fx-background-radius: 12;");
        
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-line-spacing: 4;");
        msgLabel.setWrapText(true);
        
        // If message is long, use TextArea
        if (message.length() > 300) {
            TextArea msgArea = new TextArea(message);
            msgArea.setEditable(false);
            msgArea.setWrapText(true);
            msgArea.setPrefRowCount(8);
            msgArea.setStyle("-fx-control-inner-background: transparent; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-background-color: transparent; -fx-border-color: transparent;");
            msgCard.getChildren().add(msgArea);
        } else {
            msgCard.getChildren().add(msgLabel);
        }
        
        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: " + PRIMARY_START + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 12 40; -fx-font-weight: 600; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        addHover(closeBtn);
        
        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER);
        
        bodyPane.getChildren().addAll(msgCard, btnRow);
        root.getChildren().addAll(headerPane, bodyPane);
        
        // Animation
        root.setOpacity(0);
        root.setScaleX(0.8);
        root.setScaleY(0.8);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        
        // Entrance animation
        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(root.opacityProperty(), 0),
                new KeyValue(root.scaleXProperty(), 0.8),
                new KeyValue(root.scaleYProperty(), 0.8)),
            new KeyFrame(Duration.millis(300),
                new KeyValue(root.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(root.scaleXProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(root.scaleYProperty(), 1, Interpolator.EASE_OUT))
        );
        
        dialog.setOnShown(e -> anim.play());
        dialog.showAndWait();
    }

    private void showAlert(String title, String msg, String color) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background-radius: 16; -fx-border-color: " + color + "40; -fx-border-radius: 16; -fx-border-width: 2;");
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label msgLbl = new Label(msg);
        msgLbl.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(300);
        
        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 30; -fx-cursor: hand;");
        okBtn.setOnAction(e -> dialog.close());
        
        root.getChildren().addAll(titleLbl, msgLbl, okBtn);
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}
