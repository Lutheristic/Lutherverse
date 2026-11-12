package com.qsr.customspd.windows;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Window;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 79ed98b25: the key that opens the hero window now closes it
 * again, and the same for the journal key and the journal window. Before, a window swallowed
 * every key except back and wait, so pressing the key a second time did nothing.
 */
class WindowToggleKeyPortTest {

	private static final long SEED = 20261112L;

	/** Opens a window under a parent with the real UI statics set up, runs the check, and cleans up. */
	private static void withWindow(Supplier<Window> open, WindowCheck check) {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			run.enterFirstLevel();
			// Windows need a ui camera and a text splitter; with no font generator, text is
			// laid out without being measured. HeadlessRun puts these statics back on close.
			PixelScene.uiCamera = new Camera(0, 0, 400, 400, 1);
			Game.platform = new PlatformSupport() {
				@Override public void updateDisplaySize() {}
				@Override public void updateSystemUI() {}
				@Override public boolean connectedToUnmeteredNetwork() { return false; }
				@Override public boolean supportsVibration() { return false; }
				@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
				@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
				@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
			};
			KeyBindings.setAllBindings(SPDAction.getDefaults());
			KeyBindings.setAllControllerBindings(SPDAction.getControllerDefaults());

			Group parent = new Group();
			Window wnd = open.get();
			parent.add(wnd);
			try {
				check.run(parent, wnd);
			} finally {
				if (wnd.parent != null) wnd.hide();
			}
		}
	}

	private interface WindowCheck {
		void run(Group parent, Window wnd);
	}

	private static void press(int key) {
		KeyEvent.addKeyEvent(new KeyEvent(key, true));
		KeyEvent.addKeyEvent(new KeyEvent(key, false));
		KeyEvent.processKeyEvents();
	}

	private static boolean isOpen(Group parent, Window wnd) {
		return wnd.parent == parent && parent.indexOf(wnd) >= 0;
	}

	@Test
	void theHeroInfoKeyClosesTheHeroWindow() {
		withWindow(WndHero::new, (parent, wnd) -> {
			assertTrue(isOpen(parent, wnd), "precondition: the window is open");
			press(Input.Keys.H);
			assertFalse(isOpen(parent, wnd), "the hero info key closes the hero window");
		});
	}

	@Test
	void theJournalKeyClosesTheJournalWindow() {
		withWindow(WndJournal::new, (parent, wnd) -> {
			assertTrue(isOpen(parent, wnd), "precondition: the window is open");
			press(Input.Keys.J);
			assertFalse(isOpen(parent, wnd), "the journal key closes the journal window");
		});
	}

	@Test
	void theJournalKeyLeavesTheHeroWindowOpen() {
		withWindow(WndHero::new, (parent, wnd) -> {
			press(Input.Keys.J);
			assertTrue(isOpen(parent, wnd), "only the hero info key toggles the hero window");
		});
	}

	@Test
	void theHeroInfoKeyLeavesTheJournalWindowOpen() {
		withWindow(WndJournal::new, (parent, wnd) -> {
			press(Input.Keys.H);
			assertTrue(isOpen(parent, wnd), "only the journal key toggles the journal window");
		});
	}

	@Test
	void theBackKeyStillClosesBothWindows() {
		// unchanged by the port: shows a key press reaches the window at all
		withWindow(WndHero::new, (parent, wnd) -> {
			press(Input.Keys.ESCAPE);
			assertFalse(isOpen(parent, wnd));
		});
		withWindow(WndJournal::new, (parent, wnd) -> {
			press(Input.Keys.ESCAPE);
			assertFalse(isOpen(parent, wnd));
		});
	}
}
