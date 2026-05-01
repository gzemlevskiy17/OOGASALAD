package oogasalad.view.core.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.view.core.api.AbstractAppScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.help.internal.HelpChatModel;
import oogasalad.view.accounts.api.AccountOutcome;
import oogasalad.view.help.internal.HelpChatWindow;

/**
 * Basic start menu screen for the application.
 *
 * @author Iurii Beliaev
 * @author Emilio Garcia
 */
public class GameMenuScreen extends AbstractAppScreen {

  private static final Logger LOG = LogManager.getLogger(GameMenuScreen.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.core.GameMenu");
  private static final ThreadLocal<BooleanSupplier> PENDING_LOGIN_STATE = new ThreadLocal<>();
  private static final ThreadLocal<Consumer<AccountOutcome>> PENDING_ACCOUNT_ACTION =
      new ThreadLocal<>();

  private HelpChatWindow helpChatWindow;
  private BorderPane menuLayout;
  private Map<Labeled, String> textKeys;
  private final ThemeService themeService;

  public GameMenuScreen(Consumer<Outcome> onOutcome, Consumer<AccountOutcome> onAccountAction,
      BooleanSupplier isLoggedIn, ThemeService themeService) {
    super(prepare(onOutcome, onAccountAction, isLoggedIn));
    this.themeService = themeService;
    PENDING_LOGIN_STATE.remove();
    PENDING_ACCOUNT_ACTION.remove();
  }

  private static Consumer<Outcome> prepare(Consumer<Outcome> onOutcome,
      Consumer<AccountOutcome> onAccountAction, BooleanSupplier isLoggedIn) {
    PENDING_LOGIN_STATE.set(isLoggedIn);
    PENDING_ACCOUNT_ACTION.set(onAccountAction);
    return onOutcome;
  }

  @Override
  protected Parent createRoot() {
    BooleanSupplier isLoggedIn = PENDING_LOGIN_STATE.get();
    Consumer<AccountOutcome> onAccountAction = PENDING_ACCOUNT_ACTION.get();
    Consumer<Outcome> onOutcome = this.onOutcome;
    textKeys = new LinkedHashMap<>();
    menuLayout = new BorderPane();
    menuLayout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/core/game-menu.css").toExternalForm());
    menuLayout.getStyleClass().add("screen-root");
    menuLayout.setPadding(new Insets(Double.parseDouble(PROPS.getString("rootPadding"))));

    boolean helpAvailable = false;
    try {
      helpChatWindow = new HelpChatWindow(new HelpChatModel());
      helpAvailable = true;
    } catch (Exception e) {
      LOG.warn("Help chat unavailable (missing API key?): {}", e.getMessage());
    }

    Button settingsButton = new Button(PROPS.getString("settingsButtonText"));
    Button helpButton = new Button(PROPS.getString("helpButtonText"));
    Button profileButton = new Button(PROPS.getString("profileButtonText"));
    textKeys.put(settingsButton, "settingsButtonText");
    textKeys.put(helpButton, "helpButtonText");
    textKeys.put(profileButton, "profileButtonText");
    settingsButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    helpButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    profileButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    settingsButton.setOnAction(event -> openSettingsDialog());
    if (helpAvailable) {
      helpButton.setOnAction(event -> helpChatWindow.toggle());
    } else {
      helpButton.setDisable(true);
    }
    profileButton.setOnAction(event -> {
      if (isLoggedIn.getAsBoolean()) {
        onOutcome.accept(Outcome.OPEN_PROFILE);
      } else {
        onOutcome.accept(Outcome.OPEN_LOGIN);
      }
    });
    menuLayout.setTop(wrapTopLeft(settingsButton, helpButton, profileButton));

    Label title = new Label(PROPS.getString("menuTitle"));
    textKeys.put(title, "menuTitle");
    title.setFont(Font.font(Double.parseDouble(PROPS.getString("titleFontSize"))));
    title.getStyleClass().add("menu-title");

    double buttonWidth = Double.parseDouble(PROPS.getString("buttonWidth"));

    Button playButton = new Button(PROPS.getString("playButtonText"));
    textKeys.put(playButton, "playButtonText");
    playButton.setPrefWidth(buttonWidth);
    playButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    playButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_PLAYER_ENTRY));

