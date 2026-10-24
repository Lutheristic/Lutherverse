package com.qsr.customspd;

import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.mobs.Goo;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.Amulet;
import com.qsr.customspd.items.remains.SealShard;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The v2.3.0 hero-remains badges (upstream 15cd9f330): "Taste Vengeance!" (BOSS_SLAIN_REMAINS,
 * kill a boss while holding a remains item found on that boss's floor) and "You Made It Too"
 * (HAPPY_END_REMAINS, reach the surface holding a remains item). The cluster spans Bones (a boss
 * floor's remains qualify the run), Dungeon.newLevel (every new level clears the qualification),
 * Statistics (the flag is saved) and Badges.
 *
 * Runs on the F4 HeadlessRun, which snapshots and restores every static (Badges' global set,
 * Statistics, Bones' private state). HeadlessRun plays a custom-seeded run and seeded runs never
 * unlock badges, so the badge tests clear customSeedText first.
 */
class RemainsBadgesTest {

	private static final String BONES_FILE = "bones.dat";

	@AfterEach
	void deleteBones() {
		FileUtils.deleteFile(BONES_FILE);
	}

	/** Bones' statics as a newly started process has them, with a remains file for {@code level} on disk. */
	private static void bonesFileFor(String level, HeroClass cls) throws Exception {
		Bundle bones = new Bundle();
		bones.put("level", level);
		bones.put("hero_class", cls);
		FileUtils.bundleToFile(BONES_FILE, bones);
		Field f = Bones.class.getDeclaredField("level");
		f.setAccessible(true);
		f.set(null, "surface");
	}

	@Test
	void remainsFoundOnABossFloorQualifyTheRunUntilTheNextLevel() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			assertTrue(Dungeon.layout.getDungeon().get("5").getBoss(), "test setup: floor 5 is a boss floor");
			assertFalse(Dungeon.layout.getDungeon().get("3").getBoss(), "test setup: floor 3 is not");

			bonesFileFor("3", HeroClass.MAGE);
			run.generate("3");
			assertFalse(Statistics.qualifiedForBossRemainsBadge, "remains on a regular floor must not qualify");
			assertEquals("0", FileUtils.bundleFromFile(BONES_FILE).getString("level"),
					"test setup: the regular floor must have consumed the remains");

			bonesFileFor("5", HeroClass.MAGE);
			run.generate("5");
			assertEquals("0", FileUtils.bundleFromFile(BONES_FILE).getString("level"),
					"test setup: the boss floor must have consumed the remains");
			assertTrue(Statistics.qualifiedForBossRemainsBadge, "remains found on a boss floor must qualify the run");

			run.generate("6");
			assertFalse(Statistics.qualifiedForBossRemainsBadge, "generating the next level must clear the qualification");
		}
	}

	@Test
	void qualificationIsSavedAndOldSavesLoadUnqualified() {
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			Statistics.qualifiedForBossRemainsBadge = true;
			Bundle saved = new Bundle();
			Statistics.storeInBundle(saved);
			assertTrue(saved.contains("qualifiedForBossRemainsBadge"), "the flag must be stored under upstream's key");

			Statistics.qualifiedForBossRemainsBadge = false;
			Statistics.restoreFromBundle(saved);
			assertTrue(Statistics.qualifiedForBossRemainsBadge, "the flag must be restored from a save");

			// A save written before the port has no such key: it must load, unqualified.
			saved.remove("qualifiedForBossRemainsBadge");
			Statistics.restoreFromBundle(saved);
			assertFalse(Statistics.qualifiedForBossRemainsBadge, "a pre-port save must load unqualified");

			// and a whole-game save/load keeps it
			Statistics.qualifiedForBossRemainsBadge = true;
			run.enterFirstLevel();
			Statistics.qualifiedForBossRemainsBadge = true;
			run.saveAll();
			Statistics.qualifiedForBossRemainsBadge = false;
			run.loadGame();
			assertTrue(Statistics.qualifiedForBossRemainsBadge, "the flag must survive Dungeon.saveAll/loadGame");

			Statistics.reset();
			assertFalse(Statistics.qualifiedForBossRemainsBadge, "Statistics.reset must clear the flag");
		}
	}

	@Test
	void bossSlainHoldingRemainsUnlocksTasteVengeance() {
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			Dungeon.customSeedText = ""; // seeded runs never unlock badges

			// qualified, but not holding the remains item
			Statistics.qualifiedForBossRemainsBadge = true;
			Badges.validateBossSlain(Goo.class);
			assertFalse(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_REMAINS), "the remains item must be held");

			// holding a remains item, but it was not found on this boss's floor
			Dungeon.hero.belongings.backpack.items.add(new SealShard());
			Statistics.qualifiedForBossRemainsBadge = false;
			Badges.validateBossSlain(Goo.class);
			assertFalse(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_REMAINS), "the run must be qualified");

			Statistics.qualifiedForBossRemainsBadge = true;
			Badges.validateBossSlain(Goo.class);
			assertTrue(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_REMAINS),
					"killing the boss while qualified and holding the remains item must unlock the badge");
		}
	}

	@Test
	void tasteVengeanceIsRecordedAndEndorsedOnEveryQualifyingRunNotJustTheFirst() {
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			Dungeon.customSeedText = "";
			Dungeon.hero.belongings.backpack.items.add(new SealShard());
			Statistics.qualifiedForBossRemainsBadge = true;

			List<String> log = new ArrayList<>();
			Signal.Listener<String> listener = log::add;
			GLog.update.add(listener);
			try {
				Badges.validateBossSlain(Goo.class);
				assertTrue(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_REMAINS), "test setup: first unlock");
				assertTrue(localBadgeNames().contains("BOSS_SLAIN_REMAINS"), "the run records the badge");
				assertTrue(log.contains(unlockedLine(Badges.Badge.BOSS_SLAIN_REMAINS)));

				// v2.4.0 (6fc6fb1fc): a later run must record and endorse it again; the badge being
				// unlocked already used to skip both, so the run's badge list never showed it.
				Badges.loadLocal(new Bundle()); // a new run's empty local set; global stays unlocked
				assertFalse(localBadgeNames().contains("BOSS_SLAIN_REMAINS"), "test setup: fresh run");
				log.clear();
				Badges.validateBossSlain(Goo.class);
				assertTrue(localBadgeNames().contains("BOSS_SLAIN_REMAINS"),
						"killing a boss while qualified must record the badge again on a later run");
				assertTrue(log.contains(GLog.HIGHLIGHT + Messages.get(Badges.class, "endorsed",
						Badges.Badge.BOSS_SLAIN_REMAINS.title())),
						"an already-unlocked badge is endorsed, not silently skipped; log was " + log);
			} finally {
				GLog.update.remove(listener);
			}
		}
	}

	private static List<String> localBadgeNames() {
		Bundle local = new Bundle();
		Badges.saveLocal(local);
		return Arrays.asList(local.getStringArray("badges"));
	}

	@Test
	void reachingTheSurfaceHoldingRemainsUnlocksYouMadeItToo() {
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			Dungeon.customSeedText = "";

			// v2.4.0: validateHappyEnd alone awards both badges, run once from the ending in
			// Hero.actTransition's SurfaceScene switch callback (d029c0d7d, see HappyEndTransitionTest).
			// There is no silent pre-step that records the remains badge for it to find, so it reads
			// the hero's pack.
			List<String> log = new ArrayList<>();
			Signal.Listener<String> listener = log::add;
			GLog.update.add(listener);
			try {
				Badges.validateHappyEnd();
				assertTrue(Badges.isUnlocked(Badges.Badge.HAPPY_END), "the happy end badge itself");
				assertFalse(Badges.isUnlocked(Badges.Badge.HAPPY_END_REMAINS), "no remains item held");
				assertTrue(log.contains(unlockedLine(Badges.Badge.HAPPY_END)),
						"the happy end badge must be announced; log was " + log);
				assertFalse(log.contains(unlockedLine(Badges.Badge.HAPPY_END_REMAINS)));

				Dungeon.hero.belongings.backpack.items.add(new SealShard());
				Badges.validateHappyEnd();
				assertTrue(Badges.isUnlocked(Badges.Badge.HAPPY_END_REMAINS),
						"reaching the surface holding a remains item must unlock the badge");
				assertTrue(log.contains(unlockedLine(Badges.Badge.HAPPY_END_REMAINS)),
						"the remains badge must be announced; log was " + log);
			} finally {
				GLog.update.remove(listener);
			}
		}
	}

	@Test
	void happyEndBadgesAreAddedToTheRunsLocalBadgesSoTheWinRankingKeepsThem() {
		// v2.5.0 (1c2e5e9be). The ending runs validateHappyEnd and THEN Dungeon.win (then
		// deleteGame, saveGlobal) in the afterCreate of Hero.actTransition's
		// Game.switchScene(SurfaceScene.class, ...) callback - upstream d029c0d7d, pulled forward;
		// upstream's SewerLevel.activateTransition is CPDU's Hero.actTransition. The ranking record
		// copies the run's local badges, so a badge that was only displayed (or only globally
		// unlocked) never reached the Rankings screen's badge list.
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			run.enterFirstLevel(); // Dungeon.win needs a level; a seeded run records no global badges
			Dungeon.hero.belongings.backpack.items.add(new SealShard());
			Rankings rankings = Rankings.INSTANCE;
			Rankings.Record rec = null;
			try {
				Badges.validateHappyEnd();
				Dungeon.win(Amulet.class);
				rec = rankings.records.get(rankings.lastRecord);
				assertTrue(rec.win, "test setup: the win was ranked");
				List<String> ranked = Arrays.asList(
						rec.gameData.getBundle(Rankings.BADGES).getStringArray("badges"));
				assertTrue(ranked.contains("HAPPY_END"), "the ranking must keep HAPPY_END; has " + ranked);
				assertTrue(ranked.contains("HAPPY_END_REMAINS"),
						"the ranking must keep HAPPY_END_REMAINS; has " + ranked);
			} finally {
				if (rec != null) rankings.records.remove(rec);
				FileUtils.deleteFile(Rankings.RANKINGS_FILE);
			}
		}
	}

	@Test
	void happyEndIsRecordedAndEndorsedAgainOnALaterRun() {
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			Dungeon.customSeedText = "";
			Dungeon.hero.belongings.backpack.items.add(new SealShard());
			List<String> log = new ArrayList<>();
			Signal.Listener<String> listener = log::add;
			GLog.update.add(listener);
			try {
				Badges.validateHappyEnd();
				assertTrue(localBadgeNames().containsAll(Arrays.asList("HAPPY_END", "HAPPY_END_REMAINS")));

				Badges.loadLocal(new Bundle()); // the next run: empty local set, global still unlocked
				log.clear();
				Badges.validateHappyEnd();
				assertTrue(localBadgeNames().containsAll(Arrays.asList("HAPPY_END", "HAPPY_END_REMAINS")),
						"a later win must record both badges again");
				for (Badges.Badge b : new Badges.Badge[]{Badges.Badge.HAPPY_END, Badges.Badge.HAPPY_END_REMAINS}) {
					assertTrue(log.contains(GLog.HIGHLIGHT + Messages.get(Badges.class, "endorsed", b.title())),
							b + " must be endorsed, not skipped; log was " + log);
				}
			} finally {
				GLog.update.remove(listener);
			}
		}
	}

	@Test
	void localBadgesSurviveAGameSaveAndASavePredatingThePortStillLoads() {
		// The run's local badge set is part of game.dat. The happy-end rework changes WHEN badges
		// join it, not how it is stored: it must round-trip, and a save with no badge bundle (or
		// only old badge names) must keep loading.
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			run.enterFirstLevel();
			Bundle withNew = new Bundle();
			withNew.put("badges", new String[]{"HAPPY_END", "HAPPY_END_REMAINS", "BOSS_SLAIN_REMAINS"});
			Badges.loadLocal(withNew);
			run.saveAll();
			Badges.loadLocal(new Bundle());
			assertTrue(localBadgeNames().isEmpty(), "test setup: local set cleared");
			run.loadGame();
			assertTrue(localBadgeNames().containsAll(
					Arrays.asList("HAPPY_END", "HAPPY_END_REMAINS", "BOSS_SLAIN_REMAINS")),
					"the local set must survive Dungeon.saveAll/loadGame; has " + localBadgeNames());

			// a save written before this port: local badges by old names only
			Bundle old = new Bundle();
			old.put("badges", new String[]{"NO_MONSTERS_SLAIN", "HAPPY_END"});
			Badges.loadLocal(old);
			run.saveAll();
			Badges.loadLocal(new Bundle());
			run.loadGame();
			assertTrue(localBadgeNames().containsAll(Arrays.asList("NO_MONSTERS_SLAIN", "HAPPY_END")));
			assertFalse(localBadgeNames().contains("HAPPY_END_REMAINS"));
		}
	}

	/** The log line Badges.displayBadge writes when a badge is newly unlocked. */
	private static String unlockedLine(Badges.Badge badge) {
		return GLog.HIGHLIGHT + Messages.get(Badges.class, "new", badge.title() + " (" + badge.desc() + ")");
	}

	@Test
	void remainsBadgesAreSavedByNameAndOlderBadgeFilesStillLoad() throws Exception {
		// Badges persist by enum name (badges.dat and each save's local set), not by ordinal, so the
		// upstream insertion of two enum constants must not disturb anything already stored.
		Bundle preport = new Bundle();
		// (no tiered badges here: restore() also re-adds the lower tiers a tiered badge replaces)
		preport.put("badges", new String[]{"NO_MONSTERS_SLAIN", "DEATH_FROM_SACRIFICE", "HAPPY_END", "ALL_WEAPONS_IDENTIFIED"});
		assertEquals(new HashSet<>(Arrays.asList(Badges.Badge.NO_MONSTERS_SLAIN, Badges.Badge.DEATH_FROM_SACRIFICE,
				Badges.Badge.HAPPY_END, Badges.Badge.ALL_WEAPONS_IDENTIFIED)), Badges.restore(preport),
				"a badge file from before the port must load unchanged");

		HashSet<Badges.Badge> withRemains = new LinkedHashSet<>(Arrays.asList(
				Badges.Badge.BOSS_SLAIN_REMAINS, Badges.Badge.HAPPY_END_REMAINS));
		Bundle stored = new Bundle();
		Badges.store(stored, new HashSet<>(withRemains));
		assertEquals(withRemains, Badges.restore(stored), "the remains badges must round-trip by name");

		assertEquals(GeneralAsset.BOSS_SLAIN_REMAINS, Badges.Badge.BOSS_SLAIN_REMAINS.asset);
		assertEquals(GeneralAsset.HAPPY_END_REMAINS, Badges.Badge.HAPPY_END_REMAINS.asset);
		// (Messages returns the lower-cased key for a missing string, never a marker, so pin the text)
		assertEquals("Taste Vengeance!", Badges.Badge.BOSS_SLAIN_REMAINS.title());
		assertEquals("Defeat a boss while holding the signature item of a hero who died to that boss",
				Badges.Badge.BOSS_SLAIN_REMAINS.desc());
		assertEquals("You Made It Too", Badges.Badge.HAPPY_END_REMAINS.title());
		assertEquals("Bring a fallen hero's signature item to the surface", Badges.Badge.HAPPY_END_REMAINS.desc());
		for (Badges.Badge b : withRemains) {
			assertEquals(Badges.BadgeType.LOCAL, b.type, b + " is a regular badge");
			assertImage(b.asset.getPath(), 16, 16);
		}
		assertImage(GeneralAsset.SURFACE_GRAVE.getPath(), 16, 22);
	}

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	private static void assertImage(String path, int w, int h) throws Exception {
		File file = new File(path);
		assertTrue(file.isFile(), path + " must ship (cwd " + new File(".").getAbsolutePath() + ")");
		BufferedImage img = ImageIO.read(file);
		assertNotNull(img, path + " must be a readable image");
		assertEquals(w, img.getWidth(), path + " width");
		assertEquals(h, img.getHeight(), path + " height");
	}
}
