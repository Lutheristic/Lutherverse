package com.qsr.customspd.actors.mobs.npcs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.levels.features.LevelTransition;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.RenderThreadCalls;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 4bbd69a47 and 0c0919f45 (v2.2.0): the sad ghost stays where it spawned. It no longer
 * teleports when you talk to it or heads for the hero once its quest is done, and while it
 * wanders it never picks a cell with a heap on it or the level exit.
 */
class GhostQuestgiverMovementTest {

	private static final long SEED = 20230913L;
	private static final int W = 9;

	@Test
	void wanderingNeverPicksAHeapOrTheExit() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			HeadlessLevel level = HeadlessLevel.install(W, W);
			int exit = 4 * W + 4, free = 3 * W + 5;
			level.transitions.add(new LevelTransition(level, exit, LevelTransition.Type.REGULAR_EXIT, 0));
			assertEquals(exit, level.exit(), "test setup: the exit");
			for (int cell = 0; cell < level.length(); cell++) {
				if (level.passable[cell] && cell != exit && cell != free) level.heaps.put(cell, new Heap());
			}
			Ghost ghost = HeadlessLevel.at(new Ghost(), 2 * W + 2);

			Method randomDestination = Class.forName("com.qsr.customspd.actors.mobs.Mob$Wandering").getDeclaredMethod("randomDestination");
			randomDestination.setAccessible(true);

			int heapPicks = 0, exitPicks = 0, freePicks = 0;
			for (long seed = 1; seed <= 300; seed++) {
				int stock, picked;
				Random.pushGenerator(seed);
				try {
					stock = level.randomDestination(ghost);
				} finally {
					Random.popGenerator();
				}
				Random.pushGenerator(seed);
				try {
					picked = (int) randomDestination.invoke(ghost.WANDERING);
				} finally {
					Random.popGenerator();
				}
				if (stock == exit) exitPicks++;
				else if (level.heaps.get(stock) != null) heapPicks++;
				else freePicks++;
				int expected = stock == exit || level.heaps.get(stock) != null ? -1 : stock;
				assertEquals(expected, picked, "seed " + seed + ": the level offered cell " + stock);
			}
			assertTrue(heapPicks > 0 && exitPicks > 0 && freePicks > 0,
					"the level offered heaps, the exit and the free cell: " + heapPicks + "/" + exitPicks + "/" + freePicks);
		}
	}

	@Test
	void talkingToTheGhostOnceTheQuestIsGivenLeavesItWhereItIs() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Ghost ghost = ghostAndHero(false);
			int before = ghost.pos;

			// a bare scene with an emitter group, so a teleport's light effect would not crash
			Field sceneField = GameScene.class.getDeclaredField("scene");
			sceneField.setAccessible(true);
			Object priorScene = sceneField.get(null);
			GameScene scene = allocate(GameScene.class);
			OpenGroup emitters = new OpenGroup();
			Field emittersField = GameScene.class.getDeclaredField("emitters");
			emittersField.setAccessible(true);
			emittersField.set(scene, emitters);

			List<Callback> posted;
			sceneField.set(null, scene);
			try (RenderThreadCalls calls = RenderThreadCalls.capture()) {
				ghost.interact(Dungeon.hero);
				posted = calls.callbacks();
			} finally {
				sceneField.set(null, priorScene);
			}
			assertEquals(1, posted.size(), "the ghost repeats its request");
			assertEquals(before, ghost.pos, "the ghost does not teleport");
			assertTrue(emitters.all().isEmpty(), "no teleport effect plays");
		}
	}

	@Test
	void onceTheQuestIsDoneTheGhostDoesNotHeadForTheHero() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Ghost ghost = ghostAndHero(true);
			Hero hero = Dungeon.hero;
			assertTrue(Ghost.Quest.processed(), "test setup: the quest is done");

			Field target = Mob.class.getDeclaredField("target");
			target.setAccessible(true);
			TurnPump pump = new TurnPump();
			Random.pushGenerator(SEED);
			try {
				for (int turn = 1; turn <= 10; turn++) {
					pump.untilActed(ghost, 20);
					assertNotEquals(hero.pos, target.getInt(ghost), "ghost turn " + turn + ": the ghost heads for the hero");
				}
			} finally {
				Random.popGenerator();
			}
		}
	}

	/** An open room with the hero in one corner and the quest's ghost in the other. */
	private static Ghost ghostAndHero(boolean processed) throws Exception {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		Hero hero = Dungeon.hero;
		HeadlessLevel.at(hero, W + 1);
		StubSprite.attach(hero);
		Ghost ghost = HeadlessLevel.at(new Ghost(), (W - 2) * W + W - 2);
		level.mobs.add(ghost);
		StubSprite.attach(ghost);

		questField("spawned").setBoolean(null, true);
		questField("given").setBoolean(null, true);
		questField("processed").setBoolean(null, processed);
		Ghost.Quest.weapon = new Sword();
		return ghost;
	}

	/** GameScene's emitter group, with its members readable. */
	private static final class OpenGroup extends Group {
		List<Gizmo> all() {
			return members;
		}
	}

	/** Skips GameScene's constructor, which needs a GL context. */
	private static <T> T allocate(Class<T> type) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return type.cast(unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type));
	}

	private static Field questField(String name) throws Exception {
		Field f = Ghost.Quest.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}
}
