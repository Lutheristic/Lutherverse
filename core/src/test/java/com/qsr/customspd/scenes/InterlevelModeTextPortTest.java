package com.qsr.customspd.scenes;

import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The loading screen adds its own one to three dots after the mode text (upstream ee26c346b), so
 * the English mode texts no longer carry "..." of their own. Falling gets no dots and reads
 * "Falling!".
 */
class InterlevelModeTextPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		Messages.setup(Languages.ENGLISH);
	}

	@Test
	void modeTextsLeaveTheDotsToTheLoadingScreen() {
		for (InterlevelScene.Mode mode : InterlevelScene.Mode.values()) {
			if (mode == InterlevelScene.Mode.NONE || mode == InterlevelScene.Mode.FALL) continue;
			String text = Messages.get(InterlevelScene.Mode.class, mode.name());
			assertFalse(text.startsWith("scenes."), "missing text for " + mode);
			assertFalse(text.endsWith("."), mode + ": " + text);
		}
		assertEquals("Falling!", Messages.get(InterlevelScene.Mode.class, InterlevelScene.Mode.FALL.name()));
	}
}
