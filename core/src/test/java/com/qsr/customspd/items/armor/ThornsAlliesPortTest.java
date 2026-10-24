package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Bleeding;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.glyphs.Thorns;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression for upstream 76972e358 (v2.5.0, "thorns no longer rebounds damage to allies").
 * Thorns bleeds whoever hit the wearer; the check was only the proc chance, so a hero's own ally
 * (or anything on the wearer's side) that struck the wearer, e.g. a charmed or confused ally,
 * was made to bleed. It now requires the attacker to be on a different side.
 */
class ThornsAlliesPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** Hits the hero (wearing Thorns) 300 times with a rat of the given alignment; returns the rat. */
	private Rat rebound(Char.Alignment alignment) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);

		ClothArmor armor = new ClothArmor();
		Thorns thorns = new Thorns();
		armor.inscribe(thorns);
		hero.belongings.armor = armor;

		Rat rat = HeadlessLevel.at(new Rat(), 4 * level.width() + 5);
		rat.alignment = alignment;
		StubSprite.attach(rat);

		// +0 Thorns procs about 1 hit in 6; 300 hits leave no doubt about whether it can proc.
		for (int i = 0; i < 300; i++) {
			thorns.proc(armor, rat, hero, 1);
		}
		return rat;
	}

	@Test
	void anEnemyThatHitsTheWearerBleeds() {
		assertNotNull(rebound(Char.Alignment.ENEMY).buff(Bleeding.class));
	}

	@Test
	void anAllyThatHitsTheWearerDoesNot() {
		assertNull(rebound(Char.Alignment.ALLY).buff(Bleeding.class),
				"upstream 76972e358: thorns only rebounds onto attackers of a different alignment");
	}
}
