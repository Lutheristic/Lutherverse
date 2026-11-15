package com.qsr.customspd.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.blobs.Foliage;
import com.qsr.customspd.actors.blobs.SacrificialFire;
import com.qsr.customspd.actors.blobs.WaterOfAwareness;
import com.qsr.customspd.actors.blobs.WaterOfHealth;
import com.qsr.customspd.actors.mobs.DemonSpawner;
import com.qsr.customspd.actors.mobs.Statue;
import com.qsr.customspd.actors.mobs.npcs.Blacksmith;
import com.qsr.customspd.actors.mobs.npcs.Ghost;
import com.qsr.customspd.actors.mobs.npcs.Imp;
import com.qsr.customspd.actors.mobs.npcs.ImpShopkeeper;
import com.qsr.customspd.actors.mobs.npcs.RatKing;
import com.qsr.customspd.actors.mobs.npcs.Shopkeeper;
import com.qsr.customspd.actors.mobs.npcs.Wandmaker;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.rooms.special.WeakFloorRoom;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.sprites.BlacksmithSprite;
import com.qsr.customspd.sprites.GhostSprite;
import com.qsr.customspd.sprites.ImpSprite;
import com.qsr.customspd.sprites.RatKingSprite;
import com.qsr.customspd.sprites.ShopkeeperSprite;
import com.qsr.customspd.sprites.SpawnerSprite;
import com.qsr.customspd.sprites.StatueSprite;
import com.qsr.customspd.sprites.WandmakerSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The v2.5.4 landmark list and its record text: enum order, per-landmark title and description,
 * floor-name keyed records with contains(), and the shop art following the imp shop floor.
 */
class NotesLandmarkPortTest {

	private static final List<String> V254_ORDER = Arrays.asList(
			"CHASM_FLOOR", "WATER_FLOOR", "GRASS_FLOOR", "DARK_FLOOR", "LARGE_FLOOR", "TRAPS_FLOOR",
			"SECRETS_FLOOR", "SHOP", "ALCHEMY", "GARDEN", "DISTANT_WELL", "WELL_OF_HEALTH",
			"WELL_OF_AWARENESS", "SACRIFICIAL_FIRE", "STATUE", "GHOST", "RAT_KING", "WANDMAKER",
			"TROLL", "IMP", "DEMON_SPAWNER");

