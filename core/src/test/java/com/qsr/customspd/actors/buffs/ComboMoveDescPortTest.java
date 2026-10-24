package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Every combo move description must be fully formatted. The non-empowered SLAM description has a
 * %d for its damage percentage and passed no argument for it, so a gladiator without Enhanced
 * Combo +3 saw the raw "%d%%" format string (upstream 44230c1f9).
 */
class ComboMoveDescPortTest {

	private HeadlessRun run;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240425L);
		run.enterFirstLevel();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void noMoveDescriptionLeavesAFormatSpecifierUnfilled() {
		for (Combo.ComboMove move : Combo.ComboMove.values()) {
			for (int count : new int[]{move.comboReq, move.comboReq + 5}) {
				String desc = move.desc(count);
				assertFalse(desc.contains("%d"), move + " at combo " + count + ": " + desc);
			}
		}
	}

	@Test
	void slamDescriptionShowsComboTimesTwentyPercent() {
		assertEquals(true, Combo.ComboMove.SLAM.desc(4).contains("80%"), Combo.ComboMove.SLAM.desc(4));
	}
}
