package com.qsr.customspd.actors.mobs.npcs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.items.weapon.melee.Sword;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 4bbd69a47 (v2.2.0): the sad ghost stays where it spawned. It no longer teleports
 * when you talk to it or heads for the hero once its quest is done.
 */
class GhostQuestgiverMovementTest {

	private static final long SEED = 20230913L;
	private static final int W = 9;

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
