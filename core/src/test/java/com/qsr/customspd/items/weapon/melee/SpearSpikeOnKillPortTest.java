package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.GreaterHaste;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 6d3ef615d (v2.5.0, "fixed spike ability rarely triggering 'on
 * kill' fx incorrectly"). Since ca0d8bbda / a8c429c9f the spike only knocks back an enemy that
 * is alive, still where it was hit, and not already being pushed. The 'else' branch therefore
 * also caught a living enemy that moved when hit (or that an elastic proc is pushing) and ran
 * the on-kill effects for it - Lethal Haste's Greater Haste without a kill.
 */
class SpearSpikeOnKillPortTest {

	private static final int W = 10;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** A sturdy rat that blinks one cell away whenever it is hit, as a teleport-on-hit enemy would. */
	public static class BlinkingRat extends Rat {
		{
			HP = HT = 1000;
		}

		@Override
		public int defenseProc(Char enemy, int damage) {
			pos = pos + W; // one row down, still open floor in the fixture
			return super.defenseProc(enemy, damage);
		}
	}

	private static Hero duelistWithLethalHaste() {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		hero.talents.get(1).put(Talent.LETHAL_HASTE, 2); // tier-2 talent
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 2);
		return hero;
	}

	@Test
	void spikingAnEnemyThatMovesWhenHitIsNotAKill() {
		Hero hero = duelistWithLethalHaste();
		Spear spear = new Spear();
		hero.belongings.weapon = spear;
		int cell = 4 * W + 4; // two tiles away: in reach, not adjacent
		BlinkingRat rat = HeadlessLevel.at(new BlinkingRat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;

		spear.duelistAbility(hero, cell);

		assertTrue(rat.isAlive(), "fixture: the rat survives the spike");
		assertNotEquals(cell, rat.pos, "fixture: the rat moved when hit, so it is not knocked back");
		assertNull(hero.buff(GreaterHaste.class), "a surviving enemy grants no Lethal Haste");
	}

	@Test
	void spikingAnEnemyToDeathStillTriggersLethalHaste() {
		Hero hero = duelistWithLethalHaste();
		Spear spear = new Spear();
		hero.belongings.weapon = spear;
		int cell = 4 * W + 4;
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		StubSprite.attach(rat);
		rat.HP = 1;
		Dungeon.level.heroFOV[cell] = true;

		spear.duelistAbility(hero, cell);

		assertTrue(!rat.isAlive(), "fixture: the spike killed the rat");
		assertNotNull(hero.buff(GreaterHaste.class), "control: a kill still grants Lethal Haste");
	}
}
