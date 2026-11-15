package com.qsr.customspd.journal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Assets;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.mobs.DwarfKing;
import com.qsr.customspd.actors.mobs.Ghoul;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Monk;
import com.qsr.customspd.actors.mobs.RotHeart;
import com.qsr.customspd.actors.mobs.RotLasher;
import com.qsr.customspd.actors.mobs.YogDzewa;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.scenes.InterlevelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minions a boss takes down with it are seen but not counted: DwarfKing's phase-2 cull and
 * death, RotHeart.destroy and YogDzewa.die raise Bestiary.skipCountingEncounters around their
 * minion loops and lower it again afterwards.
 *
 * The boss deaths show the boss-slain banner and drop items, so each test builds a real
 * GameScene over a stub GL, as BestiaryTrapRecordingTest does.
 */
class BestiaryBossCleanupTest {

	private static final long SEED = 20261115L;

	private HeadlessRun run;
	private Game priorInstance;
	private GL20 priorGl;
	private GL20 priorGl20;
	private PlatformSupport priorPlatform;
	private InterlevelScene.Mode priorMode;
	private GameScene scene;

	@BeforeEach
	void start() throws ReflectiveOperationException {
		run = HeadlessRun.start(SEED);
		run.enterFirstLevel();
		priorInstance = Game.instance;
		priorGl = Gdx.gl;
		priorGl20 = Gdx.gl20;
		priorPlatform = Game.platform;
		priorMode = InterlevelScene.mode;
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
		//over-levelled, so the minions skip their loot rolls
		Dungeon.hero.lvl = 30;
		clearBestiary();
		Bestiary.skipCountingEncounters = false;
	}

	@AfterEach
	void tearDown() throws ReflectiveOperationException {
		try {
			clearBestiary();
			Bestiary.skipCountingEncounters = false;
			if (scene != null) scene.destroy();
		} finally {
			Gdx.gl = priorGl;
			Gdx.gl20 = priorGl20;
			Game.instance = priorInstance;
			Game.platform = priorPlatform;
			InterlevelScene.mode = priorMode;
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

	private static void assertSeenNotCounted(Class<?> cls) {
		assertTrue(Bestiary.isSeen(cls), cls.getSimpleName() + " cleared by its boss is seen");
		assertEquals(0, Bestiary.encounterCount(cls), cls.getSimpleName() + " cleared by its boss is not counted");
		assertFalse(Bestiary.skipCountingEncounters, "the skip flag is lowered after the cleanup");
	}

	/** Puts the mob on the next free passable cell from the given start. */
	private static <T extends Mob> T place(T mob, int from) {
		for (int cell = from; cell < Dungeon.level.length(); cell++) {
			if (Dungeon.level.passable[cell] && !Dungeon.level.pit[cell] && Actor.findChar(cell) == null) {
				mob.pos = cell;
				StubSprite.attach(mob);
				Dungeon.level.mobs.add(mob);
				Actor.add(mob);
				return mob;
			}
		}
		throw new IllegalStateException("no free cell for " + mob.getClass().getSimpleName());
	}

	private static <T extends Mob> T place(T mob) {
		return place(mob, 0);
	}

	@Test
	void dwarfKingDeathSeesButDoesNotCountItsSubjects() {
		assertUnrecorded(Ghoul.class);
		DwarfKing king = place(new DwarfKing());
		Ghoul ghoul = place(new Ghoul());
		assertSame(king.alignment, ghoul.alignment, "test setup: the ghoul serves the king");

		king.die(Dungeon.hero);

		assertFalse(ghoul.isAlive(), "test setup: the king's death took the ghoul with it");
		assertSeenNotCounted(Ghoul.class);
		assertEquals(1, Bestiary.encounterCount(DwarfKing.class), "the king himself is counted");
	}

	@Test
	void dwarfKingPhaseTwoCullSeesButDoesNotCountItsSubjects() throws ReflectiveOperationException {
		assertUnrecorded(Monk.class);
		DwarfKing king = place(new DwarfKing());
		Monk monk = place(new Monk());
		Field phase = DwarfKing.class.getDeclaredField("phase");
		phase.setAccessible(true);
		phase.setInt(king, 1);
		king.HP = 60;
		//ScrollOfTeleportation.appear tweens the king's sprite through its parent
		scene.add(king.sprite);

		king.damage(20, Dungeon.hero);

		assertEquals(2, phase.getInt(king), "test setup: the king entered phase 2");
		assertFalse(monk.isAlive(), "test setup: phase 2 culled the monk");
		assertSeenNotCounted(Monk.class);
	}

	@Test
	void rotHeartDestroySeesButDoesNotCountItsLashers() {
		assertUnrecorded(RotLasher.class);
		RotHeart heart = place(new RotHeart());
		RotLasher lasher = place(new RotLasher());

		heart.destroy();

		assertFalse(lasher.isAlive(), "test setup: the heart's death took the lasher with it");
		assertSeenNotCounted(RotLasher.class);
	}

	@Test
	void yogDzewaDeathSeesButDoesNotCountItsLarvae() {
		assertUnrecorded(YogDzewa.Larva.class);
		YogDzewa yog = place(new YogDzewa());
		YogDzewa.Larva larva = place(new YogDzewa.Larva());
		assertSame(Char.Alignment.ENEMY, larva.alignment);

		yog.die(Dungeon.hero);

		assertFalse(larva.isAlive(), "test setup: Yog's death took the larva with it");
		assertSeenNotCounted(YogDzewa.Larva.class);
	}
}
