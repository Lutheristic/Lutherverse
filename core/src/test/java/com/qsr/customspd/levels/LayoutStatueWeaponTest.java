package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.ArmoredStatue;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Statue;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.test.HeadlessRun;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Statues get their weapon when the level is built, not in their constructor, so a statue a
 * pack places by name (a regular level's extra_mobs, a custom level's mobs) must be armed there
 * too, as a levelgen statue. A statue a pack lists in a floor's bestiary is armed from the decks
 * when the floor is built, and from the default probabilities when it spawns during play.
 */
class LayoutStatueWeaponTest {

	private static final String LAYOUT = "layout-statue-layout.json";
	private static final String ROTATION_LAYOUT = "rotation-statue-layout.json";

	@Test
	void statuesAPackPlacesAreArmed() {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(LAYOUT));
		for (long seed = 1; seed <= 3; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				Dungeon.layout = layout;
				Dungeon.levelName = layout.getStart();
				Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();

				Level regular = run.enterFirstLevel();
				assertTrue(regular instanceof RegularLevel, "test setup: the first floor is a regular level");
				List<Statue> statues = statues(regular);
				assertFalse(statues.isEmpty(), "seed " + seed + ": the extra_mobs statue was placed");
				for (Statue s : statues) assertArmed(s, true, "seed " + seed + " extra_mobs");

				Level custom = run.descend();
				assertTrue(custom instanceof CustomLevel, "test setup: the second floor is a custom level");
				statues = statues(custom);
				assertEquals(2, statues.size(), "seed " + seed + ": both layout statues were placed");
				for (Statue s : statues) assertArmed(s, true, "seed " + seed + " layout " + s.getClass().getSimpleName());
			}
		}
	}

	@Test
	void statuesFromAPacksBestiaryAreArmed() {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(ROTATION_LAYOUT));
		for (long seed = 1; seed <= 3; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				Dungeon.layout = layout;
				Dungeon.levelName = layout.getStart();
				Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();

				Level level = run.enterFirstLevel();
				assertTrue(level instanceof RegularLevel, "test setup: the floor is a regular level");
				List<Statue> statues = statues(level);
				assertFalse(statues.isEmpty(), "seed " + seed + ": the bestiary statues were placed");
				for (Statue s : statues) assertArmed(s, true, "seed " + seed + " levelgen " + s.getClass().getSimpleName());

				// the respawner, summoning traps and multiplicity all spawn through createMob
				String before = decks();
				int armored = 0;
				for (int i = 0; i < 20; i++) {
					Mob m = level.createMob();
					assertTrue(m instanceof Statue, "test setup: the rotation is all statues");
					if (m instanceof ArmoredStatue) armored++;
					assertArmed((Statue) m, false, "seed " + seed + " in play " + m.getClass().getSimpleName());
				}
				assertTrue(armored > 0, "seed " + seed + ": the rotation gave an armored statue");
				assertEquals(before, decks(), "seed " + seed + ": a statue spawned in play drew from the decks");
			}
		}
	}

	private static void assertArmed(Statue s, boolean levelgen, String where) {
		Weapon weapon = (Weapon) field(Statue.class, s, "weapon");
		assertNotNull(weapon, where + ": the statue has a weapon");
		assertFalse(weapon.cursed, where + ": and it is not cursed");
		assertEquals(levelgen, s.levelGenStatue, where + ": only a statue placed at levelgen is a landmark");
		if (s instanceof ArmoredStatue) {
			Armor armor = (Armor) field(ArmoredStatue.class, s, "armor");
			assertNotNull(armor, where + ": the armored statue has armor");
			assertFalse(armor.cursed, where + ": and it is not cursed");
		}
		assertNotNull(s.description());
	}

	/** Every deck's cards, seed, draw count and active half. */
	private static String decks() {
		StringBuilder sb = new StringBuilder();
		for (Generator.Category cat : Generator.Category.values()) {
			sb.append(cat).append(Arrays.toString(cat.probs)).append(' ').append(cat.seed)
					.append('/').append(cat.dropped).append('/').append(cat.using2ndProbs).append('\n');
		}
		return sb.toString();
	}

	private static List<Statue> statues(Level level) {
		List<Statue> out = new ArrayList<>();
		for (Mob m : level.mobs) {
			if (m instanceof Statue) out.add((Statue) m);
		}
		return out;
	}

	private static Object field(Class<?> owner, Object target, String name) {
		try {
			Field f = owner.getDeclaredField(name);
			f.setAccessible(true);
			return f.get(target);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String resource(String name) {
		try (InputStream in = LayoutStatueWeaponTest.class.getResourceAsStream(name)) {
			assertNotNull(in, "test resource " + name);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
