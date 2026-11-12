package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.Statistics;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Levitation;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 408846301 (its cursed wand part belongs to the later cursed
 * wand rework): a levitating piranha is out of its element and dies like one on land, and a
 * flying phantom piranha cannot save itself by teleporting to other water.
 */
class PiranhaLevitationPortTest {

	private static final int SIZE = 10;
	private static final int WATER_A = 5 * SIZE + 3;
	private static final int WATER_B = 5 * SIZE + 7;
	private static final int LAND = 2 * SIZE + 5;

	private HeadlessLevel level;
	private int prevKilled;
	private Game prevGame;
	private Object prevSceneClass;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private static Field sceneClass() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		return f;
	}

	@BeforeEach
	void setUp() throws Exception {
		prevKilled = Statistics.piranhasKilled;
		// Levitation.detach asks for the current scene; a bare Game has none
		prevGame = Game.instance;
		prevSceneClass = sceneClass().get(null);
		new Game(null, Game.platform);
		level = HeadlessLevel.install(SIZE, SIZE);
		Level.set(WATER_A, Terrain.WATER, level);
		Level.set(WATER_B, Terrain.WATER, level);
		Hero hero = new Hero();
		hero.lvl = 100; // far past the piranhas' loot level, so their deaths drop nothing
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);
	}

	@AfterEach
	void tearDown() throws Exception {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		Statistics.piranhasKilled = prevKilled;
		Game.instance = prevGame;
		sceneClass().set(null, prevSceneClass);
	}

	private static <T extends Piranha> T placed(T piranha, int cell) {
		HeadlessLevel.at(piranha, cell);
		// a teleport fades the sprite back in through its parent group
		new Group().add(StubSprite.attach(piranha));
		return piranha;
	}

	@Test
	void aLevitatingPiranhaDiesOutOfWater() {
		Piranha piranha = placed(new Piranha(), WATER_A);
		Buff.affect(piranha, Levitation.class, 5f);

		piranha.act();

		assertFalse(piranha.isAlive(), "a piranha lifted out of the water dies");
	}

	@Test
	void aLevitatingPhantomPiranhaCannotTeleportAway() {
		PhantomPiranha phantom = placed(new PhantomPiranha(), WATER_A);
		Buff.affect(phantom, Levitation.class, 5f);

		phantom.dieOnLand();

		assertFalse(phantom.isAlive(), "a flying phantom piranha dies instead of teleporting");
		assertEquals(WATER_A, phantom.pos);
	}

	@Test
	void aStrandedPhantomPiranhaStillTeleportsToWater() {
		// unchanged by the port: a phantom piranha that is not flying still escapes to water
		PhantomPiranha phantom = placed(new PhantomPiranha(), LAND);

		phantom.dieOnLand();

		assertTrue(phantom.isAlive());
		assertTrue(phantom.pos == WATER_A || phantom.pos == WATER_B, "it lands in water, at " + phantom.pos);
	}
}
