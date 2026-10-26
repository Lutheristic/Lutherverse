package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.MonkEnergy;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.HeroSubClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream bf56446d0 (v2.5.0): Combined Energy triggers when a weapon ability and a monk ability
 * of enough energy are used within 5 turns of each other, in either order, and refunds 1 energy
 * instead of half the energy spent.
 */
class CombinedEnergyPortTest {

	private static final int W = 10;

	private Hero hero;
	private MonkEnergy energy;
	private MeleeWeapon sword;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** A monk with Combined Energy +1, which counts monk abilities of 4 energy or more. */
	private void monk() {
		HeadlessLevel.install(W, 10);
		hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		hero.subClass = HeroSubClass.MONK;
		Talent.initClassTalents(hero);
		hero.talents.get(2).put(Talent.COMBINED_ENERGY, 1);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 2);
		sword = new Sword();
		hero.belongings.weapon = sword;
		energy = Buff.affect(hero, MonkEnergy.class);
		energy.energy = 10;
	}

	private static void waitTurns(int turns) {
		float until = Actor.now() + turns;
		new TurnPump(h -> h.spendConstant(Actor.TICK)).runUntil(() -> Actor.now() >= until, 50 * turns);
	}

	@Test
	void aWeaponAbilityThenAMonkAbilityRefundsOneEnergy() {
		monk();

		sword.afterAbilityUsed(hero);
		energy.abilityUsed(new MonkEnergy.MonkAbility.DragonKick());

		assertEquals(7f, energy.energy, 0.0001f, "10 - 4 for the kick + 1 back");
		assertNull(hero.buff(Talent.CombinedEnergyAbilityTracker.class), "the refund uses up the tracker");
	}

	@Test
	void aMonkAbilityThenAWeaponAbilityRefundsOneEnergy() {
		monk();

		energy.abilityUsed(new MonkEnergy.MonkAbility.DragonKick());
		sword.afterAbilityUsed(hero);

		assertEquals(7f, energy.energy, 0.0001f, "10 - 4 for the kick + 1 back");
		assertNull(hero.buff(Talent.CombinedEnergyAbilityTracker.class));
	}

	@Test
	void theSecondAbilityMayComeFourTurnsLater() {
		monk();

		sword.afterAbilityUsed(hero);
		waitTurns(4);
		energy.abilityUsed(new MonkEnergy.MonkAbility.DragonKick());

		assertEquals(7f, energy.energy, 0.0001f, "still within the 5 turn window");
	}

	@Test
	void theSecondAbilityGetsNoRefundAfterFiveTurns() {
		monk();

		sword.afterAbilityUsed(hero);
		waitTurns(6);
		energy.abilityUsed(new MonkEnergy.MonkAbility.DragonKick());

		assertEquals(6f, energy.energy, 0.0001f, "control: the window has closed");
	}

	@Test
	void theTrackerKeepsBothFlagsThroughASave() throws Exception {
		Talent.CombinedEnergyAbilityTracker monkFirst = new Talent.CombinedEnergyAbilityTracker();
		monkFirst.monkAbilused = true;
		monkFirst = SaveRoundtrip.of(monkFirst);
		assertTrue(monkFirst.monkAbilused);
		assertFalse(monkFirst.wepAbilUsed);

		Talent.CombinedEnergyAbilityTracker weaponFirst = new Talent.CombinedEnergyAbilityTracker();
		weaponFirst.wepAbilUsed = true;
		weaponFirst = SaveRoundtrip.of(weaponFirst);
		assertFalse(weaponFirst.monkAbilused);
		assertTrue(weaponFirst.wepAbilUsed);
	}
}
