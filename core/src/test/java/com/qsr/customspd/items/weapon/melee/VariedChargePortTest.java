package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.HeroSubClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Upstream aa0d58bac and 8042ebee4 (v2.4.0) replaced the Champion's Secondary Charge, which
 * 11a410f07's shared charge pool left with nothing to do, with Varied Charge: using a weapon
 * ability right after the other weapon's refunds 1/6 of a charge per point, with no time limit.
 * eeca547cc (v2.4.0) made the refund go through Charger.gainCharge, which now clears the partial
 * charge once the pool is full. Saves name talents by enum name, so the rename is mapped.
 */
class VariedChargePortTest {

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

	private static Hero champion(int variedCharge) {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		hero.subClass = HeroSubClass.CHAMPION;
		Talent.initClassTalents(hero);
		Talent.initSubclassTalents(hero);
		hero.talents.get(2).put(Talent.VARIED_CHARGE, variedCharge);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 4);
		hero.belongings.weapon = new Sword();
		hero.belongings.secondWep = new Spear();
		return hero;
	}

	private static void abilityUsed(MeleeWeapon weapon, Hero hero) {
		weapon.afterAbilityUsed(hero);
	}

	private static MeleeWeapon.Charger pool(Hero hero, int charges) {
		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0f;
		return charger;
	}

	@Test
	void switchingWeaponsRefundsASixthOfAChargePerPoint() {
		Hero hero = champion(3);
		MeleeWeapon.Charger charger = pool(hero, 1);
		MeleeWeapon sword = (MeleeWeapon) hero.belongings.weapon;
		MeleeWeapon spear = (MeleeWeapon) hero.belongings.secondWep;

		abilityUsed(sword, hero);
		assertEquals(0f, charger.partialCharge, 0f, "the first ability only remembers its weapon");
		assertNotNull(hero.buff(Talent.VariedChargeTracker.class));

		abilityUsed(sword, hero);
		assertEquals(0f, charger.partialCharge, 0f, "the same weapon again refunds nothing");

		abilityUsed(spear, hero);
		assertEquals(0.5f, charger.partialCharge, 1e-6f, "the other weapon refunds 3/6 of a charge");
		assertEquals(1, charger.charges);
		assertNull(hero.buff(Talent.VariedChargeTracker.class), "and starts over");

		abilityUsed(spear, hero);
		assertEquals(0.5f, charger.partialCharge, 1e-6f, "starting over, the next ability only remembers its weapon");
	}

	@Test
	void theRefundScalesWithPoints() {
		Hero hero = champion(1);
		MeleeWeapon.Charger charger = pool(hero, 1);
		abilityUsed((MeleeWeapon) hero.belongings.weapon, hero);
		abilityUsed((MeleeWeapon) hero.belongings.secondWep, hero);
		assertEquals(1f / 6f, charger.partialCharge, 1e-6f);
	}

	@Test
	void theTrackerHasNoTimeLimitAndSurvivesASave() throws Exception {
		Hero hero = champion(2);
		MeleeWeapon.Charger charger = pool(hero, 1);
		abilityUsed((MeleeWeapon) hero.belongings.weapon, hero);

		Talent.VariedChargeTracker tracker = hero.buff(Talent.VariedChargeTracker.class);
		for (int i = 0; i < 100; i++) tracker.act();
		Talent.VariedChargeTracker restored = SaveRoundtrip.of(tracker);
		assertSame(Sword.class, restored.weapon, "the weapon class is saved");
		tracker.detach();
		restored.attachTo(hero);

		abilityUsed((MeleeWeapon) hero.belongings.secondWep, hero);
		assertEquals(2f / 6f, charger.partialCharge, 1e-6f, "the remembered sword still counts");
	}

	@Test
	void aRefundThatFillsThePoolClearsItsPartialCharge() {
		Hero hero = champion(0);
		MeleeWeapon.Charger charger = pool(hero, charger(hero).chargeCap() - 1);
		charger.partialCharge = 0.75f;
		charger.gainCharge(0.5f);
		assertEquals(charger.chargeCap(), charger.charges);
		assertEquals(0f, charger.partialCharge, 0f, "eeca547cc: no partial charge kept at the cap");

		charger = pool(hero, 0);
		charger.partialCharge = 0.75f;
		charger.gainCharge(0.5f);
		assertEquals(1, charger.charges);
		assertEquals(0.25f, charger.partialCharge, 1e-6f, "below the cap the remainder is kept");
	}

	private static MeleeWeapon.Charger charger(Hero hero) {
		return Buff.affect(hero, MeleeWeapon.Charger.class);
	}

	/** A save whose tier-3 talents name Secondary Charge, as one written before the rename does. */
	@Test
	void secondaryChargePointsBecomeVariedCharge() throws Exception {
		Hero original = new Hero();
		original.heroClass = HeroClass.DUELIST;
		original.subClass = HeroSubClass.CHAMPION;
		Talent.initClassTalents(original);
		Talent.initSubclassTalents(original);
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, original);
		Bundle tier3 = new Bundle();
		tier3.put("SECONDARY_CHARGE", 2);
		tier3.put("TWIN_UPGRADES", 1);
		saved.put("talents_tier_3", tier3);
		Bundle loaded = SaveRoundtrip.writeRead(saved);

		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.DUELIST;
		fresh.subClass = HeroSubClass.CHAMPION;
		Talent.restoreTalentsFromBundle(loaded, fresh);

		assertEquals(2, fresh.pointsInTalent(Talent.VARIED_CHARGE));
		assertEquals(1, fresh.pointsInTalent(Talent.TWIN_UPGRADES));
	}

	@Test
	void aMetamorphosisNamingSecondaryChargeIsKept() throws Exception {
		Hero original = new Hero();
		original.heroClass = HeroClass.DUELIST;
		original.subClass = HeroSubClass.CHAMPION;
		Talent.initClassTalents(original);
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, original);
		Bundle replacements = new Bundle();
		replacements.put("SECONDARY_CHARGE", "TWIN_UPGRADES");
		replacements.put("COMBINED_LETHALITY", "SECONDARY_CHARGE");
		saved.put("replacements", replacements);
		Bundle loaded = SaveRoundtrip.writeRead(saved);

		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.DUELIST;
		fresh.subClass = HeroSubClass.CHAMPION;
		Talent.restoreTalentsFromBundle(loaded, fresh);

		assertEquals(Talent.TWIN_UPGRADES, fresh.metamorphedTalents.get(Talent.VARIED_CHARGE), "the old name as the key");
		assertEquals(Talent.VARIED_CHARGE, fresh.metamorphedTalents.get(Talent.COMBINED_LETHALITY), "and as the value");
	}
}
