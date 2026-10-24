package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.abilities.Ratmogrify;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * When a transmogrified rat expires, the mob it hands back must have its action time cleared.
 * Actor.add adds the current game time to the actor's stored time, so a restored mob keeping its
 * old absolute time would sit idle for as many turns as had elapsed in the game (upstream
 * ab57f83ee, a follow-up to c9bed0e0c which had dropped the clearTime calls).
 */
class RatmogrifyClearTimePortTest {

	private HeadlessRun run;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20230612L);
		run.enterFirstLevel();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	/** A rat whose stored action time is far in the future, as one that acted many turns ago. */
	private static final class BusyRat extends Rat {
		{
			spendConstant(500f);
		}
	}

	/** A transmog rat whose six-turn timer has already run out. */
	private static final class ExpiredTransmogRat extends Ratmogrify.TransmogRat {
		{
			spend(6f);
		}

		boolean runAct() {
			return act();
		}
	}

	@Test
	void anExpiringTransmogRatReturnsItsOriginalWithTheActionTimeCleared() {
		Rat original = new BusyRat();
		original.pos = Dungeon.hero.pos;
		ExpiredTransmogRat rat = new ExpiredTransmogRat();
		rat.setup(original);
		rat.pos = Dungeon.hero.pos;
		StubSprite.attach(rat);
		assertEquals(500f - Actor.now(), original.cooldown(), 0f, "precondition: original carries a stale action time");

		try {
			rat.runAct();
		} catch (NullPointerException noScene) {
			// act() finishes with CellEmitter.get(...).burst(...), which needs a live GameScene.
			// The original is handed back (and its time cleared) before that point.
		}

		assertNull(rat.getOriginal(), "the rat dropped its reference to the original");
		assertEquals(-Actor.now(), original.cooldown(), 0f, "action time cleared");
	}
}
