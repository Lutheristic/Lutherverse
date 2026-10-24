package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.abilities.Ratmogrify;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * When a transmogrified rat expires it hands its original mob back and drops its own reference to
 * it, so an ascension kill callback for the leftover rat (or a rat whose original never survived
 * a save/load) used to dereference a null original and crash (upstream c9bed0e0c).
 */
class RatmogrifyAscensionExpiryPortTest {

	private HeadlessRun run;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20230605L);
		run.enterFirstLevel();
		Buff.affect(Dungeon.hero, AscensionChallenge.class);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void getOriginalOfARatWithNoOriginalIsNullNotACrash() {
		Ratmogrify.TransmogRat rat = new Ratmogrify.TransmogRat();
		assertNull(rat.getOriginal());
	}

	@Test
	void ascensionKillOfAnExpiredTransmogRatIsIgnored() {
		Ratmogrify.TransmogRat rat = new Ratmogrify.TransmogRat();
		AscensionChallenge.processEnemyKill(rat); // must not throw
	}

	@Test
	void getOriginalStillCopiesHpAndPositionBackWhenThereIsAnOriginal() {
		Rat original = new Rat();
		Ratmogrify.TransmogRat rat = new Ratmogrify.TransmogRat();
		rat.setup(original);
		rat.HP = 1;
		rat.pos = 42;

		assertSame(original, rat.getOriginal());
		assertEquals(1, original.HP);
		assertEquals(42, original.pos);
	}
}
