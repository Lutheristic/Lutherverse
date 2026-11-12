package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.AscensionChallenge;
import com.qsr.customspd.actors.buffs.Buff;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A mimic placed by a custom level's layout is levelled the way Mimic.spawnAt levels the ones
 * regular levels place: by the scaling depth, so a floor first generated during the ascent gets
 * depth 26 mimics on custom and regular levels alike.
 */
class CustomLevelMimicScalingTest {

	private static final String LAYOUT = "custom-level-mimic-layout.json";
	private static final long SEED = 424242L;

	private static int mimicHtWhenGenerated(boolean ascending) {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(LAYOUT));
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Dungeon.layout = layout;
			Dungeon.levelName = layout.getStart();
			Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();
			if (ascending) Buff.affect(Dungeon.hero, AscensionChallenge.class);

			Level level = run.enterFirstLevel();

			Mimic mimic = null;
			for (Mob m : level.mobs) {
				if (m instanceof Mimic) mimic = (Mimic) m;
			}
			assertNotNull(mimic, "the layout's mimic");
			return mimic.HT;
		}
	}

	@Test
	void aLayoutMimicGeneratedDuringTheAscentUsesTheScalingDepth() {
		assertEquals((1 + 26) * 6, mimicHtWhenGenerated(true));
	}

	@Test
	void aLayoutMimicGeneratedOnTheWayDownUsesTheFloorDepth() {
		assertEquals((1 + 1) * 6, mimicHtWhenGenerated(false));
	}

	private static String resource(String name) {
		try (InputStream in = CustomLevelMimicScalingTest.class.getResourceAsStream(name)) {
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
