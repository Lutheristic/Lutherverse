package com.qsr.customspd.messages;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.qsr.customspd.Assets;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every message key is a dotted path (actors.buffs.barkskin.desc). A multi-paragraph value broken
 * across physical lines without escaped newlines parses as a new property keyed by the first word
 * of the stray line ("If", "Grants", "When"): the paragraph silently disappears from the game and
 * nothing else notices, because Messages suppresses missing-key errors. This loads every bundle
 * with the same parser the game uses and rejects any key without a dot.
 */
class PropertiesKeyShapeTest {

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

	@Test
	void everyMessageKeyIsADottedPath() throws IOException {
		List<String> offenders = new ArrayList<>();
		int files = 0;
		for (String bundle : BUNDLES) {
			Path english = Paths.get(bundle + ".properties");
			assertTrue(Files.isRegularFile(english), "missing English bundle " + english.toAbsolutePath());
			String base = english.getFileName().toString().replace(".properties", "");
			try (DirectoryStream<Path> dir = Files.newDirectoryStream(english.getParent(), base + "*.properties")) {
				for (Path file : dir) {
					files++;
					ObjectMap<String, String> props = new ObjectMap<>();
					try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
						PropertiesUtils.load(props, r);
					}
					for (ObjectMap.Entry<String, String> e : props) {
						if (!e.key.contains(".")) {
							offenders.add(file.getFileName() + ": '" + e.key + "' = " + e.value);
						}
					}
				}
			}
		}
		assertTrue(files >= BUNDLES.length * 2, "scanned only " + files + " bundle files");
		assertTrue(offenders.isEmpty(), "keys that are not dotted paths (a value split across lines?):\n  "
				+ String.join("\n  ", offenders));
	}
}
