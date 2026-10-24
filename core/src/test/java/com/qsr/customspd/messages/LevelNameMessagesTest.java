package com.qsr.customspd.messages;

import com.qsr.customspd.actors.hero.abilities.mage.WarpBeacon;
import com.qsr.customspd.items.spells.BeaconOfReturning;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPDU's layout graph names levels with Strings (Dungeon.levelName, BeaconOfReturning.returnLevel,
 * WarpBeaconTracker.level) where upstream passes an int depth, so every message that shows a level
 * must take it with %s. A %d cannot take a String: Messages.format reports an exception and shows
 * the raw pattern - in English too, which TranslationPlaceholderTest (translation vs English) cannot
 * see. items.spells.beaconofreturning.desc_set shipped exactly that.
 *
 * Each key below is rendered the way its call site renders it (grep of every Messages/GLog call that
 * passes a level name, 2026-10-24), with a level name no format string could produce by accident,
 * in every language.
 */
class LevelNameMessagesTest {

	private static final String LEVEL = "Qz7-crypt";

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void backToEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	private static void assertShowsLevel(Languages lang, String what, String text) {
		assertTrue(text.contains(LEVEL) && !text.contains("%"),
				lang + " " + what + ": level name not rendered in \"" + text + "\"");
	}

	@Test
	void everyMessageThatNamesALevelRendersTheLevelNameInEveryLanguage() {
		for (Languages lang : Languages.values()) {
			Messages.setup(lang);

			// BeaconOfReturning.desc(): Messages.get(this, "desc_set", returnLevel)
			BeaconOfReturning beacon = new BeaconOfReturning();
			beacon.returnLevel = LEVEL;
			assertShowsLevel(lang, "beaconofreturning.desc_set", beacon.desc());

			// WarpBeacon: Messages.get(WarpBeacon.class, "window_desc", tracker.level)
			assertShowsLevel(lang, "warpbeacon.window_desc",
					Messages.get(WarpBeacon.class, "window_desc", LEVEL));

			// GameScene: GLog.h(Messages.get(this, key), Dungeon.levelName) -> Messages.format
			for (String key : new String[]{"descend", "return", "resurrect"}) {
				assertShowsLevel(lang, "gamescene." + key,
						Messages.format(Messages.get(GameScene.class, key), LEVEL));
			}

			// no call site today (CPDU's Lloyd's beacon has no class), but CPDU's English takes the
			// level name as %s and its translations are restored alongside the others
			assertShowsLevel(lang, "lloydsbeacon.desc_set",
					Messages.get((Class<?>) null, "items.artifacts.lloydsbeacon.desc_set", LEVEL));
		}
	}
}
