package com.qsr.customspd.services.updates;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.watabou.noosa.Game;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Which GitHub release the update check offers: the newest one above the running build that
 * suits the player's channel and, on Android and iOS, the device's OS version. The release list
 * is served from memory, so nothing goes over the network.
 */
class GitHubUpdatesFilterTest {

	private static final int CURRENT = 800;

	private Application savedApp;
	private Net savedNet;
	private int savedVersionCode;
	private boolean savedWindows, savedLinux, savedMac, savedAndroid, savedIos;

	@BeforeEach
	void save() {
		savedApp = Gdx.app;
		savedNet = Gdx.net;
		savedVersionCode = Game.versionCode;
		savedWindows = SharedLibraryLoader.isWindows;
		savedLinux = SharedLibraryLoader.isLinux;
		savedMac = SharedLibraryLoader.isMac;
		savedAndroid = SharedLibraryLoader.isAndroid;
		savedIos = SharedLibraryLoader.isIos;
		Game.versionCode = CURRENT;
		platform("desktop", 0);
	}

	@AfterEach
	void restore() {
		Gdx.app = savedApp;
		Gdx.net = savedNet;
		Game.versionCode = savedVersionCode;
		SharedLibraryLoader.isWindows = savedWindows;
		SharedLibraryLoader.isLinux = savedLinux;
		SharedLibraryLoader.isMac = savedMac;
		SharedLibraryLoader.isAndroid = savedAndroid;
		SharedLibraryLoader.isIos = savedIos;
	}

	@SuppressWarnings("unchecked")
	private static <T> T stub(Class<T> type, InvocationHandler handler) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{ type }, handler);
	}

	private static Object defaultFor(Class<?> type) {
		if (type == boolean.class) return false;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == float.class) return 0f;
		if (type == double.class) return 0d;
		return null;
	}

	/** Runs as the given platform, reporting the given OS version (API level, or major iOS version). */
	private static void platform(String os, final int version) {
		SharedLibraryLoader.isWindows = os.equals("desktop");
		SharedLibraryLoader.isLinux = false;
		SharedLibraryLoader.isMac = false;
		SharedLibraryLoader.isAndroid = os.equals("android");
		SharedLibraryLoader.isIos = os.equals("ios");
		Gdx.app = stub(Application.class, (proxy, method, args) ->
				method.getName().equals("getVersion") ? version : defaultFor(method.getReturnType()));
	}

	private static String release(String name, int versionCode, boolean prerelease, String requirement) {
		String body = "Changes in " + name + "\\n\\n---\\n\\ninternal version number: " + versionCode;
		if (requirement != null) body += "\\n\\nRequires " + requirement;
		return "{\"name\":\"" + name + "\",\"prerelease\":" + prerelease
				+ ",\"html_url\":\"https://example.invalid/" + name + "\",\"body\":\"" + body + "\"}";
	}

	private static final class Result extends UpdateService.UpdateResultCallback {
		AvailableUpdateData offered;
		boolean none, failed;

		@Override public void onUpdateAvailable(AvailableUpdateData update) { offered = update; }
		@Override public void onNoUpdateFound() { none = true; }
		@Override public void onConnectionFailed() { failed = true; }
	}

	private static Result check(boolean includeBetas, String... releases) {
		StringBuilder json = new StringBuilder("[");
		for (int i = 0; i < releases.length; i++) {
			if (i > 0) json.append(',');
			json.append(releases[i]);
		}
		json.append(']');
		final byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);

		final Net.HttpResponse response = stub(Net.HttpResponse.class, (proxy, method, args) ->
				method.getName().equals("getResultAsStream") ? new ByteArrayInputStream(bytes) : defaultFor(method.getReturnType()));
		Gdx.net = stub(Net.class, (proxy, method, args) -> {
			if (method.getName().equals("sendHttpRequest")) {
				((Net.HttpResponseListener) args[1]).handleHttpResponse(response);
			}
			return defaultFor(method.getReturnType());
		});

		Result result = new Result();
		new GitHubUpdates().checkForUpdate(true, includeBetas, result);
		assertEquals(false, result.failed, "the release list could not be read");
		return result;
	}

	private static String offered(Result result) {
		return result.offered == null ? null : result.offered.versionName;
	}

	@Test
	void theNewestStableReleaseIsOfferedToPlayersWithoutBetas() {
		Result result = check(false,
				release("beta", 830, true, null),
				release("stable", 820, false, null),
				release("older", 810, false, null));
		assertEquals("stable", offered(result));
		assertEquals(820, result.offered.versionCode);
	}

	@Test
	void betasAreOfferedOnlyToPlayersWhoOptIn() {
		assertEquals("beta", offered(check(true,
				release("beta", 830, true, null),
				release("stable", 820, false, null))));
		Result onlyBeta = check(false, release("beta", 830, true, null));
		assertNull(onlyBeta.offered);
		assertEquals(true, onlyBeta.none);
	}

	@Test
	void releasesThatAreNotNewerAreNeverOffered() {
		Result result = check(true,
				release("same", CURRENT, false, null),
				release("older", CURRENT - 5, false, null));
		assertNull(result.offered);
		assertEquals(true, result.none);
	}

	@Test
	void androidSkipsReleasesForANewerApiLevel() {
		platform("android", 21);
		Result result = check(false,
				release("needs nougat", 830, false, "Android 7.0 (API 24)+ Devices"),
				release("lollipop is fine", 820, false, "Android 5.0 (API 21)+ Devices"));
		assertEquals("lollipop is fine", offered(result));

		platform("android", 24);
		assertEquals("needs nougat", offered(check(false,
				release("needs nougat", 830, false, "Android 7.0 (API 24)+ Devices"))));
	}

	@Test
	void iosSkipsReleasesForANewerVersion() {
		platform("ios", 11);
		Result result = check(false,
				release("needs ios 12", 830, false, "iOS 12+ Devices"),
				release("any ios", 820, false, null));
		assertEquals("any ios", offered(result));
	}

	@Test
	void desktopIgnoresTheMobileRequirements() {
		Result result = check(false,
				release("needs nougat and ios 12", 830, false, "Android 7.0 (API 24)+ Devices, iOS 12+ Devices"));
		assertEquals("needs nougat and ios 12", offered(result));
	}
}
