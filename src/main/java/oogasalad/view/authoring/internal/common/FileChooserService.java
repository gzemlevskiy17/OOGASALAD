package oogasalad.view.authoring.internal.common;

import java.io.File;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Centralizes JavaFX FileChooser configuration for authoring-level JSON files.
 *
 * <p>Keeps file I/O dialog concerns out of screen classes so Save and Load
 * dialogs look identical no matter where they are invoked (authoring entry
 * screen, authoring top bar, etc.). All chooser strings are read from
 * FileChooser.properties.
 */
public class FileChooserService {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.FileChooser");

  /**
   * Prompts the user to pick an existing level JSON file.
   *
   * @param owner the window that should own the modal dialog
   * @return the selected file, or empty if the user cancelled
   */
  public Optional<File> chooseLoadFile(Window owner) {
    FileChooser chooser = baseChooser(PROPS.getString("loadDialogTitle"));
    return Optional.ofNullable(chooser.showOpenDialog(owner));
  }

  /**
   * Prompts the user to pick a destination for saving a level JSON file.
   *
   * @param owner the window that should own the modal dialog
   * @return the selected file, or empty if the user cancelled
   */
  public Optional<File> chooseSaveFile(Window owner) {
    return chooseSaveFile(owner, null);
  }

  /**
   * Prompts the user to pick a destination for saving a level JSON file, with a suggested
   * starting directory. If {@code initialDir} is null or does not exist, falls back to the
   * default from properties.
   *
   * @param owner      the window that should own the modal dialog
   * @param initialDir preferred starting directory, or null for the default
   * @return the selected file, or empty if the user cancelled
   */
  public Optional<File> chooseSaveFile(Window owner, File initialDir) {
    FileChooser chooser = baseChooser(PROPS.getString("saveDialogTitle"));
    chooser.setInitialFileName(PROPS.getString("defaultFileName"));
    if (initialDir != null && initialDir.isDirectory()) {
      chooser.setInitialDirectory(initialDir);
    }
    return Optional.ofNullable(chooser.showSaveDialog(owner));
  }

  private FileChooser baseChooser(String title) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(title);
    File initialDir = new File(PROPS.getString("defaultDirectory"));
    if (initialDir.isDirectory()) {
      chooser.setInitialDirectory(initialDir);
    }
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
        PROPS.getString("filterDescription"),
        PROPS.getString("filterPattern")));
    return chooser;
  }
}