package oogasalad.data.api;

/**
 * Provides read-only access to parsed game settings and metadata.
 *
 * @author Paulina Vvedenskaya
 */
public interface Settings {

  /**
   * Retrieves a setting value as a String.
   *
   * @param key the unique identifier for the setting.
   * @return the value associated with the key.
   */
  String getSetting(String key);

  /**
   * Retrieves a numeric setting (convenience method).
   *
   * @param key the unique identifier.
   * @return the double representation of the setting.
   */
  double getNumericSetting(String key);
}