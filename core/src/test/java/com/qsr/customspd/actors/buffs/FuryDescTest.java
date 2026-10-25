package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buff.desc() reads the text without formatting it, so a "%%" in fury's description reached the
 * screen as "50%%". Unlike upstream, the strings say "50%".
 */
class FuryDescTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	@Test
	void furyDescriptionShowsASinglePercentSignInEveryLanguage() {
		Messages.setup(Languages.ENGLISH);
		assertTrue(new Fury().desc().contains("by 50%."), new Fury().desc());
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			String desc = new Fury().desc();
			assertFalse(desc.contains("%%"), lang + ": " + desc);
			assertTrue(desc.contains("50"), lang + ": " + desc);
		}
	}
}
