package oogasalad.view.accounts;

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
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.auth.api.AuthService;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.view.core.api.AppScreen;

public class ProfileScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.ProfileScreen");

  private final Parent root;
  private final AuthService authService;
  private final ImageView avatarView = new ImageView();
  private final Label usernameLabel = new Label();
  private final Label friendsLabel = new Label();

  public ProfileScreen(Consumer<Outcome> onOutcome, AuthService authService) {
    this.authService = authService;

    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());
    layout.getStyleClass().add("screen-root");

    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.BACK_TO_MENU));

    Button editButton = new Button(PROPS.getString("editButton"));
    editButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    editButton.setOnAction(e -> onOutcome.accept(Outcome.OPEN_UPDATE_PROFILE));

    Button logoutButton = new Button(PROPS.getString("logoutButton"));
    logoutButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    logoutButton.setOnAction(e -> {
      authService.logout();
      onOutcome.accept(Outcome.BACK_TO_MENU);
    });

    HBox topBar = new HBox(10, backButton, editButton, logoutButton);
    topBar.setPadding(new Insets(18, 0, 0, 18));
    layout.setTop(topBar);

    avatarView.setFitWidth(120);
    avatarView.setFitHeight(120);
    avatarView.setPreserveRatio(true);

    usernameLabel.getStyleClass().add("accounts-title");
    friendsLabel.getStyleClass().add("accounts-subtitle");

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        avatarView, usernameLabel, friendsLabel);
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
    if (!authService.session().isLoggedIn()) {
      return;
    }
    PlayerProfile profile = authService.session().currentUser();
    usernameLabel.setText(profile.username());
    friendsLabel.setText(
        PROPS.getString("friendsLabel") + " (" + profile.friendUsernames().size() + ")");
    if (profile.avatarPath() != null && !profile.avatarPath().isEmpty()) {
      try {
        avatarView.setImage(new Image("file:" + profile.avatarPath(), 120, 120, true, true));
      } catch (Exception ignored) {
        avatarView.setImage(null);
      }
    } else {
      avatarView.setImage(null);
    }
  }
}
