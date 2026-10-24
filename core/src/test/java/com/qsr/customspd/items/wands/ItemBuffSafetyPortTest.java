package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.abilities.duelist.Challenge;
import com.qsr.customspd.items.artifacts.CloakOfShadows;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 15e16828a (v2.5.0, in its v2.5.4 form) "added a few safety checks to item buff
 * checking": an item whose buff never attached (target still null) must not be dereferenced.
 * In the commit itself the wand guard read {@code target == null}, which is inverted; v2.5.4
 * carries the corrected {@code target != null}, which is what the port follows.
 */
class ItemBuffSafetyPortTest {

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
	void reactivatingAnArtifactWhoseBuffNeverAttachedDoesNotThrow() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		// a spectator-frozen char refuses new buffs (Char.add returns false), so the artifact's
		// passive buff is created but left without a target
		Buff.affect(hero, Challenge.SpectatorFreeze.class);

		CloakOfShadows cloak = new CloakOfShadows();
		cloak.activate(hero);
		assertDoesNotThrow(() -> cloak.activate(hero),
				"the stale, never-attached passive buff must not be detached");
	}

	@Test
	void chargesPerCastToleratesAChargerWithNoTarget() {
		WandOfRegrowth regrowth = new WandOfRegrowth();
		regrowth.charger = regrowth.new Charger();
		assertEquals(1, assertDoesNotThrow(() -> regrowth.chargesPerCast()));

		WandOfFireblast fireblast = new WandOfFireblast();
		fireblast.charger = fireblast.new Charger();
		assertEquals(1, assertDoesNotThrow(() -> fireblast.chargesPerCast()));
	}
}
