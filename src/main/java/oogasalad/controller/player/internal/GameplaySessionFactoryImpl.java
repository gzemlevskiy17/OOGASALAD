package oogasalad.controller.player.internal;

import java.util.List;
import java.util.function.Consumer;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.CollisionDetectorImpl;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.DynamicPassthroughSolver;
import oogasalad.model.engine.GameEngineImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.WinConditionChecker;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderer;
import oogasalad.view.core.api.SpriteRenderable;
import oogasalad.view.core.internal.CameraImpl;
import oogasalad.view.core.internal.FollowPlayerCameraBehavior;
import oogasalad.view.core.internal.RendererImpl;
import oogasalad.view.core.internal.SpriteCache;
import oogasalad.view.player.internal.GamePlayAppScreen;

/**
 * Default factory that assembles the full gameplay runtime graph (world, engine, camera,
 * renderer, input, win/loss checks, and controller) for one playable session.
 *
 * <p>Uses {@link GameEngineImpl} (forces, integration, collisions) with rectangular
 * {@link DynamicComponent} bodies — no circular placeholders.
 *
 * @author Iurii Beliaev
 */
public class GameplaySessionFactoryImpl implements GameplaySessionFactory {

  private static final double WORLD_WIDTH = 2600.0;
  private static final double WORLD_HEIGHT = 1600.0;
  /** Default axis-aligned box size (world units) for demo bodies. */
  private static final double BOX_W = 48.0;
  private static final double BOX_H = 48.0;

