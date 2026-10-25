package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.MonkEnergy;
import com.qsr.customspd.actors.buffs.Terror;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.stones.StoneOfAggression;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import com.qsr.customspd.ui.QuickSlotButton;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F5 mob-AI pins (roadmap test area #4) on a REAL generated floor: HeadlessRun's floor 1, the
 * hero at its entrance, real terrain, FOV and pathing, the real actor schedule driven by
 * {@link TurnPump}, sprites stubbed by {@link StubSprite}. The floor's own mobs are removed so
 * each pin watches one subject; everything else (hero buffs, blobs, the respawner) keeps acting.
 * The floor is seed-stable (HeadlessRun suppresses the unseeded floor-1 Guidebook drop) and the
 * pins' RNG is a pushed fixed seed, so each pin replays identically.
 */
class MobAiTest {

	private static final long SEED = 20261024L;

	@Test
	void aSleepingRatNextToTheHeroWakesStraightIntoHunting() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Random.pushGenerator(SEED);
			try {
				Rat rat = place(new Rat(), freeNeighbour(hero.pos, -1));
				assertSame(rat.SLEEPING, rat.state, "precondition: a new mob sleeps");

				new TurnPump().runUntil(() -> rat.state != rat.SLEEPING, 300);

				// Waking with the hero in view goes to HUNTING directly (Sleeping.awaken), not WANDERING.
				assertSame(rat.HUNTING, rat.state, "woke into " + stateName(rat));
				assertTrue(rat.isTargeting(hero), "the woken rat targets the hero");
				assertEquals(hero.pos, rat.target, "the woken rat heads for the hero");
				assertTrue(StubSprite.of(rat).alertsShown > 0, "waking shows the alert (Mob.notice)");
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	void terrorMakesAHuntingRatFleeWithGrowingDistance() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Random.pushGenerator(SEED);
			try {
				Rat rat = place(new Rat(), freeNeighbour(hero.pos, -1));
				rat.aggro(hero);
				Buff.affect(rat, Terror.class, Terror.DURATION).object = hero.id();
				assertSame(rat.FLEEING, rat.state, "Mob.add(Terror) switches the state to FLEEING");

				TurnPump pump = new TurnPump();
				List<Integer> distances = new ArrayList<>();
				distances.add(Dungeon.level.distance(rat.pos, hero.pos));
				for (int turn = 0; turn < 6; turn++) {
					pump.untilActed(rat, 50);
					assertSame(rat.FLEEING, rat.state, "turn " + turn + ": still " + stateName(rat));
					distances.add(Dungeon.level.distance(rat.pos, hero.pos));
				}
				for (int i = 1; i < distances.size(); i++) {
					assertTrue(distances.get(i) >= distances.get(i - 1), "a fleeing rat never closes in: " + distances);
				}
				assertTrue(distances.get(distances.size() - 1) >= 4, "six fleeing turns open real distance: " + distances);
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	void anAggressionStoneTargetPullsAHuntingRatOffItsTarget() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Random.pushGenerator(SEED);
			try {
				// The current target is an ally, not the hero, so a failed retarget leaves the rat
				// on the ally.
				Rat rat = place(new Rat(), freeNeighbour(hero.pos, -1));
				Rat ally = place(new Rat(), freeNeighbour(rat.pos, hero.pos));
				ally.alignment = Char.Alignment.ALLY;
				ally.state = ally.PASSIVE;
				rat.aggro(ally);
				Rat decoy = place(new Rat(), freeNeighbour(rat.pos, hero.pos));
				decoy.state = decoy.PASSIVE;
				Buff.prolong(decoy, StoneOfAggression.Aggression.class, StoneOfAggression.Aggression.DURATION);
				assertTrue(rat.isTargeting(ally), "precondition: the rat hunts the ally");

				new TurnPump().untilActed(rat, 10);

				// chooseEnemy's aggression rung outranks the existing target, even another enemy.
				assertTrue(rat.isTargeting(decoy), "the rat must retarget the aggression-marked decoy");
				assertSame(rat.HUNTING, rat.state, stateName(rat));
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	void withTwoAggressionTargetsInViewTheEarlierActorWins() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Random.pushGenerator(SEED);
			try {
				// chooseEnemy takes the first aggression-marked char it meets in Actor.chars().
				// Every round uses new objects, so an identity-hash order would hand the later
				// decoy the pick in about half the rounds.
				for (int round = 0; round < 16; round++) {
					Rat rat = place(new Rat(), freeNeighbour(hero.pos, -1));
					Rat ally = place(new Rat(), freeNeighbour(rat.pos, hero.pos));
					ally.alignment = Char.Alignment.ALLY;
					ally.state = ally.PASSIVE;
					rat.aggro(ally);
					Rat first = place(new Rat(), freeNeighbour(rat.pos, hero.pos));
					Rat second = place(new Rat(), freeNeighbour(rat.pos, hero.pos));
					for (Rat decoy : new Rat[]{first, second}) {
						decoy.state = decoy.PASSIVE;
						Buff.prolong(decoy, StoneOfAggression.Aggression.class, StoneOfAggression.Aggression.DURATION);
					}

					new TurnPump().untilActed(rat, 10);

					assertTrue(rat.isTargeting(first), "round " + round + ": the rat must pick the decoy added first");
					for (Rat r : new Rat[]{rat, ally, first, second}) {
						lift(r);
					}
				}
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	void aMissedAttackResolvesThroughTheMissBranch() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Random.pushGenerator(SEED);
			try {
				Rat rat = place(new Rat(), freeNeighbour(hero.pos, -1));
				Rat defender = place(new Rat(), freeNeighbour(rat.pos, hero.pos));
				defender.state = defender.PASSIVE;
				rat.aggro(defender);
				// Monk Focus: Char.hit treats any defender's evasion as infinite, so this attack must miss.
				Buff.affect(defender, MonkEnergy.MonkAbility.Focus.FocusBuff.class, 30f);
				int hp = defender.HP;

				new TurnPump().untilActed(rat, 10);

				assertTrue(rat.isTargeting(defender), "precondition: the rat went for the defender");
				assertEquals(hp, defender.HP, "a miss deals no damage");
				// Char.attack's miss branch shows the defender's defenseVerb() on its sprite.
				String verb = defender.defenseVerb();
				boolean shown = false;
				for (StubSprite.Status s : StubSprite.of(defender).statuses) {
					shown |= s.color == CharSprite.NEUTRAL && s.text.equals(verb);
				}
				assertTrue(shown, "the defender's sprite shows '" + verb + "': " + StubSprite.of(defender).statuses);
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	void aRatsHitOnTheHeroDamagesAndInterruptsTheHero() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Random.pushGenerator(SEED);
			try {
				Rat rat = place(new Rat(), freeNeighbour(hero.pos, -1));
				rat.aggro(hero);
				int hp = hero.HP;

				// The hero full-rests every turn, so a hit that interrupts shows up as resting = false.
				new TurnPump(h -> {
					Dungeon.observe();
					h.rest(true);
				}).runUntil(() -> hero.HP < hp, 60);

				assertTrue(rat.isTargeting(hero), "precondition: the rat went for the hero");
				assertTrue(hero.isAlive());
				assertFalse(hero.resting, "taking a hit interrupts the hero's rest");
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	void anEnemyComingIntoViewBecomesTheQuickslotTarget() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Rat rat = place(new Rat(), freeNeighbour(hero.pos, -1));
			hero.resting = true;

			// What Hero.act does before taking its action.
			hero.fieldOfView = Dungeon.level.heroFOV;
			Dungeon.observe();
			hero.checkVisibleMobs();

			assertSame(rat, QuickSlotButton.lastTarget, "the newly seen rat becomes the auto-aim target");
			assertFalse(hero.resting, "a new enemy in view interrupts the hero");
		}
	}

	@Test
	void aRatSteppingOntoHighGrassTramplesIt() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Hero hero = floorWithoutMobs(run);
			Random.pushGenerator(SEED);
			try {
				Level level = Dungeon.level;
				int cell = freeNeighbour(hero.pos, -1);
				level.map[cell] = Terrain.HIGH_GRASS;
				level.buildFlagMaps();
				Rat rat = place(new Rat(), cell);

				level.occupyCell(rat);

				assertEquals(Terrain.GRASS, level.map[cell], "the rat tramples the grass flat");
				assertFalse(level.losBlocking[cell], "trampled grass no longer blocks sight");
				// A seed or dew drop would reach Level.drop's Game.scene(); this seed rolls neither.
				assertNull(level.heaps.get(cell), "precondition: the trample dropped nothing");
			} finally {
				Random.popGenerator();
			}
		}
	}

	// ------------------------------------------------------------------------------ helpers

	/** Floor 1 of the run with its generated mobs removed; the hero (stubbed) at the entrance. */
	private static Hero floorWithoutMobs(HeadlessRun run) {
		run.enterFirstLevel();
		for (Mob m : new ArrayList<>(Dungeon.level.mobs)) {
			Dungeon.level.mobs.remove(m);
			Actor.remove(m);
		}
		// A trample can drop a seed or dew, and Level.drop reaches for the live scene (Game.scene())
		// that a headless run does not have. Where the floor's grass falls depends on the generated
		// layout, so any levelgen change could put it under a fleeing rat; flatten it so no seed's
		// layout decides whether a pin can run.
		Level level = Dungeon.level;
		for (int i = 0; i < level.length(); i++) {
			if (level.map[i] == Terrain.HIGH_GRASS || level.map[i] == Terrain.FURROWED_GRASS) {
				level.map[i] = Terrain.GRASS;
			}
		}
		level.buildFlagMaps();
		Dungeon.observe();
		StubSprite.attachAll();
		return Dungeon.hero;
	}

	/** Puts a mob on the level and into the schedule, as GameScene.add does in a live game. */
	private static <T extends Mob> T place(T mob, int cell) {
		mob.pos = cell;
		Dungeon.level.mobs.add(mob);
		Actor.add(mob);
		StubSprite.attach(mob);
		return mob;
	}

	/** Takes a mob and its buffs off the level and out of the schedule. */
	private static void lift(Mob mob) {
		Dungeon.level.mobs.remove(mob);
		for (Buff b : mob.buffs()) {
			Actor.remove(b);
		}
		Actor.remove(mob);
	}

	/** The first walkable, unoccupied cell next to {@code cell}, in NEIGHBOURS8 order. */
	private static int freeNeighbour(int cell, int exclude) {
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = cell + n;
			if (c != exclude && Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null) {
				return c;
			}
		}
		throw new IllegalStateException("no free cell next to " + cell);
	}

	private static String stateName(Mob mob) {
		return mob.state.getClass().getSimpleName();
	}
}
