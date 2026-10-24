package com.qsr.customspd.messages;

import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 62aefca1c: number formatting follows the selected language's locale instead of being pinned
 * to English. Messages.format and Messages.decimalFormat are what item/actor strings and every damage or
 * stat readout go through; the windows' NumberFormat.getInstance(Messages.locale()) shares the locale.
 */
class MessagesLocaleTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	@Test
	void formatFollowsTheLanguageLocale() {
		Messages.setup(Languages.ENGLISH);
		assertEquals("1,234,567", Messages.format("%,d", 1234567));

		Messages.setup(Languages.GERMAN);
		assertEquals("1.234.567", Messages.format("%,d", 1234567));
	}

	@Test
	void decimalFormatFollowsTheLanguageLocale() {
		Messages.setup(Languages.ENGLISH);
		assertEquals("1.5", Messages.decimalFormat("#.##", 1.5));

		// The cached DecimalFormat for "#.##" was built for English; setup() must have dropped it.
		Messages.setup(Languages.GERMAN);
		assertEquals("1,5", Messages.decimalFormat("#.##", 1.5));
	}
}
