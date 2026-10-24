package com.qsr.customspd.items.rings;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.MonkEnergy;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.HeroSubClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.MeleeWeapon;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Brawler's Stance as upstream shipped it by v2.5.4:
 *  - c448f9731 (v2.4.0) replaced the per-hit 1/6-charge drain (which CPDU lost in f85e4f3c2 without
 *    porting the replacement, leaving the stance free) with a flat damage bonus and 50% slower
 *    regen-driven weapon charge gain while the buff exists; activation needs no charge.
 *  - 25ef7a59e (v2.4.0) made toggling off only clear {@code active}: the buff lingers, still
 *    slowing charge, until 50 turns after it was last activated. It bundles active/min_turns_left.
 *  - f6f9f5eb6 (v2.4.0) fixed the toggle, which 25ef7a59e had inverted.
 *  - 90d14838a (v2.5.0) raised the bonus to 3+tier + lvl*(4+2*tier)/8.
 */
class BrawlersStanceRedesignPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Hero hero;
	private RingOfForce ring;

	/** A level-1, 10-STR Duelist wearing an identified +0 ring of force and a worn shortsword. */
	private void duelist() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		WornShortsword sword = new WornShortsword();
		sword.identify();
		hero.belongings.weapon = sword;
		ring = new RingOfForce();
		ring.identify();
		hero.belongings.ring = ring;
		ring.activate(hero);
	}

	private RingOfForce.BrawlersStance stance() {
		return hero.buff(RingOfForce.BrawlersStance.class);
	}

	private void toggle() {
		ring.execute(hero, RingOfForce.AC_ABILITY);
	}

	private MeleeWeapon.Charger emptyCharger() {
		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.charges = 0;
		charger.partialCharge = 0f;
		return charger;
	}

	// --- c448f9731: activation and the charge-speed cost ---

	@Test
	void theStanceActivatesWithNoChargeAndRoutesAttacksThroughTheRing() {
		duelist();
		emptyCharger();
		toggle();
		assertNotNull(stance(), "c448f9731: activation no longer needs 1/6 of a charge");
		assertTrue(stance().active);
		assertTrue(RingOfForce.fightingUnarmed(hero), "an active stance attacks with the ring at 0 charge");
		assertTrue(RingOfForce.unarmedGetsWeaponEnchantment(hero));
		assertTrue(RingOfForce.unarmedGetsWeaponAugment(hero));
	}

	@Test
	void theStanceHalvesRegenChargeGain() {
		duelist();
		MeleeWeapon.Charger charger = emptyCharger();
		charger.act();
		float normal = charger.partialCharge;
		assertEquals(1f / 37f, normal, 1e-6f, "test setup: cap 3, 0 charges -> 1/(40-3) per turn");

		charger.partialCharge = 0f;
		toggle();
		charger.act();
		assertEquals(normal / 2f, charger.partialCharge, 1e-6f, "c448f9731: half speed in the stance");
	}

	@Test
	void theStanceAlsoHalvesTheChampionSecondPool() {
		duelist();
		hero.subClass = HeroSubClass.CHAMPION;
		MeleeWeapon.Charger charger = emptyCharger();
		charger.charges = charger.chargeCap(); // keep the primary pool out of the way
		charger.secondCharges = 0;
		charger.secondPartialCharge = 0f;
		charger.act();
		float normal = charger.secondPartialCharge;
		assertTrue(normal > 0f, "test setup: the champion pool regenerates");

		charger.secondPartialCharge = 0f;
		toggle();
		charger.act();
		assertEquals(normal / 2f, charger.secondPartialCharge, 1e-6f,
				"upstream's single shared pool is halved; CPDU's second pool must be too");
	}

	// --- 25ef7a59e + f6f9f5eb6: toggling and the 50-turn minimum ---

	@Test
	void usingTheRingAgainSwitchesTheStanceOffAndThenOnAgain() {
		duelist();
		toggle();
		assertTrue(stance().active);

		toggle();
		assertNotNull(stance(), "25ef7a59e: an inactive stance lingers");
		assertFalse(stance().active, "f6f9f5eb6: using the ring on an active stance switches it off");
		assertSame(BuffIndicator.NONE, stance().icon(), "a lingering stance shows no icon");
		assertFalse(RingOfForce.fightingUnarmed(hero), "an inactive stance attacks with the weapon");
		assertFalse(RingOfForce.unarmedGetsWeaponEnchantment(hero));
		assertFalse(RingOfForce.unarmedGetsWeaponAugment(hero));

		toggle();
		assertTrue(stance().active, "using the ring on a lingering stance re-activates it");
		assertSame(BuffIndicator.DUEL_BRAWL, stance().icon());
	}

	@Test
	void aQuicklyDroppedStanceLingersUntilFiftyTurnsAfterActivation() {
		duelist();
		toggle();
		RingOfForce.BrawlersStance stance = stance();
		for (int i = 0; i < 10; i++) stance.act();
		toggle();
		assertFalse(stance.active);

		MeleeWeapon.Charger charger = emptyCharger();
		charger.act();
		assertEquals(1f / 74f, charger.partialCharge, 1e-6f,
				"25ef7a59e: the charge penalty persists while the inactive stance lingers");

		for (int i = 10; i < 49; i++) stance.act();
		assertNotNull(stance(), "still lingering after 49 turns");
		stance.act();
		assertNull(stance(), "gone on the 50th turn after activation");
	}

	@Test
	void aLongHeldStanceEndsOnTheFirstTurnAfterItIsDropped() {
		duelist();
		toggle();
		RingOfForce.BrawlersStance stance = stance();
		for (int i = 0; i < 60; i++) stance.act();
		assertNotNull(stance(), "an active stance never expires");
		assertTrue(stance.active);

		toggle();
		stance.act();
		assertNull(stance(), "the 50-turn minimum is already served");
	}

	@Test
	void losingTheLastRingOfForceDeactivatesTheStanceButKeepsItLingering() {
		duelist();
		toggle();
		hero.buff(RingOfForce.Force.class).detach();
		assertFalse(RingOfForce.fightingUnarmed(hero));
		assertNotNull(stance(), "25ef7a59e: no longer detached outright");
		assertFalse(stance().active);
	}

	@Test
	void onlyAnActiveStanceBlocksUnencumberedSpiritsWeaponBonus() {
		duelist();
		hero.subClass = HeroSubClass.MONK;
		hero.talents.get(2).put(Talent.UNENCUMBERED_SPIRIT, 1);

		assertEquals(1.4f, energyFromOneRat(), 1e-6f, "test setup: tier-1 weapon at +1 gives +40%");
		toggle();
		assertEquals(1f, energyFromOneRat(), 1e-6f, "an active stance forfeits the weapon bonus");
		toggle();
		assertEquals(1.4f, energyFromOneRat(), 1e-6f, "25ef7a59e: a lingering inactive stance does not");
	}

	private float energyFromOneRat() {
		MonkEnergy energy = Buff.affect(hero, MonkEnergy.class);
		energy.energy = 0;
		energy.gainEnergy(new Rat());
		return energy.energy;
	}

	// --- 90d14838a: the bonus ---

	@Test
	void anActiveStanceAddsThreePlusTierPlusScaledLevelDamage() {
		duelist();
		// STR 10 -> tier 1; +0 ring -> level 1. Ring: 2-12. Bonus: round(3+1+1*(4+2)/8) = 5.
		assertEquals(1, Ring.getBuffedBonus(hero, RingOfForce.Force.class), "test setup");
		toggle();
		int[] active = rollRange(400);
		assertEquals(7, active[0], "90d14838a: min 2 + 5 (c448f9731's 1+tier+lvl*(3+tier)/8 gave +3)");
		assertEquals(17, active[1], "max 12 + 5");

		toggle();
		int[] inactive = rollRange(400);
		assertEquals(2, inactive[0], "a lingering stance adds nothing");
		assertEquals(12, inactive[1]);

		toggle();
		assertTrue(ring.info().contains("7-17"), "the Duelist ability text states the boosted range");
	}

	private int[] rollRange(int rolls) {
		Random.pushGenerator(31337L);
		try {
			int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
			for (int i = 0; i < rolls; i++) {
				int d = RingOfForce.damageRoll(hero);
				lo = Math.min(lo, d);
				hi = Math.max(hi, d);
			}
			return new int[]{lo, hi};
		} finally {
			Random.popGenerator();
		}
	}

	// --- serialized state ---

	@Test
	void aLingeringStanceSurvivesASaveWithItsRemainingMinimum() throws Exception {
		duelist();
		toggle();
		for (int i = 0; i < 20; i++) stance().act();
		toggle();

		RingOfForce.BrawlersStance restored = SaveRoundtrip.of(stance());
		assertFalse(restored.active);
		stance().detach();
		restored.attachTo(hero);
		for (int i = 20; i < 49; i++) restored.act();
		assertNotNull(stance(), "29 of the 30 remaining turns served");
		restored.act();
		assertNull(stance(), "min_turns_left survived the save");
	}

	@Test
	void anActiveStanceSurvivesASave() throws Exception {
		duelist();
		toggle();
		RingOfForce.BrawlersStance restored = SaveRoundtrip.of(stance());
		assertTrue(restored.active);
	}

	@Test
	void aStanceFromAPreRedesignSaveRestoresInactiveAndClearsItself() throws Exception {
		duelist();
		toggle();
		// A pre-redesign BrawlersStance bundled only Buff/Actor state: strip the two new keys.
		Bundle parent = new Bundle();
		parent.put("obj", stance());
		Bundle old = parent.getBundle("obj");
		assertTrue(old.remove(RingOfForce.BrawlersStance.ACTIVE));
		assertTrue(old.remove(RingOfForce.BrawlersStance.MIN_TURNS_LEFT));
		stance().detach();

		RingOfForce.BrawlersStance restored =
				(RingOfForce.BrawlersStance) SaveRoundtrip.writeRead(parent).get("obj");
		assertNotNull(restored, "the class name is unchanged, so the old buff still loads");
		assertFalse(restored.active, "missing key -> inactive, as upstream's own migration");

		restored.attachTo(hero);
		assertFalse(RingOfForce.fightingUnarmed(hero));
		assertSame(BuffIndicator.NONE, restored.icon());
		restored.act();
		assertNull(stance(), "0 turns left -> it clears on its first turn instead of lingering forever");

		toggle();
		assertTrue(stance().active, "and the stance can be taken up again normally");
	}
}
