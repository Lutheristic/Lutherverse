package com.qsr.customspd.scenes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream e4d08e9ca: the surface victory scene shows daytime from 8:00 to 19:59, where it
 * used to run from 7:00 to 20:59.
 */
class SurfaceSceneDaytimePortTest {

	@Test
	void daytimeRunsFromEightToNineteenFiftyNine() {
		for (int hour = 0; hour < 24; hour++) {
			assertEquals(hour >= 8 && hour <= 19, SurfaceScene.dayTime(hour), "hour " + hour);
		}
	}

	@Test
	void theBoundaryHours() {
		assertEquals(false, SurfaceScene.dayTime(7), "7:00 is night");
		assertEquals(true, SurfaceScene.dayTime(8), "8:00 is day");
		assertEquals(true, SurfaceScene.dayTime(19), "19:59 is day");
		assertEquals(false, SurfaceScene.dayTime(20), "20:00 is night");
	}
}
