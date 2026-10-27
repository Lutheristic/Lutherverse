package com.qsr.customspd.messages;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import com.qsr.customspd.Assets;
import com.qsr.customspd.actors.buffs.Barkskin;
import com.qsr.customspd.actors.buffs.Charm;
import com.qsr.customspd.actors.buffs.MonkEnergy;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.levels.traps.PoisonDartTrap;
import com.qsr.customspd.levels.traps.WornDartTrap;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keys the cpdu-fz84 v2.5.4 translation sync could not see when it ran, because CPDU's English for
 * them was wrong or missing at the snapshot it was computed against (9dc90bfbd). Each was fixed in
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
	 * "in" to "id"), so a rendered-text check alone could pass a language vacuously if its bundle
	 * stopped loading.
	 */
	private static Map<Languages, ObjectMap<String, String>> translatedActorBundles() throws IOException {
		return translatedBundles(Assets.Messages.ACTORS);
	}

	private static Map<Languages, ObjectMap<String, String>> translatedBundles(String base) throws IOException {
		Map<Languages, ObjectMap<String, String>> out = new LinkedHashMap<>();
		for (Languages lang : Languages.values()) {
			if (lang == Languages.ENGLISH) continue;
			Path bundle = Paths.get(base + "_" + lang.code() + ".properties");
			if (!Files.isRegularFile(bundle)) continue;
			ObjectMap<String, String> props = new ObjectMap<>();
			try (Reader r = Files.newBufferedReader(bundle, StandardCharsets.UTF_8)) {
				PropertiesUtils.load(props, r);
			}
			out.put(lang, props);
		}
		assertEquals(18, out.size(), "translated " + base + " bundles found");
		return out;
	}

	/**
	 * v2.5.4 (5674c52ed in CPDU) added a closing paragraph to the barkskin description: only the
	 * strongest of several barkskin sources applies. The English was split across two physical lines
	 * until 804f76ac3, so the sync read it as different from v2.5.4 and left every translation on the
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
		assertKeyShipsEverywhere(Assets.Messages.ACTORS, cls, key, englishText);
	}

	private static void assertKeyShipsEverywhere(String base, Class<?> cls, String key, String englishText) throws IOException {
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
		for (Map.Entry<Languages, ObjectMap<String, String>> e : translatedBundles(base).entrySet()) {
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

	/**
	 * Char.attack (upstream 1466dea4b, v2.5.0) floats Messages.get(CombinedLethalityAbilityTracker
	 * .class, "executed") over an enemy Combined Lethality executes. CPDU took the code (6d4323214)
	 * but kept only v2.1.0's key, talent$combinedlethalitytriggertracker.executed, so the text read
	 * "java.lang.object.executed".
	 */
	@Test
	void combinedLethalityExecutedShipsInEveryLanguage() throws IOException {
		assertKeyShipsEverywhere(Talent.CombinedLethalityAbilityTracker.class, "executed", "executed");
	}

	/**
	 * Upstream 6d2aa45a2 (v2.5.0) logs an on-death line when a dart trap kills the hero, as the
	 * other killing traps already did.
	 */
	@Test
	void dartTrapDeathMessagesShipInEveryLanguage() throws IOException {
		assertKeyShipsEverywhere(Assets.Messages.LEVELS, PoisonDartTrap.class, "ondeath",
				"You were killed by the poison dart trap...");
		assertKeyShipsEverywhere(Assets.Messages.LEVELS, WornDartTrap.class, "ondeath",
				"You were killed by the worn dart trap...");
	}

	private static void assertEveryLanguage(Class<?> cls, String key, Predicate<String> holds, String what) {
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			String text = Messages.get(cls, key);
			assertTrue(holds.test(text), lang + " " + cls.getSimpleName() + "." + key + " " + what + ": " + text);
		}
	}

	/**
	 * Upstream bf56446d0 (v2.5.0) reworked the monk. These keys only matched v2.5.4's English once
	 * that port landed, so every translation was still describing the old abilities and talents,
	 * and none had the empowered ability lines at all.
	 */
	@Test
	void empoweredFlurryShipsInEveryLanguage() throws IOException {
		assertKeyShipsEverywhere(MonkEnergy.MonkAbility.Flurry.class, "empower_desc",
				"Two instant strikes that deal %1$d-%2$d damage, ignore armor, _and use your weapon's enchantment._ This ability cannot be used repeatedly.");
	}

	@Test
	void focusTranslationsLastUntilTheParry() throws IOException {
		assertEveryLanguage(MonkEnergy.MonkAbility.Focus.class, "desc", t -> !t.contains("30"), "must not run out after 30 turns");
		assertEveryLanguage(MonkEnergy.MonkAbility.Focus.class, "empower_desc", t -> !t.contains("30"), "must not run out after 30 turns");
		assertKeyShipsEverywhere(MonkEnergy.MonkAbility.Focus.class, "empower_desc",
				"The Monk _instantly focuses,_ letting her parry the next attack made against her.");
	}

	@Test
	void dashTranslationsGiveTheNewRange() throws IOException {
		assertEveryLanguage(MonkEnergy.MonkAbility.Dash.class, "desc", t -> t.contains("4") && !t.contains("3"), "must reach 4 tiles");
		assertEveryLanguage(MonkEnergy.MonkAbility.Dash.class, "empower_desc", t -> t.contains("8") && !t.contains("6"), "must reach 8 tiles");
		assertKeyShipsEverywhere(MonkEnergy.MonkAbility.Dash.class, "empower_desc",
				"An instant dash up to _8 tiles_ away. This ability can go over hazards, but not through enemies or solid terrain.");
	}

	@Test
	void unencumberedSpiritTranslationsGiveTheNewBonus() {
		assertEveryLanguage(Talent.class, "unencumbered_spirit.desc", t -> t.contains("75") && !t.contains("120"),
				"must give 50/75/100% more energy");
	}

	@Test
	void monasticVigorTranslationsGiveTheNewDashRange() {
		assertEveryLanguage(Talent.class, "monastic_vigor.desc", t -> t.contains("4"), "must give dash +4 range");
	}

	@Test
	void combinedEnergyTranslationsRefundOneEnergy() {
		assertEveryLanguage(Talent.class, "combined_energy.desc", t -> !t.contains("50"),
				"must refund 1 energy, not half the energy spent");
	}

	/**
	 * Upstream 00c45dc7f (v2.1.1) added a closing paragraph to Assassin's Reach: the blink crosses
	 * hazards and enemies but not walls. The English took it with that port.
	 */
	@Test
	void assassinsReachHasTheHazardParagraphInEveryLanguage() {
		Messages.setup(Languages.ENGLISH);
		int english = paragraphs(Messages.get(Talent.class, "assassins_reach.desc"));
		assertEquals(4, english, "test setup: English is v2.5.4's four paragraphs");
		assertEveryLanguage(Talent.class, "assassins_reach.desc", t -> paragraphs(t) == english,
				"must carry the paragraph about crossing hazards");
	}

	/**
	 * Keys our English only reached v2.5.4's text for with the tier-2 talent, shared charge pool,
	 * flat boost and alchemy ports, after the first sync. The second sync imported v2.5.4's line
	 * for each, so every translated bundle must carry its own text.
	 */
	private static void assertTranslatedEverywhere(String base, String fullKey) throws IOException {
		Messages.setup(Languages.ENGLISH);
		String english = Messages.get(fullKey);
		assertNotEquals(fullKey, english, "English lacks " + fullKey);
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			assertNotEquals(fullKey, Messages.get(fullKey), lang + " renders a missing key");
		}
		for (Map.Entry<Languages, ObjectMap<String, String>> e : translatedBundles(base).entrySet()) {
			String value = e.getValue().get(fullKey);
			assertNotNull(value, e.getKey() + " lacks " + fullKey);
			assertNotEquals(english, value, e.getKey() + " has only the English text of " + fullKey);
		}
	}

	/**
	 * Rendered text in every language and each translated file's own line must satisfy holds. The
	 * files of showEnglish must lack the key, as v2.5.4's line there is only the English.
	 */
	private static void assertEveryTranslation(String base, String fullKey, Predicate<String> holds, String what,
	                                           Languages... showEnglish) throws IOException {
		List<Languages> english = Arrays.asList(showEnglish);
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);
			String text = Messages.get(fullKey);
			assertTrue(holds.test(text), lang + " " + fullKey + " " + what + ": " + text);
		}
		for (Map.Entry<Languages, ObjectMap<String, String>> e : translatedBundles(base).entrySet()) {
			String value = e.getValue().get(fullKey);
			if (english.contains(e.getKey())) {
				assertNull(value, e.getKey() + " file must not carry " + fullKey);
				continue;
			}
			assertNotNull(value, e.getKey() + " lacks " + fullKey);
			assertTrue(holds.test(value), e.getKey() + " file's " + fullKey + " " + what + ": " + value);
		}
	}

	/** One language's line, rendered and in its file, must be exactly v2.5.4's. */
	private static void assertV254Line(String base, Languages lang, String fullKey, String v254) throws IOException {
		Messages.setup(lang);
		assertEquals(v254, Messages.get(fullKey), lang + " " + fullKey);
		assertEquals(v254, translatedBundles(base).get(lang).get(fullKey), lang + " file's " + fullKey);
	}

	@Test
	void tierTwoPotionAndScrollTalentsShipInEveryLanguage() throws IOException {
		assertTranslatedEverywhere(Assets.Messages.ACTORS, "actors.hero.talent.liquid_willpower.desc");
		assertTranslatedEverywhere(Assets.Messages.ACTORS, "actors.hero.talent.liquid_nature.desc");
		assertTranslatedEverywhere(Assets.Messages.ACTORS, "actors.hero.talent.inscribed_power.title");
		assertTranslatedEverywhere(Assets.Messages.ACTORS, "actors.hero.talent.inscribed_stealth.title");
	}

	@Test
	void variedChargeShipsInEveryLanguage() throws IOException {
		assertTranslatedEverywhere(Assets.Messages.ACTORS, "actors.hero.talent.varied_charge.title");
		assertTranslatedEverywhere(Assets.Messages.ACTORS, "actors.hero.talent.varied_charge.desc");
	}

	/**
	 * v2.1.0's Champion text gave the secondary weapon its own pool at half cap and speed; v2.5.4's
	 * shares one pool and adds 2 max charges. Aggressive barrier went from 3 shielding below 40%/60%
	 * health to 3/5 shielding at or below 50%.
	 */
	@Test
	void championAndAggressiveBarrierTranslationsDescribeTheSharedPool() throws IOException {
		assertEveryTranslation(Assets.Messages.ACTORS, "actors.hero.herosubclass.champion_desc",
				t -> t.contains("2"), "must give 2 more max charges");
		assertEveryTranslation(Assets.Messages.ACTORS, "actors.hero.talent.aggressive_barrier.desc",
				t -> t.contains("5") && t.contains("50") && !t.contains("40") && !t.contains("60"),
				"must give 3/5 shielding at or below 50% health");
	}

	/** v2.5.4's el and tr lines for these are only English, so ours went and the English shows. */
	@Test
	void duelistTalentsShowEnglishWhereUpstreamHasNoTranslation() throws IOException {
		Map<Languages, ObjectMap<String, String>> files = translatedActorBundles();
		for (String key : new String[]{"actors.hero.talent.lethal_haste.desc", "actors.hero.talent.precise_assault.desc"}) {
			Messages.setup(Languages.ENGLISH);
			String english = Messages.get(key);
			for (Languages lang : new Languages[]{Languages.GREEK, Languages.TURKISH}) {
				assertNull(files.get(lang).get(key), lang + " file must not carry " + key);
				Messages.setup(lang);
				assertEquals(english, Messages.get(key), lang + " " + key);
			}
		}
	}

	@Test
	void alchemyItemsShipInEveryLanguage() throws IOException {
		assertTranslatedEverywhere(Assets.Messages.ITEMS, "items.potions.brews.aquabrew.name");
		assertTranslatedEverywhere(Assets.Messages.ITEMS, "items.potions.brews.aquabrew.desc");
		assertTranslatedEverywhere(Assets.Messages.ITEMS, "items.potions.elixirs.elixiroffeatherfall.desc");
	}

	/**
	 * Sword dance went from +25% to +50% accuracy. Every language had a v2.1.0 line for it, so only
	 * the content shows whether the sync replaced it. v2.5.4's tr line is only English.
	 */
	@Test
	void swordDanceTranslationsGiveTheNewAccuracy() throws IOException {
		assertEveryTranslation(Assets.Messages.ITEMS, "items.weapon.melee.scimitar$sworddance.desc",
				t -> t.contains("50") && !t.contains("25"), "must give +50% accuracy", Languages.TURKISH);
	}

	/**
	 * The alchemy guide and the no healing challenge also had v2.1.0 lines in every language; their
	 * text changed without a number to check, so German pins v2.5.4's lines.
	 */
	@Test
	void alchemyGuideAndNoHealingTranslationsAreV254s() throws IOException {
		assertV254Line(Assets.Messages.JOURNAL, Languages.GERMAN, "journal.document.alchemy_guide.brews_elixirs.body",
				"Gebräue und Elixiere sind fortgeschrittene Tränke mit einer Vielzahl von Effekten, üblicherweise für eine Anwendung.");
		assertV254Line(Assets.Messages.MISC, Languages.GERMAN, "challenges.no_healing_desc",
				"Heiltränke sind sicher praktisch, leider bist du dagegen allergisch!\n\n"
						+ "- Heiltränke und Gegenstände, die mit Heiltränken hergestellt werden, sowie Sonnenfrucht vergiften den Helden, anstatt ihn zu heilen.\n"
						+ "- Instabiles Gebräu kann den Helden nicht zufällig vergiften oder heilen.\n"
						+ "- Diese Gegenstände funktionieren für andere Charaktere weiterhin normal.");
	}

	@Test
	void duelistAbilityTextsShipInEveryLanguage() throws IOException {
		assertTranslatedEverywhere(Assets.Messages.ITEMS, "items.weapon.melee.sword.typical_ability_desc");
		assertTranslatedEverywhere(Assets.Messages.ITEMS, "items.weapon.melee.dagger.typical_ability_desc");
		assertTranslatedEverywhere(Assets.Messages.ITEMS, "items.weapon.melee.greatshield.typical_ability_desc");
	}

	@Test
	void journalCatalogTitlesShipInEveryLanguage() throws IOException {
		assertTranslatedEverywhere(Assets.Messages.JOURNAL, "journal.catalog.brews_elixirs.title");
		assertTranslatedEverywhere(Assets.Messages.WINDOWS, "windows.wndjournal$catalogtab.title_equipment");
	}
}
