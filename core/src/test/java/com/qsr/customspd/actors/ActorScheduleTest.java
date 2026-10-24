package com.qsr.customspd.actors;

import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.StaticSnapshot;
import com.qsr.customspd.test.TurnPump;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Actor scheduling order (roadmap near-miss "Actor scheduler time/priority ordering"), on the
 * real Actor set driven by {@link TurnPump}: earliest time first, a higher actPriority first on
 * equal time (VFX > hero > blobs > mobs > buffs), and the order actors were added on a full tie.
 * The last is the determinism contract behind Actor.all being a LinkedHashSet: Actor does not
 * override hashCode, so a hash-ordered set would resolve a turn differently on every JVM run.
 */
class ActorScheduleTest {

	private StaticSnapshot saved;
	private final List<String> log = new ArrayList<>();

	@BeforeEach
	void isolate() {
		HeadlessGdx.boot();
		saved = StaticSnapshot.capture(Collections.<Class<?>>singletonList(Actor.class));
		Actor.clear();
	}

	@AfterEach
	void restore() {
		saved.restore();
	}

	@Test
	void earliestFirstThenHigherPriorityOnEqualTime() {
		// Added deliberately out of priority order.
		Actor.add(new Recorder("buff", Actor.BUFF_PRIO, 1f));
		Actor.add(new Recorder("fast", Actor.MOB_PRIO, 0.5f));
		Actor.add(new Recorder("mob", Actor.MOB_PRIO, 1f));
		Actor.add(new Recorder("blob", Actor.BLOB_PRIO, 1f));
		Actor.add(new Recorder("hero", Actor.HERO_PRIO, 1f));

		TurnPump pump = new TurnPump();
		for (int i = 0; i < 12; i++) pump.step();

		assertEquals(Arrays.asList(
				"hero@0.0", "blob@0.0", "fast@0.0", "mob@0.0", "buff@0.0",
				"fast@0.5",
				"hero@1.0", "blob@1.0", "fast@1.0", "mob@1.0", "buff@1.0",
				"fast@1.5"), log);
	}

	@Test
	void fullTiesResolveInTheOrderActorsWereAdded() {
		List<String> added = new ArrayList<>();
		for (int i = 0; i < 64; i++) {
			Actor.add(new Recorder("m" + i, Actor.MOB_PRIO, 1f));
			added.add("m" + i + "@0.0");
		}

		TurnPump pump = new TurnPump();
		for (int i = 0; i < 64; i++) pump.step();

		assertEquals(added, log, "equal time and priority must act in insertion order");
	}

	/** Logs name@now on each turn and spends a fixed delay. */
	private final class Recorder extends Actor {
		private final String name;
		private final float delay;

		Recorder(String name, int priority, float delay) {
			this.name = name;
			this.delay = delay;
			actPriority = priority;
		}

		@Override
		protected boolean act() {
			log.add(name + "@" + Actor.now());
			spend(delay);
			return true;
		}
	}
}
