package com.qsr.customspd;

import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroAction;
import com.qsr.customspd.items.Amulet;
import com.qsr.customspd.levels.features.LevelTransition;
import com.qsr.customspd.scenes.SurfaceScene;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream d029c0d7d (pulled forward in e8d9c32d6): reaching the surface with the Amulet records the
 * win once, at the transition. Hero.actTransition (upstream's SewerLevel.activateTransition) switches
 * to SurfaceScene with a SceneChangeCallback whose afterCreate runs validateHappyEnd, Dungeon.win,
 * deleteGame and saveGlobal, in that order. Game.switchScene() runs that callback once and clears it,
 * and a window resize re-creates the scene through Game.resetScene(), which passes no callback, so
 * the scene can be rebuilt without recording a second win. Before the pull-forward the four calls
 * sat in SurfaceScene.create and every reset ranked the run again.
 *
 * The scene itself needs a GL context, so this drives the transition on a bare Game and runs the
 * callback by hand, as Game.switchScene() would after create().
 */
class HappyEndTransitionTest {

	private HeadlessRun run;
	private Game priorInstance;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261024L);
		run.enterFirstLevel(); // saves the run, so there is a game for the win to delete
		FileUtils.deleteFile(Rankings.RANKINGS_FILE);
		priorInstance = Game.instance;
		// Game.switchScene writes Game.instance's fields; a bare Game has no scene. Its constructor
		// also writes Game.platform and sceneClass, which HeadlessRun's static snapshot puts back.
		new Game(null, null);
	}

	@AfterEach
	void tearDown() {
		try {
			FileUtils.deleteFile(Rankings.RANKINGS_FILE);
			Game.instance = priorInstance;
		} finally {
			run.close();
		}
	}

	private static Game.SceneChangeCallback pendingCallback() throws Exception {
		Field f = Game.class.getDeclaredField("onChange");
		f.setAccessible(true);
		return (Game.SceneChangeCallback) f.get(Game.instance);
	}

	private static Object requestedSceneClass() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		return f.get(null);
	}

	/** The win records on disk, as the Rankings screen would load them. */
	private static List<Rankings.Record> winRecords() {
		Rankings.INSTANCE.load();
		List<Rankings.Record> wins = new ArrayList<>();
		for (Rankings.Record r : Rankings.INSTANCE.records) if (r.win) wins.add(r);
		return wins;
	}

	/** Puts the hero, holding the Amulet, through the surface transition he is standing on. */
	private static void ascendToTheSurface() throws Exception {
		Hero hero = Dungeon.hero;
		LevelTransition transition = Dungeon.level.getTransition(hero.pos);
		assertNotNull(transition, "test setup: the hero starts on the floor's entrance");
		assertEquals(LevelTransition.Type.SURFACE, transition.type, "test setup: floor 1's entrance leads to the surface");
		hero.belongings.backpack.items.add(new Amulet());

		Method act = Hero.class.getDeclaredMethod("actTransition", HeroAction.LvlTransition.class);
		act.setAccessible(true);
		act.invoke(hero, new HeroAction.LvlTransition(hero.pos));
	}

	@Test
	void theWinIsRecordedOnceInTheSceneSwitchCallbackAndAResetCannotRepeatIt() throws Exception {
		assertTrue(winRecords().isEmpty(), "test setup: no ranked wins");
		assertNotNull(GamesInProgress.check(GamesInProgress.curSlot), "test setup: the run is saved");
		assertNull(pendingCallback(), "test setup: no scene change pending");

		ascendToTheSurface();

		// the transition only requests the scene; nothing is recorded until it has been created
		assertTrue(Statistics.ascended, "the hero has ascended");
		assertSame(SurfaceScene.class, requestedSceneClass(), "the surface scene is requested");
		assertTrue(winRecords().isEmpty(), "no win may be recorded before the surface scene exists");
		assertNotNull(GamesInProgress.check(GamesInProgress.curSlot), "the save must survive until the scene exists");
		Game.SceneChangeCallback callback = pendingCallback();
		assertNotNull(callback, "the ending must travel with the scene switch as its callback");

		// Game.switchScene(): beforeCreate, scene.create(), afterCreate, then the callback is dropped
		callback.beforeCreate();
		assertTrue(winRecords().isEmpty(), "beforeCreate records nothing");
		callback.afterCreate();

		List<Rankings.Record> wins = winRecords();
		assertEquals(1, wins.size(), "exactly one win is ranked");
		Rankings.Record rec = wins.get(0);
		assertSame(Amulet.class, rec.cause, "won by the Amulet");
		List<String> badges = Arrays.asList(rec.gameData.getBundle(Rankings.BADGES).getStringArray("badges"));
		assertTrue(badges.contains("HAPPY_END"),
				"validateHappyEnd runs before Dungeon.win, so the ranking keeps HAPPY_END; has " + badges);
		assertNull(GamesInProgress.check(GamesInProgress.curSlot), "the finished run's save is deleted");

		// a window resize re-creates the current scene through resetScene: no callback goes with it
		Game.resetScene();
		assertSame(SurfaceScene.class, requestedSceneClass(), "the reset re-creates the surface scene");
		assertNull(pendingCallback(), "a reset must not carry the ending callback, or it would rank the run again");
		assertEquals(1, winRecords().size(), "still exactly one ranked win");
	}

	@Test
	void theCounterSeesASecondWin() throws Exception {
		// proves the "exactly one" assertion above can go red: running the ending twice, as
		// SurfaceScene.create did on every reset before d029c0d7d, ranks the run twice
		ascendToTheSurface();
		Game.SceneChangeCallback callback = pendingCallback();
		assertNotNull(callback);
		callback.afterCreate();
		callback.afterCreate();
		assertEquals(2, winRecords().size());
	}
}
