package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.effects.Pushing;
import com.qsr.customspd.items.weapon.missiles.darts.Dart;
import com.qsr.customspd.items.weapon.missiles.darts.ParalyticDart;
import com.qsr.customspd.items.weapon.missiles.darts.PoisonDart;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 3f63a6b40 (v2.5.0, "overhauled the crossbow's weapon ability").
 * A charged crossbow's next attack always hits and does one of three things: a melee attack
 * knocks the enemy back (and spends the charge), an untipped dart deals bonus damage, a tipped
 * dart keeps its damage but gains 3 + bow level uses (11a410f07, v2.4.0) and an area effect,
 * pinned in ChargedShotAreaPortTest.
 */
class CrossbowChargedShotPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero heroWith(Crossbow bow) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		hero.belongings.weapon = bow;
		return hero;
	}

	@Test
	void aChargedMeleeAttackAlwaysHitsKnocksBackAndSpendsTheCharge() {
		HeadlessLevel level = HeadlessLevel.install(12, 12);
		Crossbow bow = new Crossbow();
		Hero hero = heroWith(bow);
		HeadlessLevel.at(hero, 5 * level.width() + 2);
		Rat rat = new Rat();
		StubSprite.attach(rat);
		HeadlessLevel.at(rat, 5 * level.width() + 3);

		assertTrue(Float.isFinite(bow.accuracyFactor(hero, rat)), "fixture: an uncharged crossbow can miss");

		Buff.affect(hero, Crossbow.ChargedShot.class);
		assertEquals(Float.POSITIVE_INFINITY, bow.accuracyFactor(hero, rat),
				"upstream 3f63a6b40: a charged crossbow attack always hits");

		int pushersBefore = countPushers();
		bow.proc(hero, rat, 5);

		assertNull(hero.buff(Crossbow.ChargedShot.class), "the melee hit spends the charge");
		assertEquals(pushersBefore + 1, countPushers(), "the enemy is being knocked back");
	}

	@Test
	void aChargedUntippedDartDealsBonusDamageButATippedOneDoesNot() {
		Crossbow bow = new Crossbow();
		Hero hero = heroWith(bow);

		Dart dart = new Dart();
		ParalyticDart tipped = new ParalyticDart();
		dart.throwSound(); // refreshes Dart's static crossbow from the hero
		assertEquals(4, dart.min(0), "fixture: uncharged +0 crossbow dart");
		assertEquals(12, dart.max(0));

		Buff.affect(hero, Crossbow.ChargedShot.class);
		assertEquals(7, dart.min(0), "upstream 3f63a6b40: charged base is 7-15");
		assertEquals(15, dart.max(0));
		assertEquals(4, tipped.min(0), "tipped darts keep their normal damage");
		assertEquals(12, tipped.max(0));

		// scaling doubles to +2 / +4 per bow level on the charged untipped dart
		bow.level(2);
		assertEquals(7 + 2 * 2, dart.min(0));
		assertEquals(15 + 4 * 2, dart.max(0));
	}

	@Test
	void theAbilityDescriptionStatesTheChargedDartBonus() {
		Crossbow bow = new Crossbow();
		heroWith(bow);

		String typical = bow.abilityInfo();
		assertTrue(typical.contains("typically deal _+3 damage_"), "unidentified: the typical +3 base bonus; was: " + typical);
		assertTrue(typical.contains("typically last for _3 more uses_"), typical);
		assertTrue(typical.contains("in a 7x7 area"), typical);

		bow.identify();
		bow.level(2);
		String known = bow.abilityInfo();
		assertTrue(known.contains("deal _+5 damage_"), "identified +2: the bonus is 3 + level; was: " + known);
		assertTrue(known.contains("last for _5 more uses_"), "and so are the extra tipped dart uses; was: " + known);
	}

	@Test
	void aChargedShotGivesATippedDartThreePlusBowLevelUses() {
		HeadlessLevel.install(10, 10);
		Crossbow bow = new Crossbow();
		Hero hero = heroWith(bow);
		HeadlessLevel.at(hero, 4 * 10 + 4);
		new Dart().throwSound(); // refreshes Dart's static crossbow from the hero
		PoisonDart tipped = new PoisonDart();
		assertEquals(100f + 0.001f, tipped.durabilityPerUse(), 0.0005f, "fixture: one use uncharged");

		Buff.affect(hero, Crossbow.ChargedShot.class);
		assertEquals(100f / 4f + 0.001f, tipped.durabilityPerUse(), 0.0005f, "1 use + 3 with a +0 crossbow");

		bow.level(2);
		assertEquals(100f / 6f + 0.001f, tipped.durabilityPerUse(), 0.0005f, "1 use + 3 + 2 with a +2 crossbow");
	}

	private static int countPushers() {
		int n = 0;
		for (Actor a : Actor.all()) {
			if (a instanceof Pushing) n++;
		}
		return n;
	}
}
