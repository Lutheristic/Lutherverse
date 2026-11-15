package com.qsr.customspd.journal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Assets;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.levels.traps.AlarmTrap;
import com.qsr.customspd.levels.traps.DistortionTrap;
import com.qsr.customspd.levels.traps.FlockTrap;
import com.qsr.customspd.levels.traps.SummoningTrap;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.levels.traps.WornDartTrap;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.scenes.InterlevelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bestiary records a trap when it is triggered, and when a flock, summoning or distortion
 * trap sets it off directly because something it spawned landed on it.
 *
 * The spawning traps add mob sprites and effects to the game scene, so each test builds a real
 * GameScene over a stub GL, as GameSceneRegionIntroPortTest does. The hero's view is cleared so
 * the chained alarm traps stay quiet.
 */
class BestiaryTrapRecordingTest {

	private static final long SEED = 20261115L;

	private HeadlessRun run;
	private Game priorInstance;
	private GL20 priorGl;
	private GL20 priorGl20;
	private GameScene scene;

	@BeforeEach
	void start() throws ReflectiveOperationException {
		run = HeadlessRun.start(SEED);
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
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		scene = new GameScene();
		scene.create();
		Arrays.fill(Dungeon.level.heroFOV, false);
		clearBestiary();
		Bestiary.skipCountingEncounters = false;
	}

	@AfterEach
	void tearDown() throws ReflectiveOperationException {
		try {
			clearBestiary();
			if (scene != null) scene.destroy();
		} finally {
			Gdx.gl = priorGl;
			Gdx.gl20 = priorGl20;
			Game.instance = priorInstance;
			run.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static void clearBestiary() throws ReflectiveOperationException {
		Field seen = Bestiary.class.getDeclaredField("seen");
		Field counts = Bestiary.class.getDeclaredField("encounterCount");
		seen.setAccessible(true);
		counts.setAccessible(true);
		for (Bestiary cat : Bestiary.values()) {
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
			for (Map.Entry<Class<?>, Integer> e : ((Map<Class<?>, Integer>) counts.get(cat)).entrySet()) {
				e.setValue(0);
			}
		}
	}

	private static void assertUnrecorded(Class<?> cls) {
		assertFalse(Bestiary.isSeen(cls), "test setup: " + cls.getSimpleName() + " starts unseen");
		assertEquals(0, Bestiary.encounterCount(cls), "test setup: " + cls.getSimpleName() + " starts uncounted");
	}

	private static boolean open(int cell) {
		return Dungeon.level.insideMap(cell) && Dungeon.level.passable[cell] && !Dungeon.level.pit[cell]
				&& Actor.findChar(cell) == null && Dungeon.level.traps.get(cell) == null
				&& Dungeon.level.heaps.get(cell) == null;
	}

	/** A free cell away from the hero whose eight neighbours are all free floor. */
	private static int openCellAwayFromTheHero() {
		for (int cell = 0; cell < Dungeon.level.length(); cell++) {
			if (!open(cell) || Dungeon.level.distance(cell, Dungeon.hero.pos) < 4) continue;
			boolean neighboursOpen = true;
			for (int offset : PathFinder.NEIGHBOURS8) {
				if (!open(cell + offset)) {
					neighboursOpen = false;
					break;
				}
			}
			if (neighboursOpen) return cell;
		}
		throw new IllegalStateException("no open 3x3 patch on the first floor");
	}

	/** Alarm traps on every neighbour of the cell, where the spawning trap places its mobs. */
	private static List<AlarmTrap> alarmsAround(int cell) {
		List<AlarmTrap> alarms = new ArrayList<>();
		for (int offset : PathFinder.NEIGHBOURS8) {
			AlarmTrap alarm = new AlarmTrap();
			Dungeon.level.setTrap(alarm, cell + offset);
			alarms.add(alarm);
		}
		return alarms;
	}

	private static int setOff(List<AlarmTrap> alarms) {
		int setOff = 0;
		for (AlarmTrap alarm : alarms) {
			if (!alarm.active) setOff++;
		}
		return setOff;
	}

	private static void activateSeeded(Trap trap) {
		Random.pushGenerator(SEED);
		try {
			trap.activate();
		} finally {
			Random.popGenerator();
		}
	}

	private static void assertChainedAlarmsCounted(Trap spawner) {
		assertUnrecorded(AlarmTrap.class);
		int cell = openCellAwayFromTheHero();
		spawner.set(cell);
		List<AlarmTrap> alarms = alarmsAround(cell);

		activateSeeded(spawner);

		int setOff = setOff(alarms);
		assertTrue(setOff > 0, "test setup: a spawn landed on an alarm trap");
		assertTrue(Bestiary.isSeen(AlarmTrap.class), "an alarm trap set off by a " + spawner.getClass().getSimpleName() + " is seen");
		assertEquals(setOff, Bestiary.encounterCount(AlarmTrap.class),
				"every alarm trap a " + spawner.getClass().getSimpleName() + " set off is counted once");
	}

	@Test
	void triggeringATrapRecordsIt() {
		assertUnrecorded(WornDartTrap.class);
		WornDartTrap trap = new WornDartTrap();
		Dungeon.level.setTrap(trap, openCellAwayFromTheHero());

		trap.trigger();

		assertTrue(Bestiary.isSeen(WornDartTrap.class), "a triggered worn dart trap is seen");
		assertEquals(1, Bestiary.encounterCount(WornDartTrap.class), "a triggered worn dart trap is counted once");
	}

	@Test
	void aSheepFromAFlockTrapLandingOnATrapCountsIt() {
		assertChainedAlarmsCounted(new FlockTrap());
	}

	@Test
	void aMobFromASummoningTrapLandingOnATrapCountsIt() {
		assertChainedAlarmsCounted(new SummoningTrap());
	}

	@Test
	void aMobFromADistortionTrapLandingOnATrapCountsIt() {
		assertChainedAlarmsCounted(new DistortionTrap());
	}
}
