package oogasalad.view.accounts;

import java.io.File;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.AuthService;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.view.core.api.AppScreen;

public class UpdateProfileScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.UpdateProfileScreen");

  private final Parent root;
  private final AuthService authService;
  private final Label usernameValueLabel = new Label();
  private final Label avatarPathLabel = new Label();
  private final ImageView avatarPreview = new ImageView();
  private final Label statusLabel = new Label();
  private String selectedAvatarPath = "";

  public UpdateProfileScreen(Consumer<Outcome> onOutcome, AuthService authService) {
    this.authService = authService;

    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());
    layout.getStyleClass().add("screen-root");

    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.OPEN_PROFILE));
    BorderPane.setMargin(backButton, new Insets(18, 0, 0, 18));
    layout.setTop(backButton);

    Label title = new Label(PROPS.getString("title"));
    title.getStyleClass().add("accounts-title");

    usernameValueLabel.getStyleClass().add("accounts-subtitle");
    HBox usernameRow = new HBox(8, new Label(PROPS.getString("usernameLabel")), usernameValueLabel);
    usernameRow.setAlignment(Pos.CENTER);

    avatarPathLabel.getStyleClass().add("accounts-path-label");
    Button browseButton = new Button(PROPS.getString("browseButton"));
    browseButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    browseButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle(PROPS.getString("chooseAvatarTitle"));
      chooser.getExtensionFilters().add(
          new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
      File file = chooser.showOpenDialog(layout.getScene() != null ? layout.getScene().getWindow() : null);
      if (file != null) {
        selectedAvatarPath = file.getAbsolutePath();
        avatarPathLabel.setText(file.getName());
        loadPreview(selectedAvatarPath);
      }
    });

    HBox avatarRow = new HBox(8, new Label(PROPS.getString("avatarLabel")), avatarPathLabel, browseButton);
    avatarRow.setAlignment(Pos.CENTER);

    avatarPreview.setFitWidth(100);
    avatarPreview.setFitHeight(100);
    avatarPreview.setPreserveRatio(true);

    statusLabel.getStyleClass().add("accounts-error");

    Button saveButton = new Button(PROPS.getString("saveButton"));
    saveButton.setPrefWidth(Double.parseDouble(PROPS.getString("buttonWidth")));
    saveButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    saveButton.setOnAction(e -> {
      AuthResult result = authService.updateAvatar(selectedAvatarPath);
      if (result instanceof AuthResult.Success) {
        onOutcome.accept(Outcome.OPEN_PROFILE);
      } else if (result instanceof AuthResult.Failure f) {
        statusLabel.setText(f.reason());
      }
    });

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        title, usernameRow, avatarRow, avatarPreview, saveButton, statusLabel);
    center.setAlignment(Pos.CENTER);
    center.setPadding(new Insets(Double.parseDouble(PROPS.getString("padding"))));
    layout.setCenter(center);

    this.root = layout;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void onEnter() {
    if (!authService.session().isLoggedIn()) return;
    PlayerProfile profile = authService.session().currentUser();
    usernameValueLabel.setText(profile.username());
    selectedAvatarPath = profile.avatarPath() != null ? profile.avatarPath() : "";
    if (!selectedAvatarPath.isEmpty()) {
      avatarPathLabel.setText(new File(selectedAvatarPath).getName());
      loadPreview(selectedAvatarPath);
    } else {
      avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
      avatarPreview.setImage(null);
    }
    statusLabel.setText("");
  }

  private void loadPreview(String path) {
    try {
      avatarPreview.setImage(new Image("file:" + path, 100, 100, true, true));
    } catch (Exception ignored) {
      avatarPreview.setImage(null);
    }
  }
}
