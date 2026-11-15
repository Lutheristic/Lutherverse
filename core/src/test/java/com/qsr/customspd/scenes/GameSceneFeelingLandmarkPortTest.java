package com.qsr.customspd.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Assets;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.utils.GLog;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.GameLog;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream da42665e8, the GameScene half: arriving on a floor with a feeling logs the feeling's
 * own description and records the matching floor landmark.
 */
class GameSceneFeelingLandmarkPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private GL20 priorGl;
	private GL20 priorGl20;
	private GameScene scene;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261118L);
		run.enterFirstLevel();
		Notes.reset();
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

	//GameLog takes over GLog.update when the scene builds it and queues each line here
	@SuppressWarnings("unchecked")
	private static List<String> gameLogQueue() throws Exception {
		Field f = GameLog.class.getDeclaredField("textsToAdd");
		f.setAccessible(true);
		return (List<String>) f.get(null);
	}

	private void arrive(Level.Feeling feeling) throws Exception {
		Dungeon.level.feeling = feeling;
		gameLogQueue().clear();
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		scene = new GameScene();
		scene.create();
	}

	private static int floorRecords() {
		int n = 0;
		for (Notes.Landmark l : Notes.Landmark.values()) {
			if (l.name().endsWith("_FLOOR") && Notes.contains(l)) n++;
		}
		return n;
	}

	@Test
	void aChasmFloorLogsItsFeelingAndAddsTheFloorLandmark() throws Exception {
		assertFalse(Notes.contains(Notes.Landmark.CHASM_FLOOR), "test setup: no record yet");

		arrive(Level.Feeling.CHASM);

		assertTrue(Notes.contains(Notes.Landmark.CHASM_FLOOR));
		assertEquals(1, floorRecords());
		List<String> log = gameLogQueue();
		assertTrue(log.contains(GLog.WARNING + Level.Feeling.CHASM.desc()), "log was " + log);
	}

	@ParameterizedTest
	@EnumSource(value = Level.Feeling.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
	void eachFeelingAddsItsOwnFloorLandmark(Level.Feeling feeling) throws Exception {
		arrive(feeling);

		assertTrue(Notes.contains(Notes.Landmark.valueOf(feeling.name() + "_FLOOR")), feeling + " record missing");
		assertEquals(1, floorRecords(), feeling + " added more than its own record");
		assertTrue(gameLogQueue().contains(GLog.WARNING + feeling.desc()), feeling + " log was " + gameLogQueue());
	}

	@Test
	void aFloorWithNoFeelingAddsNoFloorLandmark() throws Exception {
		arrive(Level.Feeling.NONE);

		assertEquals(0, floorRecords());
		for (Level.Feeling f : Level.Feeling.values()) {
			if (f == Level.Feeling.NONE) continue;
			assertFalse(gameLogQueue().contains(GLog.WARNING + f.desc()), f + " logged on a plain floor");
		}
	}
}
