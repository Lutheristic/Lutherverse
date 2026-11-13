package com.qsr.customspd.scenes;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.utils.DungeonSeed;
import com.qsr.customspd.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 4ed89d588: hero select fixes the run's seed before the loading screen opens, so the
 * loading screen can use it and the run itself keeps it. Each way of starting a run is driven
 * through the real hero select buttons with no display.
 */
class RunStartSeedPortTest {

	private static final long DAY = 24 * 60 * 60 * 1000L;
	private static final long STALE_SEED = 1234L;
	private static final String CUSTOM = "ABC-DEF-GHI";

	private HeadlessRun run;
	private Game priorInstance;
	private HeroSelectScene scene;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		priorInstance = Game.instance;
		// a bare Game takes the scene switch; HeadlessRun's snapshot puts its statics back
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
		// what the last run left behind
		Dungeon.seed = STALE_SEED;
		Dungeon.customSeedText = "STALE";
		SPDSettings.customSeed("");
	}

	@AfterEach
	void tearDown() {
		try {
			if (scene != null) scene.destroy();
		} finally {
			Game.instance = priorInstance;
			run.close();
		}
	}

	private static Object read(Object owner, Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	@SuppressWarnings("unchecked")
	private static void setVictory(boolean won) throws Exception {
		Set<Badges.Badge> global = (Set<Badges.Badge>) read(null, Badges.class, "global");
		if (won) global.add(Badges.Badge.VICTORY);
		else global.remove(Badges.Badge.VICTORY);
	}

	private void createScene() throws Exception {
		scene = new HeroSelectScene();
		scene.create();
		Field f = Game.class.getDeclaredField("scene");
		f.setAccessible(true);
		f.set(Game.instance, scene);
	}

	private static void click(Button button) throws Exception {
		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(button);
	}

	private void pressStart() throws Exception {
		click((Button) read(scene, HeroSelectScene.class, "startBtn"));
		assertSame(InterlevelScene.class, read(null, Game.class, "sceneClass"), "test setup: the run starts");
	}

	/** Presses the daily button and confirms the window it opens. */
	@SuppressWarnings("unchecked")
	private void startDaily() throws Exception {
		Object pane = read(scene, HeroSelectScene.class, "optionsPane");
		List<Button> buttons = (List<Button>) read(pane, pane.getClass(), "buttons");
		click(buttons.get(1));
		WndOptions confirm = null;
		for (Gizmo g : (List<Gizmo>) read(scene, Group.class, "members")) {
			if (g instanceof WndOptions) confirm = (WndOptions) g;
		}
		assertTrue(confirm != null, "test setup: the daily asks for confirmation");
		Method onSelect = WndOptions.class.getDeclaredMethod("onSelect", int.class);
		onSelect.setAccessible(true);
		onSelect.invoke(confirm, 0);
		assertSame(InterlevelScene.class, read(null, Game.class, "sceneClass"), "test setup: the daily starts");
	}

	/** What InterlevelScene.descend() does for a new game once the loading screen is up. */
	private static void beginRun() {
		Mob.clearHeldAllies();
		Dungeon.init();
	}

	@Test
	void aCustomSeedIsFixedWhenTheRunStarts() throws Exception {
		setVictory(true);
		SPDSettings.customSeed(CUSTOM);
		createScene();

		pressStart();

		assertEquals(DungeonSeed.convertFromText(CUSTOM), Dungeon.seed, "the loading screen sees the run's seed");
		assertEquals(CUSTOM, Dungeon.customSeedText);
		beginRun();
		assertEquals(DungeonSeed.convertFromText(CUSTOM), Dungeon.seed, "the run keeps it");
	}

	@Test
	void aRunWithNoCustomSeedRollsItsSeedOnceAtTheStart() throws Exception {
		setVictory(true);
		createScene();

		pressStart();

		long rolled = Dungeon.seed;
		assertNotEquals(STALE_SEED, rolled, "the last run's seed must not reach the loading screen");
		assertTrue(rolled >= 0 && rolled < DungeonSeed.TOTAL_SEEDS);
		assertEquals("", Dungeon.customSeedText);
		beginRun();
		assertEquals(rolled, Dungeon.seed, "the run keeps the seed the loading screen used, not a second roll");
	}

	@Test
	void aDailyFixesItsSeedBeforeAnythingDraws() throws Exception {
		setVictory(true);
		SPDSettings.lastDaily(0);
		Game.realTime = 20_000 * DAY + 5 * 60 * 60 * 1000L;
		createScene();

		startDaily();

		assertEquals(20_000 * DAY, SPDSettings.lastDaily(), "test setup: today's daily");
		assertFalse(Dungeon.dailyReplay);
		assertEquals(20_000 * DAY + DungeonSeed.TOTAL_SEEDS, Dungeon.seed);
		assertEquals("2024-10-04", Dungeon.customSeedText);
		beginRun();
		assertEquals(20_000 * DAY + DungeonSeed.TOTAL_SEEDS, Dungeon.seed);
	}

	@Test
	void aReplayedDailyUsesTheEarlierDailysSeed() throws Exception {
		setVictory(true);
		SPDSettings.lastDaily(20_000 * DAY);
		Game.realTime = 20_000 * DAY + 5 * 60 * 60 * 1000L;
		createScene();

		startDaily();

		assertTrue(Dungeon.dailyReplay, "test setup: today's daily was already played");
		assertEquals(20_000 * DAY + DungeonSeed.TOTAL_SEEDS, Dungeon.seed);
		assertEquals("2024-10-04", Dungeon.customSeedText);
	}

	@Test
	void theIntroRunStartsWithAFreshSeed() throws Exception {
		setVictory(false);
		SPDSettings.intro(true);
		createScene();

		pressStart();

		assertNotEquals(STALE_SEED, Dungeon.seed);
		assertEquals("", Dungeon.customSeedText);
		assertFalse(Dungeon.daily);
	}

	@Test
	void aCustomSeedClearedBeforeAWinDoesNotReachTheNextRun() throws Exception {
		setVictory(false);
		SPDSettings.customSeed(CUSTOM);
		// the seeded run before this one
		Dungeon.seed = DungeonSeed.convertFromText(CUSTOM);
		Dungeon.customSeedText = CUSTOM;
		createScene();
		assertEquals("", SPDSettings.customSeed(), "test setup: hero select clears it for a player with no win");

		pressStart();

		assertEquals("", Dungeon.customSeedText);
		assertNotEquals(DungeonSeed.convertFromText(CUSTOM), Dungeon.seed);
		beginRun();
		assertEquals("", Dungeon.customSeedText);
		assertNotEquals(DungeonSeed.convertFromText(CUSTOM), Dungeon.seed);
	}

	@Test
	void initLeavesTheSeedToInitSeed() {
		SPDSettings.customSeed(CUSTOM);
		Dungeon.daily = false;
		Dungeon.initSeed();
		SPDSettings.customSeed("XYZ-XYZ-XYZ");

		beginRun();

		assertEquals(DungeonSeed.convertFromText(CUSTOM), Dungeon.seed);
		assertEquals(CUSTOM, Dungeon.customSeedText);
	}
}
