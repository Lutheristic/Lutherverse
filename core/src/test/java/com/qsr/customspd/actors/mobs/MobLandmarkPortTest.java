package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.npcs.RatKing;
import com.qsr.customspd.actors.mobs.npcs.Shopkeeper;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shops, statues, demon spawners and the rat king name their landmark through Mob.landmark()
 * (upstream aa7b64564), so the hero records them on sight instead of their act() recording them
 * from a visited cell. The removal points stay where they were.
 */
class MobLandmarkPortTest {

	private HeadlessRun run;
	private Level level;
	private Hero hero;
	private String savedLevelName;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261118L);
		level = run.enterFirstLevel();
		hero = Dungeon.hero;
		savedLevelName = Dungeon.levelName;
		Notes.reset();
		//the hero sees nothing until a test opens a cell
		hero.fieldOfView = new boolean[level.length()];
	}

	@AfterEach
	void tearDown() {
		Dungeon.levelName = savedLevelName;
		run.close();
	}

	@Test
	void shopkeeperIsRecordedOnlyWhenInView() {
		int cell = quietCell();
		Shopkeeper keeper = new Shopkeeper();
		keeper.pos = cell;
		level.mobs.add(keeper);
		level.visited[cell] = true;

		hero.checkVisibleMobs();
		assertFalse(Notes.contains(Notes.Landmark.SHOP), "a shopkeeper out of view must not be recorded");

		hero.fieldOfView[cell] = true;
		hero.checkVisibleMobs();
		assertTrue(Notes.contains(Notes.Landmark.SHOP), "a shopkeeper in view must be recorded");
	}

	@Test
	void shopkeeperFleeRemovesTheShop() {
		Shopkeeper keeper = new Shopkeeper();
		keeper.pos = quietCell();
		level.mobs.add(keeper);
		assertEquals(Notes.Landmark.SHOP, keeper.landmark());
		Notes.add(Notes.Landmark.SHOP);

		keeper.flee();

		assertFalse(Notes.contains(Notes.Landmark.SHOP), "a fled shopkeeper removes the shop");
	}

	@Test
	void onlyALevelGenStatueHasALandmark() {
		Statue statue = new Statue();
		statue.levelGenStatue = false;
		assertNull(statue.landmark(), "a statue not placed by levelgen has no landmark");

		statue.levelGenStatue = true;
		assertEquals(Notes.Landmark.STATUE, statue.landmark());
	}

	@Test
	void levelGenStatueIsRecordedOnlyWhenInView() {
		int cell = quietCell();
		Statue statue = new Statue();
		statue.levelGenStatue = true;
		statue.pos = cell;
		level.mobs.add(statue);
		level.visited[cell] = true;

		hero.checkVisibleMobs();
		assertFalse(Notes.contains(Notes.Landmark.STATUE), "a statue out of view must not be recorded");

		hero.fieldOfView[cell] = true;
		hero.checkVisibleMobs();
		assertTrue(Notes.contains(Notes.Landmark.STATUE), "a levelgen statue in view must be recorded");
	}

	@Test
	void statueDestroyRemovesTheStatue() {
		Statue statue = new Statue();
		statue.levelGenStatue = true;
		statue.pos = quietCell();
		level.mobs.add(statue);
		Notes.add(Notes.Landmark.STATUE);

		statue.destroy();

		assertFalse(Notes.contains(Notes.Landmark.STATUE), "a destroyed levelgen statue removes its landmark");
	}

	@Test
	void ratKingHasALandmarkOnlyOnTheRatKingFloor() {
		RatKing king = new RatKing();
		String ratKingLevel = Dungeon.layout.getRatKingLevel();

		Dungeon.levelName = ratKingLevel;
		assertEquals(Notes.Landmark.RAT_KING, king.landmark(), "the rat king on his floor is a landmark");

		Dungeon.levelName = ratKingLevel + "_elsewhere";
		assertNull(king.landmark(), "a rat king summoned elsewhere is no landmark");
	}

	@Test
	void demonSpawnerHasItsLandmarkAndDeathRemovesIt() {
		DemonSpawner spawner = new DemonSpawner();
		assertEquals(Notes.Landmark.DEMON_SPAWNER, spawner.landmark());

		spawner.pos = quietCell();
		spawner.spawnRecorded = true;
		level.mobs.add(spawner);
		Notes.add(Notes.Landmark.DEMON_SPAWNER);

		try {
			spawner.die(null);
		} catch (NullPointerException withoutAScene) {
			//death effects after the journal update need the game scene; an NPE thrown before
			//the journal update is a real failure and must surface as itself
			if (Notes.contains(Notes.Landmark.DEMON_SPAWNER)) {
				throw new AssertionError("spawner death failed before removing its landmark", withoutAScene);
			}
		}

		assertFalse(Notes.contains(Notes.Landmark.DEMON_SPAWNER), "a dead spawner removes its landmark");
	}

	/** A cell inside the map, away from the hero, with no character on it or next to it. */
	private int quietCell() {
		for (int cell = 0; cell < level.length(); cell++) {
			if (!level.insideMap(cell) || level.distance(cell, hero.pos) <= 2) continue;
			boolean quiet = true;
			for (int k : PathFinder.NEIGHBOURS9) {
				if (Actor.findChar(cell + k) != null) {
					quiet = false;
					break;
				}
			}
			if (quiet) return cell;
		}
		throw new IllegalStateException("no quiet cell on the floor");
	}
}
