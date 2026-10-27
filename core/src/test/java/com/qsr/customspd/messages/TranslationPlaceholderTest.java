package com.qsr.customspd.messages;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.qsr.customspd.Assets;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPDU's translated bundles were frozen near upstream v2.1.0 while the English bundles and the
 * code that formats them moved on to ~v2.5.4. Messages falls back to English only for keys a
 * translation lacks, so a stale translated key is used as-is: a %d the code now fills with a
 * String reports a formatting error and shows the raw text, a %s the code no longer fills shows
 * up literally (items.rings.ring.identify), and a dropped argument silently loses the number.
 *
 * For every key present in both an English bundle and a translation of it, this checks the
 * translation consumes the same arguments with the same conversion types as the English string
 * (positional %1$s forms may reorder; %% and %n are literals and are not compared). A key whose
 * English text is not a format string (no specifiers, or prose like "50% damage") must not gain
 * argument specifiers in translation.
 *
 * Generalises EnhancedRingsTranslationTest, which covered the one key that crashed.
 */
class TranslationPlaceholderTest {

	private static final String[] BUNDLES = {
			Assets.Messages.ACTORS,
			Assets.Messages.ITEMS,
			Assets.Messages.JOURNAL,
			Assets.Messages.LEVELS,
			Assets.Messages.MISC,
			Assets.Messages.PLANTS,
			Assets.Messages.SCENES,
			Assets.Messages.UI,
			Assets.Messages.WINDOWS
	};

	// java.util.Formatter's specifier grammar, minus the space flag: no shipped string uses it as
	// a flag, while prose like "50% damage" or "25% chance" would otherwise read as "% d"/"% c".
	private static final Pattern SPEC =
			Pattern.compile("%(\\d+\\$|<)?([-#+0,(]*)(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

	/** Argument signature of a string: argument index -> conversion category. */
	static final class Signature {
		/** False if some '%' does not start a specifier java.util.Formatter accepts. */
		final boolean valid;
		/** True if the string contains at least one accepted specifier (including %% and %n). */
		final boolean hasSpecifiers;
		final Map<Integer, String> args;

		Signature(boolean valid, boolean hasSpecifiers, Map<Integer, String> args) {
			this.valid = valid;
			this.hasSpecifiers = hasSpecifiers;
			this.args = args;
		}

		/** A string the code is meant to pass through String.format. */
		boolean isFormat() {
			return valid && hasSpecifiers;
		}
	}

	static Signature signature(String s) {
		Map<Integer, String> args = new TreeMap<>();
		boolean valid = true;
		boolean hasSpecifiers = false;
		int ordinary = 0;
		Integer last = null;
		int pos = 0;
		while (true) {
			int p = s.indexOf('%', pos);
			if (p < 0) break;
			Matcher m = SPEC.matcher(s);
			String category = null;
			if (m.find(p) && m.start() == p) {
				char conv = m.group(6).charAt(0);
				category = m.group(5) != null ? "t" : category(conv);
			}
			if (category == null || !accepted(m, category)) {
				valid = false;
				pos = p + 1;
				continue;
			}
			hasSpecifiers = true;
			pos = m.end();
			if (category.isEmpty()) continue; // %% or %n: consumes no argument

			Integer idx;
			String index = m.group(1);
			if ("<".equals(index)) {
				if (last == null) {
					valid = false;
					continue;
				}
				idx = last;
			} else if (index != null) {
				idx = Integer.valueOf(index.substring(0, index.length() - 1));
			} else {
				idx = ++ordinary;
			}
			last = idx;
			String prev = args.get(idx);
			args.put(idx, prev == null || prev.equals(category) ? category : prev + "|" + category);
		}
		return new Signature(valid, hasSpecifiers, args);
	}

	/** Conversion category, "" for conversions that take no argument, null for unknown ones. */
	private static String category(char conv) {
		switch (conv) {
			case 's': case 'S': return "s";
			case 'd': case 'o': case 'x': case 'X': return "d";
			case 'e': case 'E': case 'f': case 'g': case 'G': case 'a': case 'A': return "f";
			case 'c': case 'C': return "c";
			case 'b': case 'B': return "b";
			case 'h': case 'H': return "h";
			case '%': case 'n': return "";
			default: return null;
		}
	}

	/** Lets the real Formatter judge flags/width/precision (e.g. Hungarian "50%-os" is prose). */
	private static boolean accepted(Matcher m, String category) {
		String spec = "%" + m.group(2)
				+ (m.group(3) == null ? "" : m.group(3))
				+ (m.group(4) == null ? "" : m.group(4))
				+ (m.group(5) == null ? "" : m.group(5))
				+ m.group(6);
		Object arg;
		switch (category) {
			case "d": arg = 1; break;
			case "f": arg = 1.5; break;
			case "c": arg = 'c'; break;
			case "b": arg = true; break;
			case "t": arg = 0L; break;
			default: arg = "x"; break;
		}
		try {
			String.format(Locale.ROOT, spec, arg);
			return true;
		} catch (IllegalFormatException e) {
			return false;
		}
	}

	/** Null if the translation is compatible with the English string, else why it is not. */
	static String incompatibility(String english, String translation) {
		Signature e = signature(english);
		Signature t = signature(translation);
		if (e.isFormat()) {
			if (!t.valid) return "translation has a '%' that is not a valid specifier";
			if (!e.args.equals(t.args)) return "arguments differ";
			return null;
		}
		if (!t.args.isEmpty()) return "English takes no arguments but the translation has specifiers";
		return null;
	}

	private static Map<String, String> load(Path file) throws IOException {
		ObjectMap<String, String> raw = new ObjectMap<>();
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			PropertiesUtils.load(raw, r);
		}
		Map<String, String> sorted = new TreeMap<>();
		for (ObjectMap.Entry<String, String> entry : raw) sorted.put(entry.key, entry.value);
		return sorted;
	}

