package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Buffs that grant immunity to a status effect must also strip that effect the moment they
 * attach: immunity only blocks <i>new</i> applications, so an already-burning hero who drank
 * a fire-immunity potion kept burning (and kept the flame fx) until it ran out (upstream
 * 88a80fb28).
 */
class ImmunityBuffClearsFxPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240601L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private <T extends Buff> void assertClearedOnAttach(Class<? extends Buff> effect, Class<T> immunityGranter) {
		Buff.affect(hero, effect);
		assertNotNull(hero.buff(effect), "test setup: " + effect.getSimpleName() + " must be attached first");
		Buff.affect(hero, immunityGranter);
		assertNotNull(hero.buff(immunityGranter), "test setup: " + immunityGranter.getSimpleName() + " must attach");
		assertNull(hero.buff(effect), immunityGranter.getSimpleName() + " must clear an existing " + effect.getSimpleName());
		//the granter makes the hero immune to the next effect under test, so remove it between checks
		Buff.detach(hero, immunityGranter);
	}

	@Test
	void fireImbueClearsBurning() {
		assertClearedOnAttach(Burning.class, FireImbue.class);
	}

	@Test
	void toxicImbueClearsPoison() {
		assertClearedOnAttach(Poison.class, ToxicImbue.class);
	}

	@Test
	void frostImbueClearsFrostAndChill() {
		assertClearedOnAttach(Frost.class, FrostImbue.class);
		assertClearedOnAttach(Chill.class, FrostImbue.class);
	}

	@Test
	void ankhInvulnerabilityClearsParalysisAndFrost() {
		assertClearedOnAttach(Paralysis.class, AnkhInvulnerability.class);
		assertClearedOnAttach(Frost.class, AnkhInvulnerability.class);
	}
}
