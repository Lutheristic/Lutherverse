package com.qsr.customspd.items.food;

import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.utils.Holiday;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression test for upstream SPD e06d9a918 (v2.3.0): "added a default case for pasty
 * sprites".
 *
 * Pasty.reset() picked its sprite with a switch that only listed NONE, HALLOWEEN and
 * WINTER_HOLIDAYS. On any other holiday (Easter, April Fools, Pride, ...) no case matched, the
 * image kept Item's placeholder, and the pasty rendered as the "something" icon. Every holiday
 * without its own pasty now gets the plain one.
 *
 * Holiday caches the current holiday in a private static; the test pins it by reflection.
 */
class PastyHolidaySpritePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Holiday.clearCachedHoliday();
	}

	private static void pinHoliday(Holiday holiday) throws Exception {
		Field cached = Holiday.class.getDeclaredField("cached");
		cached.setAccessible(true);
		cached.set(null, holiday);
	}

	@Test
	void aHolidayWithoutItsOwnPastyShowsThePlainPasty() throws Exception {
		pinHoliday(Holiday.APRIL_FOOLS);

		assertEquals(GeneralAsset.PASTY, new Pasty().image());
	}

	@Test
	void controlHalloweenStillShowsThePumpkinPie() throws Exception {
		pinHoliday(Holiday.HALLOWEEN);

		assertEquals(GeneralAsset.PUMPKIN_PIE, new Pasty().image());
	}
}
