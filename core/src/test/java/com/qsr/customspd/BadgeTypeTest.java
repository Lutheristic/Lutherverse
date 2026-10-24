package com.qsr.customspd;

import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Badge.type (upstream 3aab64684) replaces Badge.meta and the "no icon means hidden" convention with
 * an explicit HIDDEN / LOCAL / GLOBAL / JOURNAL. CPDU has no JOURNAL badges yet (the catalog badges
 * are not ported), so the seeded-run exemption JOURNAL gets is covered by the unlock rule only:
 * every badge that exists must still be refused in a custom-seeded run.
 */
class BadgeTypeTest {

	private static final List<Badges.Badge> FORMER_META = Arrays.asList(
			Badges.Badge.GAMES_PLAYED_1, Badges.Badge.GAMES_PLAYED_2, Badges.Badge.GAMES_PLAYED_3,
			Badges.Badge.GAMES_PLAYED_4, Badges.Badge.GAMES_PLAYED_5, Badges.Badge.BOSS_SLAIN_1_ALL_CLASSES,
			Badges.Badge.VICTORY_ALL_CLASSES, Badges.Badge.DEATH_FROM_ALL,
			Badges.Badge.BOSS_SLAIN_3_ALL_SUBCLASSES, Badges.Badge.ALL_ITEMS_IDENTIFIED);

	@Test
	void everyBadgeHasTheTypeItsOldFlagsImplied() {
		for (Badges.Badge b : Badges.Badge.values()) {
			if (b.asset == null) {
				assertEquals(Badges.BadgeType.HIDDEN, b.type, b + " has no icon, so it is internal");
			} else if (FORMER_META.contains(b)) {
				assertEquals(Badges.BadgeType.GLOBAL, b.type, b + " was a meta badge");
			} else {
				assertEquals(Badges.BadgeType.LOCAL, b.type, b + " was a regular badge");
			}
		}
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
		// CPDU has no JOURNAL badge yet, so retype one for the duration of the test.
		Badges.Badge probe = Badges.Badge.NO_MONSTERS_SLAIN;
		Badges.BadgeType original = probe.type;
		try (HeadlessRun run = HeadlessRun.start(20261024L)) {
			assertFalse(Dungeon.customSeedText.isEmpty(), "test setup: HeadlessRun plays a custom-seeded run");
			probe.type = Badges.BadgeType.JOURNAL;
			Badges.unlock(probe);
			assertTrue(Badges.isUnlocked(probe), "a journal badge is profile-based and unlocks even in a seeded run");
			Badges.disown(probe);
		} finally {
			probe.type = original;
		}
		assertEquals(Badges.BadgeType.LOCAL, probe.type);
	}
}
