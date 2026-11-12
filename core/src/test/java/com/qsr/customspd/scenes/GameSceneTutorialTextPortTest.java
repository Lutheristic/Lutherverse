package com.qsr.customspd.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Assets;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.GameLog;
import com.qsr.customspd.utils.GLog;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream a7ded9be4, the GameScene half: entering a floor in the intro with the guide's intro
 * page already found only flashes the journal, and the line that ends the intro names the bound
 * hero info and inventory keys on the desktop interface.
 *
 * The scene is built for real on floor 1 with no display. GL calls go to a stub that does
 * nothing, and the tile sheets the game compiles at launch are blank textures.
 */
class GameSceneTutorialTextPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private GL20 priorGl;
	private GL20 priorGl20;
	private GameScene scene;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

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
		// big enough that interfaceSize() keeps the full interface
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
		InterlevelScene.mode = InterlevelScene.Mode.NONE;
		KeyBindings.setAllBindings(SPDAction.getDefaults());
		SPDSettings.interfaceSize(2);
		SPDSettings.intro(true);
	}

	@AfterEach
	void tearDown() {
		try {
			GLog.update.remove(logListener);
			if (scene != null) scene.destroy();
		} finally {
			Gdx.gl = priorGl;
			Gdx.gl20 = priorGl20;
			Game.instance = priorInstance;
			run.close();
		}
	}

	private void enterFloor() {
		assertEquals(2, SPDSettings.interfaceSize(), "test setup: the full interface");
		scene = new GameScene();
		scene.create();
	}

	/** The lines logged since the scene's game log was built, which it shows on its next update. */
	@SuppressWarnings("unchecked")
	private static List<String> pendingLogLines() throws Exception {
		Field f = GameLog.class.getDeclaredField("textsToAdd");
		f.setAccessible(true);
		return new ArrayList<>((List<String>) f.get(null));
	}

	@Test
	void theIntroWithTheGuidePageFoundOnlyFlashesTheJournal() throws Exception {
		assertTrue(Document.ADVENTURERS_GUIDE.isPageFound(Document.GUIDE_INTRO), "test setup: the intro page is found");

		enterFloor();

		for (String line : pendingLogLines()) {
			assertFalse(line.startsWith(GLog.POSITIVE), "the intro must not log a tutorial line here, logged: " + line);
		}
	}

	@Test
	void theIntroWithoutTheGuidePageStillLogsTheMoveLine() throws Exception {
		Document.ADVENTURERS_GUIDE.deletePage(Document.GUIDE_INTRO);

		enterFloor();

		assertEquals(1, pendingLogLines().size(), "control: the move line, log was " + pendingLogLines());
		assertEquals(GLog.POSITIVE + Messages.get(GameScene.class, "tutorial_move_desktop"), pendingLogLines().get(0));
	}

	@Test
	void endingTheIntroOnTheDesktopNamesTheHeroInfoAndInventoryKeys() {
		assertEquals("H", KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(SPDAction.HERO_INFO, false)),
				"test setup: the default hero info key");
		assertEquals("F", KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(SPDAction.INVENTORY, false)),
				"test setup: the default inventory key");
		enterFloor();
		GLog.update.add(logListener);

		GameScene.endIntro();

		assertEquals(1, log.size(), "test setup: one line ends the intro, log was " + log);
		String line = log.get(0);
		assertTrue(line.contains("(H)") && line.contains("(F)"), "the desktop line must name both keys, was: " + line);
	}
}
