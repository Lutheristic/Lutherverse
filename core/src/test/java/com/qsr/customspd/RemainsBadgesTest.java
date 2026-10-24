package com.qsr.customspd;

import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.mobs.Goo;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.remains.SealShard;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

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
	void reachingTheSurfaceHoldingRemainsUnlocksYouMadeItToo() {
		try (HeadlessRun run = HeadlessRun.start(20261023L)) {
			Dungeon.customSeedText = "";

			Badges.silentValidateHappyEnd();
			Badges.validateHappyEnd();
			assertTrue(Badges.isUnlocked(Badges.Badge.HAPPY_END), "test setup: the happy end badge itself");
			assertFalse(Badges.isUnlocked(Badges.Badge.HAPPY_END_REMAINS), "no remains item held");

			Dungeon.hero.belongings.backpack.items.add(new SealShard());
			Badges.silentValidateHappyEnd();
			Bundle local = new Bundle();
			Badges.saveLocal(local);
			assertTrue(Arrays.asList(local.getStringArray("badges")).contains("HAPPY_END_REMAINS"),
					"winning while holding a remains item must record the badge for the run");
			Badges.validateHappyEnd();
			assertTrue(Badges.isUnlocked(Badges.Badge.HAPPY_END_REMAINS),
					"reaching the surface holding a remains item must unlock the badge");
		}
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
