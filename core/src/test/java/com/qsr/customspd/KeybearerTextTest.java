package com.qsr.customspd;

import com.qsr.customspd.items.weapon.melee.FrostKeyblade;
import com.qsr.customspd.items.weapon.melee.Keyblade;
import com.qsr.customspd.items.weapon.melee.StormKeyblade;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Keybearer's class and keyblade text describes the kit heroes/keybearer.json and HeroClass give. */
class KeybearerTextTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void classDescriptionNamesTheStartingKeybladesAndWhatItIdentifies() {
		String desc = Messages.get("actors.hero.heroclass.keybearer_desc");
		assertTrue(desc.contains("_" + new Keyblade().name() + "_ in hand"), desc);
		assertTrue(desc.contains("_" + new FrostKeyblade().name() + "_ in the off-hand"), desc);
		assertTrue(desc.contains("_" + new StormKeyblade().name() + "_ in the pack"), desc);
		// heroes/any.json identifies Scrolls of Identify for every hero, then heroes/keybearer.json adds the rest.
		assertTrue(desc.endsWith("The Keybearer automatically identifies:\n_-_ Scrolls of Identify\n_-_ Potions of Strength\n_-_ Scrolls of Mirror Image"), desc);
	}

	@Test
	void noKeybearerTextUsesBuildJargon() {
		String[] texts = {
				Messages.get("actors.hero.heroclass.keybearer_desc"),
				Messages.get("actors.hero.heroclass.keybearer_desc_short"),
				new Keyblade().desc(),
				new FrostKeyblade().desc(),
				new StormKeyblade().desc(),
		};
		for (String text : texts) {
			String lower = text.toLowerCase(Locale.ROOT);
			assertFalse(lower.contains("build-craft") || lower.contains("being built")
					|| lower.contains("being forged") || lower.contains(" build"), text);
		}
	}

	@Test
	void keybladeTextDoesNotCallThemPrototypes() {
		// The hero select screen still marks the class itself as a prototype; the keyblades are not.
		String[] texts = {
				Messages.get("actors.hero.heroclass.keybearer_desc"),
				new Keyblade().desc(),
				new FrostKeyblade().desc(),
				new StormKeyblade().desc(),
		};
		for (String text : texts) {
			assertFalse(text.toLowerCase(Locale.ROOT).contains("prototype"), text);
		}
	}
}
