package com.qsr.customspd.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.EquipableItem;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Custom notes in the journal model (v2.5.0/2.5.1). A floor note is keyed by floor name and takes
 * its depth from the layout; text and item notes have no floor.
 */
class CustomNotesPortTest {

	private HeadlessRun run;
	private BitmapText.Font priorFont;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261115L);
		Notes.reset();
		Notes.nextCustomID = 0;
		priorFont = PixelScene.pixelFont;
		PixelScene.pixelFont = new BitmapText.Font(
				TextureCache.create("test-custom-notes-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
				4, BitmapText.Font.LATIN_FULL);
	}

	@AfterEach
	void tearDown() {
		PixelScene.pixelFont = priorFont;
		run.close();
	}

	@Test
	void aCustomNoteRoundTripsThroughASave() {
		run.enterFirstLevel();
		Notes.reset();

		Notes.CustomRecord text = new Notes.CustomRecord("text title", "text body");
		Notes.CustomRecord floor = new Notes.CustomRecord(Dungeon.levelName, "floor title", "floor body");
		Notes.CustomRecord type = new Notes.CustomRecord(new PotionOfHealing(), "type title", "type body");
		Notes.CustomRecord item = new Notes.CustomRecord(Dungeon.hero.belongings.weapon, "item title", "item body");
		assertTrue(Notes.add(text));
		assertTrue(Notes.add(floor));
		assertTrue(Notes.add(type));
		item.assignID();
		((EquipableItem) Dungeon.hero.belongings.weapon).customNoteID = item.ID();
		assertTrue(Notes.add(item));
		String floorName = Dungeon.levelName;
		Class weaponClass = Dungeon.hero.belongings.weapon.getClass();

		run.saveAll();
		run.loadGame();

		ArrayList<Notes.CustomRecord> back = Notes.getRecords(Notes.CustomRecord.class);
		assertEquals(4, back.size(), "four custom notes come back: " + back);
		check(back.get(0), 0, Notes.CustomType.TEXT, "text title", "text body", null, null);
		check(back.get(1), 1, Notes.CustomType.DEPTH, "floor title", "floor body", null, floorName);
		check(back.get(2), 2, Notes.CustomType.ITEM, "type title", "type body", PotionOfHealing.class, null);
		check(back.get(3), 3, Notes.CustomType.ITEM, "item title", "item body", weaponClass, null);
		assertEquals(3, ((EquipableItem) Dungeon.hero.belongings.weapon).customNoteID,
				"the weapon keeps its note ID");

		Notes.CustomRecord next = new Notes.CustomRecord("next", "");
		assertTrue(Notes.add(next), "a new note after the reload does not collide with a restored one");
		assertEquals(4, next.ID());
	}

	private static void check(Notes.CustomRecord rec, int id, Notes.CustomType type, String title, String desc,
							  Class itemClass, String levelName) {
		assertEquals(id, rec.ID());
		assertEquals(type, rec.type);
		assertEquals(title, rec.title());
		assertEquals(desc, rec.desc());
		assertSame(itemClass, rec.itemClass);
		if (levelName != null) {
			assertEquals(levelName, rec.levelName());
		} else {
			assertTrue(rec.levelName() == null || rec.levelName().isEmpty(),
					"a note with no floor has no floor name: '" + rec.levelName() + "'");
		}
	}

	@Test
	void aFloorNoteStoresTheFloorNameNotADepth() throws IOException {
		Notes.CustomRecord floor = new Notes.CustomRecord("3", "t", "b");
		Bundle bundle = new Bundle();
		floor.storeInBundle(bundle);
		assertEquals("3", bundle.getString("levelName"));
		assertFalse(bundle.contains("depth"), "no depth key is saved: " + bundle);
		assertEquals(Dungeon.layout.getDungeon().get("3").getDepth(), floor.depth());

		DungeonLayout prior = Dungeon.layout;
		try {
			Dungeon.layout = testLayout();
			Notes.CustomRecord gone = SaveRoundtrip.of(floor);
			assertEquals(Notes.UNKNOWN_DEPTH, gone.depth(), "a floor missing from the layout has no depth");
			assertEquals("3", gone.levelName(), "the note keeps its floor name");
			assertEquals("?", secondIconText(gone));
			assertEquals("t", gone.title());

			Notes.CustomRecord zero = new Notes.CustomRecord("z0", "t", "b");
			assertEquals(0, zero.depth(), "depth 0 is a real floor");
			assertEquals("0", secondIconText(zero));
		} finally {
			Dungeon.layout = prior;
		}
	}

	@Test
	void textAndItemNotesHaveNoFloor() throws IOException {
		Notes.CustomRecord text = new Notes.CustomRecord("t", "b");
		Notes.CustomRecord item = new Notes.CustomRecord(new PotionOfHealing(), "t", "b");
		assertEquals(Notes.UNKNOWN_DEPTH, text.depth());
		assertEquals(Notes.UNKNOWN_DEPTH, item.depth());
		assertEquals(Notes.UNKNOWN_DEPTH, SaveRoundtrip.of(text).depth());
		assertEquals(Notes.UNKNOWN_DEPTH, SaveRoundtrip.of(item).depth());
	}

	@Test
	void aTypeNoteIsFoundByClassWhateverItsID() {
		Notes.CustomRecord type = new Notes.CustomRecord(new PotionOfHealing(), "t", "b");
		assertTrue(Notes.add(type));
		assertTrue(type.ID() != -1, "adding assigns an ID");
		assertSame(type, Notes.findCustomRecord(PotionOfHealing.class));
		assertSame(type, Notes.findCustomRecord(type.ID()));
	}

	@Test
	void floorListsLeaveCustomNotesOut() {
		run.enterFirstLevel();
		Notes.reset();
		Notes.add(Notes.Landmark.STATUE);
		Notes.CustomRecord floor = new Notes.CustomRecord(Dungeon.levelName, "t", "b");
		Notes.add(floor);

		ArrayList<Notes.Record> here = Notes.getRecords(Dungeon.levelName);
		assertEquals(1, here.size(), "only the landmark is listed for the floor: " + here);
		assertInstanceOf(Notes.LandmarkRecord.class, here.get(0));
		assertTrue(Notes.getRecords(Notes.CustomRecord.class).contains(floor));
		assertEquals(5, Notes.customRecordLimit());
	}

	@Test
	void customNotesSortBetweenLandmarksAndKeys() {
		Dungeon.levelName = "2";
		Notes.add(new IronKey("2"));
		Notes.CustomRecord first = new Notes.CustomRecord("first", "");
		Notes.add(first);
		Notes.add(Notes.Landmark.STATUE);
		Notes.CustomRecord second = new Notes.CustomRecord("2", "second", "");
		Notes.add(second);

		List<String> order = new ArrayList<>();
		for (Notes.Record rec : Notes.getRecords(Notes.Record.class)) {
			order.add(rec.getClass().getSimpleName() + ":" + rec.title());
		}
		assertEquals(Arrays.asList(
				"LandmarkRecord:" + new Notes.LandmarkRecord(Notes.Landmark.STATUE, "2").title(),
				"CustomRecord:first",
				"CustomRecord:second",
				"KeyRecord:" + new IronKey("2").title()), order);
	}

	private static String secondIconText(Notes.CustomRecord rec) {
		Visual second = rec.secondIcon();
		assertNotNull(second);
		assertInstanceOf(BitmapText.class, second);
		return ((BitmapText) second).text();
	}

	private static DungeonLayout testLayout() {
		try (InputStream in = CustomNotesPortTest.class.getResourceAsStream("/notes-floor-order-layout.json")) {
			assertNotNull(in, "test resource notes-floor-order-layout.json");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
			return Json.Default.decodeFromString(DungeonLayout.Companion.serializer(),
					new String(out.toByteArray(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
