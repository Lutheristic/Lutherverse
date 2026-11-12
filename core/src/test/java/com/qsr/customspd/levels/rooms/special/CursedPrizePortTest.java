package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.features.Maze;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.secret.SecretMazeRoom;
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

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 9ebf0357e: the pool, sentry, traps and secret maze rooms keep the weapon or armor they
 * roll first and strip its curse, instead of throwing a cursed roll away and rolling again.
 *
 * Each case replays the room's own draws up to that first roll from the same seed (and the same
 * Generator decks), then checks the room's prize is that item, uncursed. About 30% of first rolls
 * are cursed, which is where the old re-roll gave a different item.
 */
class CursedPrizePortTest {

	private static final int SEEDS = 40;
	private static final int FLOOR_SET = 3 / 5 + 1;

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

	/** The weapon-or-armor roll every one of these rooms makes, from wherever the stream is now. */
	private static Item firstRoll(boolean weaponUsesDefaults) {
		if (Random.Int(2) == 0) {
			return Generator.randomWeapon(FLOOR_SET, weaponUsesDefaults);
		} else {
			return Generator.randomArmor(FLOOR_SET);
		}
	}

	private static Item roomPrize(Class<? extends SpecialRoom> room, long seed) throws Exception {
		Method prize = room.getDeclaredMethod("prize", Level.class);
		prize.setAccessible(true);
		HeadlessLevel level = HeadlessLevel.install(9, 9);
		Random.pushGenerator(seed);
		try {
			Generator.fullReset();
			return (Item) prize.invoke(null, level);
		} finally {
			Random.popGenerator();
		}
	}

	/** The room's first weapon-or-armor roll for the seed; its prize() first rolls a die of this size. */
	private static Item replay(int prizeItemDie, long seed) {
		Random.pushGenerator(seed);
		try {
			Generator.fullReset();
			Random.Int(prizeItemDie);
			return firstRoll(false);
		} finally {
			Random.popGenerator();
		}
	}

	private static void assertKeptAndCleaned(Item first, Item prize, String where) {
		assertSame(first.getClass(), prize.getClass(), where + ": the prize is the first roll");
		int extra = prize.level() - first.level();
		assertTrue(extra == 0 || extra == 1, where + ": first roll +" + first.level() + ", prize +" + prize.level());
		assertFalse(prize.cursed, where + ": cursed prize");
		assertTrue(prize.cursedKnown, where);
		if (prize instanceof Weapon) assertFalse(((Weapon) prize).hasCurseEnchant(), where + ": curse enchant left on");
		if (prize instanceof Armor) assertFalse(((Armor) prize).hasCurseGlyph(), where + ": curse glyph left on");
	}

	private void checkRoom(Class<? extends SpecialRoom> room, int prizeItemDie) throws Exception {
		int cursedFirstRolls = 0;
		for (long seed = 1; seed <= SEEDS; seed++) {
			Item first = replay(prizeItemDie, seed);
			if (first.cursed) cursedFirstRolls++;
			Item prize = roomPrize(room, seed);
			assertKeptAndCleaned(first, prize, room.getSimpleName() + " seed " + seed);
		}
		assertTrue(cursedFirstRolls > 0, "no seed rolled a cursed item first, nothing was checked");
	}

	@Test
	void poolRoomKeepsItsFirstRoll() throws Exception {
		checkRoom(PoolRoom.class, 3);
	}

	@Test
	void sentryRoomKeepsItsFirstRoll() throws Exception {
		checkRoom(SentryRoom.class, 2);
	}

	@Test
	void trapsRoomKeepsItsFirstRoll() throws Exception {
		checkRoom(TrapsRoom.class, 3);
	}

	@Test
	void secretMazeRoomKeepsItsFirstRoll() {
		int cursedFirstRolls = 0;
		for (long seed = 1; seed <= SEEDS; seed++) {
			Item first;
			Random.pushGenerator(seed);
			try {
				Generator.fullReset();
				Maze.allowDiagonals = false;
				Maze.generate(maze());
				first = firstRoll(true);
			} finally {
				Random.popGenerator();
			}
			if (first.cursed) cursedFirstRolls++;

			HeadlessLevel level = HeadlessLevel.install(19, 19);
			Random.pushGenerator(seed);
			try {
				Generator.fullReset();
				maze().paint(level);
			} finally {
				Random.popGenerator();
			}
			assertEquals(1, level.heaps.valueList().size(), "seed " + seed + ": the maze holds one chest");
			Heap chest = level.heaps.valueList().get(0);
			assertEquals(Heap.Type.CHEST, chest.type);
			assertKeptAndCleaned(first, chest.peek(), "SecretMazeRoom seed " + seed);
		}
		assertTrue(cursedFirstRolls > 0, "no seed rolled a cursed item first, nothing was checked");
	}

	/** A 15x15 maze with its door on the left wall, built under the seed on both sides (its neighbour rolls a size). */
	private static SecretMazeRoom maze() {
		SecretMazeRoom room = new SecretMazeRoom();
		room.set(2, 2, 16, 16);
		room.connected.put(new EmptyRoom(), new Room.Door(room.left, room.top + 7));
		return room;
	}
}
