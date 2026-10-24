package com.qsr.customspd.items.weapon.enchantments;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream fdbcb877d (v2.5.3, "vampiric no longer triggers on allies or
 * neutral characters"). Vampiric healed the attacker off any hit, so the hero could heal by
 * hitting an ally or a neutral character. It now needs a character of a different alignment, and
 * a neutral one only counts if it is a mimic (which is hostile in all but alignment).
 */
class VampiricAlignmentPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** Hero at 1/100 HP (about a 30% heal chance per hit) hitting a rat of the given alignment. */
	private int healedHpAfterManyHits(Char.Alignment targetAlignment) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		Rat rat = HeadlessLevel.at(new Rat(), 4 * level.width() + 5);
		StubSprite.attach(rat);
		rat.alignment = targetAlignment;
		hero.HT = 100;

		Vampiric vampiric = new Vampiric();
		WornShortsword sword = new WornShortsword();
		int best = 1;
		for (int i = 0; i < 400; i++) {
			hero.HP = 1;
			vampiric.proc(sword, hero, rat, 10);
			best = Math.max(best, hero.HP);
		}
		return best;
	}

	@Test
	void controlHittingAnEnemyCanHeal() {
		assertTrue(healedHpAfterManyHits(Char.Alignment.ENEMY) > 1,
				"fixture: at ~30% a heal shows up within 400 hits");
	}

	@Test
	void hittingAnAllyNeverHeals() {
		assertEquals(1, healedHpAfterManyHits(Char.Alignment.ALLY), "upstream fdbcb877d");
	}

	@Test
	void hittingANeutralCharacterNeverHeals() {
		assertEquals(1, healedHpAfterManyHits(Char.Alignment.NEUTRAL), "upstream fdbcb877d");
	}
}