    Button createButton = new Button(PROPS.getString("createButtonText"));
    textKeys.put(createButton, "createButtonText");
    createButton.setPrefWidth(buttonWidth);
    createButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    createButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_AUTHORING_ENTRY));

    VBox centerContent = new VBox(
        Double.parseDouble(PROPS.getString("buttonSpacing")), title, playButton, createButton);
    centerContent.setAlignment(Pos.CENTER);
    centerContent.getStyleClass().add("menu-center-region");
    menuLayout.setCenter(centerContent);

    Button logoutButton = new Button(PROPS.getString("logoutButtonText"));
    textKeys.put(logoutButton, "logoutButtonText");
    logoutButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    logoutButton.setOnAction(event -> onAccountAction.accept(new AccountOutcome.LogoutRequested()));
    menuLayout.setBottom(wrapBottomLeft(logoutButton));

    if (helpChatWindow != null) {
      return new StackPane(menuLayout, helpChatWindow.getRoot());
    }
    return new StackPane(menuLayout);
  }

  private VBox wrapTopLeft(Button... buttons) {
    VBox container = new VBox(
        Double.parseDouble(PROPS.getString("cornerButtonSpacing")), buttons);
    container.setAlignment(Pos.TOP_LEFT);
    container.getStyleClass().add("menu-corner-region");
    return container;
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    container.getStyleClass().add("menu-login-region");
    return container;
  }

  private void openSettingsDialog() {
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle(PROPS.getString("settingsDialogTitle"));
    dialog.setHeaderText(PROPS.getString("settingsDialogHeader"));

    Label themeLabel = new Label(PROPS.getString("themeLabel"));
    ComboBox<String> themeCombo = new ComboBox<>();
    themeCombo.getItems().addAll(
        PROPS.getString("themeLightOption"),
        PROPS.getString("themeDarkOption"),
        PROPS.getString("themeDukeOption"));
    themeCombo.setValue(PROPS.getString("themeLightOption"));
    themeCombo.valueProperty().addListener((obs, old, val) -> applyTheme(val));

    Label languageLabel = new Label(PROPS.getString("languageLabel"));
    ComboBox<String> languageCombo = new ComboBox<>();
    languageCombo.getItems().addAll(
        PROPS.getString("languageEnglishOption"),
        PROPS.getString("languageSpanishOption"));
    languageCombo.setValue(PROPS.getString("languageEnglishOption"));
    languageCombo.valueProperty().addListener((obs, old, val) -> applyLanguage(val));

    VBox content = new VBox(12, themeLabel, themeCombo, languageLabel, languageCombo);
    content.setPadding(new Insets(16));
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    dialog.showAndWait();
  }

  private void applyTheme(String optionLabel) {
    if (optionLabel.equals(PROPS.getString("themeDarkOption"))) {
      themeService.apply("/oogasalad/view/core/themes/theme-dark.css");
    } else if (optionLabel.equals(PROPS.getString("themeDukeOption"))) {
      themeService.apply("/oogasalad/view/core/themes/theme-duke.css");
    } else {
      themeService.clear();
    }
  }

  private void applyLanguage(String optionLabel) {
    Locale locale = optionLabel.equals(PROPS.getString("languageSpanishOption"))
        ? Locale.forLanguageTag("es")
        : Locale.ENGLISH;
    ResourceBundle bundle = ResourceBundle.getBundle("oogasalad.view.core.GameMenu", locale);
    textKeys.forEach((node, key) -> node.setText(bundle.getString(key)));
  }
}
