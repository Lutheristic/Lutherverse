package com.qsr.customspd.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Assets;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.Statistics;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.windows.WndStory;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Upstream 92905ae81, the GameScene half: the region intro now shows on the loading screen, so
 * arriving on a region's first floor no longer opens a story window or marks the intro read.
 */
class GameSceneRegionIntroPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private GL20 priorGl;
	private GL20 priorGl20;
	private GameScene scene;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		run.enterFirstLevel();
		priorInstance = Game.instance;
		priorGl = Gdx.gl;
		priorGl20 = Gdx.gl20;
		new Game(null, null);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		Game.width = Game.dispWidth = 1500;
		Game.height = Game.dispHeight = 1000;
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(),
				new Class<?>[]{GL20.class}, (proxy, method, args) -> {
					Class<?> r = method.getReturnType();
					if (r == int.class) return 0;
					if (r == boolean.class) return false;
					if (r == float.class) return 0f;
					if (r == long.class) return 0L;
					if (r == String.class) return "";
					return null;
				});
		TextureCache.create(Assets.Environment.TILES_SEWERS, 256, 256);
		TextureCache.create(Assets.Environment.TERRAIN_FEATURES, 256, 128);
		TextureCache.create(Assets.Environment.VISUAL_GRID, 64, 64);
		TextureCache.create(Assets.Environment.WALL_BLOCKING, 64, 16);
		KeyBindings.setAllBindings(SPDAction.getDefaults());
		SPDSettings.interfaceSize(2);
		SPDSettings.intro(false);
	}

	@AfterEach
	void tearDown() {
		try {
			if (scene != null) scene.destroy();
		} finally {
			Gdx.gl = priorGl;
			Gdx.gl20 = priorGl20;
			Game.instance = priorInstance;
			run.close();
		}
	}

	private static int storyWindows(Group scene) throws Exception {
		Field f = Group.class.getDeclaredField("members");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) f.get(scene);
		int open = 0;
		for (Gizmo g : members) {
			if (g instanceof WndStory) open++;
		}
		return open;
	}

	private void arrive(InterlevelScene.Mode mode) {
		InterlevelScene.mode = mode;
		scene = new GameScene();
		scene.create();
	}

	@Test
	void arrivingOnTheFirstFloorOpensNoStoryWindow() throws Exception {
		assertEquals(1, Dungeon.depth, "test setup: floor 1");
		assertEquals(1, Statistics.deepestFloor, "test setup: a new deepest floor");
		assertFalse(Document.INTROS.isPageRead(1), "test setup: the sewers intro is unread");

		arrive(InterlevelScene.Mode.DESCEND);

		assertEquals(0, storyWindows(scene));
		assertFalse(Document.INTROS.isPageRead(1), "only the loading screen's continue marks it read");
	}

	@Test
	void fallingOntoTheFirstFloorOpensNoStoryWindow() throws Exception {
		arrive(InterlevelScene.Mode.FALL);

		assertEquals(0, storyWindows(scene));
		assertFalse(Document.INTROS.isPageRead(1));
	}
}
