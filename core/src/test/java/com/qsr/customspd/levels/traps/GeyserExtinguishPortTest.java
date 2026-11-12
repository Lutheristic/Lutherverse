package com.qsr.customspd.levels.traps;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Burning;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.brews.AquaBrew;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 464061607 (v2.4.0): a geyser, and so a thrown aqua brew, puts out the burning
 * characters it hits, both those it knocks back from around it and the one standing on it.
 */
class GeyserExtinguishPortTest {

	private static final int W = 11;
	private static final int CENTRE = 5 * W + 5;

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		prevHero = Dungeon.hero;
		HeadlessLevel.install(W, W);
		// the hero stands well out of the blast
		Hero hero = new Hero();
		StubSprite.attach(hero);
		Dungeon.hero = HeadlessLevel.at(hero, W + 1);
		curUser().set(null, hero);
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	private static Rat burningRat(int cell) {
		Rat rat = new Rat();
		rat.HP = rat.HT = 1000;
		StubSprite.attach(rat);
		HeadlessLevel.at(rat, cell);
		Buff.affect(rat, Burning.class).reignite(rat);
		assertNotNull(rat.buff(Burning.class), "fixture: the rat is on fire");
		return rat;
	}

	private static void geyserAt(int cell) {
		GeyserTrap geyser = new GeyserTrap();
		geyser.pos = cell;
		Random.pushGenerator(77L);
		try {
			geyser.activate();
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void aGeyserPutsOutABurningCharItKnocksBack() {
		Rat rat = burningRat(CENTRE + 1);

		geyserAt(CENTRE);

		assertNull(rat.buff(Burning.class));
	}

	@Test
	void aGeyserPutsOutABurningCharStandingOnIt() {
		Rat rat = burningRat(CENTRE);

		geyserAt(CENTRE);

		assertNull(rat.buff(Burning.class));
	}

	@Test
	void aThrownAquaBrewPutsOutTheCharItLandsOn() {
		Rat rat = burningRat(CENTRE);

		Random.pushGenerator(78L);
		try {
			new AquaBrew().shatter(CENTRE);
		} finally {
			Random.popGenerator();
		}

		assertNull(rat.buff(Burning.class));
	}
}
