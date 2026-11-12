package com.qsr.customspd.windows;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.services.updates.AvailableUpdateData;
import com.qsr.customspd.services.updates.UpdateService;
import com.qsr.customspd.services.updates.Updates;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.CheckBox;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings window's tabs, built for real with no display. Each test names the upstream
 * change it pins.
 */
class WndSettingsPortTest {

	private HeadlessRun run;
	private Component tab;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
	}

	@AfterEach
	void tearDown() {
		try {
			if (tab != null) tab.destroy();
		} finally {
			// Updates.service and every other game static go back with the snapshot
			run.close();
		}
	}

	private Component createTab(String name) throws Exception {
		Constructor<?> c = Class.forName(WndSettings.class.getName() + "$" + name).getDeclaredConstructor();
		c.setAccessible(true);
		tab = (Component) c.newInstance();
		return tab;
	}

	private static Object read(Object owner, String name) throws Exception {
		Field f = owner.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	private static void click(Button button) throws Exception {
		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(button);
	}

	/** An update service that offers a beta channel, as the release builds' GitHub one does. */
	private static final class BetaChannelService extends UpdateService {
		@Override public boolean isUpdateable() { return true; }
		@Override public boolean supportsBetaChannel() { return true; }
		@Override public void checkForUpdate(boolean useMetered, boolean includeBetas, UpdateResultCallback callback) {}
		@Override public void initializeUpdate(AvailableUpdateData update) {}
		@Override public boolean isInstallable() { return false; }
		@Override public void initializeInstall() {}
		@Override public boolean supportsReviews() { return false; }
		@Override public void initializeReview(ReviewResultCallback callback) {}
		@Override public void openReviewURI() {}
	}

	// upstream eb641ac1f
	@Test
	void theBetasCheckboxSavesTheBetaChannelChoice() throws Exception {
		Updates.service = new BetaChannelService();
		SPDSettings.updates(false);
		SPDSettings.betas(false);
		createTab("DataTab");
		CheckBox betas = (CheckBox) read(tab, "chkBetas");
		assertNotNull(betas, "test setup: the beta channel checkbox is shown");
		assertFalse(betas.checked(), "test setup: starts unticked");

		click(betas);

		assertTrue(betas.checked(), "test setup: the click ticks it");
		assertTrue(SPDSettings.betas(), "ticking betas must save the beta channel choice");
		assertFalse(SPDSettings.updates(), "ticking betas must leave the update check alone");
	}
}
