package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.actors.mobs.npcs.PrismaticImage;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The prismatic guardian buff summons an image against the closest visible hunting enemy, and
 * that image could do nothing to an enemy that is invulnerable to it, so it appeared and
 * simply faded. Invulnerable enemies are no longer picked as the trigger (upstream c0f6627fc).
 *
 * The positive spawn path needs a live GameScene (GameScene.add), so the test asserts the
 * negative: with only an invulnerable enemy in view the buff must not summon and stays armed.
 */
class PrismaticGuardInvulnerablePortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240102L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void anInvulnerableHuntingEnemyDoesNotTriggerTheGuardian() throws Exception {
		Rat rat = new Rat() {
			@Override
			public boolean isInvulnerable(Class effect) {
				return true;
			}
		};
		int cell = -1;
		for (int offset : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + offset;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) {
				cell = c;
				break;
			}
		}
		assertTrue(cell >= 0, "test setup: a free cell next to the hero");
		rat.pos = cell;
		rat.sprite = StubSprite.attach(rat);
		rat.state = rat.HUNTING;
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		// checkVisibleMobs drags in the quickslot/target UI; seed the list it would have produced
		Field f = Hero.class.getDeclaredField("visibleEnemies");
		f.setAccessible(true);
		f.set(hero, new ArrayList<>(Collections.singletonList(rat)));
		assertEquals(1, hero.visibleEnemies(), "test setup: the rat must be a visible enemy");
		assertTrue(rat.isInvulnerable(PrismaticImage.class), "test setup: the rat is invulnerable to the image");

		PrismaticGuard guard = Buff.affect(hero, PrismaticGuard.class);
		guard.set(10);
		guard.act();

		assertNotNull(hero.buff(PrismaticGuard.class), "no summon, so the guard stays armed");
		for (Char mob : Dungeon.level.mobs) {
			assertTrue(!(mob instanceof PrismaticImage), "no image may have been summoned");
		}
	}
}
