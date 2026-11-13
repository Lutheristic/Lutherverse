package com.qsr.customspd.sprites;

import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 6827da804's SpawnerSprite half: the demon spawner's death splash
 * starts across the whole sprite (Splash.around), not from its centre point.
 *
 * Headless there is no scene or GL context, so the test gives GameScene a bare emitter group and
 * the sprite skips its texture-loading constructor. The test checks the burst's own factory (colour,
 * direction, cone), then, since its particles need a texture, swaps in a recording factory that
 * records where each one would start.
 */
class SpawnerDeathSplashPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void theDeathSplashStartsAcrossTheWholeSprite() throws Exception {
		Field sceneField = GameScene.class.getDeclaredField("scene");
		sceneField.setAccessible(true);
		Object priorScene = sceneField.get(null);
		float priorElapsed = Game.elapsed;

		GameScene scene = allocate(GameScene.class);
		OpenGroup emitters = new OpenGroup();
		Field emittersField = GameScene.class.getDeclaredField("emitters");
		emittersField.setAccessible(true);
		emittersField.set(scene, emitters);
		sceneField.set(null, scene);
		try {
			SpawnerSprite sprite = allocate(SpawnerSprite.class);
			sprite.scale = new PointF(1, 1);
			sprite.x = 32;
			sprite.y = 48;
			sprite.width = 16;
			sprite.height = 16;

			sprite.die();

			List<Emitter> bursts = new ArrayList<>();
			for (Gizmo g : emitters.all()) {
				if (g instanceof Emitter) bursts.add((Emitter) g);
			}
			assertEquals(1, bursts.size(), "the spawner's death makes one splash");

			Field factoryField = Emitter.class.getDeclaredField("factory");
			factoryField.setAccessible(true);
			Object splash = factoryField.get(bursts.get(0));
			assertEquals("com.qsr.customspd.effects.Splash$SplashFactory", splash.getClass().getName(),
					"the burst uses the splash's own per-colour factory");
			assertEquals(sprite.blood(), (int) factoryValue(splash, "color"), "the splash is the spawner's blood colour");
			assertEquals(-3.1415926f / 2, (float) factoryValue(splash, "dir"), "the splash sprays upward");
			assertEquals(3.1415926f, (float) factoryValue(splash, "cone"), "the splash spreads over a half circle");

			Recorder recorder = new Recorder();
			factoryField.set(bursts.get(0), recorder);
			Game.elapsed = 1f;
			Random.pushGenerator(6827L);
			try {
				bursts.get(0).update();
			} finally {
				Random.popGenerator();
			}

			assertEquals(100, recorder.xs.size(), "the splash bursts 100 particles");
			float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
			for (int i = 0; i < recorder.xs.size(); i++) {
				float x = recorder.xs.get(i), y = recorder.ys.get(i);
				assertTrue(x >= sprite.x && x <= sprite.x + sprite.width && y >= sprite.y && y <= sprite.y + sprite.height,
						"particle " + i + " starts on the sprite, at " + x + "," + y);
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
			assertTrue(maxX - minX > sprite.width / 2 && maxY - minY > sprite.height / 2,
					"the particles start spread over the sprite, not from one point: x " + minX + ".." + maxX
							+ ", y " + minY + ".." + maxY);
		} finally {
			Game.elapsed = priorElapsed;
			sceneField.set(null, priorScene);
		}
	}

	/** GameScene's emitter group, with its members readable. */
	private static final class OpenGroup extends Group {
		List<Gizmo> all() {
			return members;
		}
	}

	private static final class Recorder extends Emitter.Factory {
		final List<Float> xs = new ArrayList<>();
		final List<Float> ys = new ArrayList<>();

		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			xs.add(x);
			ys.add(y);
		}
	}

	private static Object factoryValue(Object factory, String name) throws Exception {
		Field f = factory.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(factory);
	}

	/** Skips the constructor, which needs a GL context (the sprite loads its texture). */
	private static <T> T allocate(Class<T> type) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return type.cast(unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type));
	}
}
