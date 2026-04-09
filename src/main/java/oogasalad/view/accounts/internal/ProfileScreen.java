package oogasalad.view.accounts.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.view.accounts.api.AccountOutcome;
import oogasalad.view.core.api.AppScreen;

public class ProfileScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.ProfileScreen");

  private final Parent root;
  private final Supplier<PlayerProfile> profileSupplier;
  private final ImageView avatarView = new ImageView();
  private final Label usernameLabel = new Label();

  public ProfileScreen(Consumer<Outcome> onNav, Consumer<AccountOutcome> onAction,
      Supplier<PlayerProfile> profileSupplier) {
    this.profileSupplier = profileSupplier;
    BorderPane layout = new BorderPane();

    Button logoutButton = new Button(PROPS.getString("logoutButton"));
    logoutButton.setOnAction(e -> onAction.accept(new AccountOutcome.LogoutRequested()));

    Button editButton = new Button(PROPS.getString("editButton"));
    editButton.setOnAction(e -> onNav.accept(Outcome.OPEN_UPDATE_PROFILE));

    layout.setTop(new HBox(10, editButton, logoutButton));
    layout.setCenter(new VBox(10, avatarView, usernameLabel));

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
      avatarView.setImage(new Image("file:" + profile.avatarPath(), 120, 120, true, true));
    }
  }
}