package com.qsr.customspd.levels;

import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream da42665e8: level feelings carry their own title and description under
 * levels.level$feeling, the old scenes.gamescene feeling lines are gone, and each floor
 * landmark record reads the feeling text.
 */
class LevelFeelingTextPortTest {

	private static final String[] FEELING_KEYS = {"chasm", "water", "grass", "dark", "large", "traps", "secrets"};

	private static final String[] LOCALES = {"be", "cs", "de", "el", "es", "fr", "hu", "in", "it",
			"ja", "ko", "nl", "pl", "pt", "ru", "tr", "uk", "vi", "zh"};

	//v2.5.4's be files carry these lines only as copies of the English text, so the locale
	//takes nothing and the English base line shows instead
	private static final HashSet<String> ENGLISH_ONLY_IN_BE = new HashSet<>(Arrays.asList(
			"levels.level$feeling.chasm_title", "levels.level$feeling.water_title",
			"levels.level$feeling.grass_title", "levels.level$feeling.dark_title",
			"levels.level$feeling.large_title", "levels.level$feeling.traps_title",
			"levels.level$feeling.secrets_title", "journal.notes$landmark.distant_well"));

	private HeadlessRun run;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261117L);
	}

	@AfterEach
	void tearDown() {
		run.close();
	}

	private static boolean isFallback(String text) {
		//Messages walks up to Object when a key is missing, so the fallback reads java.lang.*
		return text.startsWith("java.lang.") || text.startsWith("levels.level$feeling.");
	}

	@Test
	void everyFeelingHasTitleAndDesc() {
		for (Level.Feeling f : Level.Feeling.values()) {
			if (f == Level.Feeling.NONE) continue;
			String key = f.name().toLowerCase(Locale.ENGLISH);
			assertFalse(isFallback(f.title()), f + " title is the missing-key fallback: " + f.title());
			assertFalse(isFallback(f.desc()), f + " desc is the missing-key fallback: " + f.desc());
			assertEquals(key + " floor", f.title(), f + " title");
		}
		assertEquals("Your steps echo across the dungeon.", Level.Feeling.CHASM.desc());
		assertEquals("The atmosphere hints that this floor hides many secrets.", Level.Feeling.SECRETS.desc());
	}

	private static Properties load(String path) throws IOException {
		Properties p = new Properties();
		try (InputStream in = new FileInputStream(new File(path))) {
			p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		assertFalse(p.isEmpty(), "test setup: " + path + " read");
		return p;
	}

	@Test
	void sceneFeelingLinesAreGone() throws IOException {
		Properties p = load("messages/scenes/scenes.properties");
		for (String k : FEELING_KEYS) {
			assertFalse(p.containsKey("scenes.gamescene." + k), "scenes.gamescene." + k + " still present");
		}
	}

	@Test
	void floorLandmarksReadTheFeelingText() {
		for (Level.Feeling f : Level.Feeling.values()) {
			if (f == Level.Feeling.NONE) continue;
			String key = f.name().toLowerCase(Locale.ENGLISH);
			Notes.LandmarkRecord rec = new Notes.LandmarkRecord(Notes.Landmark.valueOf(f.name() + "_FLOOR"), "3");
			assertEquals(Messages.get(Level.Feeling.class, key + "_title"), rec.title(), f + " title");
			assertEquals(Messages.get(Level.Feeling.class, key + "_desc"), rec.desc(), f + " desc");
			assertEquals(f.title(), rec.title());
			assertEquals(f.desc(), rec.desc());
			assertFalse(isFallback(rec.title()), f + " record title is the fallback: " + rec.title());
			assertFalse(isFallback(rec.desc()), f + " record desc is the fallback: " + rec.desc());
		}
	}

	@Test
	void translatedSceneFeelingLinesAreGone() throws IOException {
		for (String lang : LOCALES) {
			Properties p = load("messages/scenes/scenes_" + lang + ".properties");
			for (String k : FEELING_KEYS) {
				assertFalse(p.containsKey("scenes.gamescene." + k),
						"scenes_" + lang + " still has scenes.gamescene." + k);
			}
		}
	}

	@Test
	void everyLocaleHasTheFeelingLines() throws IOException {
		for (String lang : LOCALES) {
			Properties p = load("messages/levels/levels_" + lang + ".properties");
			for (String f : FEELING_KEYS) {
				for (String suffix : new String[]{"_title", "_desc"}) {
					String key = "levels.level$feeling." + f + suffix;
					if (lang.equals("be") && ENGLISH_ONLY_IN_BE.contains(key)) {
						assertFalse(p.containsKey(key), "levels_be should leave " + key + " to English");
					} else {
						assertTrue(p.containsKey(key), "levels_" + lang + " lacks " + key);
					}
				}
			}
		}
	}

	@Test
	void everyLocaleHasTheNewLandmarkLines() throws IOException {
		for (String lang : LOCALES) {
			Properties p = load("messages/journal/journal_" + lang + ".properties");
			for (String name : new String[]{"distant_well", "rat_king"}) {
				String key = "journal.notes$landmark." + name;
				if (lang.equals("be") && ENGLISH_ONLY_IN_BE.contains(key)) {
					assertFalse(p.containsKey(key), "journal_be should leave " + key + " to English");
				} else {
					assertTrue(p.containsKey(key), "journal_" + lang + " lacks " + key);
				}
			}
		}
	}
}
