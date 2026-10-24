package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.effects.Pushing;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dragon kick only knocked its target back (and stunned it) while the target was still alive. A
 * downed ghoul reports alive but inactive, and a killed target was never pushed at all, so the
 * kick now always knocks back and only the stun is gated, on the target being active
 * (upstream e0d703dc5). The knockback itself is the delayed Pushing actor throwChar queues.
 *
 * The oldPos snapshot the new condition reads (skip the knockback if the hit already moved the
 * target) is the MonkEnergy hunk of upstream ca0d8bbda, carried here because e0d703dc5 edits the
 * line it sits on.
 */
class MonkDragonKickKnockbackPortTest {

	private HeadlessRun run;
	private Hero hero;
	private int behind;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231022L);
		run.enterFirstLevel();
		for (Mob m : new ArrayList<>(Dungeon.level.mobs)) {
			Dungeon.level.mobs.remove(m);
			Actor.remove(m);
		}
		StubSprite.attachAll();
		hero = Dungeon.hero;
		Dungeon.observe();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	/** A neighbouring cell with three more open, empty cells in line behind it; returns the neighbour. */
	private int roomyNeighbour() {
		for (int d : PathFinder.NEIGHBOURS8) {
			boolean open = true;
			for (int i = 1; i <= 4 && open; i++) {
				int c = hero.pos + d * i;
				open = c >= 0 && c < Dungeon.level.length()
						&& Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null;
			}
			if (open) {
				behind = hero.pos + d * 2;
				return hero.pos + d;
			}
		}
		throw new IllegalStateException("no open line next to the hero on this floor");
	}

	private Rat ratWithHp(int hp) {
		Rat rat = new Rat();
		rat.HP = hp;
		rat.pos = roomyNeighbour();
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[rat.pos] = true;
		return rat;
	}

	private static int pushings() {
		int n = 0;
		for (Actor a : Actor.all()) {
			if (a instanceof Pushing) n++;
		}
		return n;
	}

	@Test
	void aKickThatKillsTheTargetStillKnocksItBackWithoutStunningIt() {
		Rat rat = ratWithHp(1);
		int before = pushings();
		Random.pushGenerator(20231022L);
		try {
			new MonkEnergy.MonkAbility.DragonKick().doAbility(hero, rat.pos);
		} finally {
			Random.popGenerator();
		}

		assertFalse(rat.isAlive(), "test setup: the kick must kill the one-HP rat");
		assertEquals(before + 1, pushings(), "the dead target is still thrown back along the kick");
		assertNull(rat.buff(Paralysis.class), "a dead target is not stunned");
	}

	@Test
	void aKickThatLeavesTheTargetAliveKnocksItBackAndStunsIt() {
		Rat rat = ratWithHp(1000);
		int before = pushings();
		Random.pushGenerator(20231022L);
		try {
			new MonkEnergy.MonkAbility.DragonKick().doAbility(hero, rat.pos);
		} finally {
			Random.popGenerator();
		}

		assertTrue(rat.isAlive(), "test setup: a 1000-HP rat survives");
		assertEquals(before + 1, pushings());
		assertTrue(rat.buff(Paralysis.class) != null, "control: a living target is still stunned");
	}
}
