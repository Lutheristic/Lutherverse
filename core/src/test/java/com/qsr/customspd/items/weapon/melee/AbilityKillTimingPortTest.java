package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 11a410f07 (v2.4.0): cleave and retribution take no time when they kill, but still end
 * the hero's turn. A cleave kill opens a 4-turn free follow-up (one less than before, as the kill
 * was instant), and a kill made with that free cleave does not open another.
 */
class AbilityKillTimingPortTest {

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

	public static class SturdyRat extends Rat {
		{
			HP = HT = 1000;
		}
	}

	// A kill must still end the hero's turn, or the actor loop waits on the hero forever.
	public static class CountingHero extends Hero {
		int nextCalls;
		int spendAndNextCalls;

		@Override
		public void next() {
			nextCalls++;
			super.next();
		}

		@Override
		public void spendAndNext(float time) {
			spendAndNextCalls++;
			super.spendAndNext(time);
		}
	}

	private static CountingHero heroWith(MeleeWeapon weapon) {
		HeadlessLevel.install(W, 10);
		CountingHero hero = new CountingHero();
		hero.STR = 20;
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 4);
		hero.belongings.weapon = weapon;
		Buff.affect(hero, MeleeWeapon.Charger.class).charges = 2;
		return hero;
	}

	private static <T extends Rat> T ratAt(T rat, int cell, int hp) {
		HeadlessLevel.at(rat, cell);
		StubSprite.attach(rat);
		rat.HP = hp;
		Dungeon.level.heroFOV[cell] = true;
		return rat;
	}

	@Test
	void aCleaveKillIsInstantAndOpensAFourTurnFollowUp() {
		Sword sword = new Sword();
		CountingHero hero = heroWith(sword);
		int cell = 4 * W + 5;
		Rat rat = ratAt(new Rat(), cell, 1);
		float before = hero.cooldown();

		sword.duelistAbility(hero, cell);

		assertFalse(rat.isAlive(), "fixture: the cleave kills");
		assertEquals(before, hero.cooldown(), 0.0001f, "a killing cleave takes no time");
		assertEquals(1, hero.nextCalls, "a killing cleave still ends the turn");
		assertEquals(0, hero.spendAndNextCalls);
		Sword.CleaveTracker followUp = hero.buff(Sword.CleaveTracker.class);
		assertNotNull(followUp);
		assertEquals(4f, followUp.cooldown(), 0.0001f, "4 turns, one less than before as the kill was instant");
	}

	@Test
	void aKillWithTheFreeCleaveDoesNotOpenAnother() {
		Sword sword = new Sword();
		CountingHero hero = heroWith(sword);
		Buff.prolong(hero, Sword.CleaveTracker.class, 4f);
		int cell = 4 * W + 5;
		ratAt(new Rat(), cell, 1);

		sword.duelistAbility(hero, cell);

		assertNull(hero.buff(Sword.CleaveTracker.class), "the follow-up is used up, not renewed");
		assertEquals(1, hero.nextCalls, "a killing free cleave still ends the turn");
		assertEquals(0, hero.spendAndNextCalls);
	}

	@Test
	void aCleaveThatDoesNotKillTakesAnAttack() {
		Sword sword = new Sword();
		CountingHero hero = heroWith(sword);
		int cell = 4 * W + 5;
		ratAt(new SturdyRat(), cell, 1000);
		float before = hero.cooldown();

		sword.duelistAbility(hero, cell);

		assertEquals(before + hero.attackDelay(), hero.cooldown(), 0.0001f);
		assertEquals(1, hero.spendAndNextCalls, "the turn ends through spendAndNext");
		assertEquals(1, hero.nextCalls);
		assertNull(hero.buff(Sword.CleaveTracker.class));
	}

	@Test
	void aRetributionKillIsInstant() {
		Greataxe axe = new Greataxe();
		CountingHero hero = heroWith(axe);
		hero.HP = hero.HT / 4;
		int cell = 4 * W + 5;
		Rat rat = ratAt(new Rat(), cell, 1);
		float before = hero.cooldown();

		axe.duelistAbility(hero, cell);

		assertFalse(rat.isAlive(), "fixture: retribution kills");
		assertEquals(before, hero.cooldown(), 0.0001f, "a killing retribution takes no time");
		assertEquals(1, hero.nextCalls, "a killing retribution still ends the turn");
		assertEquals(0, hero.spendAndNextCalls);
	}

	@Test
	void aRetributionThatDoesNotKillTakesAnAttack() {
		Greataxe axe = new Greataxe();
		CountingHero hero = heroWith(axe);
		hero.HP = hero.HT / 4;
		int cell = 4 * W + 5;
		Rat rat = ratAt(new SturdyRat(), cell, 1000);
		float before = hero.cooldown();

		axe.duelistAbility(hero, cell);

		assertTrue(rat.isAlive());
		assertEquals(before + hero.attackDelay(), hero.cooldown(), 0.0001f);
		assertEquals(1, hero.spendAndNextCalls, "the turn ends through spendAndNext");
		assertEquals(1, hero.nextCalls);
	}
}
