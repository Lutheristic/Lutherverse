package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.enchantments.Projecting;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An empowered flurry of blows takes on the equipped weapon's enchantment, but it only attached
 * the tracker saying so after it had already asked whether the target was in reach, so a
 * Projecting weapon's extra reach never counted. The tracker is now attached before the reach
 * check, and removed again if the check refuses the attack (upstream f7c6a2ad2).
 */
class MonkFlurryProjectingPortTest {

	private HeadlessRun run;
	private Hero hero;
	private int dir;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240306L);
		run.enterFirstLevel();
		for (Mob m : new ArrayList<>(Dungeon.level.mobs)) {
			Dungeon.level.mobs.remove(m);
			Actor.remove(m);
		}
		StubSprite.attachAll();
		hero = Dungeon.hero;
		Dungeon.observe();

		hero.talents.get(0).put(Talent.MONASTIC_VIGOR, 3); // empowered from 60% energy
		MonkEnergy energy = Buff.affect(hero, MonkEnergy.class);
		energy.energy = energy.energyCap();
		Sword sword = new Sword();
		sword.enchant(new Projecting());
		hero.belongings.weapon = sword;

		dir = openLineDirection();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	/** A direction with four open, empty cells in a line from the hero. */
	private int openLineDirection() {
		for (int d : PathFinder.NEIGHBOURS8) {
			boolean open = true;
			for (int i = 1; i <= 4 && open; i++) {
				int c = hero.pos + d * i;
				open = c >= 0 && c < Dungeon.level.length()
						&& Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null;
			}
			if (open) return d;
		}
		throw new IllegalStateException("no open line next to the hero on this floor");
	}

	private Rat ratAt(int distance) {
		Rat rat = new Rat();
		rat.HP = rat.HT = 1000;
		rat.pos = hero.pos + dir * distance;
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[rat.pos] = true;
		return rat;
	}

	private void flurry(Rat rat) {
		Random.pushGenerator(20240306L);
		try {
			new MonkEnergy.MonkAbility.Flurry().doAbility(hero, rat.pos);
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void anEmpoweredFlurryReachesTwoCellsWithAProjectingWeapon() {
		Rat rat = ratAt(2);

		flurry(rat);

		assertTrue(rat.HP < rat.HT, "the flurry must have hit the rat two cells away; HP " + rat.HP);
		assertNull(hero.buff(MonkEnergy.MonkAbility.FlurryEmpowerTracker.class), "the tracker is spent with the ability");
	}

	@Test
	void aRefusedFlurryLeavesNoEmpowerTrackerBehind() {
		Rat rat = ratAt(4);

		flurry(rat);

		assertEquals(rat.HT, rat.HP, "test setup: four cells is out of reach, nothing is hit");
		assertNull(hero.buff(MonkEnergy.MonkAbility.FlurryEmpowerTracker.class),
				"a refused ability must not leave its empower tracker granting weapon enchants");
		assertNull(hero.buff(MonkEnergy.MonkAbility.UnarmedAbilityTracker.class));
	}
}
