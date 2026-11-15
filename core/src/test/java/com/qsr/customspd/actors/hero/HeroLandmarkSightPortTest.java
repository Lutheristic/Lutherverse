package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.blobs.Alchemy;
import com.qsr.customspd.actors.blobs.Blob;
import com.qsr.customspd.actors.blobs.Foliage;
import com.qsr.customspd.actors.blobs.SacrificialFire;
import com.qsr.customspd.actors.blobs.WaterOfHealth;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.effects.BlobEmitter;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Landmarks are added when the hero sees them (upstream aa7b64564), not when a blob's evolve finds
 * a visited cell: a blob on a visited cell out of view records nothing, and checkVisibleMobs adds
 * the landmark once any of its cells is in the field of view. Mobs with a landmark work the same.
 */
class HeroLandmarkSightPortTest {

	private HeadlessRun run;
	private Level level;
	private Hero hero;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261117L);
		level = run.enterFirstLevel();
		hero = Dungeon.hero;
		Notes.reset();
		//the hero sees nothing until a test opens a cell
		hero.fieldOfView = new boolean[level.length()];
	}

	@AfterEach
	void tearDown() {
		run.close();
	}

	@Test
	void wellOfHealthIsRecordedOnSightNotOnAVisitedCell() {
		int cell = quietCell();
		WaterOfHealth water = Blob.seed(cell, 1, WaterOfHealth.class, level);
		assertVisitedButUnseenRecordsNothing(water, cell, Notes.Landmark.WELL_OF_HEALTH);
	}

	@Test
	void alchemyIsRecordedOnSightNotOnAVisitedCell() {
		int cell = quietCell();
		Alchemy pot = Blob.seed(cell, 1, Alchemy.class, level);
		assertVisitedButUnseenRecordsNothing(pot, cell, Notes.Landmark.ALCHEMY);
	}

	@Test
	void sacrificialFireIsRecordedOnSightNotOnAVisitedCell() throws Exception {
		int cell = quietCell();
		SacrificialFire fire = Blob.seed(cell, 10, SacrificialFire.class, level);
		//evolve pours into the emitter the scene would have attached
		Field emitter = SacrificialFire.class.getDeclaredField("curEmitter");
		emitter.setAccessible(true);
		emitter.set(fire, new BlobEmitter(fire));
		assertVisitedButUnseenRecordsNothing(fire, cell, Notes.Landmark.SACRIFICIAL_FIRE);
	}

	@Test
	void gardenIsRecordedOnSightNotOnAVisitedCell() {
		level.blobs.remove(Foliage.class);
		int cell = quietCell();
		Foliage foliage = Blob.seed(cell, 1, Foliage.class, level);
		assertVisitedButUnseenRecordsNothing(foliage, cell, Notes.Landmark.GARDEN);
	}

	@Test
	void sacrificialFireStaysRecordedUntilItsLastCellIsSpent() {
		level.blobs.remove(SacrificialFire.class);
		int first = quietCell();
		int second = quietCellAwayFrom(first);
		SacrificialFire fire = Blob.seed(first, 1, SacrificialFire.class, level);
		Blob.seed(second, 1, SacrificialFire.class, level);
		assertEquals(2, fire.volume, "fixture: one unit of fire on each of two cells");
		Notes.add(Notes.Landmark.SACRIFICIAL_FIRE);

		sacrificeRatAt(fire, first);
		assertEquals(1, fire.volume, "fixture: the first cell is spent");
		assertTrue(Notes.contains(Notes.Landmark.SACRIFICIAL_FIRE),
				"the fire is still recorded while another of its cells burns");

		sacrificeRatAt(fire, second);
		assertEquals(0, fire.volume, "fixture: the last cell is spent");
		assertFalse(Notes.contains(Notes.Landmark.SACRIFICIAL_FIRE),
				"the fire is removed once its last cell is spent");
	}

	@Test
	void aMobWithALandmarkIsRecordedOnlyWhenInView() {
		int cell = quietCell();
		Mob mob = new Rat() {
			{
				alignment = Char.Alignment.NEUTRAL;
			}

			@Override
			public Notes.Landmark landmark() {
				return Notes.Landmark.GHOST;
			}
		};
		mob.pos = cell;
		level.mobs.add(mob);

		hero.checkVisibleMobs();
		assertFalse(Notes.contains(Notes.Landmark.GHOST), "a landmark mob out of view must not be recorded");

		hero.fieldOfView[cell] = true;
		hero.checkVisibleMobs();
		assertTrue(Notes.contains(Notes.Landmark.GHOST), "a landmark mob in view must be recorded");
	}

	@Test
	void aMobWithoutALandmarkRecordsNothing() {
		int cell = quietCell();
		Rat rat = new Rat();
		rat.alignment = Char.Alignment.NEUTRAL;
		rat.pos = cell;
		level.mobs.add(rat);
		hero.fieldOfView[cell] = true;

		hero.checkVisibleMobs();

		assertTrue(Notes.getRecords(Notes.Record.class).isEmpty(), "a plain mob in view records nothing: " + Notes.getRecords(Notes.Record.class));
	}

	private void assertVisitedButUnseenRecordsNothing(Blob blob, int cell, Notes.Landmark landmark) {
		level.visited[cell] = true;
		blob.act();
		assertTrue(blob.volume > 0, "fixture: the blob survives its evolve");
		assertFalse(Notes.contains(landmark), landmark + " must not be recorded from a visited cell out of view");

		hero.checkVisibleMobs();
		assertFalse(Notes.contains(landmark), landmark + " must not be recorded while out of view");

		hero.fieldOfView[cell] = true;
		hero.checkVisibleMobs();
		assertTrue(Notes.contains(landmark), landmark + " must be recorded once the hero sees it");
	}

	/**
	 * Sacrifices a rat standing on a fire cell. A rat is worth more than the one unit of fire on the
	 * cell, so the cell is spent. The reward burst after the journal update needs CellEmitter.get,
	 * which needs the game scene, so the headless run stops there.
	 */
	private void sacrificeRatAt(SacrificialFire fire, int cell) {
		Rat rat = new Rat();
		rat.pos = cell;
		try {
			fire.sacrifice(rat);
		} catch (NullPointerException expectedWithoutAScene) {
			//the reward effects run after the fire cell is cleared and the journal updated
		}
	}

	/** A quiet cell whose neighbourhood does not touch {@code other}'s. */
	private int quietCellAwayFrom(int other) {
		for (int cell = 0; cell < level.length(); cell++) {
			if (!level.insideMap(cell) || level.distance(cell, hero.pos) <= 2 || level.distance(cell, other) <= 2) continue;
			boolean quiet = true;
			for (int k : PathFinder.NEIGHBOURS9) {
				if (Actor.findChar(cell + k) != null) {
					quiet = false;
					break;
				}
			}
			if (quiet) return cell;
		}
		throw new IllegalStateException("no second quiet cell on the floor");
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
