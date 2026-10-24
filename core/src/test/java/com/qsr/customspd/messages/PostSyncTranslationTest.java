package com.qsr.customspd.messages;

import com.qsr.customspd.actors.buffs.Barkskin;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Keys the cpdu-fz84 v2.5.4 translation sync could not see when it ran, because CPDU's English for
 * them was wrong or missing at the snapshot it was computed against (fd96ad582). Each was fixed in
 * English afterwards, so the sync's own rule now selects it; these pin the result in every language.
 *
 * TranslationPlaceholderTest cannot catch these: a stale translation with the same placeholders as
 * the new English passes it, and a key no bundle defines is never compared at all.
 */
class PostSyncTranslationTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	private static int paragraphs(String text) {
		return text.split("\n\n", -1).length;
	}

	/**
	 * v2.5.4 (4ad161622 in CPDU) added a closing paragraph to the barkskin description: only the
	 * strongest of several barkskin sources applies. The English was split across two physical lines
	 * until a0a54ce9f, so the sync read it as different from v2.5.4 and left every translation on the
	 * v2.1.0 text, one paragraph short. Every language must tell the player the stacking rule.
	 */
	@Test
	void barkskinDescriptionHasTheStackingParagraphInEveryLanguage() {
		Messages.setup(Languages.ENGLISH);
		String english = Messages.get(Barkskin.class, "desc", 4, "7");
		assertEquals(4, paragraphs(english), "test setup: English is v2.5.4's four paragraphs: " + english);

		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			String desc = Messages.get(Barkskin.class, "desc", 4, "7");
			assertEquals(paragraphs(english), paragraphs(desc),
					lang + " barkskin description must carry v2.5.4's stacking paragraph: " + desc);
		}
	}
}
