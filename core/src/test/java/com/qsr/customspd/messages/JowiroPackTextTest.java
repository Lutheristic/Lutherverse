package com.qsr.customspd.messages;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.qsr.customspd.Assets;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jowiro's Class Reskins only renames the heroes (Chieftain, Sorceress, Ronin, Ranger, and a male
 * Monk for the Duelist), so each of its overrides must say what the English says with the pack's
 * names: a key the English still has, the same numbers, format arguments, paragraphs and
 * highlights, no stock class name, and the pack's own pronouns.
 */
class JowiroPackTextTest {

	private static final String[] BUNDLES = {
			Assets.Messages.ACTORS, Assets.Messages.ITEMS, Assets.Messages.JOURNAL,
			Assets.Messages.LEVELS, Assets.Messages.MISC, Assets.Messages.PLANTS,
			Assets.Messages.SCENES, Assets.Messages.UI, Assets.Messages.WINDOWS
	};

	private static final Path PACK = Paths.get("../../../../marketplace/Jowiro's Class Reskins/messages.properties");

	private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?");
	private static final Pattern SPEC = Pattern.compile("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]");
	private static final Pattern STOCK_CLASS = Pattern.compile("\\b(Warrior|Mage|Rogue|Huntress|Duelist)\\b");
	private static final Pattern MONK = Pattern.compile("\\bMonk\\b");
	private static final Pattern SORCERESS = Pattern.compile("\\bSorceress\\b");
	private static final Pattern FEMININE = Pattern.compile("\\b([Ss]he|[Hh]er|[Hh]erself)\\b");
	private static final Pattern MASCULINE = Pattern.compile("\\b([Hh]e|[Hh]is|[Hh]im|[Hh]imself)\\b");

	private static Map<String, String> load(Path file) throws IOException {
		ObjectMap<String, String> raw = new ObjectMap<>();
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			PropertiesUtils.load(raw, r);
		}
		Map<String, String> sorted = new TreeMap<>();
		for (ObjectMap.Entry<String, String> e : raw) sorted.put(e.key, e.value);
		return sorted;
	}

	private static List<String> all(Pattern p, String s) {
		List<String> out = new ArrayList<>();
		Matcher m = p.matcher(s);
		while (m.find()) out.add(m.group());
		return out;
	}

	private static List<String> sorted(List<String> list) {
		Collections.sort(list);
		return list;
	}

	private static int count(String s, String part) {
		int n = 0;
		for (int i = s.indexOf(part); i >= 0; i = s.indexOf(part, i + part.length())) n++;
		return n;
	}

	@Test
	void everyOverrideMatchesTheEnglishInThePacksNames() throws IOException {
		Map<String, String> english = new TreeMap<>();
		for (String bundle : BUNDLES) english.putAll(load(Paths.get(bundle + ".properties")));
		assertTrue(Files.isRegularFile(PACK), "missing " + PACK.toAbsolutePath());
		Map<String, String> pack = load(PACK);
		assertTrue(pack.size() > 600, "only " + pack.size() + " pack keys");

		List<String> wrong = new ArrayList<>();
		for (Map.Entry<String, String> e : pack.entrySet()) {
			String key = e.getKey();
			String text = e.getValue();
			String en = english.get(key);
			if (en == null) {
				wrong.add(key + ": the English has no such key");
				continue;
			}
			if (!sorted(all(NUMBER, text)).equals(sorted(all(NUMBER, en)))) {
				wrong.add(key + ": numbers " + sorted(all(NUMBER, text)) + ", English " + sorted(all(NUMBER, en)));
			}
			if (!all(SPEC, text).equals(all(SPEC, en))) {
				wrong.add(key + ": format " + all(SPEC, text) + ", English " + all(SPEC, en));
			}
			if (count(text, "\n\n") != count(en, "\n\n")) {
				wrong.add(key + ": " + (count(text, "\n\n") + 1) + " paragraphs, English " + (count(en, "\n\n") + 1));
			}
			if (count(text, "_") != count(en, "_")) {
				wrong.add(key + ": " + count(text, "_") + " highlight marks, English " + count(en, "_"));
			}
			if (STOCK_CLASS.matcher(text).find()) {
				wrong.add(key + ": names a stock class: " + text);
			}
			if (MONK.matcher(text).find() && FEMININE.matcher(text).find()) {
				wrong.add(key + ": the Monk is a he here: " + text);
			}
			if (SORCERESS.matcher(text).find() && MASCULINE.matcher(text).find()) {
				wrong.add(key + ": the Sorceress is a she here: " + text);
			}
		}
		assertTrue(wrong.isEmpty(), wrong.size() + " stale pack lines:\n" + String.join("\n", wrong));
	}

	@Test
	void theCheckSeesAStaleLine() {
		// the pre-port hold fast line: armor 0-2 where the English now gives 1-2
		assertTrue(!sorted(all(NUMBER, "_0-2 armor_")).equals(sorted(all(NUMBER, "_1-2 armor_"))));
		assertTrue(STOCK_CLASS.matcher("the real Rogue!").find());
		assertTrue(MONK.matcher("The Monk has just spent a turn waiting, her next").find()
				&& FEMININE.matcher("The Monk has just spent a turn waiting, her next").find());
		assertTrue(!all(SPEC, "has +300% enchantment power.").equals(all(SPEC, "has _+%d%% enchantment power._")));
	}
}
