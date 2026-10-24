package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.Statistics;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mimic;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A mimic lying in wait has NEUTRAL alignment, not ENEMY, so converting it to an ally with
 * AllyBuff.affectAndLoot (Corruption, Doom-style conversions) skipped the "as if it died" loot
 * and exp block and the mimic's chest contents were lost (upstream 95fb84bd7).
 */
class AllyBuffMimicLootPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240102L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private Mimic hidingMimicNextToHero() {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int cell = hero.pos + offset;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				Mimic mimic = new Mimic();
				mimic.setLevel(Dungeon.depth);
				mimic.pos = cell;
				mimic.sprite = new StubSprite();
				mimic.sprite.ch = mimic;
				//out of the hero's view, so waking it up skips the reveal particles (they need a live GameScene)
				Dungeon.level.heroFOV[cell] = false;
				Dungeon.level.mobs.add(mimic);
				Actor.add(mimic);
				return mimic;
			}
		}
		throw new IllegalStateException("no free cell next to the hero");
	}

	@Test
	void convertingAHidingMimicCountsAsAKillAndRollsItsLoot() {
		Mimic mimic = hidingMimicNextToHero();
		assertSame(Char.Alignment.NEUTRAL, mimic.alignment, "test setup: a mimic lies in wait as NEUTRAL");
		int slainBefore = Statistics.enemiesSlain;

		AllyBuff.affectAndLoot(mimic, hero, Corruption.class);

		assertNotNull(mimic.buff(Corruption.class), "test setup: the mimic must have been converted");
		assertEquals(slainBefore + 1, Statistics.enemiesSlain,
				"the conversion must be processed as a kill (exp and loot) even though the mimic was not ENEMY-aligned");
	}
}
