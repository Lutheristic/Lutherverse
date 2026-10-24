package com.qsr.customspd.messages;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.qsr.customspd.Assets;
import com.qsr.customspd.actors.buffs.Barkskin;
import com.qsr.customspd.actors.buffs.Charm;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Keys the cpdu-fz84 v2.5.4 translation sync could not see when it ran, because CPDU's English for
 * them was wrong or missing at the snapshot it was computed against (fd96ad582). Each was fixed in
 * English afterwards, so the sync's own rule now selects it; these pin the result in every language.
 *
 * TranslationPlaceholderTest cannot catch these: a stale translation with the same placeholders as
 * the new English passes it, and a key no bundle defines is never compared at all.
 */
class PostSyncTranslationTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	private static int paragraphs(String text) {
		return text.split("\n\n", -1).length;
	}

	private static String fullKey(Class<?> cls, String key) {
		return cls.getName().replace("com.qsr.customspd.", "").toLowerCase(Locale.ENGLISH) + "." + key;
	}

	/**
	 * The shipped translated actors bundles, keyed by language, read from the files themselves.
	 * What Messages resolves for a language also depends on the JVM's Locale mapping (Java 17+ maps
	 * "in" to "id", so Indonesian renders the English fallback here), so a rendered-text check alone
	 * would pass Indonesian vacuously.
	 */
	private static Map<Languages, ObjectMap<String, String>> translatedActorBundles() throws IOException {
		Map<Languages, ObjectMap<String, String>> out = new LinkedHashMap<>();
		for (Languages lang : Languages.values()) {
			if (lang == Languages.ENGLISH) continue;
			Path bundle = Paths.get(Assets.Messages.ACTORS + "_" + lang.code() + ".properties");
			if (!Files.isRegularFile(bundle)) continue;
			ObjectMap<String, String> props = new ObjectMap<>();
			try (Reader r = Files.newBufferedReader(bundle, StandardCharsets.UTF_8)) {
				PropertiesUtils.load(props, r);
			}
			out.put(lang, props);
		}
		assertEquals(18, out.size(), "translated actors bundles found");
		return out;
	}

	/**
	 * v2.5.4 (4ad161622 in CPDU) added a closing paragraph to the barkskin description: only the
	 * strongest of several barkskin sources applies. The English was split across two physical lines
	 * until a0a54ce9f, so the sync read it as different from v2.5.4 and left every translation on the
	 * v2.1.0 text, one paragraph short. Every language must tell the player the stacking rule.
	 */
	@Test
	void barkskinDescriptionHasTheStackingParagraphInEveryLanguage() throws IOException {
		Messages.setup(Languages.ENGLISH);
		String english = Messages.get(Barkskin.class, "desc", 4, "7");
		assertEquals(4, paragraphs(english), "test setup: English is v2.5.4's four paragraphs: " + english);

		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			String desc = Messages.get(Barkskin.class, "desc", 4, "7");
			assertEquals(paragraphs(english), paragraphs(desc),
					lang + " barkskin description must carry v2.5.4's stacking paragraph: " + desc);
		}
		String key = fullKey(Barkskin.class, "desc");
		for (Map.Entry<Languages, ObjectMap<String, String>> e : translatedActorBundles().entrySet()) {
			String value = e.getValue().get(key);
			assertNotNull(value, e.getKey() + " lacks " + key);
			assertEquals(paragraphs(english), paragraphs(value),
					e.getKey() + " file's barkskin description must carry v2.5.4's stacking paragraph: " + value);
		}
	}

	/**
	 * A key the code reads must exist: Messages.get walks the class's superclasses and, finding
	 * nothing, returns the last key it tried, ending at java.lang.Object. Checks the English and
	 * that each shipped translation of the bundle has its own line (v2.5.4's, imported by the
	 * cpdu-fz84 4/4 rule), and that nothing renders as a key path in any language.
	 */
	private static void assertKeyShipsEverywhere(Class<?> cls, String key, String englishText) throws IOException {
		Messages.setup(Languages.ENGLISH);
		String english = Messages.get(cls, key);
		assertEquals(englishText, english, "English " + cls.getSimpleName() + "." + key);
		String fullKey = fullKey(cls, key);

		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			String text = Messages.get(cls, key);
			assertFalse(text.startsWith("java.lang.object.") || text.equals(fullKey),
					lang + " renders a missing key: " + text);
		}
		for (Map.Entry<Languages, ObjectMap<String, String>> e : translatedActorBundles().entrySet()) {
			String value = e.getValue().get(fullKey);
			assertNotNull(value, e.getKey() + " lacks " + fullKey);
			assertNotEquals(english, value, e.getKey() + " has only the English text of " + fullKey);
		}
	}

	/**
	 * Hero.actAttack (upstream f857349a5, v2.4.0) logs Messages.get(Charm.class, "cant_attack") when
	 * the hero taps the enemy that charmed them. CPDU ported the call but never the key, so the log
	 * read "java.lang.object.cant_attack"; the sync could not import translations for a key CPDU's
	 * English lacked.
	 */
	@Test
	void charmCantAttackShipsInEveryLanguage() throws IOException {
		assertKeyShipsEverywhere(Charm.class, "cant_attack",
				"You are charmed by that enemy and can't attack them.");
	}
}
