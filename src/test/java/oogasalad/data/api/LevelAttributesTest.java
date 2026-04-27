package oogasalad.data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LevelAttributesTest {

  private LevelAttributes attrs;

  @BeforeEach
  void setUp() {
    attrs = new LevelAttributes();
  }

  @Test
  void defaults_areReasonable() {
    assertEquals("Untitled Level", attrs.getLevelName());
    assertEquals(-9.8, attrs.getGravity(), 0.001);
    assertEquals(0.01, attrs.getDrag(), 0.0001);
    assertEquals(3200, attrs.getScreenWidth(), 0.001);
    assertEquals(1600, attrs.getScreenHeight(), 0.001);
    assertEquals(60, attrs.getGridSize(), 0.001);
    assertEquals(5, attrs.getMaxSelectable());
    assertEquals(100, attrs.getMaxHP(), 0.001);
    assertTrue(attrs.getElementsAllowed().isEmpty());
  }

  @Test
  void setLevelName_updatesValue() {
    attrs.setLevelName("Rolling Hills");
    assertEquals("Rolling Hills", attrs.getLevelName());
  }

  @Test
  void setGameType_updatesValue() {
    attrs.setGameType("badpiggies");
    assertEquals("badpiggies", attrs.getGameType());
  }

  @Test
  void setGravity_updatesValue() {
    attrs.setGravity(-20.0);
    assertEquals(-20.0, attrs.getGravity(), 0.001);
  }

  @Test
  void setDrag_updatesValue() {
    attrs.setDrag(0.05);
    assertEquals(0.05, attrs.getDrag(), 0.0001);
  }

  @Test
  void setElementsAllowed_storesDefensiveCopy() {
    List<String> ids = new java.util.ArrayList<>(List.of("body", "wheel"));
    attrs.setElementsAllowed(ids);
    ids.add("motor");
    assertEquals(2, attrs.getElementsAllowed().size());
  }

  @Test
  void setElementsAllowed_null_treatedAsEmpty() {
    attrs.setElementsAllowed(null);
    assertTrue(attrs.getElementsAllowed().isEmpty());
  }

  @Test
  void setMaxSelectable_updatesValue() {
    attrs.setMaxSelectable(3);
    assertEquals(3, attrs.getMaxSelectable());
  }

  @Test
  void setMaxHP_updatesValue() {
    attrs.setMaxHP(200.0);
    assertEquals(200.0, attrs.getMaxHP(), 0.001);
  }

  // ── Deprecated alias compatibility ───────────────────────────────────────

  @Test
  void deprecatedAlias_getAllowedVehicleComponents_delegatesToElementsAllowed() {
    attrs.setElementsAllowed(List.of("body", "wheel"));
    assertEquals(List.of("body", "wheel"), attrs.getAllowedVehicleComponents());
  }

  @Test
  void deprecatedAlias_setAllowedVehicleComponents_delegatesToElementsAllowed() {
    attrs.setAllowedVehicleComponents(List.of("fan"));
    assertEquals(List.of("fan"), attrs.getElementsAllowed());
  }

  @Test
  void deprecatedAlias_getMaxSelectableVehicleItems_delegatesToMaxSelectable() {
    attrs.setMaxSelectable(7);
    assertEquals(7, attrs.getMaxSelectableVehicleItems());
  }

  @Test
  void deprecatedAlias_setMaxSelectableVehicleItems_delegatesToMaxSelectable() {
    attrs.setMaxSelectableVehicleItems(9);
    assertEquals(9, attrs.getMaxSelectable());
  }
}
