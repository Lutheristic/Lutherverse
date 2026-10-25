package com.qsr.customspd.messages;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.qsr.customspd.Assets;
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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 62aefca1c: number formatting follows the selected language's locale instead of being pinned
 * to English. Messages.format and Messages.decimalFormat are what item/actor strings and every damage or
 * stat readout go through; the windows' NumberFormat.getInstance(Messages.locale()) shares the locale.
 */
class MessagesLocaleTest {

	private static final String[] BUNDLES = {
			Assets.Messages.ACTORS, Assets.Messages.ITEMS, Assets.Messages.JOURNAL,
			Assets.Messages.LEVELS, Assets.Messages.MISC, Assets.Messages.PLANTS,
			Assets.Messages.SCENES, Assets.Messages.UI, Assets.Messages.WINDOWS
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
	void formatFollowsTheLanguageLocale() {
		Messages.setup(Languages.ENGLISH);
		assertEquals("1,234,567", Messages.format("%,d", 1234567));

		Messages.setup(Languages.GERMAN);
		assertEquals("1.234.567", Messages.format("%,d", 1234567));
	}

	@Test
	void decimalFormatFollowsTheLanguageLocale() {
		Messages.setup(Languages.ENGLISH);
		assertEquals("1.5", Messages.decimalFormat("#.##", 1.5));

		// The cached DecimalFormat for "#.##" was built for English; setup() must have dropped it.
		Messages.setup(Languages.GERMAN);
		assertEquals("1,5", Messages.decimalFormat("#.##", 1.5));
	}

	private static TreeMap<String, String> read(Path file) throws IOException {
		ObjectMap<String, String> raw = new ObjectMap<>();
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			PropertiesUtils.load(raw, r);
		}
		TreeMap<String, String> sorted = new TreeMap<>();
		for (ObjectMap.Entry<String, String> e : raw) sorted.put(e.key, e.value);
		return sorted;
	}

	/**
	 * Java 17+ turns new Locale("in") into "id", so I18NBundle looked for *_id.properties and
	 * Indonesian players got English everywhere on desktop (upstream 0b93fe2f6). For every
	 * language and every bundle, the first key whose translation differs from English must
	 * render that translation.
	 */
	@Test
	void everyLanguageLoadsItsOwnBundles() throws IOException {
		List<String> failures = new ArrayList<>();
		int checked = 0;
		for (Languages lang : Languages.values()) {
			if (lang == Languages.ENGLISH) continue;
			Messages.setup(lang);
			for (String bundle : BUNDLES) {
				Path file = Paths.get(bundle + "_" + lang.code() + ".properties");
				if (!Files.isRegularFile(file)) continue;
				TreeMap<String, String> english = read(Paths.get(bundle + ".properties"));
				for (java.util.Map.Entry<String, String> e : read(file).entrySet()) {
					String en = english.get(e.getKey());
					if (en == null || en.equals(e.getValue())) continue;
					String shown = Messages.get(e.getKey());
					if (!e.getValue().equals(shown)) {
						failures.add(lang + " " + file.getFileName() + " " + e.getKey() + " shows: " + shown);
					}
					checked++;
					break;
				}
			}
		}
		assertTrue(failures.isEmpty(), String.join("\n", failures));
		assertEquals(BUNDLES.length * (Languages.values().length - 1), checked, "bundles checked");
	}

	@Test
	void indonesianShowsIndonesian() {
		Messages.setup(Languages.INDONESIAN);
		assertEquals("pendekar", Messages.get("actors.hero.heroclass.warrior"));
	}

	/** A key no translation has must still show the English text, Indonesian included. */
	@Test
	void keysATranslationLacksFallBackToEnglish() {
		Messages.setup(Languages.ENGLISH);
		String english = Messages.get("actors.hero.heroclass.keybearer");
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			assertEquals(english, Messages.get("actors.hero.heroclass.keybearer"), lang.name());
		}
	}
}
