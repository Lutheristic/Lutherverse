package com.qsr.customspd.levels.rooms.standard.entrance;

import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.journal.Guidebook;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream da1e844e0: when floor 1 drops the guidebook, the intro guide page is deleted. A page
 * found but never read in an earlier run would otherwise start the new run's tutorial in the
 * flashing-journal state with the toolbar hidden, before the book has been picked up.
 */
class GuidebookDropPortTest {

	private static boolean dropsGuidebook(Level level) {
		for (Heap heap : level.heaps.valueList()) {
			for (Item item : heap.items) {
				if (item instanceof Guidebook) return true;
			}
		}
		return false;
	}

	@Test
	void anUnreadIntroPageFromAnEarlierRunIsDeletedWhenTheGuidebookDrops() {
		try (HeadlessRun run = HeadlessRun.start(20261112L)) {
			Document.ADVENTURERS_GUIDE.unreadPage(Document.GUIDE_INTRO);
			assertTrue(Document.ADVENTURERS_GUIDE.isPageFound(Document.GUIDE_INTRO), "test setup: the page was found before");

			Level level = run.generate("1");

			assertTrue(dropsGuidebook(level), "test setup: floor 1 drops the guidebook");
			assertFalse(Document.ADVENTURERS_GUIDE.isPageFound(Document.GUIDE_INTRO),
					"the intro page must wait for the new guidebook to be picked up");
		}
	}

	@Test
	void aReplayedTutorialAlsoStartsWithoutTheIntroPage() {
		try (HeadlessRun run = HeadlessRun.start(20261112L)) {
			SPDSettings.intro(true);
			assertTrue(Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_INTRO), "test setup: the page was read before");

			Level level = run.generate("1");

			assertTrue(dropsGuidebook(level), "test setup: floor 1 drops the guidebook in the tutorial");
			assertFalse(Document.ADVENTURERS_GUIDE.isPageFound(Document.GUIDE_INTRO),
					"the intro page must wait for the new guidebook to be picked up");
		}
	}

	@Test
	void noGuidebookMeansTheReadIntroPageStays() {
		try (HeadlessRun run = HeadlessRun.start(20261112L)) {
			Level level = run.generate("1");

			assertFalse(dropsGuidebook(level), "test setup: a returning player gets no guidebook");
			assertTrue(Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_INTRO), "control: the read page stays read");
		}
	}
}