  private static ImageView createImageView(SpriteRenderable sr, SpriteCache spriteCache) {
    ImageView imageView = new ImageView(spriteCache.get(sr.getSpritePath()));
    imageView.setFitWidth(sr.getSpriteWidth());
    imageView.setFitHeight(sr.getSpriteHeight());
    imageView.setTranslateX(-sr.getSpriteWidth() / 2);
    imageView.setTranslateY(-sr.getSpriteHeight() / 2);
    return imageView;
  }

  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome) {
    screen.getGameLayer().getChildren().clear();

    World world = buildWorldForCurrentLevel();
    GameEngine engine = buildEngine(world);
    GameConfig config = buildConfig();

    WinConditionChecker winChecker = buildWinChecker();
    LossConditionChecker lossChecker = buildLossChecker();
    InputRouter inputRouter = buildInputRouter();

    double viewWidth = screen.getGameLayer().getPrefWidth();
    double viewHeight = screen.getGameLayer().getPrefHeight();
    if (viewWidth <= 0) {
      viewWidth = 1280;
    }
    if (viewHeight <= 0) {
      viewHeight = 720;
    }

    Rectangle worldBounds = new Rectangle(WORLD_WIDTH, WORLD_HEIGHT);
    worldBounds.setManaged(false);
    worldBounds.setFill(Color.TRANSPARENT);
    worldBounds.setStroke(Color.LIMEGREEN);
    worldBounds.setStrokeWidth(2.0);
    worldBounds.getStrokeDashArray().addAll(10.0, 8.0);

    screen.getGameLayer().getChildren().add(worldBounds);
    worldBounds.toBack();

    Camera camera = new CameraImpl(0, 0, viewWidth, viewHeight);
    GameObject player = resolveControlledPlayer(world);

    CameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> player, viewWidth, viewHeight, 0, 0, 0.2);
    camera.setBehavior(behavior);
    SpriteCache spriteCache = new SpriteCache();
    Renderer objectRenderer =
        new RendererImpl(
            screen.getGameLayer(),
            r -> {
              if (r instanceof SpriteRenderable sr) {
                return createImageView(sr, spriteCache);
              }
              if (r instanceof DynamicComponent dc) {
                double[] wh = axisAlignedSize(dc.getPolygon());
                Rectangle rect = new Rectangle(wh[0], wh[1]);
                Vector v = dc.getDynamicState().getLinearVelocity();
                boolean moving = Math.hypot(v.x(), v.y()) > 0.001;
                if (r == player) {
                  rect.setFill(Color.ORANGE);
                  rect.setStroke(Color.DARKORANGE);
                  rect.setStrokeWidth(1.5);
                } else {
                  rect.setFill(moving ? Color.DODGERBLUE : Color.DARKGRAY);
                }
                return rect;
              }
              Rectangle fallback = new Rectangle(BOX_W, BOX_H);
              fallback.setFill(Color.DODGERBLUE);
              return fallback;
            });

    Renderer renderer =
        (cameraRef, objects) -> {
          objectRenderer.render(cameraRef, objects);
          worldBounds.setLayoutX(-cameraRef.getX());
          worldBounds.setLayoutY(-cameraRef.getY());
        };

    GameController controller =
        new GameControllerImpl(
            engine,
            world,
            renderer,
            camera,
            behavior,
            winChecker,
            lossChecker,
            inputRouter,
            config);
    controller.setOnOutcome(onOutcome);

    return new GameplaySession(controller);
  }

  private World buildWorldForCurrentLevel() {
    World world = new WorldImpl();

    world.addComponent(createBox(350, 300, BOX_W, BOX_H, 180, 120, false, 1.0));

    world.addComponent(createBox(900, 500, BOX_W, BOX_H, -140, 100, false, 1.0));
    world.addComponent(createBox(1500, 900, BOX_W, BOX_H, 160, -130, false, 1.0));
    world.addComponent(createBox(2100, 400, BOX_W, BOX_H, -120, -90, false, 1.0));

    world.addComponent(createBox(200, 1200, BOX_W, BOX_H, 0, 0, false, 1.0));
    world.addComponent(createBox(1200, 200, BOX_W, BOX_H, 0, 0, false, 1.0));
    world.addComponent(createBox(2400, 1300, BOX_W, BOX_H, 0, 0, false, 1.0));
    world.addComponent(createBox(2500, 300, BOX_W, BOX_H, 0, 0, false, 1.0));

    return world;
  }

  /**
   * Axis-aligned box in local space from (0,0) to (w,h); {@code (x,y)} is the top-left in world
   * space (rotation 0).
   */
  private DynamicComponent createBox(
      double x,
      double y,
      double w,
      double h,
      double vx,
      double vy,
      boolean affectedByGravity,
      double mass) {
    ConvexPolygon poly =
        new ConvexPolygon(
            List.of(
                new Vector(0, 0),
                new Vector(w, 0),
                new Vector(w, h),
                new Vector(0, h)));
    DynamicComponent body =
        new DynamicComponent(x, y, 0, poly, "box", 0, mass, 1, 0.5, affectedByGravity) {};
    body.activate();
    body.getDynamicState().setLinearVelocity(new Vector(vx, vy));
    return body;
  }

  private GameEngine buildEngine(World world) {
    GameEngineImpl physics =
        new GameEngineImpl(
            world, new CollisionDetectorImpl(),

            new DynamicPassthroughSolver());
    return elapsed -> {
      physics.step(elapsed);
      clampDynamicsToWorldBounds(world);
    };
  }

  /**
   * Keeps axis-aligned boxes (rotation 0, polygon with min at origin) inside the world rectangle and
   * reflects linear velocity on impact — same role as the old demo bounce bounds.
   */
  private void clampDynamicsToWorldBounds(World world) {
    for (GameObject go : world.getObjects()) {
      if (!(go instanceof DynamicComponent dc)) {
        continue;
      }
      double[] wh = axisAlignedSize(dc.getPolygon());
      double w = wh[0];
      double h = wh[1];

      var st = dc.getStaticState();
      var dyn = dc.getDynamicState();
      double x = st.getLinearPosition().x();
      double y = st.getLinearPosition().y();
      double vx = dyn.getLinearVelocity().x();
      double vy = dyn.getLinearVelocity().y();

      double nx = x;
      double ny = y;
      double nvx = vx;
      double nvy = vy;

      if (nx < 0) {
        nvx = -nvx;
        nx = 0;
      } else if (nx + w > WORLD_WIDTH) {
        nvx = -nvx;
        nx = WORLD_WIDTH - w;
      }

      if (ny < 0) {
        nvy = -nvy;
        ny = 0;
      } else if (ny + h > WORLD_HEIGHT) {
        nvy = -nvy;
        ny = WORLD_HEIGHT - h;
      }

      st.setLinearPosition(new Vector(nx, ny));
      dyn.setLinearVelocity(new Vector(nvx, nvy));
    }
  }

  private static double[] axisAlignedSize(ConvexPolygon poly) {
    List<Vector> pts = poly.getPoints();
    double minX = pts.stream().mapToDouble(Vector::x).min().orElse(0);
    double maxX = pts.stream().mapToDouble(Vector::x).max().orElse(BOX_W);
    double minY = pts.stream().mapToDouble(Vector::y).min().orElse(0);
    double maxY = pts.stream().mapToDouble(Vector::y).max().orElse(BOX_H);
    return new double[] {maxX - minX, maxY - minY};
  }

  private GameConfig buildConfig() {
    return new GameConfig() {
      @Override
      public String getSetting(String key) {
        return "";
      }

      @Override
      public double getNumericSetting(String key) {
        return 0.0;
      }
    };
  }

  private WinConditionChecker buildWinChecker() {
    return world -> false;
  }

  private LossConditionChecker buildLossChecker() {
    return world -> false;
  }

  private InputRouterImpl buildInputRouter() {
    return new InputRouterImpl();
  }

  private GameObject resolveControlledPlayer(World world) {
    return world.getObjects().stream().findFirst().orElse(null);
  }
}