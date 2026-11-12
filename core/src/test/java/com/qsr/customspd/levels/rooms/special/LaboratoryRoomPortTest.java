package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.EnergyCrystal;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.potions.PotionOfStrength;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.secret.SecretLaboratoryRoom;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.test.GameClasses;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StaticSnapshot;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Alchemy rooms painted on a headless level, one 7x7 room with a door on its left wall per seed.
 *
 * Upstream 21804614e cut the free alchemy energy: a laboratory room drops a flat 5 energy, a
 * secret laboratory two piles of 3-5, and an energy crystal no longer rolls its own 4-6 quantity.
 *
 * Upstream 2b6d21be5: of the items queued to spawn on the floor, a laboratory only takes a potion
 * of strength as its prize, not any potion.
 */
class LaboratoryRoomPortTest {

	private static final int SEEDS = 30;

	private StaticSnapshot statics;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void capture() {
		// the snapshot covers the engine too, so the bare Game below (Level.drop asks Game.scene()) is undone
		statics = StaticSnapshot.capture(GameClasses.all());
		new Game(null, null);
		Dungeon.depth = 3;
		Dungeon.levelName = "3";
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		statics.restore();
	}

	private interface RoomMaker {
		SpecialRoom make();
	}

	private static HeadlessLevel paint(RoomMaker maker, long seed, Item... queued) {
		HeadlessLevel level = HeadlessLevel.install(11, 11);
		for (Item item : queued) level.addItemToSpawn(item);
		Random.pushGenerator(seed);
		try {
			Generator.fullReset();
			SpecialRoom room = maker.make();
			room.set(2, 2, 8, 8);
			room.connected.put(new EmptyRoom(), new Room.Door(room.left, room.top + 3));
			room.paint(level);
		} finally {
			Random.popGenerator();
		}
		return level;
	}

	private static boolean dropped(HeadlessLevel level, Item item) {
		for (Heap h : level.heaps.valueList()) {
			for (Item i : h.items) {
				if (i == item) return true;
			}
		}
		return false;
	}

	private static List<Integer> energyPiles(HeadlessLevel level) {
		List<Integer> piles = new ArrayList<>();
		for (Heap h : level.heaps.valueList()) {
			for (Item item : h.items) {
				if (item instanceof EnergyCrystal) piles.add(item.quantity());
			}
		}
		return piles;
	}

	@Test
	void aLaboratoryDropsAFlatFiveEnergy() {
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<Integer> piles = energyPiles(paint(LaboratoryRoom::new, seed));
			assertEquals(1, piles.size(), "seed " + seed + ": one energy pile");
			assertEquals(5, (int) piles.get(0), "seed " + seed + ": energy in the laboratory");
		}
	}

	@Test
	void aSecretLaboratoryDropsTwoPilesOfThreeToFive() {
		boolean sawThree = false;
		for (long seed = 1; seed <= SEEDS; seed++) {
			List<Integer> piles = energyPiles(paint(SecretLaboratoryRoom::new, seed));
			assertEquals(2, piles.size(), "seed " + seed + ": two energy piles");
			for (int q : piles) {
				assertTrue(q >= 3 && q <= 5, "seed " + seed + ": a pile of " + q);
				sawThree |= q == 3;
			}
		}
		assertTrue(sawThree, "no pile of 3 in " + SEEDS + " seeds");
	}

	@Test
	void anEnergyCrystalKeepsItsQuantityWhenRandomized() {
		Random.pushGenerator(5L);
		try {
			assertEquals(1, new EnergyCrystal().random().quantity());
			assertEquals(7, new EnergyCrystal(7).random().quantity());
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void aLaboratoryOnlyTakesAStrengthPotionFromTheSpawnQueue() {
		for (long seed = 1; seed <= 10; seed++) {
			PotionOfHealing healing = new PotionOfHealing();
			assertFalse(dropped(paint(LaboratoryRoom::new, seed, healing), healing),
					"seed " + seed + ": a queued potion of healing became the laboratory's prize");

			PotionOfStrength strength = new PotionOfStrength();
			assertTrue(dropped(paint(LaboratoryRoom::new, seed, strength), strength),
					"seed " + seed + ": a queued potion of strength is still taken");
		}
	}
}
