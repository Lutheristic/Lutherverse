package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mimic;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.test.HeadlessRun;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mimic a pack lists in a regular level's extra_mobs is scaled to the floor like any other
 * mimic, and an hp the pack gives it survives the scaling, the same order
 * CustomLevel.createMobs uses for a layout's mimics.
 */
class RegularLevelExtraMimicScalingTest {

	private static final String LAYOUT = "regular-level-extra-mimic-layout.json";

	@Test
	void anExtraMobsMimicIsScaledAndKeepsItsHp() {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(LAYOUT));
		for (long seed = 1; seed <= 3; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				Dungeon.layout = layout;
				Dungeon.levelName = layout.getStart();
				Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();

				Level level = run.enterFirstLevel();
				assertTrue(level instanceof RegularLevel, "test setup: the floor is a regular level");

				// mimics the level makes from item heaps carry those items; the pack's two do not
				List<Mimic> extra = new ArrayList<>();
				for (Mob m : level.mobs) {
					if (m.getClass() == Mimic.class && ((Mimic) m).items == null) extra.add((Mimic) m);
				}
				assertEquals(2, extra.size(), "seed " + seed + ": both extra_mobs mimics were placed");

				int scaled = (1 + Dungeon.scalingDepth()) * 6;
				assertEquals(30, scaled, "test setup: a depth 4 floor");
				Mimic withHp = extra.get(0).HP == 5 ? extra.get(0) : extra.get(1);
				Mimic withoutHp = withHp == extra.get(0) ? extra.get(1) : extra.get(0);
				assertEquals(5, withHp.HP, "seed " + seed + ": the pack's hp");
				assertEquals(scaled, withHp.HT, "seed " + seed + ": the mimic with an hp is still scaled");
				assertEquals(scaled, withoutHp.HT, "seed " + seed + ": the mimic without an hp is scaled");
				assertEquals(withoutHp.HT, withoutHp.HP, "seed " + seed + ": with no hp given it starts at full health");
			}
		}
	}

	private static String resource(String name) {
		try (InputStream in = RegularLevelExtraMimicScalingTest.class.getResourceAsStream(name)) {
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
