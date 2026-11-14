package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.RotHeart;
import com.qsr.customspd.actors.mobs.npcs.Wandmaker;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.items.quest.CeremonialCandle;
import com.qsr.customspd.items.quest.CorpseDust;
import com.qsr.customspd.items.quest.Embers;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.special.MassGraveRoom;
import com.qsr.customspd.levels.rooms.special.RotGardenRoom;
import com.qsr.customspd.levels.rooms.standard.RitualSiteRoom;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.plants.Rotberry;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.RenderThreadCalls;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Callback;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wandmaker's quest room and the wandmaker himself come from the same regular floor build,
 * on whatever floor the layout names as the wandmaker floor. v2.5.4 only hosts the quest on prison
 * floors; Lutherverse lets a pack layout put it on any regular floor. Here the layout names a sewer
 * floor: it gets the room, its quest pieces and the wandmaker, and the next prison floor gets
 * neither. The quest room flag never outlives the floor that armed it, and the default layout's
 * prison wandmaker floor builds exactly as it did when PrisonLevel held the quest.
 */
class WandmakerQuestRoomFloorTest {

	private static final String LAYOUT = "sewer-wandmaker-layout.json";

	@Test
	void aSewerWandmakerFloorHostsTheWholeQuestAndLeavesNothingArmed() throws Exception {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(LAYOUT));
		for (int type = 1; type <= 3; type++) {
			try (HeadlessRun run = HeadlessRun.start(type)) {
				Dungeon.layout = layout;
				Dungeon.levelName = layout.getStart();
				Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();
				Dungeon.wandmakerLevel = layout.getStart();
				questField("type").setInt(null, type);
				String where = "quest type " + type;

				Level sewer = run.enterFirstLevel();
				assertTrue(sewer instanceof SewerLevel, "test setup: the wandmaker floor is a sewer floor");
				assertEquals(1, wandmakers(sewer), where + ": the sewer wandmaker floor has the wandmaker");
				assertQuestRoomOnlyWithTheWandmaker((RegularLevel) sewer, where + " sewer floor");
				assertFalse(questRoomSpawned(), where + ": the quest room flag outlived the sewer floor");
				assertQuestPieces((RegularLevel) sewer, type, where);
				assertWandmakerTalksAndRewards((RegularLevel) sewer, type, where);

				Level prison = run.descend();
				assertTrue(prison instanceof PrisonLevel, "test setup: the next floor is a prison floor");
				assertEquals(0, wandmakers(prison), where + ": the next prison floor is not the wandmaker floor");
				assertQuestRoomOnlyWithTheWandmaker((RegularLevel) prison, where + " prison floor");
				assertFalse(questRoomSpawned(), where + ": the quest room flag outlived the prison floor");
			}
		}
	}

	@Test
	void theDefaultLayoutStillHostsTheQuestOnAPrisonFloor() throws Exception {
		for (long seed = 1; seed <= 2; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				Level floor = run.generate(Dungeon.wandmakerLevel);
				assertTrue(floor instanceof PrisonLevel, "test setup: the default wandmaker floor is a prison floor");
				assertEquals(1, wandmakers(floor), "seed " + seed + ": the prison wandmaker floor has the wandmaker");
				assertQuestRoomOnlyWithTheWandmaker((RegularLevel) floor, "seed " + seed + " prison wandmaker floor");
				assertFalse(questRoomSpawned(), "seed " + seed + ": the quest room flag outlived the prison wandmaker floor");
			}
		}
	}

	/*
	 * Digests (LevelgenDeterminismTest.Floor) of the default layout's wandmaker floor, a prison floor,
	 * for two seeds, taken while PrisonLevel still added the quest room and placed the wandmaker
	 * itself. The golden digests stop at floor 6, so these hold the prison build order: the quest
	 * room joins the room list after every other room, and the wandmaker is placed before any other
	 * mob. Like the golden digests, update them only for an intended levelgen change.
	 */
	private static final long[] PRISON_SEEDS = {31337L, 8675309L};
	private static final String[] PRISON_WANDMAKER_FLOOR = {"a648934e8522afea", "74fc58f563b7ceae"};

	@Test
	void thePrisonWandmakerFloorBuildsInTheSameOrderAsBefore() {
		String[] actual = new String[PRISON_SEEDS.length];
		for (int i = 0; i < PRISON_SEEDS.length; i++) {
			try (HeadlessRun run = HeadlessRun.start(PRISON_SEEDS[i])) {
				Level floor = run.generate(Dungeon.wandmakerLevel);
				assertTrue(floor instanceof PrisonLevel, "test setup: the default wandmaker floor is a prison floor");
				assertEquals(1, wandmakers(floor), "test setup: seed " + PRISON_SEEDS[i] + " hosts the quest");
				actual[i] = new LevelgenDeterminismTest.Floor(floor).digest();
			}
		}
		assertEquals(Arrays.toString(PRISON_WANDMAKER_FLOOR), Arrays.toString(actual),
				"the prison wandmaker floor no longer builds as it did: the quest room must join the room list"
						+ " last and the wandmaker must be placed before the other mobs");
	}

	/** The room for the drawn quest type holds what the quest needs, painted on a sewer floor. */
	private static void assertQuestPieces(RegularLevel level, int type, String where) {
		Room room = null;
		for (Room r : level.rooms()) {
			if (r instanceof MassGraveRoom || r instanceof RitualSiteRoom || r instanceof RotGardenRoom) room = r;
		}
		assertNotNull(room, where + ": a quest room was built");
		switch (type) {
			case 1:
				assertTrue(room instanceof MassGraveRoom, where + ": the corpse dust quest builds the mass grave");
				int dust = 0;
				for (Heap h : level.heaps.valueList()) {
					for (Item i : h.items) {
						if (i instanceof CorpseDust) {
							dust++;
							assertSame(room, level.room(h.pos), where + ": the corpse dust lies in the mass grave");
							assertEquals(Terrain.CUSTOM_DECO_EMPTY, level.map[h.pos], where + ": the corpse dust lies on the grave floor");
						}
					}
				}
				assertEquals(1, dust, where + ": the mass grave holds the corpse dust");
				break;
			case 2:
				assertTrue(room instanceof RitualSiteRoom, where + ": the embers quest builds the ritual site");
				assertSame(room, level.room(CeremonialCandle.ritualPos), where + ": the ritual marker is inside the ritual site");
				assertEquals(Terrain.CUSTOM_DECO_EMPTY, level.map[CeremonialCandle.ritualPos], where + ": the ritual marker is painted");
				assertEquals(4, itemsOnFloor(level, CeremonialCandle.class), where + ": the four candles are on the floor");
				break;
			default:
				assertTrue(room instanceof RotGardenRoom, where + ": the rotberry quest builds the rot garden");
				int hearts = 0;
				for (Mob m : level.mobs) {
					if (m instanceof RotHeart) {
						hearts++;
						assertSame(room, level.room(m.pos), where + ": the rot heart grows in the rot garden");
					}
				}
				assertEquals(1, hearts, where + ": the rot garden has its rot heart");
				assertEquals(1, itemsOnFloor(level, IronKey.class), where + ": the rot garden key is on the floor");
				break;
		}
	}

	/** The wandmaker stands in the entrance room, gives the quest and takes the quest item back. */
	private static void assertWandmakerTalksAndRewards(RegularLevel level, int type, String where) throws Exception {
		Wandmaker npc = null;
		for (Mob m : level.mobs) {
			if (m instanceof Wandmaker) npc = (Wandmaker) m;
		}
		assertNotNull(npc, where + ": the wandmaker is on the floor");
		assertSame(level.roomEntrance, level.room(npc.pos), where + ": the wandmaker waits in the entrance room");
		assertNotNull(Wandmaker.Quest.wand1, where + ": the first reward wand is rolled");
		assertNotNull(Wandmaker.Quest.wand2, where + ": the second reward wand is rolled");
		assertNotEquals(Wandmaker.Quest.wand1.getClass(), Wandmaker.Quest.wand2.getClass(), where + ": the reward wands differ");

		StubSprite.attach(Dungeon.hero);
		StubSprite.attach(npc);

		List<Callback> posted;
		try (RenderThreadCalls calls = RenderThreadCalls.capture()) {
			npc.interact(Dungeon.hero);
			posted = calls.callbacks();
		}
		assertEquals(1, posted.size(), where + ": talking to the wandmaker opens his intro");
		String intro = type == 1 ? "intro_dust" : type == 2 ? "intro_ember" : "intro_berry";
		assertEquals(Messages.get(Wandmaker.class, intro) + Messages.get(Wandmaker.class, "intro_2"),
				RenderThreadCalls.captured(posted.get(0), "msg2Final"), where + ": the wandmaker asks for this quest's item");
		assertTrue(questField("given").getBoolean(null), where + ": the quest is given");

		Item questItem = type == 1 ? new CorpseDust() : type == 2 ? new Embers() : new Rotberry.Seed();
		assertTrue(questItem.collect(Dungeon.hero.belongings.backpack), "test setup: the hero carries the quest item");
		try (RenderThreadCalls calls = RenderThreadCalls.capture()) {
			npc.interact(Dungeon.hero);
			posted = calls.callbacks();
		}
		assertEquals(1, posted.size(), where + ": bringing the quest item opens the reward window");
		assertSame(questItem, RenderThreadCalls.captured(posted.get(0), "item"), where + ": the reward window takes the quest item");
	}

	private static void assertQuestRoomOnlyWithTheWandmaker(RegularLevel level, String where) {
		boolean questRoom = false;
		for (Room r : level.rooms()) {
			if (r instanceof MassGraveRoom || r instanceof RitualSiteRoom || r instanceof RotGardenRoom) questRoom = true;
		}
		assertEquals(questRoom, wandmakers(level) == 1,
				where + ": the quest room and the wandmaker come together (quest room " + questRoom + ", wandmakers " + wandmakers(level) + ")");
	}

	private static int itemsOnFloor(Level level, Class<? extends Item> type) {
		int n = 0;
		for (Heap h : level.heaps.valueList()) {
			for (Item i : h.items) {
				if (type.isInstance(i)) n += i.quantity();
			}
		}
		return n;
	}

	private static int wandmakers(Level level) {
		int n = 0;
		for (Mob m : level.mobs) {
			if (m instanceof Wandmaker) n++;
		}
		return n;
	}

	private static boolean questRoomSpawned() throws Exception {
		return questField("questRoomSpawned").getBoolean(null);
	}

	private static Field questField(String name) throws Exception {
		Field f = Wandmaker.Quest.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static String resource(String name) {
		try (InputStream in = WandmakerQuestRoomFloorTest.class.getResourceAsStream(name)) {
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
