package oogasalad.view.authoring.internal;

import java.util.Arrays;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import oogasalad.controller.player.internal.cheats.CheatEffectRegistry;
import oogasalad.controller.player.internal.cheats.CheatTargetResolver;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.model.engine.api.DefaultTag;

/**
 * Displays author-facing cheat-key examples and runtime registry options.
 *
 * @author Emilio Garcia
 */
public class CheatAuthoringHelpDialog {

  private static final String EXAMPLE = """
      Example: Spawn a heart powerup on the main character

      ID: spawn_heart
      Name: Spawn Heart
      Keys: SHIFT,P
      Effect Type: spawnPowerup
      Target Kind: mainCharacter
      Target ID: leave blank
      Parameters: componentId=heart

      Click Add Effect first, then Add Cheat.
      """;

  private final CheatEffectRegistry effectRegistry;
  private final CheatTargetResolver targetResolver;
  private final String mainCharacterId;
  private final List<String> componentIds;
  private final List<String> staticComponentIds;
  private final List<String> terrainTypeIds;
  private final List<String> runtimeTagIds;

  public CheatAuthoringHelpDialog() {
    this(CheatEffectRegistry.defaults(), CheatTargetResolver.defaults(), "", List.of(), List.of(),
        List.of(), defaultTagIds());
  }

  public CheatAuthoringHelpDialog(String mainCharacterId, List<ComponentInfo> components,
      List<StaticComponentInfo> staticComponents, List<TerrainInfo> terrainTypes) {
    this(CheatEffectRegistry.defaults(), CheatTargetResolver.defaults(), mainCharacterId,
        components.stream().map(ComponentInfo::id).toList(),
        staticComponents.stream().map(StaticComponentInfo::id).toList(),
        terrainTypes.stream().map(TerrainInfo::id).toList(),
        defaultTagIds());
  }

  CheatAuthoringHelpDialog(CheatEffectRegistry effectRegistry, CheatTargetResolver targetResolver) {
    this(effectRegistry, targetResolver, "", List.of(), List.of(), List.of(), defaultTagIds());
  }

  CheatAuthoringHelpDialog(CheatEffectRegistry effectRegistry, CheatTargetResolver targetResolver,
      String mainCharacterId, List<String> componentIds, List<String> staticComponentIds,
      List<String> terrainTypeIds, List<String> runtimeTagIds) {
    this.effectRegistry = effectRegistry;
    this.targetResolver = targetResolver;
    this.mainCharacterId = cleanMainCharacterId(mainCharacterId);
    this.componentIds = List.copyOf(componentIds);
    this.staticComponentIds = List.copyOf(staticComponentIds);
    this.terrainTypeIds = List.copyOf(terrainTypeIds);
    this.runtimeTagIds = List.copyOf(runtimeTagIds);
  }

  /**
   * Opens the cheat help dialog.
   */
  public void show() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Cheat Key Help");
    alert.setHeaderText("Author-defined cheats are stored in game.json.");
    alert.getDialogPane().setContent(helpTabs());
    alert.showAndWait();
  }

  String helpText() {
    return exampleText() + System.lineSeparator() + availableText();
  }

  String exampleText() {
    return EXAMPLE;
  }

  String availableText() {
    return "Available Effect Types"
        + System.lineSeparator()
        + options(effectRegistry.registeredTypes())
        + System.lineSeparator()
        + "Effect Parameters"
        + System.lineSeparator()
        + effectParameterHelp()
        + System.lineSeparator()
        + "Available Target Kinds"
        + System.lineSeparator()
        + options(targetResolver.registeredKinds())
        + System.lineSeparator()
        + "Target IDs"
        + System.lineSeparator()
        + targetIdHelp()
        + System.lineSeparator()
        + "Current Game Catalog IDs"
        + System.lineSeparator()
        + catalogIdHelp();
  }

  private TabPane helpTabs() {
    TabPane tabs = new TabPane();
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.getTabs().addAll(
        new Tab("Example", textArea(exampleText())),
        new Tab("Available", textArea(availableText())));
    tabs.setPrefSize(680, 520);
    return tabs;
  }

  private TextArea textArea(String text) {
    TextArea content = new TextArea(text);
    content.setEditable(false);
    content.setWrapText(true);
    return content;
  }

  private String options(List<String> values) {
    return values.stream()
        .map(value -> "- " + value)
        .reduce((left, right) -> left + System.lineSeparator() + right)
        .orElse("- none registered");
  }

  private String effectParameterHelp() {
    return """
        - repair: Target Kind mainCharacter, allDamageable, componentType, or tag; Target ID only
          for componentType/tag or a mainCharacter override; Parameters amount=<number>
        - damage: Target Kind mainCharacter, allDamageable, componentType, or tag; Target ID only
          for componentType/tag or a mainCharacter override; Parameters amount=<number>
        - teleport: Target Kind mainCharacter, vehicle, componentType, or tag; Target ID only for
          componentType/tag or a mainCharacter override; Parameters x=<number>,y=<number>
        - finishRun: Target Kind world; Target ID blank; Parameters none
        - loseRun: Target Kind world; Target ID blank; Parameters none
        - addScore: Target Kind world; Target ID blank; Parameters amount=<integer>
        - destroy: Target Kind mainCharacter, vehicle, componentType, or tag; Target ID only for
          componentType/tag or a mainCharacter override; Parameters none
        - launch: Target Kind mainCharacter, vehicle, componentType, or tag; Target ID only for
          componentType/tag or a mainCharacter override; Parameters xVelocity=<number>,yVelocity=<number>
        - spawnPowerup: Target Kind world when x/y are provided, or mainCharacter, vehicle,
          componentType, or tag to spawn at a resolved object; Target ID only for componentType/tag
          or a mainCharacter override; Parameters componentId=<static component id>; optional
          x=<number>,y=<number>
        """;
  }

  private String targetIdHelp() {
    return """
        - world: leave Target ID blank
        - vehicle: leave Target ID blank
        - mainCharacter: leave blank unless overriding the main character id
        - allDamageable: leave Target ID blank
        - componentType: use a movable component id
        - tag: use a runtime tag id
        """;
  }

  private String catalogIdHelp() {
    return "- mainCharacterId: " + mainCharacterId
        + System.lineSeparator()
        + "- movable component ids: " + joinedIds(componentIds)
        + System.lineSeparator()
        + "- static component ids for spawnPowerup componentId: " + joinedIds(staticComponentIds)
        + System.lineSeparator()
        + "- terrain type ids: " + joinedIds(terrainTypeIds)
        + System.lineSeparator()
        + "- runtime tag ids for tag targets: " + joinedIds(runtimeTagIds);
  }

  private String joinedIds(List<String> ids) {
    return ids.stream()
        .filter(id -> !id.isBlank())
        .reduce((left, right) -> left + ", " + right)
        .orElse("none available");
  }

  private String cleanMainCharacterId(String value) {
    return java.util.Optional.ofNullable(value)
        .map(String::strip)
        .filter(id -> !id.isBlank())
        .orElse("not set");
  }

  private static List<String> defaultTagIds() {
    return Arrays.stream(DefaultTag.values())
        .map(DefaultTag::value)
        .toList();
  }
}
