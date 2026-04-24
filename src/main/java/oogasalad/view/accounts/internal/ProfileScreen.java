package oogasalad.view.accounts.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.view.accounts.api.AccountOutcome;
import oogasalad.view.core.api.AppScreen;

public class ProfileScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.ProfileScreen");
  private static final double AVATAR_SIZE = 140;

  private final Parent root;
  private final Supplier<PlayerProfile> profileSupplier;
  private final ImageView avatarView = new ImageView();
  private final Label usernameLabel = new Label();

  public ProfileScreen(Consumer<Outcome> onNav, Consumer<AccountOutcome> onAction,
      Supplier<PlayerProfile> profileSupplier) {
    this.profileSupplier = profileSupplier;

    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());
    layout.getStyleClass().add("screen-root");

    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onNav.accept(Outcome.BACK_TO_MENU));
    BorderPane.setMargin(backButton, new Insets(18, 0, 0, 18));
    layout.setTop(backButton);

    Label title = new Label(PROPS.getString("title"));
    title.getStyleClass().add("accounts-title");

    avatarView.setFitWidth(AVATAR_SIZE);
    avatarView.setFitHeight(AVATAR_SIZE);
    avatarView.setPreserveRatio(true);

    usernameLabel.getStyleClass().add("accounts-subtitle");

    Button editButton = new Button(PROPS.getString("editButton"));
    editButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    editButton.setOnAction(e -> onNav.accept(Outcome.OPEN_UPDATE_PROFILE));

    Button logoutButton = new Button(PROPS.getString("logoutButton"));
    logoutButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    logoutButton.setOnAction(e -> onAction.accept(new AccountOutcome.LogoutRequested()));

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        title, avatarView, usernameLabel, editButton, logoutButton);
    center.setAlignment(Pos.CENTER);
    center.getStyleClass().add("accounts-form");
    layout.setCenter(center);

    this.root = layout;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void onEnter() {
    PlayerProfile profile = profileSupplier.get();
    if (profile == null) {
      return;
    }
    usernameLabel.setText(profile.username());
    if (!profile.avatarPath().isEmpty()) {
      avatarView.setImage(
          new Image("file:" + profile.avatarPath(), AVATAR_SIZE, AVATAR_SIZE, true, true));
    }
  }
}
