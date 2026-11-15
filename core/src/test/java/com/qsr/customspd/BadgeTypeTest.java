package com.qsr.customspd;

import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Badge.type (upstream 3aab64684) replaces Badge.meta and the "no icon means hidden" convention with
 * an explicit HIDDEN / LOCAL / GLOBAL / JOURNAL. The JOURNAL badges are the catalog badges from the
 * badge redesign (b003d1129); they are the only ones a custom-seeded run may unlock.
 */
class BadgeTypeTest {

	private static final List<Badges.Badge> FORMER_META = Arrays.asList(
			Badges.Badge.GAMES_PLAYED_1, Badges.Badge.GAMES_PLAYED_2, Badges.Badge.GAMES_PLAYED_3,
			Badges.Badge.GAMES_PLAYED_4, Badges.Badge.GAMES_PLAYED_5, Badges.Badge.BOSS_SLAIN_1_ALL_CLASSES,
			Badges.Badge.VICTORY_ALL_CLASSES, Badges.Badge.DEATH_FROM_ALL,
			Badges.Badge.BOSS_SLAIN_3_ALL_SUBCLASSES);

	private static final List<Badges.Badge> JOURNAL = Arrays.asList(
			Badges.Badge.CATALOG_ONE_EQUIPMENT, Badges.Badge.RESEARCHER_1, Badges.Badge.RESEARCHER_2,
			Badges.Badge.RESEARCHER_3, Badges.Badge.RESEARCHER_4, Badges.Badge.RESEARCHER_5,
			Badges.Badge.RODNEY, Badges.Badge.ALL_RARE_ENEMIES);

	// the identification badges the redesign replaced; still loaded from old badge files
	private static final List<Badges.Badge> OLD_IDENTIFICATION = Arrays.asList(
			Badges.Badge.ALL_WEAPONS_IDENTIFIED, Badges.Badge.ALL_ARMOR_IDENTIFIED,
			Badges.Badge.ALL_WANDS_IDENTIFIED, Badges.Badge.ALL_RINGS_IDENTIFIED,
			Badges.Badge.ALL_ARTIFACTS_IDENTIFIED, Badges.Badge.ALL_POTIONS_IDENTIFIED,
			Badges.Badge.ALL_SCROLLS_IDENTIFIED, Badges.Badge.ALL_ITEMS_IDENTIFIED);

	@Test
	void everyBadgeHasTheTypeItsOldFlagsImplied() {
		for (Badges.Badge b : Badges.Badge.values()) {
			if (b.asset == null) {
				assertEquals(Badges.BadgeType.HIDDEN, b.type, b + " has no icon, so it is internal");
			} else if (JOURNAL.contains(b)) {
				assertEquals(Badges.BadgeType.JOURNAL, b.type, b + " is a catalog badge");
			} else if (FORMER_META.contains(b)) {
				assertEquals(Badges.BadgeType.GLOBAL, b.type, b + " was a meta badge");
			} else {
				assertEquals(Badges.BadgeType.LOCAL, b.type, b + " was a regular badge");
			}
		}
		for (Badges.Badge b : OLD_IDENTIFICATION) {
			assertEquals(Badges.BadgeType.HIDDEN, b.type, b + " was replaced by the catalog badges");
		}
	}

	@Test
	void everyBadgeIconResolvesToAnExistingFile() {
		// core:test runs with src/main/assets as its working directory (see HeadlessGdx)
		File assets = new File(".");
		assertTrue(new File(assets, Badges.Badge.RODNEY.asset.getPath()).isFile(),
				"fixture sanity: working directory must be core/src/main/assets, was " + assets.getAbsolutePath());
		List<String> missing = new ArrayList<>();
		for (Badges.Badge b : Badges.Badge.values()) {
			if (b.asset != null && !new File(assets, b.asset.getPath()).isFile()) {
				missing.add(b + " -> " + b.asset.getPath());
			}
		}
		assertTrue(missing.isEmpty(), "badge icons with no file: " + missing);
	}

	@Test
	void theLocalListShowsOnlyLocalBadgesAndNeverHiddenOnes() {
		try (HeadlessRun run = HeadlessRun.start(20261024L)) {
			Dungeon.customSeedText = ""; // seeded runs never unlock badges
			for (Badges.Badge b : new Badges.Badge[]{Badges.Badge.NO_MONSTERS_SLAIN, Badges.Badge.BOSS_SLAIN_1_WARRIOR,
					Badges.Badge.GAMES_PLAYED_1}) {
				Badges.unlock(b);
			}
			assertTrue(Badges.isUnlocked(Badges.Badge.GAMES_PLAYED_1));
			assertTrue(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_1_WARRIOR));

			List<Badges.Badge> global = Badges.filterReplacedBadges(true);
			assertTrue(global.contains(Badges.Badge.GAMES_PLAYED_1), "a global badge belongs on the global list");
			assertTrue(global.contains(Badges.Badge.NO_MONSTERS_SLAIN));
			assertFalse(global.contains(Badges.Badge.BOSS_SLAIN_1_WARRIOR), "a hidden badge is never listed");

			com.watabou.utils.Bundle saved = new com.watabou.utils.Bundle();
			Badges.store(saved, new LinkedHashSet<>(Arrays.asList(Badges.Badge.GAMES_PLAYED_1,
					Badges.Badge.NO_MONSTERS_SLAIN, Badges.Badge.BOSS_SLAIN_1_WARRIOR)));
			Badges.loadLocal(saved);
			List<Badges.Badge> local = Badges.filterReplacedBadges(false);
			assertEquals(Arrays.asList(Badges.Badge.NO_MONSTERS_SLAIN), local,
					"the per-run list holds the local badge only: not the global one, not the hidden one");
			for (Badges.Badge b : local) {
				assertEquals(Badges.BadgeType.LOCAL, b.type, b + " is not a per-run badge");
			}
		}
	}

	@Test
	void customSeededRunsUnlockNoBadgeAtAll() {
		try (HeadlessRun run = HeadlessRun.start(20261024L)) {
			assertFalse(Dungeon.customSeedText.isEmpty(), "test setup: HeadlessRun plays a custom-seeded run");
			for (Badges.Badge b : new LinkedHashSet<>(Arrays.asList(Badges.Badge.GAMES_PLAYED_1,
					Badges.Badge.NO_MONSTERS_SLAIN, Badges.Badge.BOSS_SLAIN_1_WARRIOR))) {
				Badges.unlock(b);
				assertFalse(Badges.isUnlocked(b), b + " (" + b.type + ") must not unlock in a seeded run");
			}
		}
	}

	@Test
	void journalBadgesUnlockEvenInSeededRuns() {
		try (HeadlessRun run = HeadlessRun.start(20261024L)) {
			assertFalse(Dungeon.customSeedText.isEmpty(), "test setup: HeadlessRun plays a custom-seeded run");
			for (Badges.Badge b : JOURNAL) {
				Badges.unlock(b);
				assertTrue(Badges.isUnlocked(b), b + " is profile-based and unlocks even in a seeded run");
			}
		}
	}
}
