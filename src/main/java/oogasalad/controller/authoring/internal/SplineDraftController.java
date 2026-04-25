package oogasalad.controller.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Owns the interactive drafting flow for spline-based terrain. While the SPLINE tool is active,
 * the controller collects knot positions on each click, paints a live "polygon ghost" of the
 * current tessellation on the canvas overlay, and finally commits a {@link TerrainSpline} to the
 * world when the designer confirms (via right-click, double-click, or an explicit commit call).
 *
 * <p>State is intentionally tiny: a list of knots in grid-cell units, a cursor hint in grid-cell
 * units, and the currently chosen resolution. The controller never touches the world during
 * drafting — only {@link #commit()} mutates the model, which keeps the undo/redo logic in
 * {@link AuthoringControllerImpl} focused on its existing action types.
 *
 * <p>All knot and baseline positions are stored in grid-cell units to match the rest of the
 * authoring pipeline; the {@link oogasalad.data.internal.LevelSaverImpl} scale factor converts to
 * world pixels on save. The baseline Y is anchored at the world bottom (the last grid row) so the
 * filled polygon represents actual ground extending down from the drawn curve, and the spawn line
 * from {@code AuthoringCanvas.properties} becomes the <em>upper</em> bound for knot placement:
 * designers can only drop knots at or below the spawn line, so every committed spline sits
 * underneath the vehicle instead of floating above it.
 *
 * @author Bad Piggies team
 */
public class SplineDraftController {

  private static final ResourceBundle CANVAS_PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringCanvas");

  private static final double KNOT_RADIUS = 5.0;
  private static final Color KNOT_COLOR = Color.CORNFLOWERBLUE;
  private static final Color GHOST_STROKE_COLOR = Color.CORNFLOWERBLUE;
  private static final Color GHOST_FILL_COLOR = Color.rgb(70, 130, 180, 0.35);
  private static final Color CURSOR_HINT_COLOR = Color.rgb(100, 100, 100, 0.55);
  private static final int MIN_KNOTS_TO_COMMIT = 2;
  private static final int MIN_RESOLUTION = 1;
  private static final int MAX_RESOLUTION = 64;
  private static final int DEFAULT_RESOLUTION = 10;
  private static final String DEFAULT_COLOR = "#4A2C0F";
  private static final String DEFAULT_SPRITE = "";
  private static final double DEFAULT_FRICTION = 0.6;

  private final World world;
  private final AuthoringCanvas canvas;
  private final double baselineYCells;
  private final double spawnYCells;

  private final List<Vector> draftKnots = new ArrayList<>();
  private int resolution = DEFAULT_RESOLUTION;
  private Vector cursorHint;

  /**
   * @param world  the world that committed splines are added to
   * @param canvas the canvas used to paint draft overlays
   */
  public SplineDraftController(World world, AuthoringCanvas canvas) {
    this.world = world;
    this.canvas = canvas;
    double spawnYWorld = Double.parseDouble(CANVAS_PROPS.getString("spawnY"));
    this.spawnYCells = spawnYWorld / canvas.getCellSize();
    // Baseline at the bottom of the grid: the filled spline polygon represents ground extending
    // from the authored curve all the way down to the world floor, and the ParabolaSpline
    // invariant "knot.y < baselineY" is trivially satisfied for any knot on-canvas.
    this.baselineYCells = Double.parseDouble(CANVAS_PROPS.getString("rows"));
  }

  /** @return the number of draft knots currently placed. */
  public int draftSize() {
    return draftKnots.size();
  }

  /** @return current tessellation resolution applied to future tessellations. */
  public int getResolution() {
    return resolution;
  }

  /**
   * Updates the resolution (clamped to a reasonable range) and re-renders the draft ghost so the
   * designer sees the effect immediately.
   */
  public void setResolution(int newResolution) {
    this.resolution = Math.max(MIN_RESOLUTION, Math.min(MAX_RESOLUTION, newResolution));
    renderDraftOverlay();
  }

  /** Adds a knot at the given grid-cell coordinate. */
  public void addKnot(double col, double row) {
    if (row < spawnYCells) {
      // Refuse knots above the spawn guide: terrain is modelled as ground under the vehicle, so
      // knots that sit higher on-screen than the spawn line would produce floating terrain above
      // the car. Designers who need elevated platforms can still use block terrain for those.
      return;
    }
    if (row >= baselineYCells) {
      // Safety net: knots must also be strictly above the baseline or ParabolaSpline would reject
      // them. In practice this only matters if a click lands at or below the very last grid row.
      return;
    }
    draftKnots.add(new Vector(col, row));
    renderDraftOverlay();
  }

  /** Updates the cursor hint position used to draw a ghost edge from the last knot. */
  public void moveCursor(double col, double row) {
    this.cursorHint = new Vector(col, row);
    renderDraftOverlay();
  }

  /**
   * Commits the current draft as a {@link TerrainSpline} in the world, or does nothing when the
   * draft has fewer than {@value #MIN_KNOTS_TO_COMMIT} knots. The draft is cleared regardless.
   *
   * @return the committed component, or {@code null} when the draft was too small to commit
   */
  public TerrainSpline commit() {
    TerrainSpline committed = null;
    if (draftKnots.size() >= MIN_KNOTS_TO_COMMIT) {
      ParabolaSpline spline = new ParabolaSpline(
          new ArrayList<>(draftKnots), baselineYCells, resolution);
      committed = new TerrainSpline(
          spline, DEFAULT_SPRITE, false, DEFAULT_FRICTION, DEFAULT_COLOR);
      world.addObject(committed);
      committed.spawnSlabColliders(world);
    }
    cancel();
    return committed;
  }

  /** Discards the draft without committing. */
  public void cancel() {
    draftKnots.clear();
    cursorHint = null;
    canvas.clearOverlays();
    canvas.refreshGrid();
  }

  /**
   * Draws the current draft (knots, polygon ghost, cursor hint) onto the canvas overlay. Called
   * after every mutation so the designer always sees an up-to-date preview.
   */
  public void renderDraftOverlay() {
    canvas.clearOverlays();
    double cellSize = canvas.getCellSize();

    if (draftKnots.isEmpty()) {
      return;
    }

    if (draftKnots.size() >= MIN_KNOTS_TO_COMMIT) {
      canvas.addOverlay(buildGhostPath(cellSize));
    }

    for (Vector knot : draftKnots) {
      canvas.addOverlay(buildKnotMarker(knot, cellSize));
    }

    if (cursorHint != null) {
      canvas.addOverlay(buildCursorHintLine(cellSize));
    }
  }

  private Path buildGhostPath(double cellSize) {
    Path path = new Path();
    Vector first = draftKnots.get(0);
    path.getElements().add(new MoveTo(first.x() * cellSize, first.y() * cellSize));
    for (int i = 0; i < draftKnots.size() - 1; i++) {
      Vector a = draftKnots.get(i);
      Vector b = draftKnots.get(i + 1);
      Vector midpointControl = new Vector(
          0.5 * (a.x() + b.x()), 0.5 * (a.y() + b.y()));
      path.getElements().add(new QuadCurveTo(
          midpointControl.x() * cellSize, midpointControl.y() * cellSize,
          b.x() * cellSize, b.y() * cellSize));
    }
    Vector last = draftKnots.get(draftKnots.size() - 1);
    double baselinePx = baselineYCells * cellSize;
    path.getElements().add(new LineTo(last.x() * cellSize, baselinePx));
    path.getElements().add(new LineTo(first.x() * cellSize, baselinePx));
    path.getElements().add(new ClosePath());

    path.setFill(GHOST_FILL_COLOR);
    path.setStroke(GHOST_STROKE_COLOR);
    path.setStrokeWidth(1.5);
    path.setMouseTransparent(true);
    return path;
  }

  private Circle buildKnotMarker(Vector knot, double cellSize) {
    Circle marker = new Circle(knot.x() * cellSize, knot.y() * cellSize, KNOT_RADIUS, KNOT_COLOR);
    marker.setStroke(Color.WHITE);
    marker.setStrokeWidth(1.5);
    marker.setMouseTransparent(true);
    return marker;
  }

  private Line buildCursorHintLine(double cellSize) {
    Vector last = draftKnots.get(draftKnots.size() - 1);
    Line line = new Line(
        last.x() * cellSize, last.y() * cellSize,
        cursorHint.x() * cellSize, cursorHint.y() * cellSize);
    line.setStroke(CURSOR_HINT_COLOR);
    line.getStrokeDashArray().addAll(6.0, 6.0);
    line.setMouseTransparent(true);
    return line;
  }
}
