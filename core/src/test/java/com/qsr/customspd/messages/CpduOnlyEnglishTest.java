package com.qsr.customspd.messages;

import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPDU's English for these keys differs from both v2.1.0 and v2.5.4, so no upstream translation
 * matches it, and the translations still described v2.1.0 (Shattered's name, old talent numbers,
 * the old blacksmith deal). They were dropped so every language shows the English text.
 */
class CpduOnlyEnglishTest {

	static final String[] KEYS = {
			"items.scrolls.exotic.scrollofantimagic.desc",
			"scenes.titlescene$changesbutton.desc",
			"scenes.welcomescene.patch_intro",
			"scenes.welcomescene.save_warning",
			"scenes.welcomescene.update_intro",
			"scenes.welcomescene.update_msg",
			"scenes.welcomescene.what_msg",
			"windows.wndblacksmith.prompt",
	};

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	@Test
	void cpduOnlyEnglishShowsInEveryLanguage() {
		Messages.setup(Languages.ENGLISH);
		String[] english = new String[KEYS.length];
		for (int i = 0; i < KEYS.length; i++) {
			english[i] = Messages.get(KEYS[i]);
			assertTrue(!english[i].equals(KEYS[i]), "English lacks " + KEYS[i]);
		}
		List<String> wrong = new ArrayList<>();
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			for (int i = 0; i < KEYS.length; i++) {
				String shown = Messages.get(KEYS[i]);
				if (!english[i].equals(shown)) wrong.add(lang + " " + KEYS[i] + ": " + shown);
			}
		}
		assertTrue(wrong.isEmpty(), String.join("\n", wrong));
	}
}
