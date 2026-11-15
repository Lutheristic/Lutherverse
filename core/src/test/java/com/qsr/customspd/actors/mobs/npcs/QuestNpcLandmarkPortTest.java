package com.qsr.customspd.actors.mobs.npcs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.AscensionChallenge;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.items.weapon.melee.Dagger;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The quest NPCs name their landmark through Mob.landmark() (upstream aa7b64564), so the hero
 * records them on sight instead of their act() or quest start recording them. The quest ends,
 * the blacksmith's reforge and the ascension departures remove the record.
 */
class QuestNpcLandmarkPortTest {

	private HeadlessRun run;
	private Level level;
	private Hero hero;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261119L);
		level = run.enterFirstLevel();
		hero = Dungeon.hero;
		Notes.reset();
		setReforged(false);
		//the hero sees nothing until a test opens a cell
		hero.fieldOfView = new boolean[level.length()];
	}

	@AfterEach
	void tearDown() {
		setReforged(false);
		run.close();
	}

	@Test
	void questNpcsNameTheirLandmark() {
		assertEquals(Notes.Landmark.GHOST, new Ghost().landmark());
		assertEquals(Notes.Landmark.IMP, new Imp().landmark());
		assertEquals(Notes.Landmark.WANDMAKER, new Wandmaker().landmark());
		assertEquals(Notes.Landmark.TROLL, new Blacksmith().landmark());
	}

	@Test
	void questNpcsAreRecordedOnlyWhenInView() {
		Mob[] npcs = {new Ghost(), new Imp(), new Wandmaker(), new Blacksmith()};
		Notes.Landmark[] marks = {Notes.Landmark.GHOST, Notes.Landmark.IMP,
				Notes.Landmark.WANDMAKER, Notes.Landmark.TROLL};
		int[] cells = new int[npcs.length];
		for (int i = 0; i < npcs.length; i++) {
			cells[i] = quietCell();
			npcs[i].pos = cells[i];
			level.mobs.add(npcs[i]);
			level.visited[cells[i]] = true;
		}

		hero.checkVisibleMobs();
		for (Notes.Landmark mark : marks) {
			assertFalse(Notes.contains(mark), mark + " out of view must not be recorded");
		}

		for (int cell : cells) hero.fieldOfView[cell] = true;
		hero.checkVisibleMobs();
		for (Notes.Landmark mark : marks) {
			assertTrue(Notes.contains(mark), mark + " in view must be recorded");
		}
	}

	@Test
	void blacksmithHasNoLandmarkOnceReforged() {
		Blacksmith smith = new Blacksmith();
		assertEquals(Notes.Landmark.TROLL, smith.landmark(), "an unused blacksmith is a landmark");

		setReforged(true);
		assertNull(smith.landmark(), "a blacksmith who already reforged is no landmark");
	}

	@Test
	void visitedQuestNpcsNoLongerRecordThemselvesInAct() {
		final Ghost ghost = new Ghost();
		final Imp imp = new Imp();
		final Wandmaker wandmaker = new Wandmaker();
		final Blacksmith smith = new Blacksmith();
		Mob[] npcs = {ghost, imp, wandmaker, smith};
		Runnable[] acts = {
				new Runnable() { public void run() { ghost.act(); } },
				new Runnable() { public void run() { imp.act(); } },
				new Runnable() { public void run() { wandmaker.act(); } },
				new Runnable() { public void run() { smith.act(); } }
		};
		//the old act() recorded a visited wandmaker only while his quest wands existed
		Wandmaker.Quest.wand1 = new com.qsr.customspd.items.wands.WandOfMagicMissile();
		try {
			for (Mob npc : npcs) {
				int cell = quietCell();
				npc.pos = cell;
				level.mobs.add(npc);
				level.visited[cell] = true;
				level.heroFOV[cell] = true;
				StubSprite.attach(npc);
			}
			for (int i = 0; i < npcs.length; i++) {
				//no catch: an act() that threw early would skip the old record line unseen
				acts[i].run();
				assertTrue(Notes.getRecords().isEmpty(),
						npcs[i].getClass().getSimpleName() + ".act() must not record a landmark any more");
			}
		} finally {
			Wandmaker.Quest.wand1 = null;
		}
	}

	@Test
	void ascensionDepartureRemovesTheBlacksmithAndGhost() {
		Buff.affect(hero, AscensionChallenge.class);
		Blacksmith smith = new Blacksmith();
		Ghost ghost = new Ghost();
		for (Mob npc : new Mob[]{smith, ghost}) {
			npc.pos = quietCell();
			level.mobs.add(npc);
			StubSprite.attach(npc);
		}
		Notes.add(Notes.Landmark.TROLL);
		Notes.add(Notes.Landmark.GHOST);

		smith.act();
		ghost.act();

		assertFalse(Notes.contains(Notes.Landmark.TROLL), "a blacksmith leaving the ascent removes the troll");
		assertFalse(Notes.contains(Notes.Landmark.GHOST), "a ghost leaving the ascent removes the ghost");
	}

	@Test
	void reforgeStillRemovesTheTroll() {
		StubSprite.attach(hero);
		Sword first = new Sword();
		Dagger second = new Dagger();
		first.collect(hero.belongings.backpack);
		second.collect(hero.belongings.backpack);
		Notes.add(Notes.Landmark.TROLL);

		Blacksmith.upgrade(first, second);

		assertFalse(Notes.contains(Notes.Landmark.TROLL), "a reforge removes the troll");
		assertNull(new Blacksmith().landmark(), "after a reforge the blacksmith is no landmark");
	}

	@Test
	void questEndsStillRemoveTheirLandmark() {
		Notes.add(Notes.Landmark.GHOST);
		Notes.add(Notes.Landmark.IMP);
		Notes.add(Notes.Landmark.WANDMAKER);

		Ghost.Quest.complete();
		Imp.Quest.complete();
		Wandmaker.Quest.complete();

		assertFalse(Notes.contains(Notes.Landmark.GHOST));
		assertFalse(Notes.contains(Notes.Landmark.IMP));
		assertFalse(Notes.contains(Notes.Landmark.WANDMAKER));
	}

	private static void setReforged(boolean reforged) {
		Bundle node = new Bundle();
		node.put("spawned", true);
		node.put("alternative", false);
		node.put("given", true);
		node.put("completed", false);
		node.put("reforged", reforged);
		Bundle bundle = new Bundle();
		bundle.put("blacksmith", node);
		Blacksmith.Quest.restoreFromBundle(bundle);
	}

	/** A cell inside the map, away from the hero and from any character, not yet used. */
	private int quietCell() {
		for (int cell = 0; cell < level.length(); cell++) {
			if (!level.insideMap(cell) || level.distance(cell, hero.pos) <= 2) continue;
			boolean quiet = true;
			for (int k : PathFinder.NEIGHBOURS9) {
				if (Actor.findChar(cell + k) != null || occupied(cell + k)) {
					quiet = false;
					break;
				}
			}
			if (quiet) return cell;
		}
		throw new IllegalStateException("no quiet cell on the floor");
	}

	private boolean occupied(int cell) {
		for (Mob m : level.mobs) {
			if (m.pos == cell) return true;
		}
		return false;
	}
}
