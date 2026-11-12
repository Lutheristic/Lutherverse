package com.qsr.customspd.windows;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.services.updates.AvailableUpdateData;
import com.qsr.customspd.services.updates.UpdateService;
import com.qsr.customspd.services.updates.Updates;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.CheckBox;
import com.watabou.noosa.Game;
import com.watabou.utils.DeviceCompat;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings window's tabs, built for real with no display. Each test names the upstream
 * change it pins.
 */
class WndSettingsPortTest {

	private HeadlessRun run;
	private Component tab;
	private boolean canVibrate = false;
	private final List<Integer> buzzes = new ArrayList<>();
	// libGDX statics the snapshot does not cover, put back after each test
	private Application realApp;
	private boolean realIsAndroid;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		realApp = Gdx.app;
		realIsAndroid = SharedLibraryLoader.isAndroid;
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return canVibrate; }
			@Override public void vibrate(int millis) { buzzes.add(millis); }
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
			Gdx.app = realApp;
			SharedLibraryLoader.isAndroid = realIsAndroid;
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

	// upstream f36dda306 and 5f735ffe6
	@Test
	void theVibrationCheckboxSavesTheSettingAndBuzzesWhenTicked() throws Exception {
		canVibrate = true;
		SPDSettings.vibration(true);
		createTab("UITab");
		CheckBox vibration = (CheckBox) read(tab, "chkVibrate");
		assertTrue(vibration.active, "a device that can vibrate may change the setting");
		assertTrue(vibration.checked(), "the box shows the saved setting");

		click(vibration);
		assertFalse(SPDSettings.vibration(), "unticking turns vibration off");
		assertTrue(buzzes.isEmpty(), "unticking does not buzz");

		click(vibration);
		assertTrue(SPDSettings.vibration(), "ticking turns vibration back on");
		assertEquals(1, buzzes.size(), "ticking gives one test buzz");
		assertEquals(250, (int) buzzes.get(0));
	}

	// upstream f36dda306 and 5f735ffe6
	@Test
	void withoutVibrationSupportTheCheckboxIsDisabledAndUnticked() throws Exception {
		canVibrate = false;
		SPDSettings.vibration(true);
		createTab("UITab");
		CheckBox vibration = (CheckBox) read(tab, "chkVibrate");

		assertFalse(vibration.active, "a device that cannot vibrate cannot change the setting");
		assertFalse(vibration.checked(), "the saved setting is not shown where it does nothing");
	}

	/** Reports the device as the given platform and OS version, the rest from the headless app. */
	private void pretendDevice(Application.ApplicationType type, int version) {
		SharedLibraryLoader.isAndroid = type == Application.ApplicationType.Android;
		Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
				new Class<?>[]{Application.class}, (proxy, method, args) -> {
					if (method.getName().equals("getType")) return type;
					if (method.getName().equals("getVersion")) return version;
					try {
						return method.invoke(realApp, args);
					} catch (InvocationTargetException e) {
						throw e.getCause();
					}
				});
		PixelScene.maxScreenZoom = 2;
	}

	/** The saved choice alone: the setter also resizes the display, which needs a running game. */
	private static void savePowerSaver(boolean on) {
		SPDSettings.put(SPDSettings.KEY_POWER_SAVER, on);
	}

	private Object powerSaverBox() throws Exception {
		createTab("DisplayTab");
		return read(tab, "chkSaver");
	}

	// upstream 6233de2f7 and 5bc753f3b
	@Test
	void anAndroidDeviceThatCanGoFullscreenNoLongerOffersPowerSaver() throws Exception {
		pretendDevice(Application.ApplicationType.Android, 19);
		assertTrue(DeviceCompat.isAndroid() && DeviceCompat.supportsFullScreen(), "test setup: Android 4.4");
		savePowerSaver(false);

		assertNull(powerSaverBox(), "power saver is only offered where it still helps");
	}

	// upstream 6233de2f7 and 5bc753f3b
	@Test
	void powerSaverStaysWhereItIsAlreadyOn() throws Exception {
		pretendDevice(Application.ApplicationType.Android, 19);
		savePowerSaver(true);

		assertNotNull(powerSaverBox(), "a player who turned it on can still turn it off");
	}

	// upstream 6233de2f7 and 5bc753f3b
	@Test
	void anAndroidDeviceThatCannotGoFullscreenKeepsPowerSaver() throws Exception {
		pretendDevice(Application.ApplicationType.Android, 18);
		assertTrue(DeviceCompat.isAndroid() && !DeviceCompat.supportsFullScreen(), "test setup: Android 4.3");
		savePowerSaver(false);

		assertNotNull(powerSaverBox(), "Android 4.3 and older still get the option");
	}

	// upstream 5bc753f3b
	@Test
	void aNonAndroidDeviceWithoutFullscreenNeverOffersPowerSaver() throws Exception {
		// iOS with no safe area inset reports no fullscreen support
		pretendDevice(Application.ApplicationType.iOS, 17);
		assertTrue(!DeviceCompat.isAndroid() && !DeviceCompat.supportsFullScreen(), "test setup: no fullscreen, not Android");
		savePowerSaver(false);

		assertNull(powerSaverBox(), "power saver is an Android option only");
	}
}
