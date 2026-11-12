package com.qsr.customspd.actors.hero.abilities.warrior;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.armor.ClassArmor;
import com.qsr.customspd.items.armor.WarriorArmor;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream b0a77799e (v2.2.1): using Endure again while its buff is still up throws the old buff
 * away and starts a fresh one, where it used to prolong the old one and could leave it in its
 * spent, post-endure state. The buff now lasts 12 turns instead of 13, and its icon is tinted red.
 */
class EndureReusePortTest {

	private Hero hero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		HeadlessLevel.install(8, 8);
		hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 3 * 8 + 3);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private void endure() {
		ClassArmor armor = new WarriorArmor();
		armor.charge = 100;
		new Endure().activate(armor, hero, hero.pos);
	}

	@Test
	void endureLastsTwelveTurns() {
		endure();
		Endure.EndureTracker buff = hero.buff(Endure.EndureTracker.class);
		assertNotNull(buff, "Endure attaches its buff");
		assertEquals(12f, buff.cooldown(), 0.001f);
	}

	@Test
	void usingEndureAgainStartsAFreshBuff() {
		endure();
		Endure.EndureTracker spent = hero.buff(Endure.EndureTracker.class);
		assertNotNull(spent, "test setup");
		// the state endEnduring leaves behind: bonus damage banked for the next hits
		spent.enduring = false;
		spent.damageBonus = 9;
		spent.hitsLeft = 2;

		endure();

		Endure.EndureTracker fresh = hero.buff(Endure.EndureTracker.class);
		assertNotNull(fresh);
		assertNotSame(spent, fresh, "the old buff is replaced");
		assertTrue(fresh.enduring, "the new buff is enduring");
		assertEquals(0, fresh.damageBonus, "no bonus carried over");
		assertEquals(0, fresh.hitsLeft, "no hits carried over");
		assertEquals(12f, fresh.cooldown(), 0.001f, "a full 12 turns again");
	}

	@Test
	void theBuffIconIsTintedRed() {
		Image icon = new Image();
		new Endure.EndureTracker().tintIcon(icon);
		assertEquals(1f, icon.rm, 0.001f);
		assertEquals(0f, icon.gm, 0.001f);
		assertEquals(0f, icon.bm, 0.001f);
	}
}
