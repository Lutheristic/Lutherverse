package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.FlavourBuff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.HeroSubClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.BuffIndicator;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 1466dea4b (v2.5.0): Combined Lethality now executes on the first
 * melee attack made right after a different weapon's ability, regular or not. The old delayed
 * trigger (a 5-turn buff applied when the second ability started) is gone.
 */
class CombinedLethalityPortTest {

	private static final int W = 10;
	private static final String OLD_TRIGGER = "com.qsr.customspd.actors.hero.Talent$CombinedLethalityTriggerTracker";

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		ShatteredPixelDungeon.registerBundleAliases();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero champion() {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		hero.subClass = HeroSubClass.CHAMPION;
		Talent.initClassTalents(hero);
		hero.talents.get(2).put(Talent.COMBINED_LETHALITY, 3); // tier-3 talent, 40% threshold
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 2);
		hero.belongings.weapon = new Sword();
		hero.belongings.secondWep = new Spear();
		Buff.affect(hero, MeleeWeapon.Charger.class).charges = 3;
		return hero;
	}

	/** A sturdy rat at 30% HP: below the threshold, but far from dying to one hit. */
	private static Rat woundedRat() {
		Rat rat = HeadlessLevel.at(new Rat(), 4 * W + 3);
		StubSprite.attach(rat);
		rat.HT = 1000;
		rat.HP = 300;
		return rat;
	}

	private static void useAbilityWithoutAttacking(Hero hero, MeleeWeapon wep) {
		wep.beforeAbilityUsed(hero, null);
		wep.afterAbilityUsed(hero);
	}

	private static boolean hasCombinedLethalityBuff(Hero hero) {
		for (Buff b : hero.buffs()) {
			if (b.getClass().getName().contains("CombinedLethality")) return true;
		}
		return false;
	}

	@Test
	void regularAttackRightAfterTheOtherWeaponsAbilityExecutes() {
		Hero hero = champion();
		Rat rat = woundedRat();

		useAbilityWithoutAttacking(hero, (MeleeWeapon) hero.belongings.secondWep);
		hero.attack(rat, 1f, 0f, Char.INFINITE_ACCURACY);

		assertFalse(rat.isAlive(), "a plain attack with the other weapon executes the wounded rat");
		assertNull(hero.buff(Talent.CombinedLethalityAbilityTracker.class), "the execute uses up the tracker");
	}

	@Test
	void regularAttackWithTheSameWeaponDoesNotExecute() {
		Hero hero = champion();
		Rat rat = woundedRat();

		useAbilityWithoutAttacking(hero, (MeleeWeapon) hero.belongings.weapon);
		hero.attack(rat, 1f, 0f, Char.INFINITE_ACCURACY);

		assertTrue(rat.isAlive(), "the weapon whose ability was used cannot trigger the execute");
	}

	@Test
	void secondAbilityWithoutAnAttackLeavesNoDelayedExecute() {
		Hero hero = champion();
		Rat rat = woundedRat();

		useAbilityWithoutAttacking(hero, (MeleeWeapon) hero.belongings.secondWep);
		useAbilityWithoutAttacking(hero, (MeleeWeapon) hero.belongings.weapon);

		assertFalse(hasCombinedLethalityBuff(hero), "a second ability with no attack leaves no buff behind");
		hero.attack(rat, 1f, 0f, Char.INFINITE_ACCURACY);
		assertTrue(rat.isAlive(), "nor does a later attack execute");
	}

	@Test
	void anOldSavesTriggerBuffLoadsAsAPlainExpiringBuff() throws Exception {
		Bundle buff = new Bundle();
		buff.put("__className", OLD_TRIGGER);
		buff.put("time", 3f);
		Bundle save = new Bundle();
		save.put("buff", buff);

		Bundlable restored = SaveRoundtrip.writeRead(save).get("buff");

		assertSame(FlavourBuff.class, restored.getClass(), "the removed buff loads through its alias");
		assertEquals(BuffIndicator.NONE, ((Buff) restored).icon(), "and shows nothing");
	}
}
