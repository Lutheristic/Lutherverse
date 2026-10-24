package com.qsr.customspd.effects;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.enchantments.Elastic;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream a8c429c9f (v2.4.0, "fixed elastic enchant and spike ability
 * competing with eachother"). Spear's spike ability now declines to push an enemy while another
 * push is already pending on it, asking {@link Pushing#pushingExistsForChar}. The spike itself
 * cannot be driven headlessly (its attack callback targets the HUD's AttackIndicator before the
 * hit), so this pins the contract the spear relies on: an Elastic knockback queues a pending
 * Pushing for the knocked char (the move itself happens when that actor acts), and only for
 * that char, until it is consumed.
 */
class PushingExistsPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void anElasticKnockbackLeavesAPendingPushForTheKnockedCharOnly() {
		HeadlessLevel level = HeadlessLevel.install(12, 12);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 6 * level.width() + 2);
		Rat rat = HeadlessLevel.at(new Rat(), 6 * level.width() + 3);
		StubSprite.attach(rat);
		Rat bystander = HeadlessLevel.at(new Rat(), 2 * level.width() + 8);
		StubSprite.attach(bystander);
		rat.HP = rat.HT = 1000;

		assertFalse(Pushing.pushingExistsForChar(rat), "fixture: nothing pending yet");

		new Elastic().proc(1f, 3, hero, rat, 1); // a certain proc

		assertTrue(Pushing.pushingExistsForChar(rat),
				"upstream a8c429c9f: the knockback is visible to anything that wants to push the rat again");
		assertFalse(Pushing.pushingExistsForChar(bystander));
		assertFalse(Pushing.pushingExistsForChar(hero));

		// Acting consumes the effect (Pushing removes itself from the actor list), after which
		// the char may be pushed again.
		for (Actor a : Actor.all()) {
			if (a instanceof Pushing) {
				Actor.remove(a);
			}
		}
		assertFalse(Pushing.pushingExistsForChar(rat));
	}
}
