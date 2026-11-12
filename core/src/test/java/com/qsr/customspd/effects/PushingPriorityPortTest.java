package com.qsr.customspd.effects;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.wands.WandOfBlastWave;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Upstream 345ddffd1 (v2.2.0): with negative delays gone, Pushing moves to VFX_PRIO+10 so a
 * knockback resolves before any other visual effect queued for the same moment, and the blast
 * wave staff's follow-up moves to VFX_PRIO+9, still after the pushes it waits on but ahead of
 * other effects.
 */
class PushingPriorityPortTest {

	private static final int W = 10;

	/** Any other visual effect actor, at plain VFX priority. */
	private static class OtherEffect extends Actor {
		{
			actPriority = VFX_PRIO;
		}

		@Override
		protected boolean act() {
			Actor.remove(this);
			return true;
		}
	}

	private Rat rat;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		HeadlessLevel.install(W, W);
		Dungeon.hero = null;
		rat = new Rat();
		StubSprite.attach(rat);
		HeadlessLevel.at(rat, 4 * W + 4);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void aPushGoesBeforeOtherEffectsQueuedWithIt() {
		OtherEffect other = new OtherEffect();
		// queued first, so it would win a tie on equal priority
		Actor.add(other);
		Pushing push = new Pushing(rat, rat.pos, rat.pos + 1);
		Actor.add(push);

		TurnPump pump = new TurnPump();
		assertSame(push, pump.step(), "the push resolves first");
		assertSame(other, pump.step());
	}

	@Test
	void theBlastWaveFollowUpComesAfterPushesButBeforeOtherEffects() {
		OtherEffect other = new OtherEffect();
		Actor.add(other);
		Rat attacker = HeadlessLevel.at(new Rat(), 4 * W + 2);
		// a dead defender keeps the follow-up from doing anything but act
		rat.HP = 0;
		new WandOfBlastWave().onHit(null, attacker, rat, 1);
		Pushing push = new Pushing(rat, rat.pos, rat.pos + 1);
		Actor.add(push);

		TurnPump pump = new TurnPump();
		List<String> order = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			order.add(name(pump.step()));
		}

		assertEquals(Arrays.asList("push", "blast wave follow-up", "other effect"), order);
	}

	private String name(Actor a) {
		if (a instanceof Pushing) return "push";
		if (a instanceof OtherEffect) return "other effect";
		if (a.getClass().getEnclosingClass() == WandOfBlastWave.class) return "blast wave follow-up";
		return String.valueOf(a);
	}
}
