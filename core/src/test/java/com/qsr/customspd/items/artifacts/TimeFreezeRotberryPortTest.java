package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.plants.Plant;
import com.qsr.customspd.plants.Rotberry;
import com.qsr.customspd.plants.Sungrass;
import com.qsr.customspd.plants.Swiftthistle;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Upstream ebb384aff (v2.3.0): when a time freeze (hourglass) or swiftthistle bubble is cut short
 * (taking the stairs) its delayed presses are disarmed. That uproots plants, which deleted a
 * rotberry the player had grown; a rotberry now survives, other plants are still uprooted.
 */
class TimeFreezeRotberryPortTest {

	private int cellRotberry;
	private int cellOther;
	private Plant rotberry;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private HeadlessLevel setUp() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 3 * level.width() + 2);
		cellRotberry = 3 * level.width() + 5;
		cellOther = 4 * level.width() + 5;
		rotberry = new Rotberry();
		rotberry.pos = cellRotberry;
		level.plants.put(cellRotberry, rotberry);
		Plant other = new Sungrass();
		other.pos = cellOther;
		level.plants.put(cellOther, other);
		return level;
	}

	@Test
	void disarmingTheHourglassKeepsARotberryButUprootsOtherPlants() {
		HeadlessLevel level = setUp();
		TimekeepersHourglass.timeFreeze freeze = new TimekeepersHourglass().new timeFreeze();
		freeze.setDelayedPress(cellRotberry);
		freeze.setDelayedPress(cellOther);

		freeze.disarmPresses();

		assertSame(rotberry, level.plants.get(cellRotberry), "the rotberry survives");
		assertNull(level.plants.get(cellOther), "other plants are still uprooted");
	}

	@Test
	void disarmingTheSwiftthistleBubbleKeepsARotberryButUprootsOtherPlants() {
		HeadlessLevel level = setUp();
		Swiftthistle.TimeBubble bubble = new Swiftthistle.TimeBubble();
		bubble.setDelayedPress(cellRotberry);
		bubble.setDelayedPress(cellOther);

		bubble.disarmPresses();

		assertSame(rotberry, level.plants.get(cellRotberry), "the rotberry survives");
		assertNull(level.plants.get(cellOther), "other plants are still uprooted");
	}
}