	private HeadlessRun run;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261116L);
		Notes.reset();
	}

	@AfterEach
	void tearDown() {
		run.close();
	}

	@Test
	void landmarkEnumIsV254Order() {
		assertEquals(V254_ORDER, names());
	}

	@Test
	void everyNonFloorLandmarkHasTitleAndPlainDesc() {
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("SHOP", Messages.get(Shopkeeper.class, "desc"));
		expected.put("ALCHEMY", Messages.get(Level.class, "alchemy_desc"));
		expected.put("GARDEN", Messages.get(Foliage.class, "desc"));
		expected.put("DISTANT_WELL", Messages.get(WeakFloorRoom.HiddenWell.class, "desc"));
		expected.put("WELL_OF_HEALTH", Messages.get(WaterOfHealth.class, "desc"));
		expected.put("WELL_OF_AWARENESS", Messages.get(WaterOfAwareness.class, "desc"));
		expected.put("SACRIFICIAL_FIRE", Messages.get(SacrificialFire.class, "desc"));
		expected.put("STATUE", Messages.get(Statue.class, "desc"));
		expected.put("GHOST", Messages.get(Ghost.class, "desc"));
		expected.put("RAT_KING", Messages.get(RatKing.class, "desc"));
		expected.put("WANDMAKER", Messages.get(Wandmaker.class, "desc"));
		expected.put("TROLL", Messages.get(Blacksmith.class, "desc"));
		expected.put("IMP", Messages.get(Imp.class, "desc"));
		expected.put("DEMON_SPAWNER", Messages.get(DemonSpawner.class, "desc"));

		//English titles from v2.5.4's journal.properties block
		Map<String, String> titles = new LinkedHashMap<>();
		titles.put("SHOP", "shop");
		titles.put("ALCHEMY", "alchemy pot");
		titles.put("GARDEN", "garden");
		titles.put("DISTANT_WELL", "distant well");
		titles.put("WELL_OF_HEALTH", "well of health");
		titles.put("WELL_OF_AWARENESS", "well of awareness");
		titles.put("SACRIFICIAL_FIRE", "sacrificial fire");
		titles.put("STATUE", "animated statue");
		titles.put("GHOST", "sad ghost");
		titles.put("RAT_KING", "rat king");
		titles.put("WANDMAKER", "old wandmaker");
		titles.put("TROLL", "troll blacksmith");
		titles.put("IMP", "ambitious imp");
		titles.put("DEMON_SPAWNER", "demon spawner");
		assertEquals(titles.keySet(), expected.keySet());

		for (Map.Entry<String, String> e : expected.entrySet()) {
			Notes.LandmarkRecord rec = new Notes.LandmarkRecord(lm(e.getKey()), "3");
			//Messages walks up to Object when a key is missing, so the fallback reads java.lang.object.*
			assertEquals(titles.get(e.getKey()), rec.title(), e.getKey() + " title");
			assertFalse(e.getValue().startsWith("java.lang."), e.getKey() + " source key is missing: " + e.getValue());
			assertEquals(e.getValue(), rec.desc(), e.getKey() + " desc");
			assertFalse(rec.desc().startsWith("java.lang."), e.getKey() + " desc is the missing-key fallback: " + rec.desc());
			assertFalse(rec.desc().isEmpty(), e.getKey() + " desc is empty");
			assertFalse(rec.desc().contains("%"), e.getKey() + " desc has a format marker");
		}
	}

	@Test
	void recordsKeyByFloorNameAndContainsUsesIt() {
		Dungeon.levelName = "3";
		assertFalse(Notes.contains(Notes.Landmark.SHOP));
		assertTrue(Notes.add(Notes.Landmark.SHOP));
		assertTrue(Notes.contains(Notes.Landmark.SHOP));
		assertFalse(Notes.add(Notes.Landmark.SHOP), "adding twice on one floor is a no-op");

		//a5 and b5 share layout depth 5 but are different floors
		DungeonLayout prior = Dungeon.layout;
		try {
			Dungeon.layout = testLayout();
			assertEquals(Notes.depthOf("a5"), Notes.depthOf("b5"));
			Dungeon.levelName = "a5";
			assertFalse(Notes.contains(Notes.Landmark.SHOP), "contains keys by floor name");
			assertTrue(Notes.add(Notes.Landmark.SHOP));
			assertTrue(Notes.contains(Notes.Landmark.SHOP));

			Dungeon.levelName = "b5";
			assertFalse(Notes.contains(Notes.Landmark.SHOP), "same depth, other floor: not contained");
			assertTrue(Notes.add(Notes.Landmark.SHOP), "same depth, other floor: a second record");
			assertEquals(3, Notes.getRecords(Notes.LandmarkRecord.class).size());

			assertTrue(Notes.remove(Notes.Landmark.SHOP));
			assertFalse(Notes.contains(Notes.Landmark.SHOP));
			Dungeon.levelName = "a5";
			assertTrue(Notes.contains(Notes.Landmark.SHOP), "remove only removes the current floor's record");
			Dungeon.levelName = "3";
			assertTrue(Notes.contains(Notes.Landmark.SHOP));
			assertEquals(2, Notes.getRecords(Notes.LandmarkRecord.class).size());
		} finally {
			Dungeon.layout = prior;
		}
	}

	@Test
	void oldLandmarkBundleStillLoads() throws IOException {
		assertTrue(names().contains("DISTANT_WELL"), "DISTANT_WELL exists");
		String json = "{\"records\":[" + landmarkJson("WELL_OF_HEALTH", "3") + "]}";
		Notes.restoreFromBundle(Bundle.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
		assertEquals(new Notes.LandmarkRecord(Notes.Landmark.WELL_OF_HEALTH, "3"),
				Notes.getRecords(Notes.Record.class).get(0));

		Dungeon.levelName = "4";
		Notes.add(lm("DISTANT_WELL"));
		Bundle saved = new Bundle();
		Notes.storeInBundle(saved);
		Notes.restoreFromBundle(SaveRoundtrip.writeRead(saved));
		ArrayList<Notes.Record> records = Notes.getRecords(Notes.Record.class);
		assertEquals(2, records.size());
		assertTrue(records.contains(new Notes.LandmarkRecord(Notes.Landmark.WELL_OF_HEALTH, "3")));
		assertTrue(records.contains(new Notes.LandmarkRecord(lm("DISTANT_WELL"), "4")));
	}

	@Test
	void withinAFloorLandmarksPrecedeKeysInEnumOrder() {
		Dungeon.levelName = "3";
		Notes.add(Notes.Landmark.SHOP);
		Notes.add(lm("CHASM_FLOOR"));
		Notes.add(new IronKey("3"));
		Dungeon.levelName = "5";
		Notes.add(Notes.Landmark.STATUE);
		Notes.add(lm("WATER_FLOOR"));

		assertEquals(Arrays.asList("CHASM_FLOOR@3", "SHOP@3", "IronKey@3"), describe(Notes.getRecords("3")));
		assertEquals(Arrays.asList("CHASM_FLOOR@3", "WATER_FLOOR@5", "SHOP@3", "STATUE@5", "IronKey@3"),
				describe(Notes.getRecords(Notes.Record.class)), "the global list is ordered by order() only");
	}

	@Test
	void shopArtFollowsTheImpShopFloor() {
		String imp = Messages.get(ImpShopkeeper.class, "desc");
		String shop = Messages.get(Shopkeeper.class, "desc");
		assertNotEquals(imp, shop);

		assertEquals(imp, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "20").desc(), "floor 20 is CityBossLevel");
		assertEquals(shop, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "21").desc(), "floor 21 is HallsLevel");
		assertEquals(shop, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "nowhere").desc());

		DungeonLayout prior = Dungeon.layout;
		try {
			Dungeon.layout = null;
			assertEquals(shop, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "20").desc());
		} finally {
			Dungeon.layout = prior;
		}
	}

	@Test
	void landmarkIconsFollowV254() {
		assertSame(texture(GeneralAsset.ICON_STAIRS_CHASM), new Notes.LandmarkRecord(lm("CHASM_FLOOR"), "3").icon().texture);
		assertSame(texture(GeneralAsset.ICON_STAIRS_SECRETS), new Notes.LandmarkRecord(lm("SECRETS_FLOOR"), "3").icon().texture);
		assertSame(texture(GeneralAsset.ICON_DISTANT_WELL), new Notes.LandmarkRecord(lm("DISTANT_WELL"), "3").icon().texture);
		assertSame(texture(GeneralAsset.ICON_WELL_HEALTH), new Notes.LandmarkRecord(Notes.Landmark.WELL_OF_HEALTH, "3").icon().texture);
		assertNotEquals(texture(GeneralAsset.ICON_GOLD), new Notes.LandmarkRecord(Notes.Landmark.SHOP, "3").icon().texture,
				"the shop shows a shopkeeper sprite, not the gold icon");

		Object impTex = new Image(new ImpSprite()).texture;
		Object shopTex = new Image(new ShopkeeperSprite()).texture;
		assertNotEquals(impTex, shopTex);
		assertSame(impTex, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "20").icon().texture, "floor 20 shop shows the imp");
		assertSame(shopTex, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "21").icon().texture, "floor 21 shop shows the shopkeeper");
		assertSame(shopTex, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "3").icon().texture);

		assertSame(new Image(new StatueSprite()).texture, new Notes.LandmarkRecord(Notes.Landmark.STATUE, "3").icon().texture);
		assertSame(new Image(new GhostSprite()).texture, new Notes.LandmarkRecord(Notes.Landmark.GHOST, "3").icon().texture);
		assertSame(new Image(new RatKingSprite()).texture, new Notes.LandmarkRecord(lm("RAT_KING"), "3").icon().texture);
		assertSame(new Image(new WandmakerSprite()).texture, new Notes.LandmarkRecord(Notes.Landmark.WANDMAKER, "3").icon().texture);
		assertSame(new Image(new BlacksmithSprite()).texture, new Notes.LandmarkRecord(Notes.Landmark.TROLL, "3").icon().texture);
		assertSame(impTex, new Notes.LandmarkRecord(Notes.Landmark.IMP, "3").icon().texture);
		assertSame(new Image(new SpawnerSprite()).texture, new Notes.LandmarkRecord(Notes.Landmark.DEMON_SPAWNER, "3").icon().texture);
	}

	@Test
	void unknownFloorRecordUsesSharedSentinel() {
		assertEquals(Notes.UNKNOWN_DEPTH, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "nowhere").depth());
		assertEquals(3, new Notes.LandmarkRecord(Notes.Landmark.SHOP, "3").depth());
	}

	private static DungeonLayout testLayout() {
		try (InputStream in = NotesLandmarkPortTest.class.getResourceAsStream("/notes-floor-order-layout.json")) {
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

	private static Object texture(GeneralAsset asset) {
		return TextureCache.get(Asset.getAssetFilePath(asset));
	}

	private static List<String> names() {
		List<String> out = new ArrayList<>();
		for (Notes.Landmark l : Notes.Landmark.values()) out.add(l.name());
		return out;
	}

	private static Notes.Landmark lm(String name) {
		assertTrue(names().contains(name), "Landmark." + name + " exists");
		return Notes.Landmark.valueOf(name);
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
}
