package com.qsr.customspd.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The journal's note records: depth comes from the current layout by floor name, the global list
 * is sorted by order() only, and each floor's records are filtered and sorted by order().
 */
class NotesRecordModelTest {

	private HeadlessRun run;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261115L);
		Notes.reset();
	}

	@AfterEach
	void tearDown() {
		run.close();
	}

	@Test
	void depthOfReadsTheLayout() {
		assertEquals(3, Notes.depthOf("3"));
		assertEquals(Notes.UNKNOWN_DEPTH, Notes.depthOf(null));
		assertEquals(Notes.UNKNOWN_DEPTH, Notes.depthOf("gone"));
		DungeonLayout layout = Dungeon.layout;
		try {
			Dungeon.layout = null;
			assertEquals(Notes.UNKNOWN_DEPTH, Notes.depthOf("3"));
		} finally {
			Dungeon.layout = layout;
		}
	}

	@Test
	void compareFloorsDeepestFirstZeroRealUnknownLast() {
		DungeonLayout prior = Dungeon.layout;
		try {
			Dungeon.layout = testLayout();
			assertEquals(0, Notes.depthOf("z0"), "depth 0 is a real depth");
			List<String> floors = new ArrayList<>(Arrays.asList("z0", "gone", "b5", "x6", "a5"));
			Collections.sort(floors, new Comparator<String>() {
				@Override
				public int compare(String a, String b) {
					return Notes.compareFloors(a, b);
				}
			});
			assertEquals(Arrays.asList("x6", "a5", "b5", "z0", "gone"), floors);
		} finally {
			Dungeon.layout = prior;
		}
	}

	@Test
	void globalSortIsOrderOnly() {
		Dungeon.levelName = "1";
		Notes.add(Notes.Landmark.STATUE);
		Dungeon.levelName = "5";
		Notes.add(Notes.Landmark.ALCHEMY);
		Notes.add(new IronKey("3"));
		Dungeon.levelName = "3";
		Notes.add(Notes.Landmark.GARDEN);

		assertEquals(Arrays.asList("ALCHEMY@5", "GARDEN@3", "STATUE@1", "IronKey@3"),
				describe(Notes.getRecords(Notes.Record.class)));
	}

	@Test
	void sameOrderKeepsInsertionNotFloor() {
		Dungeon.levelName = "2";
		Notes.add(Notes.Landmark.GARDEN);
		Dungeon.levelName = "7";
		Notes.add(Notes.Landmark.GARDEN);

		List<String> floors = new ArrayList<>();
		for (Notes.Record rec : Notes.getRecords(Notes.Record.class)) floors.add(rec.levelName());
		assertEquals(Arrays.asList("2", "7"), floors);
	}

	@Test
	void perFloorGetterFiltersAndSortsByOrder() {
		Dungeon.levelName = "4";
		Notes.add(new IronKey("4"));
		Notes.add(Notes.Landmark.STATUE);
		Notes.add(Notes.Landmark.ALCHEMY);
		Dungeon.levelName = "2";
		Notes.add(Notes.Landmark.GARDEN);

		assertEquals(Arrays.asList("ALCHEMY@4", "STATUE@4", "IronKey@4"), describe(Notes.getRecords("4")));
		assertEquals(Collections.singletonList("GARDEN@2"), describe(Notes.getRecords("2")));
		assertTrue(Notes.getRecords("gone").isEmpty());
		assertTrue(Notes.getRecords((String) null).isEmpty());
	}

	@Test
	void perFloorGetterSortsAnUnsortedRestore() throws IOException {
		Bundle keyHolder = new Bundle();
		keyHolder.put("key", new IronKey("4"));
		String json = "{\"records\":["
				+ "{\"__className\":\"" + Notes.KeyRecord.class.getName() + "\","
				+ "\"key\":" + keyHolder.getBundle("key").toString() + "},"
				+ landmarkJson("STATUE", "4") + ","
				+ landmarkJson("ALCHEMY", "4")
				+ "]}";
		Notes.restoreFromBundle(Bundle.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));

		assertEquals(Arrays.asList("IronKey@4", "STATUE@4", "ALCHEMY@4"),
				describe(Notes.getRecords(Notes.Record.class)), "restore keeps the stored order");
		assertEquals(Arrays.asList("ALCHEMY@4", "STATUE@4", "IronKey@4"), describe(Notes.getRecords("4")),
				"the per-floor getter sorts by order() even when the global list is unsorted");
	}

	@Test
	void oldSaveFormatRestores() throws IOException {
		Bundle keyHolder = new Bundle();
		keyHolder.put("key", new IronKey("4"));
		String json = "{\"records\":["
				+ landmarkJson("GARDEN", "1") + ","
				+ landmarkJson("STATUE", "4") + ","
				+ "{\"__className\":\"" + Notes.KeyRecord.class.getName() + "\","
				+ "\"key\":" + keyHolder.getBundle("key").toString() + "},"
				+ landmarkJson("GARDEN", "gone")
				+ "]}";
		Bundle old = Bundle.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		Notes.restoreFromBundle(old);
		List<String> expected = Arrays.asList("GARDEN@1", "STATUE@4", "IronKey@4", "GARDEN@gone");
		assertEquals(expected, describe(Notes.getRecords(Notes.Record.class)), "restore keeps the stored order");

		Bundle saved = new Bundle();
		Notes.storeInBundle(saved);
		Notes.restoreFromBundle(SaveRoundtrip.writeRead(saved));
		ArrayList<Notes.Record> records = Notes.getRecords(Notes.Record.class);
		assertEquals(expected, describe(records), "a round trip keeps the stored order");

		assertEquals(new Notes.LandmarkRecord(Notes.Landmark.GARDEN, "1"), records.get(0));
		assertEquals(new Notes.LandmarkRecord(Notes.Landmark.STATUE, "4"), records.get(1));
		assertEquals(1, Notes.keyCount(new IronKey("4")));
		assertEquals(Notes.UNKNOWN_DEPTH, records.get(3).depth());
		assertEquals(4, records.get(1).depth());

		Bundle again = new Bundle();
		Notes.storeInBundle(again);
		for (Bundle rec : again.getBundleArray("records")) {
			assertFalse(rec.contains("depth"), "no depth key is saved: " + rec);
		}
	}

	@Test
	void recordIconsTitlesAndSecondIcon() {
		BitmapText.Font priorFont = PixelScene.pixelFont;
		try {
			PixelScene.pixelFont = new BitmapText.Font(
					TextureCache.create("test-notes-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
					4, BitmapText.Font.LATIN_FULL);

			Notes.LandmarkRecord well = new Notes.LandmarkRecord(Notes.Landmark.WELL_OF_HEALTH, "1");
			assertSame(texture(GeneralAsset.ICON_WELL_HEALTH), well.icon().texture);
			assertSame(texture(GeneralAsset.ICON_GRASS),
					new Notes.LandmarkRecord(Notes.Landmark.GARDEN, "1").icon().texture);
			assertEquals(Messages.get(Notes.Landmark.class, "well_of_health"), well.title());
			assertEquals(Messages.get(com.qsr.customspd.actors.blobs.WaterOfHealth.class, "desc"), well.desc());

			IronKey key = new IronKey("1");
			Notes.KeyRecord rec = new Notes.KeyRecord(key);
			assertEquals(key.title(), rec.title());
			assertEquals(key.desc(), rec.desc());
			assertNull(rec.secondIcon(), "no count at quantity 1");

			key.quantity(2);
			Visual second = rec.secondIcon();
			assertNotNull(second);
			assertInstanceOf(BitmapText.class, second);
			assertEquals("2", ((BitmapText) second).text());
		} finally {
			PixelScene.pixelFont = priorFont;
		}
	}

	private static Object texture(GeneralAsset asset) {
		return TextureCache.get(Asset.getAssetFilePath(asset));
	}

	private static String landmarkJson(String landmark, String levelName) {
		return "{\"__className\":\"" + Notes.LandmarkRecord.class.getName() + "\","
				+ "\"levelName\":\"" + levelName + "\",\"landmark\":\"" + landmark + "\"}";
	}

	private static List<String> describe(List<? extends Notes.Record> records) {
		List<String> out = new ArrayList<>();
		for (Notes.Record rec : records) {
			String what = rec instanceof Notes.LandmarkRecord
					? ((Notes.LandmarkRecord) rec).landmark.name()
					: ((Notes.KeyRecord) rec).type().getSimpleName();
			out.add(what + "@" + rec.levelName());
		}
		return out;
	}

	private static DungeonLayout testLayout() {
		try (InputStream in = NotesRecordModelTest.class.getResourceAsStream("/notes-floor-order-layout.json")) {
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
