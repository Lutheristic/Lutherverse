package com.qsr.customspd.ui;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.GameSettings;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * upstream 2466974a7: the game log only reads the interface size and walks its pending lines on
 * a frame that has new text. A frame with nothing new leaves the log as it was and reads no
 * settings.
 */
class GameLogUpdatePortTest {

	@Test
	void aNewLineShowsAndAnEmptyFrameDoesNoWork() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20261113L)) {
			Game.platform = new PlatformSupport() {
				@Override public void updateDisplaySize() {}
				@Override public void updateSystemUI() {}
				@Override public boolean connectedToUnmeteredNetwork() { return false; }
				@Override public boolean supportsVibration() { return false; }
				@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
				@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
				@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
			};
			int[] uiSizeReads = countUiSizeReads();
			GameLog.wipe();

			GameLog log = new GameLog();
			try {
				log.setRect(0, 100, 120, 0);
				GLog.i("The air smells of damp stone.");
				log.update();

				List<RenderedTextBlock> lines = lines(log);
				assertEquals(1, lines.size(), "the new text becomes one line");
				assertEquals("The air smells of damp stone.", lines.get(0).text());
				assertTrue(uiSizeReads[0] > 0, "test setup: a frame with new text reads the interface size");

				int readsBefore = uiSizeReads[0];
				log.update();

				assertEquals(lines, lines(log), "an empty frame leaves the log as it was");
				assertEquals(readsBefore, uiSizeReads[0], "an empty frame reads no settings");
			} finally {
				GLog.update.remove(log);
				GameLog.wipe();
			}
		}
	}

	/** Wraps the run's settings so every read of the interface size is counted. */
	private static int[] countUiSizeReads() throws Exception {
		Field f = GameSettings.class.getDeclaredField("prefs");
		f.setAccessible(true);
		Preferences inner = (Preferences) f.get(null);
		int[] reads = {0};
		Preferences counting = (Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
				new Class<?>[]{Preferences.class}, (proxy, method, args) -> {
					if (method.getName().equals("getInteger") && SPDSettings.KEY_UI_SIZE.equals(args[0])) reads[0]++;
					try {
						return method.invoke(inner, args);
					} catch (InvocationTargetException e) {
						throw e.getCause();
					}
				});
		GameSettings.set(counting);
		return reads;
	}

	private static List<RenderedTextBlock> lines(Group log) throws Exception {
		Field f = Group.class.getDeclaredField("members");
		f.setAccessible(true);
		List<RenderedTextBlock> lines = new ArrayList<>();
		for (Object g : (List<?>) f.get(log)) {
			if (g instanceof RenderedTextBlock) lines.add((RenderedTextBlock) g);
			else if (g != null) throw new AssertionError("unexpected log member " + ((Gizmo) g).getClass());
		}
		return lines;
	}
}
