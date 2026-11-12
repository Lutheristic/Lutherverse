package com.qsr.customspd.ui;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream f36dda306: a long press only buzzes when the vibration setting is on.
 */
class ButtonLongPressVibrationPortTest {

	private HeadlessRun run;
	private final List<Integer> buzzes = new ArrayList<>();
	private LongPressButton button;

	private static final class LongPressButton extends Button {
		int longClicks = 0;

		@Override
		protected boolean onLongClick() {
			longClicks++;
			return true;
		}

		void holdDown() {
			pressedButton = this;
			pressTime = 0;
			clickReady = true;
		}
	}

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return true; }
			@Override public void vibrate(int millis) { buzzes.add(millis); }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		button = new LongPressButton();
	}

	@AfterEach
	void tearDown() {
		try {
			button.destroy();
		} finally {
			run.close();
		}
	}

	private void longPress() {
		button.holdDown();
		Game.elapsed = Button.longClick + 0.1f;
		button.update();
		assertEquals(1, button.longClicks, "test setup: the hold counts as a long press");
	}

	@Test
	void aLongPressDoesNotBuzzWithVibrationOff() {
		SPDSettings.vibration(false);

		longPress();

		assertTrue(buzzes.isEmpty(), "vibration is off, but the long press buzzed " + buzzes);
	}

	@Test
	void aLongPressStillBuzzesWithVibrationOn() {
		SPDSettings.vibration(true);

		longPress();

		assertEquals(1, buzzes.size(), "control: one buzz for the long press");
		assertEquals(50, (int) buzzes.get(0));
	}
}
