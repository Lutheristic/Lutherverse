package com.qsr.customspd.actors.hero;

import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Talent text is looked up by enum name, so renaming a talent without renaming its keys leaves
 * the talent window without its text. The v2.2.0 renames (liquid willpower, inscribed power and
 * the rest) and desperate power all moved keys. CPDU's Messages.get returns the lowercased key
 * path for a missing key rather than upstream's NO_TEXT_FOUND, so both are treated as missing.
 */
class TalentTextTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	private static boolean missing(String text, Talent talent, String suffix) {
		return text.equals(Messages.NO_TEXT_FOUND)
				|| text.endsWith(talent.name().toLowerCase(Locale.ENGLISH) + "." + suffix);
	}

	@Test
	void everyTalentHasATitleAndDescriptionInEnglish() {
		Messages.setup(Languages.ENGLISH);
		List<String> missing = new ArrayList<>();
		for (Talent talent : Talent.values()) {
			if (missing(talent.title(), talent, "title")) missing.add(talent + ".title");
			if (missing(talent.desc(), talent, "desc")) missing.add(talent + ".desc");
		}
		assertEquals(new ArrayList<String>(), missing);
	}
}