	// Keys whose English changed its arguments ahead of the translations, which the next
	// translation sync replaces. Remove an entry once no translation of it mismatches.
	private static final List<String> KNOWN_TRANSLATION_MISMATCHES = Arrays.asList(
			"items.bombs.bomb.desc",
			"items.weapon.melee.magesstaff.imbue_talent"
	);

	@Test
	void everyTranslatedFormatStringTakesTheSameArgumentsAsTheEnglishOne() throws IOException {
		Map<String, List<String>> offenders = new LinkedHashMap<>();
		List<String> stillParked = new ArrayList<>();
		int filesCompared = 0;
		int keysCompared = 0;
		int formatKeysCompared = 0;

		for (String bundle : BUNDLES) {
			Path englishFile = Paths.get(bundle + ".properties");
			assertTrue(Files.isRegularFile(englishFile), "missing English bundle " + englishFile.toAbsolutePath());
			Map<String, String> english = load(englishFile);

			for (Languages lang : Languages.values()) {
				if (lang == Languages.ENGLISH) continue;
				Path file = Paths.get(bundle + "_" + lang.code() + ".properties");
				if (!Files.isRegularFile(file)) continue;
				filesCompared++;

				for (Map.Entry<String, String> entry : load(file).entrySet()) {
					String en = english.get(entry.getKey());
					if (en == null) continue;
					keysCompared++;
					if (signature(en).isFormat()) formatKeysCompared++;
					String why = incompatibility(en, entry.getValue());
					if (why != null && KNOWN_TRANSLATION_MISMATCHES.contains(entry.getKey())) {
						if (!stillParked.contains(entry.getKey())) stillParked.add(entry.getKey());
					} else if (why != null) {
						List<String> list = offenders.get(lang.code());
						if (list == null) offenders.put(lang.code(), list = new ArrayList<>());
						list.add(file.getFileName() + " " + entry.getKey() + ": " + why
								+ " (English " + signature(en).args + ", translation "
								+ signature(entry.getValue()).args + ")");
					}
				}
			}
		}

		// Guard against passing vacuously (wrong working directory, renamed bundles, empty files).
		assertEquals(BUNDLES.length * (Languages.values().length - 1), filesCompared, "translated bundle files compared");
		assertTrue(keysCompared > 10000, "only " + keysCompared + " translated keys compared");
		assertTrue(formatKeysCompared > 1000, "only " + formatKeysCompared + " translated format strings compared");

		if (!offenders.isEmpty()) {
			StringBuilder msg = new StringBuilder("Translated strings whose format placeholders do not match English:\n");
			int total = 0;
			for (Map.Entry<String, List<String>> e : offenders.entrySet()) {
				msg.append("  ").append(e.getKey()).append(": ").append(e.getValue().size()).append('\n');
				total += e.getValue().size();
			}
			for (List<String> list : offenders.values()) {
				for (String line : list) msg.append("    ").append(line).append('\n');
			}
			msg.append(total).append(" offending keys in ").append(offenders.size()).append(" languages");
			throw new AssertionError(msg.toString());
		}

		List<String> fixed = new ArrayList<>(KNOWN_TRANSLATION_MISMATCHES);
		fixed.removeAll(stillParked);
		assertTrue(fixed.isEmpty(), "translations now match English, remove from KNOWN_TRANSLATION_MISMATCHES: " + fixed);
	}

	// Pack overrides already stale before the pack check existed. Anything else that fails it is new;
	// remove an entry here once its pack string is fixed.
	private static final List<String> KNOWN_PACK_MISMATCHES = Arrays.asList(
			"Jowiro's Class Reskins/messages.properties items.weapon.melee.runicblade.ability_desc",
			"Slugcat skins/messages.properties items.weapon.spiritbow.stats"
	);

