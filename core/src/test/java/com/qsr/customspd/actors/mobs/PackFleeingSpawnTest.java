package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Dread;
import com.qsr.customspd.actors.buffs.Terror;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A mob a pack layout spawns with ai_state FLEEING acts like any other fleeing mob: once it has
 * nothing to run from it stops fleeing and goes back to wandering, instead of fleeing forever.
 * The layout walls the rat off from the entrance, so it starts with no enemy in view.
 */
class PackFleeingSpawnTest {

	private static final String LAYOUT = "pack-fleeing-spawn-layout.json";
	private static final long SEED = 515151L;

	@Test
	void aPacksFleeingSpawnStopsFleeingWithNothingToRunFrom() {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(LAYOUT));
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Dungeon.layout = layout;
			Dungeon.levelName = layout.getStart();
			Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();

			Level level = run.enterFirstLevel();

			Rat rat = null;
			for (Mob m : level.mobs) {
				if (m instanceof Rat && m.pos == 7 + 3 * level.width()) rat = (Rat) m;
			}
			assertNotNull(rat, "the layout's rat");
			assertSame(rat.FLEEING, rat.state, "the layout spawns it fleeing");
			assertNull(rat.buff(Terror.class));
			assertNull(rat.buff(Dread.class));

			StubSprite.attach(rat);
			Random.pushGenerator(SEED);
			try {
				rat.act();
			} finally {
				Random.popGenerator();
			}

			assertNull(rat.enemy, "precondition: nothing is in the rat's view");
			assertNotSame(rat.FLEEING, rat.state, "with nothing to run from it stops fleeing");
			assertSame(rat.WANDERING, rat.state, rat.state.getClass().getSimpleName());
		}
	}

	private static String resource(String name) {
		try (InputStream in = PackFleeingSpawnTest.class.getResourceAsStream(name)) {
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
