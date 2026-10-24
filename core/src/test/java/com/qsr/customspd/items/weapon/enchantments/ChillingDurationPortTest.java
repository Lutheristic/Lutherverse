package com.qsr.customspd.items.weapon.enchantments;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Chill;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 55ba5757d (v2.4.0, "fixed the chilling enchant possibly reducing
 * chill duration"). Chilling tops a target's chill up to a cap (6 turns for a plain proc); when
 * the target was already chilled for longer than the cap, the top-up came out negative and
 * Buff.affect spent a negative duration, SHORTENING the chill the proc was meant to extend.
 */
class ChillingDurationPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** A hero and a rat, the rat already chilled for 10 turns (above the 6-turn proc cap). */
	private Rat chilledRat() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		Rat rat = HeadlessLevel.at(new Rat(), 4 * level.width() + 5);
		StubSprite.attach(rat);
		Buff.affect(rat, Chill.class, 10f);
		assertEquals(10f, rat.buff(Chill.class).cooldown(), 0.0001f, "fixture: rat starts chilled for 10 turns");
		return rat;
	}

	@Test
	void aWeaponProcNeverShortensAnOverCapChill() {
		Rat rat = chilledRat();
		Sword sword = new Sword();
		// A +0 chilling weapon procs 25% of the time; 200 swings all but guarantee some procs.
		for (int i = 0; i < 200; i++) {
			new Chilling().proc(sword, Dungeon.hero, rat, 1);
		}
		assertEquals(10f, rat.buff(Chill.class).cooldown(), 0.0001f,
				"upstream 55ba5757d: a proc past the cap adds nothing rather than a negative duration");
	}

	@Test
	void aMobEnchantProcNeverShortensAnOverCapChill() {
		Rat rat = chilledRat();
		// probability 2 always procs; strength 3 caps the chill at 6 turns.
		new Chilling().proc(2f, 3, Dungeon.hero, rat, 1);
		assertEquals(10f, rat.buff(Chill.class).cooldown(), 0.0001f,
				"the strength-based overload tops up the same way and has the same cap");
	}
}
