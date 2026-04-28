package oogasalad.controller.player.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.TypedComponent;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.Sprite;
import oogasalad.view.core.internal.PaletteSpriteRegistry;
import oogasalad.view.core.internal.SpriteCache;
import oogasalad.view.core.internal.TerrainPaints;

/**
 * Creates and updates JavaFX nodes for renderables used by gameplay sessions.
 *
 * @author Cody
 */
final class GameplayRenderableFactory {

  private static final double NO_ANIMATION_SPEED = 0.0;
  private static final double TERRAIN_STROKE_WIDTH = 0.5;
  private static final double SPLINE_STROKE_WIDTH = 4.0;

  private final SpriteCache spriteCache = new SpriteCache();
  private final PaletteSpriteRegistry spriteRegistry = new PaletteSpriteRegistry();
  private final Map<Renderable, Sprite> spritesByRenderable = new IdentityHashMap<>();
  private final Map<Renderable, Renderable> wheelConnectorRotationSources;

  GameplayRenderableFactory(GameConfig gameConfig,
      Map<Renderable, Renderable> wheelConnectorRotationSources) {
    this.wheelConnectorRotationSources = wheelConnectorRotationSources;
    if (gameConfig != null) {
      spriteRegistry.registerFromConfig(gameConfig);
    }
  }

  Node createNode(Renderable renderable) {
    if (renderable instanceof TerrainSpline terrainSpline) {
      return createSplineNode(terrainSpline);
    }
    if (renderable instanceof TerrainBlock terrainBlock) {
      return createTerrainBlockNode(terrainBlock);
    }
    if (renderable instanceof SpawnPointComponent) {
      return createInvisibleNode();
    }
    if (renderable instanceof TypedComponent) {
      return createSpriteNode(renderable);
    }
    throw new IllegalStateException(unsupportedRenderableMessage(renderable));
  }

  void updateNode(Renderable renderable, Node node) {
    Sprite sprite = spritesByRenderable.get(renderable);
    if (sprite == null) {
      return;
    }
    sprite.update(node, animationSpeedFor(renderable));
    applyConnectorRotation(renderable, node);
  }

  private Node createSplineNode(TerrainSpline terrainSpline) {
    ParabolaSpline spline = terrainSpline.getSpline();
    double originX = terrainSpline.x();
    double originY = terrainSpline.y();
    Path path = new Path();
    path.setManaged(false);
    addSplineCurve(path, spline, originX, originY);
    TerrainPaints.applyTerrainFill(path, terrainSpline.getSprite(), terrainSpline.getColor(),
        terrainSpline.getWidth(), terrainSpline.getHeight(), terrainSpline.isTiled());
    path.setStroke(TerrainPaints.parseColor(terrainSpline.getTrackColor()));
    path.setStrokeWidth(SPLINE_STROKE_WIDTH);
    return path;
  }

  private void addSplineCurve(Path path, ParabolaSpline spline, double originX, double originY) {
    Vector firstKnot = spline.knots().get(0);
    path.getElements().add(new MoveTo(firstKnot.x() - originX, firstKnot.y() - originY));
    for (int index = 0; index < spline.segmentCount(); index++) {
      addSplineSegment(path, spline, originX, originY, index);
    }
    closeSplinePath(path, spline, originX, originY);
  }

  private void addSplineSegment(Path path, ParabolaSpline spline, double originX, double originY,
      int index) {
    Vector control = spline.controls().get(index);
    Vector next = spline.knots().get(index + 1);
    path.getElements().add(new QuadCurveTo(
        control.x() - originX, control.y() - originY,
        next.x() - originX, next.y() - originY));
  }

  private void closeSplinePath(Path path, ParabolaSpline spline, double originX, double originY) {
    Vector firstKnot = spline.knots().get(0);
    Vector lastKnot = spline.knots().get(spline.knots().size() - 1);
    double localBaselineY = spline.baselineY() - originY;
    path.getElements().add(new LineTo(lastKnot.x() - originX, localBaselineY));
    path.getElements().add(new LineTo(firstKnot.x() - originX, localBaselineY));
    path.getElements().add(new ClosePath());
  }

  private Node createTerrainBlockNode(TerrainBlock terrainBlock) {
    Rectangle rectangle = new Rectangle(terrainBlock.getWidth(), terrainBlock.getHeight());
    rectangle.setManaged(false);
    rectangle.setStrokeWidth(TERRAIN_STROKE_WIDTH);
    rectangle.setStroke(Color.BLACK);
    rectangle.setFill(Paint.valueOf(terrainBlock.getColor()));
    return rectangle;
  }

  private Node createInvisibleNode() {
    Group stub = new Group();
    stub.setManaged(false);
    stub.setVisible(false);
    return stub;
  }

  private Node createSpriteNode(Renderable renderable) {
    Sprite sprite = spriteRegistry.resolve(renderable);
    spritesByRenderable.put(renderable, sprite);
    return sprite.render(spriteCache);
  }

  private double animationSpeedFor(Renderable renderable) {
    if (renderable instanceof DynamicComponent dynamicComponent) {
      return dynamicComponent.getDynamicState().getLinearVelocity().x();
    }
    return NO_ANIMATION_SPEED;
  }

  private void applyConnectorRotation(Renderable renderable, Node node) {
    Renderable connectorRotationSource = wheelConnectorRotationSources.get(renderable);
    if (connectorRotationSource != null) {
      node.setRotate(Math.toDegrees(connectorRotationSource.rotation()));
    }
  }

  private String unsupportedRenderableMessage(Renderable renderable) {
    String type;
    if (renderable == null) {
      type = "null";
    } else {
      type = renderable.getClass().getName();
    }
    return "No renderer for " + type
        + " - must be TerrainSpline, TerrainBlock, SpawnPointComponent, or TypedComponent";
  }
}
