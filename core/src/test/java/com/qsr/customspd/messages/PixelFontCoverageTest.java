package com.qsr.customspd.messages;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.qsr.customspd.Assets;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The desktop and Android font setup draws any text without Korean, Chinese or Japanese
 * characters with fonts/pixel_font.ttf unless the player turns on the system font, and the
 * system font is only on by default for those three languages. Every other language therefore
 * needs a pixel font glyph for each character its bundles and its own name use.
 */
class PixelFontCoverageTest {

	private static final String[] BUNDLES = {
			Assets.Messages.ACTORS, Assets.Messages.ITEMS, Assets.Messages.JOURNAL,
			Assets.Messages.LEVELS, Assets.Messages.MISC, Assets.Messages.PLANTS,
			Assets.Messages.SCENES, Assets.Messages.UI, Assets.Messages.WINDOWS
	};

	// drawn with the system or fallback CJK fonts instead
	private static final List<Languages> CJK = Arrays.asList(Languages.KOREAN, Languages.CHINESE, Languages.JAPANESE);

	private static List<String> values(Path file) throws IOException {
		ObjectMap<String, String> raw = new ObjectMap<>();
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			PropertiesUtils.load(raw, r);
		}
		List<String> out = new ArrayList<>();
		for (ObjectMap.Entry<String, String> e : raw) out.add(e.value);
		return out;
	}

	@Test
	void thePixelFontHasEveryCharacterTheNonCjkLanguagesShow() throws Exception {
		System.setProperty("java.awt.headless", "true");
		File ttf = new File("fonts/pixel_font.ttf");
		assertTrue(ttf.isFile(), "missing " + ttf.getAbsolutePath());
		Font font = Font.createFont(Font.TRUETYPE_FONT, ttf);
		// a CJK ideograph is outside the pixel font, so a missing glyph is reported
		assertTrue(!font.canDisplay(0x4E00), "the pixel font claims a CJK glyph");

		Languages belarusian = Languages.matchCode("be");
		assertNotEquals(Languages.ENGLISH, belarusian, "no language has the code be");

		Map<String, Set<String>> missing = new TreeMap<>();
		List<Languages> checked = new ArrayList<>();
		int files = 0;
		for (Languages lang : Languages.values()) {
			if (CJK.contains(lang)) continue;
			List<String> texts = new ArrayList<>();
			texts.add(lang.nativeName());
			for (String bundle : BUNDLES) {
				Path file = Paths.get(bundle + (lang == Languages.ENGLISH ? "" : "_" + lang.code()) + ".properties");
				if (!Files.isRegularFile(file)) continue;
				files++;
				texts.addAll(values(file));
			}
			for (String text : texts) {
				for (int i = 0; i < text.length(); ) {
					int cp = text.codePointAt(i);
					i += Character.charCount(cp);
					if (cp == '\n' || cp == '\r' || cp == '\t') continue;
					if (!font.canDisplay(cp)) {
						Set<String> set = missing.get(lang.name());
						if (set == null) missing.put(lang.name(), set = new TreeSet<>());
						set.add(String.format(Locale.ROOT, "U+%04X", cp));
					}
				}
			}
			checked.add(lang);
		}

		assertTrue(checked.contains(belarusian), "Belarusian was not checked");
		assertEquals(BUNDLES.length * checked.size(), files, "bundle files read");
		assertTrue(missing.isEmpty(), "characters the pixel font cannot draw: " + missing);
	}
}