	// Messages loads a pack's messages.properties (or messages_<code>.properties) ahead of the
	// base bundles, so a pack override of a key the code formats must take the same arguments.
	@Test
	void everyPackOverrideTakesTheSameArgumentsAsTheEnglishOne() throws IOException {
		Map<String, String> english = new TreeMap<>();
		for (String bundle : BUNDLES) english.putAll(load(Paths.get(bundle + ".properties")));

		Path marketplace = Paths.get("../../../../marketplace");
		assertTrue(Files.isDirectory(marketplace), "missing " + marketplace.toAbsolutePath());
		List<Path> packFiles = new ArrayList<>();
		try (DirectoryStream<Path> packs = Files.newDirectoryStream(marketplace)) {
			for (Path pack : packs) {
				if (!Files.isDirectory(pack)) continue;
				try (DirectoryStream<Path> files = Files.newDirectoryStream(pack, "messages*.properties")) {
					for (Path file : files) packFiles.add(file);
				}
			}
		}
		Collections.sort(packFiles);

		List<String> offenders = new ArrayList<>();
		List<String> stillParked = new ArrayList<>();
		int keysCompared = 0;
		for (Path file : packFiles) {
			Map<String, String> pack = load(file);
			for (Map.Entry<String, String> entry : pack.entrySet()) {
				String en = english.get(entry.getKey());
				if (en == null) continue;
				keysCompared++;
				String why = incompatibility(en, entry.getValue());
				// unidentified items read the typical_ form, which would otherwise fall back to English
				String key = entry.getKey();
				int dot = key.lastIndexOf('.');
				String typical = key.substring(0, dot + 1) + "typical_" + key.substring(dot + 1);
				if (why == null && english.containsKey(typical) && !pack.containsKey(typical)) {
					why = "overridden without " + typical.substring(dot + 1);
				}
				if (why == null) continue;
				String id = file.getParent().getFileName() + "/" + file.getFileName() + " " + entry.getKey();
				if (KNOWN_PACK_MISMATCHES.contains(id)) {
					stillParked.add(id);
				} else {
					offenders.add(id + ": " + why + " (English " + signature(en).args
							+ ", pack " + signature(entry.getValue()).args + ")");
				}
			}
		}

		assertTrue(packFiles.size() > 20, "only " + packFiles.size() + " pack message files found");
		assertTrue(keysCompared > 1000, "only " + keysCompared + " pack keys compared");
		assertTrue(offenders.isEmpty(), "Pack strings whose format placeholders do not match English:\n    "
				+ String.join("\n    ", offenders) + "\n" + offenders.size() + " offending keys");
		List<String> fixed = new ArrayList<>(KNOWN_PACK_MISMATCHES);
		fixed.removeAll(stillParked);
		assertTrue(fixed.isEmpty(), "now match English, remove from KNOWN_PACK_MISMATCHES: " + fixed);
	}

	@Test
	void staleKeysRenderThroughEveryLanguage() {
		HeadlessGdx.boot();
		try {
			for (Languages lang : Languages.values()) {
				Messages.setup(lang);
				// CPDU passes the level name as a String: whatever the language (today the
				// English fallback, later perhaps a '%s' translation), the name must show and
				// no unformatted '%d' may leak through
				String descend = Messages.get((Class<?>) null, "scenes.gamescene.descend", "Qz7");
				assertTrue(descend.contains("Qz7") && !descend.contains("%"), lang + " descend: " + descend);
				// code passes no argument (5e5989124): no literal placeholder may show
				String identify = Messages.get("items.rings.ring.identify");
				assertTrue(!identify.contains("%"), lang + ": " + identify);
				// two-argument armor range: both bounds must show
				String holdfast = Messages.get("actors.buffs.holdfast.desc", 17, 29);
				assertTrue(holdfast.contains("17") && holdfast.contains("29"), lang + ": " + holdfast);
			}
		} finally {
			Messages.setup(Languages.ENGLISH);
		}
	}

	@Test
	void theCheckerSeesTheMismatchesItIsMeantToCatch() {
		// conversion type changed: the e13af36b2 EnhancedRings crash
		assertNotNull(incompatibility("for %s turns", "für %d Züge"));
		// a placeholder the code no longer fills: items.rings.ring.identify
		assertNotNull(incompatibility("You are now familiar enough.", "Es ist ein %s."));
		// a dropped argument: actors.buffs.holdfast.desc
		assertNotNull(incompatibility("%1$d armor, %2$d dmg", "%d Rüstung"));
		// an extra argument
		assertNotNull(incompatibility("%d turns", "%d turns, %d more"));
		// a lone '%' inside a format string
		assertNotNull(incompatibility("%d damage, 50%% chance", "%d Schaden, 50% Chance"));

		// reordered positional forms, %% literals, prose and plain strings are fine
		assertNull(incompatibility("%s hits %s for %d", "%2$s wird von %1$s für %3$d getroffen"));
		assertNull(incompatibility("%1$s and %1$s", "%1$s und %<s"));
		assertNull(incompatibility("%d damage, 50%% chance", "%d Schaden, 50%% Chance"));
		assertNull(incompatibility("deals +50% damage", "50% mehr Schaden"));
		assertNull(incompatibility("deals +50% damage", "+50%-os sebzést"));
		assertNull(incompatibility("plain", "einfach"));
		assertNull(incompatibility("%,d gold", "%,d Gold"));
	}
}
