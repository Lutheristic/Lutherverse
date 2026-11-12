package com.qsr.customspd.messages;

import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Belarusian came in with upstream 2eb2f1c12. Its bundles carry v2.5.4's line only where CPDU's
 * English still reads as v2.5.4's, so text CPDU changed or added shows in English.
 */
class BelarusianLanguageTest {

	// one or two keys from each of the nine bundles, with v2.5.4's Belarusian text
	private static final String[][] SHOWN = {
			{"actors.blobs.blizzard.desc", "Тут кружыцца ледзяны буран."},
			{"actors.hero.heroclass.warrior", "ваяр"},
			{"items.armor.curses.bulk.name", "%s непаваротлівасці"},
			{"items.potions.potionofhealing.name", "Зелле Ацалення"},
			{"journal.document.adventurers_guide.intro.title", "Уступленне"},
			{"levels.features.chasm.chasm", "Бездань"},
			{"badges.endorsed", "Прысвоены медаль: %s."},
			{"plants.blandfruitbush.name", "пустафрукт"},
			{"scenes.alchemyscene.title", "Алхімія"},
			{"ui.talentspane.tier", "узровень %d"},
			{"windows.wndchooseability.message_no_crown", "Выберыце здольнасць для свайго даспеха!"},
	};

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	private static Languages belarusian() {
		Languages lang = Languages.matchCode("be");
		assertNotEquals(Languages.ENGLISH, lang, "no language has the code be");
		return lang;
	}

	@Test
	void belarusianIsListedWithItsTranslators() {
		Languages be = belarusian();
		assertEquals("беларуская", be.nativeName());
		assertEquals(Languages.Status.UNFINISHED, be.status());
		assertArrayEquals(new String[]{"AprilRain(Vadzim Navumaû)"}, be.reviewers());
		assertArrayEquals(new String[]{"4ebotar"}, be.translators());
	}

	@Test
	void switchingToBelarusianShowsItsTextFromEveryBundle() {
		Languages be = belarusian();
		Messages.setup(be);
		assertEquals(be, Messages.lang());
		assertEquals("be", Messages.locale().getLanguage());

		List<String> wrong = new ArrayList<>();
		for (String[] row : SHOWN) {
			String shown = Messages.get(row[0]);
			if (shown.contains(Messages.NO_TEXT_FOUND) || !row[1].equals(shown)) {
				wrong.add(row[0] + " shows: " + shown);
			}
		}
		assertTrue(wrong.isEmpty(), String.join("\n", wrong));

		assertEquals("ваяр", Messages.get(HeroClass.class, "warrior"));
		assertEquals("узровень 2", Messages.get("ui.talentspane.tier", 2));
		assertEquals("Прысвоены медаль: Qz7.", Messages.get((Class<?>) null, "badges.endorsed", "Qz7"));
	}

	@Test
	void textWithoutAMatchingTranslationShowsEnglish() {
		// keybearer is CPDU's own class, and CPDU's blacksmith offers a different deal than v2.5.4's
		String[] keys = {"actors.hero.heroclass.keybearer", "windows.wndblacksmith.prompt"};
		Messages.setup(Languages.ENGLISH);
		String[] english = new String[keys.length];
		for (int i = 0; i < keys.length; i++) english[i] = Messages.get(keys[i]);

		Messages.setup(belarusian());
		for (int i = 0; i < keys.length; i++) {
			assertNotEquals(keys[i], english[i], "English lacks " + keys[i]);
			assertEquals(english[i], Messages.get(keys[i]), keys[i]);
		}
	}
}
