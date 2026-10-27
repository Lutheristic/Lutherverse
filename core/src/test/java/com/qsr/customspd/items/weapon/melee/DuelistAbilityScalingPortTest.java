package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Callback;
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
 * The sneak (dagger family) hunks landed separately and are pinned in
 * DaggerSneakPortTest. The guard durations are 11a410f07's (v2.4.0) retune and the runic slash
 * boost is 753b340f0's (v2.4.2); the crossbow's tipped-dart uses are pinned in
 * CrossbowChargedShotPortTest.
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
		assertTrue(staff.abilityInfo().contains("_6 turns._"), staff.abilityInfo());
	}

	@Test
	void scimitarSwordDanceLastsLongerWithUpgrades() {
		Hero hero = heroOnFixture();
		Scimitar scimitar = identifiedAt(new Scimitar(), 3);

		scimitar.duelistAbility(hero, null);

		assertEquals(6f, hero.buff(Scimitar.SwordDance.class).cooldown(), 0.0001f);
		assertTrue(scimitar.abilityInfo().contains("_7 turns._"), scimitar.abilityInfo());
		assertTrue(scimitar.abilityInfo().contains("+60% attack speed"), "%% must render as a literal percent");
		assertTrue(scimitar.abilityInfo().contains("+50% accuracy"), "11a410f07: the text states Hero's 1.5x sword dance accuracy");
	}

	@Test
	void shieldGuardsLastLongerWithUpgrades() {
		Hero hero = heroOnFixture();
		RoundShield round = identifiedAt(new RoundShield(), 2);
		Greatshield great = identifiedAt(new Greatshield(), 2);

		round.duelistAbility(hero, null);
		assertEquals(7f, hero.buff(RoundShield.GuardTracker.class).cooldown(), 0.0001f, "5 + level");
		assertTrue(round.abilityInfo().contains("for _7 turns_"), round.abilityInfo());

		hero.buff(RoundShield.GuardTracker.class).detach(); // prolong keeps the longer of two
		great.duelistAbility(hero, null);
		assertEquals(5f, hero.buff(RoundShield.GuardTracker.class).cooldown(), 0.0001f, "3 + level");
		assertTrue(great.abilityInfo().contains("for _5 turns_"), great.abilityInfo());
		assertTrue(great.abilityInfo().contains("all physical or magical attacks"),
				"a guard blocks every attack until it lapses, not just the next one");
	}

	@Test
	void unidentifiedWeaponsDescribeTheTypicalBaseValue() {
		assertTrue(new Quarterstaff().abilityInfo().contains("typically for _4 turns._"));
		assertTrue(new Scimitar().abilityInfo().contains("typically for _4 turns._"));
		assertTrue(new RoundShield().abilityInfo().contains("typically for _5 turns_"), new RoundShield().abilityInfo());
		assertTrue(new Greatshield().abilityInfo().contains("typically for _3 turns_"), new Greatshield().abilityInfo());
		assertTrue(new RunicBlade().abilityInfo().contains("typically has _+300% enchantment power._"),
				new RunicBlade().abilityInfo());
	}

	@Test
	void runicSlashEnchantBoostScalesWithLevelAndIsConsumed() {
		Hero hero = heroOnFixture();
		RunicBlade blade = identifiedAt(new RunicBlade(), 2);
		assertTrue(blade.abilityInfo().contains("_+400% enchantment power._"), blade.abilityInfo());

		RunicBlade.RunicSlashTracker tracker = Buff.affect(hero, RunicBlade.RunicSlashTracker.class);
		assertEquals(2f, tracker.boost, 0.0001f, "the buff's own default, which the ability overwrites");
		tracker.boost = 3f;

		// 1 (no arcana, no berserk) + 3 from the slash
		assertEquals(4f, Weapon.Enchantment.genericProcChanceMultiplier(hero), 0.0001f);
		assertEquals(null, hero.buff(RunicBlade.RunicSlashTracker.class), "the tracker is spent by the proc");
	}

	/** Holds the swing, so what the ability set up can be read before the attack spends it. */
	private static final class HeldSwingSprite extends StubSprite {
		@Override
		public synchronized void attack(int cell, Callback callback) {
		}
	}

	@Test
	void runicSlashSetsThreePlusHalfALevelOfEnchantPower() {
		Hero hero = heroOnFixture();
		HeldSwingSprite sprite = new HeldSwingSprite();
		sprite.ch = hero;
		hero.sprite = sprite;
		RunicBlade blade = identifiedAt(new RunicBlade(), 2);
		hero.belongings.weapon = blade;
		int cell = hero.pos + 1;
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;

		blade.duelistAbility(hero, cell);

		assertEquals(4f, hero.buff(RunicBlade.RunicSlashTracker.class).boost, 0.0001f, "3 + 0.5 per level at +2");
	}

	@Test
	void whipDescribesItsDamageRange() {
		heroOnFixture(); // min()/max() read the hero's strength
		Whip whip = identifiedAt(new Whip(), 1);
		String known = whip.abilityInfo();
		// 11a410f07: plus a 2+round(0.5*lvl) boost, 3 at +1
		assertTrue(known.contains("_" + whip.augment.damageFactor(whip.min() + 3) + "-"
				+ whip.augment.damageFactor(whip.max() + 3) + " damage_"), known);
		assertNotNull(new Whip().abilityInfo());
	}
}
