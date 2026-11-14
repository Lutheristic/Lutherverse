package com.qsr.customspd.scenes;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.MeleeWeapon;
import com.qsr.customspd.items.weapon.melee.Rapier;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.MemoryPreferences;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Camera;
import com.watabou.utils.GameSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 02210b0a8 (v2.1.3) adds a screen shake setting. Every shake goes through
 * PixelScene.shake, which multiplies the magnitude by the saved setting (0 to 4) before
 * handing it to the camera. Upstream's default is 2, so a shake at the default is twice the
 * magnitude the call asks for, and the top of the slider is twice the default.
 */
class ScreenShakePortTest {

	private static final int W = 10;

	private MemoryPreferences prefs;
	private Object savedPrefs;
	private Camera prevCamera;
	private final List<float[]> shakes = new ArrayList<>();

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void start() throws Exception {
		savedPrefs = prefsField().get(null);
		prefs = new MemoryPreferences();
		GameSettings.set(prefs);
		prevCamera = Camera.main;
		shakes.clear();
		Camera.main = new Camera(0, 0, 100, 100, 1f) {
			@Override
			public void shake(float magnitude, float duration) {
				shakes.add(new float[]{magnitude, duration});
			}
		};
	}

	@AfterEach
	void tearDown() throws Exception {
		Camera.main = prevCamera;
		prefsField().set(null, savedPrefs);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field prefsField() throws NoSuchFieldException {
		Field f = GameSettings.class.getDeclaredField("prefs");
		f.setAccessible(true);
		return f;
	}

	private float[] onlyShake() {
		assertEquals(1, shakes.size(), "one call, one camera shake");
		return shakes.get(0);
	}

	@Test
	void theSettingIsSavedUnderUpstreamsKeyWithUpstreamsDefault() {
		assertEquals("screen_shake", SPDSettings.KEY_SCREEN_SHAKE, "a player's saved choice carries over");
		assertEquals(2, SPDSettings.screenShake(), "nothing saved yet reads as the default");
	}

	@Test
	void eachSliderValueRoundTripsThroughTheSavedKey() {
		for (int value = 0; value <= 4; value++) {
			SPDSettings.screenShake(value);
			assertEquals(value, prefs.getInteger("screen_shake", -1), "saved under screen_shake");
			assertEquals(value, SPDSettings.screenShake(), "read back as saved");
		}
	}

	@Test
	void aSavedValueOutsideTheSliderIsClamped() {
		prefs.putInteger("screen_shake", 9);
		assertEquals(4, SPDSettings.screenShake());
		prefs.putInteger("screen_shake", -3);
		assertEquals(0, SPDSettings.screenShake());
	}

	@Test
	void offMeansNoShake() {
		SPDSettings.screenShake(0);

		PixelScene.shake(3, 0.7f);

		float[] shake = onlyShake();
		assertEquals(0f, shake[0], 0.0001f, "a zero setting shakes by nothing");
		assertEquals(0.7f, shake[1], 0.0001f, "the duration is the call's own");
	}

	@Test
	void theDefaultMultipliesTheMagnitudeByTwo() {
		PixelScene.shake(3, 0.7f);

		float[] shake = onlyShake();
		assertEquals(6f, shake[0], 0.0001f, "upstream scales by the setting itself, 2 by default");
		assertEquals(0.7f, shake[1], 0.0001f);
	}

	@Test
	void theTopSettingIsTwiceTheDefault() {
		PixelScene.shake(1, 0.4f);
		SPDSettings.screenShake(4);
		PixelScene.shake(1, 0.4f);

		assertEquals(2, shakes.size());
		assertEquals(2f * shakes.get(0)[0], shakes.get(1)[0], 0.0001f);
		assertEquals(4f, shakes.get(1)[0], 0.0001f);
		assertEquals(0.4f, shakes.get(1)[1], 0.0001f, "the setting never stretches the duration");
	}

	/** A rooted rapier lunge, refused with a shake (upstream 70f5b796e), as a real call site. */
	private void rootedLunge() throws Exception {
		int start = 4 * W + 2;
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, start);
		Rapier rapier = new Rapier();
		hero.belongings.weapon = rapier;
		Buff.affect(hero, MeleeWeapon.Charger.class);
		hero.rooted = true;
		Rat rat = HeadlessLevel.at(new Rat(), start + 2);
		StubSprite.attach(rat);
		Dungeon.level.mobs.add(rat);
		Dungeon.level.heroFOV[rat.pos] = true;

		Method ability = Rapier.class.getDeclaredMethod("duelistAbility", Hero.class, Integer.class);
		ability.setAccessible(true);
		ability.invoke(rapier, hero, rat.pos);
		assertEquals(start, hero.pos, "fixture: the rooted hero stays put");
	}

	@Test
	void aGameShakeFollowsTheSetting() throws Exception {
		SPDSettings.screenShake(0);
		rootedLunge();
		assertEquals(0f, onlyShake()[0], 0.0001f, "with shake off the refusal does not move the camera");

		shakes.clear();
		HeadlessLevel.uninstall();
		SPDSettings.screenShake(4);
		rootedLunge();
		assertEquals(4f, onlyShake()[0], 0.0001f, "the refusal's 1 times the top setting");
		assertEquals(1f, onlyShake()[1], 0.0001f);
	}

	// any .shake( call that is not PixelScene.shake reaches the camera without the setting
	private static final Pattern DIRECT_SHAKE = Pattern.compile("(?<!PixelScene)\\.shake\\s*\\(");

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	@Test
	void everyShakeInTheGameGoesThroughTheSetting() throws IOException {
		Path src = Paths.get("..", "java");
		Path wrapper = src.resolve("com/qsr/customspd/scenes/PixelScene.java");
		assertTrue(Files.isRegularFile(wrapper),
				"fixture: core/src/main/java not found from " + new File(".").getAbsolutePath());

		List<String> direct = new ArrayList<>();
		List<Path> sources;
		try (Stream<Path> files = Files.walk(src)) {
			sources = files.filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".kt"))
					.collect(Collectors.toList());
		}
		for (Path p : sources) {
			List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				int comment = line.indexOf("//");
				String code = comment >= 0 ? line.substring(0, comment) : line;
				if (!DIRECT_SHAKE.matcher(code).find()) continue;
				if (p.equals(wrapper) && code.contains("Camera.main.shake(magnitude, duration)")) continue;
				direct.add(src.relativize(p) + ":" + (i + 1) + ": " + line.trim());
			}
		}
		assertTrue(direct.isEmpty(), "shakes that skip the setting: " + direct);
	}
}
