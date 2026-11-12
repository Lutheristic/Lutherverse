package com.qsr.customspd.levels.features;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Dewdrop;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.artifacts.SandalsOfNature;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream dd9026ee2 (v2.4.0): trampled grass rolls dew on the float chance 1/(6 - n/2), where n
 * is the sandals' naturalism level, instead of the integer Random.Int(6 - n/2), which rounded
 * the odd levels down to the next whole die. The petrified seed half of that commit is a trinket
 * and not ported.
 *
 * Every roll here comes from one fixed uniform value u: a float roll returns u and an integer
 * roll below k returns floor(u * k), so the two forms of the same chance can be compared on
 * equal terms.
 */
class HighGrassDewPortTest {

	private static final int W = 8;
	private static final int GRASS = 3 * W + 3;

	private Hero prevHero;
	private Camera prevCamera;
	private Game prevGame;
	private Object prevSceneClass;
	private HeadlessLevel level;
	private Hero hero;
	private Heap heap;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		prevHero = Dungeon.hero;
		prevCamera = Camera.main;
		// item sprites place themselves through the main camera
		Camera.main = new Camera(0, 0, 100, 100, 1f);
		// dropping an item asks for the current scene; a bare Game has none
		prevGame = Game.instance;
		prevSceneClass = sceneClass().get(null);
		new Game(null, Game.platform);
		level = HeadlessLevel.install(W, 8);
		level.map[GRASS] = Terrain.HIGH_GRASS;
		level.buildFlagMaps();
		hero = new Hero();
		StubSprite.attach(hero);
		Dungeon.hero = HeadlessLevel.at(hero, GRASS);
		// anything the grass drops lands in this heap; a new heap would have no sprite headless
		heap = new Heap();
		heap.pos = GRASS;
		heap.sprite = new ItemSprite();
		heap.sprite.link(heap);
		level.heaps.put(GRASS, heap);
	}

	@AfterEach
	void tearDown() throws Exception {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		Camera.main = prevCamera;
		Game.instance = prevGame;
		sceneClass().set(null, prevSceneClass);
	}

	private static Field sceneClass() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		return f;
	}

	private void wearSandals() {
		SandalsOfNature sandals = new SandalsOfNature();
		sandals.activate(hero);
	}

	private boolean trampleDropsDew(float u) throws Exception {
		pushFixedRolls(u);
		try {
			HighGrass.trample(level, GRASS);
		} finally {
			Random.popGenerator();
		}
		assertEquals(Terrain.GRASS, level.map[GRASS], "fixture: the grass was trampled");
		boolean dew = false;
		for (Item i : heap.items) {
			if (i instanceof Dewdrop) dew = true;
			else throw new AssertionError("the grass dropped something other than dew: " + i);
		}
		return dew;
	}

	@SuppressWarnings("unchecked")
	private static void pushFixedRolls(final float u) throws Exception {
		Field f = Random.class.getDeclaredField("generators");
		f.setAccessible(true);
		((ArrayDeque<java.util.Random>) f.get(null)).push(new java.util.Random() {
			@Override
			public float nextFloat() {
				return u;
			}

			@Override
			public int nextInt(int bound) {
				return (int) (u * bound);
			}
		});
	}

	@Test
	void sandalsAtTheirFirstLevelDropDewOneTimeInFiveAndAHalf() throws Exception {
		wearSandals();

		// 1/6 < 0.17 < 1/5.5
		assertTrue(trampleDropsDew(0.17f), "with naturalism 1 the dew chance is 1/5.5, not 1/6");
	}

	@Test
	void withoutSandalsTheDewChanceStaysOneInSix() throws Exception {
		assertTrue(trampleDropsDew(0.16f));
	}

	@Test
	void withoutSandalsARollAboveOneInSixDropsNoDew() throws Exception {
		assertFalse(trampleDropsDew(0.17f));
	}
}
