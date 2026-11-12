package com.qsr.customspd.items.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.utils.GLog;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Game;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream a7ded9be4: picking up the guidebook names the bound journal key on the desktop
 * interface, and keeps the plain tap line on the mobile one.
 */
class GuidebookTutorialTextPortTest {

	private HeadlessRun run;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		run.enterFirstLevel();
		// big enough that interfaceSize() keeps the full interface
		Game.width = Game.dispWidth = 1500;
		Game.height = Game.dispHeight = 1000;
		KeyBindings.setAllBindings(SPDAction.getDefaults());
		GLog.update.add(logListener);
	}

	@AfterEach
	void tearDown() {
		try {
			GLog.update.remove(logListener);
		} finally {
			run.close();
		}
	}

	/** The tutorial line logged after the pickup message. */
	private String pickUpGuidebook() {
		log.clear();
		new Guidebook().doPickUp(Dungeon.hero, Dungeon.hero.pos);
		assertEquals(2, log.size(), "test setup: the pickup message, then the tutorial line; log was " + log);
		return log.get(1);
	}

	@Test
	void theDesktopLineNamesTheJournalKey() {
		SPDSettings.interfaceSize(2);
		assertEquals(2, SPDSettings.interfaceSize(), "test setup: the full interface");
		String key = KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(SPDAction.JOURNAL, false));
		assertEquals("J", key, "test setup: the default journal key");

		String line = pickUpGuidebook();

		assertTrue(line.contains("(J)"), "the desktop line must name the journal key, was: " + line);
	}

	@Test
	void theMobileLineAsksForATap() {
		SPDSettings.interfaceSize(0);

		String line = pickUpGuidebook();

		assertEquals(GLog.POSITIVE + "Tap the blinking journal button to read the book you just picked up.", line);
	}
}
