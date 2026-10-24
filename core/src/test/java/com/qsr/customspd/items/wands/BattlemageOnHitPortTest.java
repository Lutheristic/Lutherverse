package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroSubClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.MagesStaff;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Battlemage staff on-hit fixes ported from upstream:
 *  - 21a0cf9e8 (v2.4.0): the Empowered Strike tracker must outlive the blast wave on-hit, which
 *    resolves in a delayed actor; detaching it in MagesStaff.proc stripped the empowerment
 *    (Wand.procChanceMultiplier) before the wave read it.
 */
class BattlemageOnHitPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** The anonymous actor WandOfBlastWave.onHit schedules, or null if there is none. */
	private static Actor blastWaveActor() {
		for (Actor a : Actor.all()) {
			if (a.getClass().getName().startsWith("com.qsr.customspd.items.wands.WandOfBlastWave$")
					&& a.getClass().isAnonymousClass()) {
				return a;
			}
		}
		return null;
	}

	@Test
	void empoweredStrikeIsHeldUntilTheBlastWaveHasResolved() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.subClass = HeroSubClass.BATTLEMAGE;
		hero.pos = 3 * level.width() + 3;
		StubSprite.attach(hero);
		Talent.EmpoweredStrikeTracker tracker = Buff.prolong(hero, Talent.EmpoweredStrikeTracker.class, 10f);
		assertNotNull(tracker, "fixture sanity: the tracker attached");

		Rat rat = HeadlessLevel.at(new Rat(), hero.pos + 1);
		rat.HP = 0; // already dead: the delayed wave then skips its elastic push, which needs a scene
		StubSprite.attach(rat);

		MagesStaff staff = new MagesStaff(new WandOfBlastWave());
		staff.proc(hero, rat, 5);

		assertNotNull(hero.buff(Talent.EmpoweredStrikeTracker.class),
				"proc must not detach the tracker while a blast wave on-hit is still pending");
		assertTrue(tracker.delayedDetach, "the blast wave on-hit takes over the detach");
		Actor wave = blastWaveActor();
		assertNotNull(wave, "the blast wave on-hit is scheduled as a delayed actor");

		Method act = wave.getClass().getDeclaredMethod("act");
		act.setAccessible(true);
		act.invoke(wave);

		assertNull(hero.buff(Talent.EmpoweredStrikeTracker.class),
				"once the blast wave has resolved it detaches the tracker");
	}

	@Test
	void anyOtherStaffStillDetachesTheTrackerAtOnce() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.subClass = HeroSubClass.BATTLEMAGE;
		hero.pos = 3 * level.width() + 3;
		StubSprite.attach(hero);
		Buff.prolong(hero, Talent.EmpoweredStrikeTracker.class, 10f);

		Rat rat = HeadlessLevel.at(new Rat(), hero.pos + 1);
		rat.HP = 0;
		StubSprite.attach(rat);

		new MagesStaff(new WandOfMagicMissile()).proc(hero, rat, 5);

		assertNull(hero.buff(Talent.EmpoweredStrikeTracker.class),
				"only blast wave delays the detach; every other wand keeps the immediate one");
		assertFalse(blastWaveActor() != null, "and schedules nothing");
	}
}
