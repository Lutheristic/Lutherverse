package com.qsr.customspd.messages;

import com.qsr.customspd.actors.buffs.EnhancedRings;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream c1ed0f8ab (buffs-ui lane, ffd4eabb6) deleted EnhancedRings.desc(), so the buff now
 * renders through FlavourBuff.desc() with dispTurns(), a String. The English text moved to %s with
 * it, but CPDU's translations are still v2.1.0's and formatted the turns with %d, which a String
 * cannot fill: Messages.format reported an exception and showed the raw "%d" text in every
 * language but English. v2.5.4's synced translations use %s, as these now do.
 */
class EnhancedRingsTranslationTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	@Test
	void theEnhancedRingsDescriptionRendersItsTurnCountInEveryLanguage() {
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			String desc = new EnhancedRings().desc();
			assertFalse(desc.contains("%"), lang + ": unformatted placeholder in " + desc);
			assertTrue(desc.matches("(?s).*\\d.*"), lang + ": no turn count in " + desc);
		}
	}
}
