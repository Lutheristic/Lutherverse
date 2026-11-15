package com.qsr.customspd.windows;

import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The grid catalog (v2.5.4) titles the tab and fills its "???" popups from four catalogtab keys.
 * A missing key shows its raw name on screen, and upstream briefly shipped "enchantmet"
 * (a3f2ab7a8, fixed by 8b846e612), so pin the exact v2.5.4 English.
 */
class WndJournalCatalogKeysTest {

	private static Class<?> catalogTab;

	@BeforeAll
	static void boot() throws ClassNotFoundException {
		HeadlessGdx.boot();
		catalogTab = Class.forName("com.qsr.customspd.windows.WndJournal$CatalogTab");
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	@Test
	void catalogKeysCarryTheUpstreamEnglishText() {
		Messages.setup(Languages.ENGLISH);
		assertEquals("Catalogs", Messages.get(catalogTab, "title"));
		assertEquals("You haven't found and identified that item in any of your runs yet.",
				Messages.get(catalogTab, "not_seen_item"));
		assertEquals("You haven't found and identified a weapon with that enchantment in any of your runs yet.",
				Messages.get(catalogTab, "not_seen_enchantment"));
		assertEquals("You haven't found and identified an armor with that glyph in any of your runs yet.",
				Messages.get(catalogTab, "not_seen_glyph"));
	}

	@Test
	void germanCarriesTheUpstreamTranslation() {
		Messages.setup(Languages.GERMAN);
		assertEquals("Du hast diesen Gegenstand bisher bei keinem Lauf gefunden und identifiziert.",
				Messages.get(catalogTab, "not_seen_item"));
		assertEquals("Katalog", Messages.get(catalogTab, "title"));
	}

	@Test
	void englishOnlyUpstreamLinesFallBackToEnglish() {
		// v2.5.4's tr not_seen_* lines and be title are still English, so they are not taken.
		Messages.setup(Languages.TURKISH);
		assertEquals("You haven't found and identified that item in any of your runs yet.",
				Messages.get(catalogTab, "not_seen_item"));
		assertEquals("Kataloglar", Messages.get(catalogTab, "title"));

		Messages.setup(Languages.BELARUSIAN);
		assertEquals("Catalogs", Messages.get(catalogTab, "title"));
		// be takes upstream's untranslated line as it stands, typo included.
		assertEquals("You haven't found and identified a weapon with that enchantmet in any of your runs yet.",
				Messages.get(catalogTab, "not_seen_enchantment"));
	}
}
