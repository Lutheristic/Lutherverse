package com.qsr.customspd.items.weapon.missiles;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.PinCushion;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Ghoul;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.missiles.darts.Dart;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 780966db4 (v2.4.0, "fixed thrown weapons sticking to ghouls as
 * they are downed"). A ghoul that is being life-linked (downed, waiting to be revived) still
 * reports isAlive(), so a missile that killed it stuck to the corpse as a PinCushion and was
 * lost with it. Sticking now requires isActive(), which is false for a downed ghoul, so the
 * missile drops to the floor instead.
 */
class MissileDownedGhoulPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static void down(Ghoul ghoul) throws Exception {
		Field f = Ghoul.class.getDeclaredField("beingLifeLinked");
		f.setAccessible(true);
		f.setBoolean(ghoul, true);
	}

	private static void hit(Dart dart, Char target, int cell) {
		try {
			dart.rangedHit(target, cell);
		} catch (NullPointerException e) {
			// A dropped missile ends in Level.drop() -> Game.scene(), which reads Game.instance; the
			// headless run has no Game. By then the item is already in the heap, which is all the
			// tests read. Only that NPE is tolerated: one thrown anywhere else is a real failure.
			StackTraceElement[] st = e.getStackTrace();
			boolean headlessGame = st.length > 1
					&& st[0].getClassName().endsWith(".Game") && "scene".equals(st[0].getMethodName())
					&& st[1].getClassName().endsWith(".Level") && "drop".equals(st[1].getMethodName());
			if (!headlessGame) {
				throw e;
			}
		}
	}

	@Test
	void aDartThatHitsADownedGhoulIsDroppedNotStuck() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);

		int cell = 4 * level.width() + 6;
		Ghoul ghoul = HeadlessLevel.at(new Ghoul(), cell);
		ghoul.alignment = Char.Alignment.ENEMY;
		StubSprite.attach(ghoul);
		down(ghoul);
		assertTrue(ghoul.isAlive() && !ghoul.isActive(), "fixture: a downed ghoul is alive but not active");

		hit(new Dart(), ghoul, cell);

		assertNull(ghoul.buff(PinCushion.class),
				"upstream 780966db4: nothing sticks to a downed ghoul");
		assertNotNull(level.heaps.get(cell), "the dart fell to the floor");
	}

	@Test
	void aDartThatHitsALiveEnemyStillSticks() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);

		int cell = 4 * level.width() + 6;
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		StubSprite.attach(rat);

		hit(new Dart(), rat, cell);

		assertNotNull(rat.buff(PinCushion.class), "control: darts still stick to active enemies");
	}
}
