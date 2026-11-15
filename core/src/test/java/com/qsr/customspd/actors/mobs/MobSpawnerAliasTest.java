package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The level respawner moved from the nested Level.Respawner into MobSpawner (upstream 16977c418).
 * Saves made before the move name the old class, so the pre-v2.5.0 alias has to carry them over.
 * Both rotation snapshots were captured from the old spawn object before the move. The short one
 * pins exact rotations; the long one runs enough rotations that rare mobs and alt swaps fire, so
 * the order of the rare-mob and alt-swap draws is pinned as well.
 */
class MobSpawnerAliasTest {

	private static final String OLD_NAME = "com.qsr.customspd.levels.Level$Respawner";

	@Test
	void oldRespawnerSaveRestoresAsMobSpawner() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20261114L)) {
			Level level = run.enterFirstLevel();
			ShatteredPixelDungeon.registerBundleAliases();

			Bundle stored = new Bundle();
			level.storeInBundle(stored);
			assertTrue(stored.contains("respawner"), "the first floor should have a respawner to save");
			stored.getBundle("respawner").put("__className", OLD_NAME);

			Bundle read = SaveRoundtrip.writeRead(stored);
			Level restored = (Level) Reflection.newInstance(level.getClass());
			restored.restoreFromBundle(read);

			Object respawner = respawnerOf(restored);
			assertNotNull(respawner, "an old Level$Respawner entry should restore");
			assertEquals(MobSpawner.class, respawner.getClass());

			Dungeon.level = restored;
			Actor added = restored.addRespawner();
			assertSame(respawner, added);
			assertTrue(Actor.all().contains(added), "addRespawner should re-add the restored spawner");
		}
	}

	@Test
	void mobRotationMatchesThePreRenameSnapshot() {
		// captured on the tree before the rename, from the old Kotlin spawn object
		String[][] expected = {
				{"3",
						"Gnoll Snake Keywraith Rat Gnoll Gnoll Crab Swarm",
						"Gnoll Swarm Keywraith Snake Crab Rat Gnoll Gnoll",
						"Swarm Snake Gnoll Rat Gnoll Keywraith Crab Gnoll",
						"Rat Swarm Snake Gnoll Gnoll Keywraith Crab Gnoll",
						"Rat Gnoll Snake Keywraith Gnoll Crab Swarm Gnoll",
						"Rat Snake Gnoll Keywraith Gnoll Swarm Gnoll Crab"},
				{"8",
						"DM100 Skeleton Necromancer Skeleton DM100 Thief Guard Guard",
						"DM100 Guard Necromancer Skeleton Guard Skeleton Thief DM100",
						"Guard Skeleton DM100 Skeleton Thief Necromancer Guard DM100",
						"Skeleton Guard Skeleton DM100 Thief Necromancer Guard DM100",
						"Skeleton DM100 Skeleton Necromancer DM100 Guard Guard Thief",
						"Skeleton Skeleton Thief Necromancer DM100 Guard DM100 Guard"},
				{"14",
						"PurpleShaman DM200 DM200 Spinner Spinner BlueShaman Bat Brute",
						"DM200 BlueShaman Spinner Bat DM200 Spinner Brute RedShaman",
						"PurpleShaman Brute DM200 Bat BlueShaman Spinner Spinner DM200",
						"Spinner PurpleShaman Bat DM200 PurpleShaman DM200 Spinner Brute",
						"Bat Brute RedShaman Spinner RedShaman DM200 DM200 Spinner",
						"Bat Spinner Spinner RedShaman Brute BlueShaman DM200 DM200"},
				{"18",
						"Warlock FrostElemental Monk Ghoul Monk Warlock Golem",
						"Warlock Monk Monk FireElemental Golem Ghoul Warlock",
						"Monk FireElemental Monk Ghoul Warlock Warlock Golem",
						"Ghoul Monk FrostElemental Monk Warlock Warlock Golem",
						"Ghoul Warlock FireElemental Warlock Monk Golem Monk",
						"Ghoul FrostElemental Warlock Golem Monk Monk Warlock"},
		};
		try (HeadlessRun run = HeadlessRun.start(20261114L)) {
			for (String[] floor : expected) {
				run.generate(floor[0]);
				List<String> actual = new ArrayList<>();
				Random.pushGenerator(777L);
				try {
					for (int i = 1; i < floor.length; i++) {
						StringBuilder sb = new StringBuilder();
						for (Class<? extends Mob> c : MobSpawner.getMobRotation()) {
							if (sb.length() > 0) sb.append(' ');
							sb.append(c.getSimpleName());
						}
						actual.add(sb.toString());
					}
				} finally {
					Random.popGenerator();
				}
				assertEquals(Arrays.asList(floor).subList(1, floor.length), actual, "rotation on floor " + floor[0]);
			}
		}
	}

	@Test
	void longRotationRunMatchesThePreRenameSnapshot() {
		// captured on the tree before the rename, from the old Kotlin spawn object:
		// floor, hashCode of 200 joined rotations, mob counts over those rotations
		String[][] expected = {
				{"4", "1242551104",
						"{Swarm=200, Crab=400, HexwardMoth=200, Gnoll=200, Slime=390, CausticSlime=10, Thief=5}"},
				{"9", "-260781056",
						"{DM100=400, Thief=194, Necromancer=388, Skeleton=200, Guard=400, SpectralNecromancer=12, Bandit=6, Bat=6}"},
				{"14", "742929268",
						"{PurpleShaman=84, DM200=394, Spinner=400, BlueShaman=144, Bat=200, Brute=195, RedShaman=172, DM201=6, ArmoredBrute=5, Ghoul=4}"},
				{"19", "-1935309713",
						"{Warlock=400, Golem=600, Monk=388, FrostElemental=79, Senior=12, ShockElemental=34, FireElemental=82, Succubus=9, ChaosElemental=5}"},
		};
		try (HeadlessRun run = HeadlessRun.start(20261114L)) {
			for (String[] floor : expected) {
				run.generate(floor[0]);
				StringBuilder all = new StringBuilder();
				Map<String, Integer> counts = new LinkedHashMap<>();
				Random.pushGenerator(777L);
				try {
					for (int i = 0; i < 200; i++) {
						for (Class<? extends Mob> c : MobSpawner.getMobRotation()) {
							String name = c.getSimpleName();
							all.append(name).append(' ');
							Integer n = counts.get(name);
							counts.put(name, n == null ? 1 : n + 1);
						}
						all.append('|');
					}
				} finally {
					Random.popGenerator();
				}
				assertEquals(floor[2], counts.toString(), "mob counts on floor " + floor[0]);
				assertEquals(floor[1], String.valueOf(all.toString().hashCode()), "rotation digest on floor " + floor[0]);
			}
		}
	}

	private static Object respawnerOf(Level level) throws Exception {
		Field f = Level.class.getDeclaredField("respawner");
		f.setAccessible(true);
		return f.get(level);
	}
}
