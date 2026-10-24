package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.glyphs.Viscosity.DeferedDamage;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Port regression for upstream c8b4b87e0 (v2.5.0, "fixed deferred damage being postponed on
 * save/load"). Viscosity's DeferedDamage postponed itself one turn from attachTo, and restoring a
 * saved char re-attaches its buffs, so every save/load pushed the next tick of deferred damage
 * back a turn. The "wait one turn when freshly applied" rule now lives in prolong (taken only
 * while the buff holds no damage yet), which a restore never calls.
 */
class ViscositySaveLoadPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static void setActorTime(Actor actor, float time) throws Exception {
		Field f = Actor.class.getDeclaredField("time");
		f.setAccessible(true);
		f.setFloat(actor, time);
	}

	private Rat rat() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Rat rat = HeadlessLevel.at(new Rat(), 4 * level.width() + 4);
		StubSprite.attach(rat);
		return rat;
	}

	@Test
	void freshlyAppliedDeferredDamageWaitsOneTurn() {
		Rat rat = rat();
		DeferedDamage deferred = Buff.affect(rat, DeferedDamage.class);
		deferred.prolong(10);
		assertEquals(Actor.TICK, deferred.cooldown(), 0.0001f,
				"a fresh application is still postponed by a turn (now from prolong)");
	}

	@Test
	void furtherDeferredDamageDoesNotPostponeAgain() throws Exception {
		Rat rat = rat();
		DeferedDamage deferred = Buff.affect(rat, DeferedDamage.class);
		deferred.prolong(10);
		setActorTime(deferred, 0f); // it is due now
		deferred.prolong(5);
		assertEquals(0f, deferred.cooldown(), 0.0001f,
				"stacking more damage onto a running DeferedDamage does not delay its next tick");
	}

	@Test
	void aSaveLoadDoesNotPostponeADueDeferredDamage() throws Exception {
		Rat rat = rat();
		DeferedDamage deferred = Buff.affect(rat, DeferedDamage.class);
		deferred.prolong(10);
		setActorTime(deferred, 0f); // it is due now, as when the player saved mid-drain

		Rat restored = SaveRoundtrip.of(rat);

		DeferedDamage back = restored.buff(DeferedDamage.class);
		assertNotNull(back, "fixture: the buff survived the save");
		assertEquals(0f, back.cooldown(), 0.0001f,
				"upstream c8b4b87e0: restoring does not re-postpone the next tick by a turn");
	}
}
