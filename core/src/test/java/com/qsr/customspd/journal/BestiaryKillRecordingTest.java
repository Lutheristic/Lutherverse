package com.qsr.customspd.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.AllyBuff;
import com.qsr.customspd.actors.buffs.Corruption;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.abilities.Ratmogrify;
import com.qsr.customspd.actors.mobs.Crab;
import com.qsr.customspd.actors.mobs.Gnoll;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.actors.mobs.Snake;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bestiary records a kill when an enemy is destroyed with the hero alive, when an enemy is
 * converted into an ally with AllyBuff.affectAndLoot, and for Ratmogrify's transmogrified rats:
 * making one an ally credits the original mob, and so does destroying one that is still an enemy.
 */
class BestiaryKillRecordingTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() throws ReflectiveOperationException {
		run = HeadlessRun.start(20241115L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
		clearBestiary();
		Bestiary.skipCountingEncounters = false;
	}

	@AfterEach
	void closeRun() throws ReflectiveOperationException {
		try {
			clearBestiary();
		} finally {
			run.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static void clearBestiary() throws ReflectiveOperationException {
		Field seen = Bestiary.class.getDeclaredField("seen");
		Field counts = Bestiary.class.getDeclaredField("encounterCount");
		seen.setAccessible(true);
		counts.setAccessible(true);
		for (Bestiary cat : Bestiary.values()) {
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
			for (Map.Entry<Class<?>, Integer> e : ((Map<Class<?>, Integer>) counts.get(cat)).entrySet()) {
				e.setValue(0);
			}
		}
	}

	private <T extends Mob> T nextToHero(T mob) {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int cell = hero.pos + offset;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				mob.pos = cell;
				StubSprite.attach(mob);
				Dungeon.level.mobs.add(mob);
				Actor.add(mob);
				return mob;
			}
		}
		throw new IllegalStateException("no free cell next to the hero");
	}

	private static void assertUnrecorded(Class<?> cls) {
		assertFalse(Bestiary.isSeen(cls), "test setup: " + cls.getSimpleName() + " starts unseen");
		assertEquals(0, Bestiary.encounterCount(cls), "test setup: " + cls.getSimpleName() + " starts uncounted");
	}

	@Test
	void killingAnEnemyRecordsIt() {
		assertUnrecorded(Rat.class);
		Rat rat = nextToHero(new Rat());
		assertSame(Char.Alignment.ENEMY, rat.alignment);
		assertTrue(hero.isAlive());

		rat.die(hero);

		assertTrue(Bestiary.isSeen(Rat.class), "a killed rat is seen");
		assertEquals(1, Bestiary.encounterCount(Rat.class), "a killed rat is counted once");
	}

	@Test
	void corruptingAnEnemyRecordsIt() {
		assertUnrecorded(Gnoll.class);
		Gnoll gnoll = nextToHero(new Gnoll());
		//over-levelled, so rollToDropLoot skips the gold drop (Level.drop needs a live GameScene)
		hero.lvl = gnoll.maxLvl + 3;

		AllyBuff.affectAndLoot(gnoll, hero, Corruption.class);

		assertNotNull(gnoll.buff(Corruption.class), "test setup: the gnoll was converted");
		assertTrue(Bestiary.isSeen(Gnoll.class), "a corrupted gnoll is seen");
		assertEquals(1, Bestiary.encounterCount(Gnoll.class), "a corrupted gnoll is counted once");
	}

	@Test
	void makingATransmogRatAnAllyCountsTheOriginal() {
		assertUnrecorded(Snake.class);
		Ratmogrify.TransmogRat rat = new Ratmogrify.TransmogRat();
		rat.setup(new Snake());

		rat.makeAlly();

		assertTrue(Bestiary.isSeen(Snake.class), "the original of an allied transmog rat is seen");
		assertEquals(1, Bestiary.encounterCount(Snake.class), "the original of an allied transmog rat is counted");
	}

	@Test
	void destroyingAnEnemyTransmogRatCountsTheOriginal() {
		assertUnrecorded(Crab.class);
		Ratmogrify.TransmogRat rat = new Ratmogrify.TransmogRat();
		rat.setup(new Crab());
		nextToHero(rat);
		assertSame(Char.Alignment.ENEMY, rat.alignment);

		rat.destroy();

		assertTrue(Bestiary.isSeen(Crab.class), "the original of a destroyed enemy transmog rat is seen");
		assertEquals(1, Bestiary.encounterCount(Crab.class), "the original of a destroyed enemy transmog rat is counted");
	}

	@Test
	void destroyingATransmogRatWithNoOriginalDoesNotThrow() {
		Ratmogrify.TransmogRat rat = nextToHero(new Ratmogrify.TransmogRat());
		assertSame(Char.Alignment.ENEMY, rat.alignment);

		assertDoesNotThrow(rat::destroy);
	}
}
