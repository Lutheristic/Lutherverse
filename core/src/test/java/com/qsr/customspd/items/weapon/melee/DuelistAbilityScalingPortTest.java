package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the ported slice of upstream 289a43def (v2.4.0, "added scaling to all non-damage based
 * duelist abilities"): the shield, stance and runic-slash abilities now grow with the weapon's
 * level, and their descriptions state the real numbers. The assertions are on both halves because
 * the description text is built from format strings (%d, %%, %1$d) that only fail when rendered.
 *
 * The sneak (dagger family) hunks landed after the wave-4 merge and are pinned in
 * DaggerSneakPortTest. The crossbow's tipped-dart uses still wait on 11a410f07.
 */
class DuelistAbilityScalingPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero heroOnFixture() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		return hero;
	}

	private static <T extends MeleeWeapon> T identifiedAt(T weapon, int level) {
		weapon.level(level);
		weapon.identify();
		return weapon;
	}

	@Test
	void quarterstaffStanceLastsLongerWithUpgrades() {
		Hero hero = heroOnFixture();
		Quarterstaff staff = identifiedAt(new Quarterstaff(), 2);

		staff.duelistAbility(hero, null);

		// 3 + level: the extra turn of the stated 4 + level is spent by the instant activation
		assertEquals(5f, hero.buff(Quarterstaff.DefensiveStance.class).cooldown(), 0.0001f);
		assertTrue(staff.abilityInfo().contains("_6 turns_"), staff.abilityInfo());
	}

	@Test
	void scimitarSwordDanceLastsLongerWithUpgrades() {
		Hero hero = heroOnFixture();
		Scimitar scimitar = identifiedAt(new Scimitar(), 3);

		scimitar.duelistAbility(hero, null);

		assertEquals(6f, hero.buff(Scimitar.SwordDance.class).cooldown(), 0.0001f);
		assertTrue(scimitar.abilityInfo().contains("_7 turns_"), scimitar.abilityInfo());
		assertTrue(scimitar.abilityInfo().contains("+60% attack speed"), "%% must render as a literal percent");
	}

	@Test
	void shieldGuardsLastLongerWithUpgrades() {
		Hero hero = heroOnFixture();
		RoundShield round = identifiedAt(new RoundShield(), 2);
		Greatshield great = identifiedAt(new Greatshield(), 2);

		round.duelistAbility(hero, null);
		assertEquals(10f, hero.buff(RoundShield.GuardTracker.class).cooldown(), 0.0001f);
		assertTrue(round.abilityInfo().contains("_10 turns_"), round.abilityInfo());

		hero.buff(RoundShield.GuardTracker.class).detach(); // prolong keeps the longer of two
		great.duelistAbility(hero, null);
		assertEquals(7f, hero.buff(RoundShield.GuardTracker.class).cooldown(), 0.0001f);
		assertTrue(great.abilityInfo().contains("_7 turns_"), great.abilityInfo());
	}

	@Test
	void unidentifiedWeaponsDescribeTheTypicalBaseValue() {
		assertTrue(new Quarterstaff().abilityInfo().contains("typically for _4 turns_"));
		assertTrue(new Scimitar().abilityInfo().contains("typically for _4 turns_"));
		assertTrue(new RoundShield().abilityInfo().contains("typically within _8 turns_"));
		assertTrue(new Greatshield().abilityInfo().contains("typically within _5 turns_"));
		assertTrue(new RunicBlade().abilityInfo().contains("typically has _+200% enchantment power._"),
				new RunicBlade().abilityInfo());
	}

	@Test
	void runicSlashEnchantBoostScalesWithLevelAndIsConsumed() {
		Hero hero = heroOnFixture();
		RunicBlade blade = identifiedAt(new RunicBlade(), 2);
		assertTrue(blade.abilityInfo().contains("_+250% enchantment power._"), blade.abilityInfo());

		RunicBlade.RunicSlashTracker tracker = Buff.affect(hero, RunicBlade.RunicSlashTracker.class);
		assertEquals(2f, tracker.boost, 0.0001f, "unscaled tracker default is the +200% base");
		tracker.boost = 2f + 0.25f * blade.buffedLvl();

		// 1 (no arcana, no berserk) + 2.5 from the slash
		assertEquals(3.5f, Weapon.Enchantment.genericProcChanceMultiplier(hero), 0.0001f);
		assertEquals(null, hero.buff(RunicBlade.RunicSlashTracker.class), "the tracker is spent by the proc");
	}

	@Test
	void whipDescribesItsDamageRange() {
		heroOnFixture(); // min()/max() read the hero's strength
		Whip whip = identifiedAt(new Whip(), 1);
		String known = whip.abilityInfo();
		assertTrue(known.contains("_" + whip.augment.damageFactor(whip.min()) + "-"
				+ whip.augment.damageFactor(whip.max()) + " damage_"), known);
		assertNotNull(new Whip().abilityInfo());
	}
}
