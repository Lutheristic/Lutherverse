package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.plants.Plant;
import com.qsr.customspd.plants.Swiftthistle;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 995d84e53 (v2.4.0): presses delayed by a time freeze (hourglass) or a swiftthistle bubble
 * resolve the cell's plant before its trap, so a fadeleaf's teleport lands before the trap fires.
 */
class TimeFreezePressOrderPortTest {

	private static final List<String> LOG = new ArrayList<>();

	private static class LoggingPlant extends Plant {
		@Override
		public void activate(Char ch) {
			LOG.add("plant");
		}
	}

	private static class LoggingTrap extends Trap {
		@Override
		public void activate() {
			LOG.add("trap");
		}
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		LOG.clear();
	}

	private static int stackPlantAndTrap() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 3 * level.width() + 2);
		int cell = 3 * level.width() + 5;
		LoggingPlant plant = new LoggingPlant();
		plant.pos = cell;
		level.plants.put(cell, plant);
		LoggingTrap trap = new LoggingTrap();
		trap.pos = cell;
		level.traps.put(cell, trap);
		return cell;
	}

	@Test
	void theHourglassResolvesAPlantBeforeATrapOnTheSameCell() {
		int cell = stackPlantAndTrap();
		TimekeepersHourglass.timeFreeze freeze = new TimekeepersHourglass().new timeFreeze();
		freeze.setDelayedPress(cell);
		freeze.triggerPresses();
		assertEquals(Arrays.asList("plant", "trap"), LOG);
	}

	@Test
	void theSwiftthistleBubbleResolvesAPlantBeforeATrapOnTheSameCell() {
		int cell = stackPlantAndTrap();
		Swiftthistle.TimeBubble bubble = new Swiftthistle.TimeBubble();
		bubble.setDelayedPress(cell);
		bubble.triggerPresses();
		assertEquals(Arrays.asList("plant", "trap"), LOG);
	}
}
