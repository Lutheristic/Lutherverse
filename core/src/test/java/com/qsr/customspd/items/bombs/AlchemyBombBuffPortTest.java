package com.qsr.customspd.items.bombs;

import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.blobs.Blob;
import com.qsr.customspd.actors.blobs.SmokeScreen;
import com.qsr.customspd.actors.buffs.Healing;
import com.qsr.customspd.actors.buffs.Paralysis;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.PotionOfInvisibility;
import com.qsr.customspd.items.scrolls.ScrollOfRecharging;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.journal.Journal;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StaticSnapshot;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * upstream a2a39d92a and fb08349cf (v2.5.3): every alchemy bomb breaks terrain, items and
 * characters within its own range instead of a fixed 3x3, the arcane, holy and shrapnel bombs
 * hit harder, the regrowth bomb reaches 3 tiles, the flashbang becomes the smoke bomb and the
 * shock bomb becomes the flashbang. Old saves convert to the new names.
 */
class AlchemyBombBuffPortTest {

	private static final int W = 21;
	private static final int CENTER = 10 * W + 10;
	private static final int DEPTH = 10;
	private static final String PKG = "com.qsr.customspd.items.bombs.";

	/** One hit a recording rat took. */
	static final class Hit {
		final Object src;
		final int dmg;
		final int taken;

		Hit(Object src, int dmg, int taken) {
			this.src = src;
			this.dmg = dmg;
			this.taken = taken;
		}
	}

	/** A tough rat that keeps every hit it takes. */
	public static class RecordingRat extends Rat {
		final List<Hit> hits = new ArrayList<>();

		{
			HP = HT = 1000;
		}

		@Override
		public void damage(int dmg, Object src) {
			int before = HP;
			super.damage(dmg, src);
			hits.add(new Hit(src, dmg, before - HP));
		}
	}

	public static class UndeadRecordingRat extends RecordingRat {
		{
			properties.add(Property.UNDEAD);
		}
	}

	private int savedDepth;
	private Hero hero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		Messages.setup(Languages.ENGLISH);
		ShatteredPixelDungeon.registerBundleAliases();
	}

	@BeforeEach
	void setUp() {
		savedDepth = Dungeon.depth;
		Dungeon.depth = DEPTH;
		freshLevel();
	}

	@AfterEach
	void tearDown() {
		Dungeon.depth = savedDepth;
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** An open room with the hero in a corner, out of reach of every blast. */
	private HeadlessLevel freshLevel() {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, W + 1);
		return level;
	}

	private static <T extends RecordingRat> T ratAt(T rat, int cell) {
		HeadlessLevel.at(rat, cell);
		StubSprite.attach(rat);
		Dungeon.level.mobs.add(rat);
		return rat;
	}

	/** The bombs added by the rename, by name, so this test also runs against the old classes. */
	private static Bomb bomb(String simpleName) throws Exception {
		return (Bomb) Class.forName(PKG + simpleName).getDeclaredConstructor().newInstance();
	}

	/** Headless, a bomb's own visuals stop at CellEmitter (no game scene); its effects come first. */
	private static void explodeUpToTheScene(Bomb bomb, int cell) {
		try {
			bomb.explode(cell);
		} catch (NullPointerException e) {
			for (StackTraceElement el : e.getStackTrace()) {
				if (el.getClassName().endsWith(".CellEmitter")) return;
			}
			throw e;
		}
	}

	@Test
	void aPlainBombStillOnlyReachesAdjacentCells() {
		RecordingRat near = ratAt(new RecordingRat(), CENTER + 1);
		RecordingRat far = ratAt(new RecordingRat(), CENTER + 2);
		Dungeon.level.map[CENTER - 2 * W] = Terrain.HIGH_GRASS;
		Dungeon.level.buildFlagMaps();

		new Bomb().explode(CENTER);

		assertEquals(1, near.hits.size(), "an adjacent rat is caught");
		assertTrue(far.hits.isEmpty(), "a rat two tiles off is not");
		assertEquals(Terrain.HIGH_GRASS, Dungeon.level.map[CENTER - 2 * W], "nor is grass two tiles off");
	}

	@Test
	void aNoisemakerBlastReachesTwoTiles() {
		RecordingRat far = ratAt(new RecordingRat(), CENTER + 2);
		Dungeon.level.map[CENTER - 2 * W] = Terrain.HIGH_GRASS;
		Dungeon.level.buildFlagMaps();

		new Noisemaker().explode(CENTER);

		assertEquals(1, far.hits.size(), "a rat two tiles off is caught in the blast");
		assertEquals(Terrain.EMBERS, Dungeon.level.map[CENTER - 2 * W], "and grass two tiles off burns");
	}

	@Test
	void theArcaneBombDealsFullDamageThroughoutItsRange() {
		RecordingRat rat = ratAt(new RecordingRat(), CENTER + 2);
		for (long seed = 1; seed <= 20; seed++) {
			Random.pushGenerator(seed);
			int expected = Random.NormalIntRange(4 + DEPTH, 12 + 3 * DEPTH);
			Random.popGenerator();

			rat.hits.clear();
			rat.HP = rat.HT;
			Random.pushGenerator(seed);
			try {
				new ArcaneBomb().explode(CENTER);
			} finally {
				Random.popGenerator();
			}
			assertEquals(1, rat.hits.size());
			assertEquals(expected, rat.hits.get(0).dmg, "seed " + seed + ": full bomb damage two tiles out");
		}
	}

	@Test
	void theHolyBombBlastsTwoTilesAndItsLightHitsHarder() {
		RecordingRat rat = ratAt(new UndeadRecordingRat(), CENTER + 2);
		int highest = 0;
		for (long seed = 1; seed <= 40; seed++) {
			rat.hits.clear();
			rat.HP = rat.HT;
			Random.pushGenerator(seed);
			try {
				new HolyBomb().explode(CENTER);
			} finally {
				Random.popGenerator();
			}
			assertEquals(2, rat.hits.size(), "seed " + seed + ": the blast and the holy light both reach two tiles");
			int light = rat.hits.get(1).dmg;
			assertTrue(light >= 7 && light <= 21, "seed " + seed + ": half of 14-42, got " + light);
			highest = Math.max(highest, light);
		}
		assertTrue(highest > 15, "the light exceeds the old cap of half of 15-30; highest " + highest);
	}

	@Test
	void theShrapnelBombNoLongerWeakensWithDistance() {
		RecordingRat rat = ratAt(new RecordingRat(), CENTER + 7);
		int highest = 0;
		for (long seed = 1; seed <= 40; seed++) {
			rat.hits.clear();
			rat.HP = rat.HT;
			Random.pushGenerator(seed);
			try {
				new ShrapnelBomb().explode(CENTER);
			} finally {
				Random.popGenerator();
			}
			assertEquals(1, rat.hits.size());
			int dmg = rat.hits.get(0).dmg;
			assertTrue(dmg <= 12 + 3 * DEPTH, "seed " + seed + ": at most full bomb damage, got " + dmg);
			highest = Math.max(highest, dmg);
		}
		assertTrue(highest > 20, "seven tiles out it beats the old 65% falloff cap of 20; highest " + highest);
	}

	@Test
	void theRegrowthBombHealsThreeTilesAway() {
		hero.pos = CENTER + 3;

		new RegrowthBomb().explode(CENTER);

		assertNotNull(hero.buff(Healing.class), "the hero three tiles away is healed");
	}

	@Test
	void theRegrowthBombAlwaysGrowsAtLeastTwoSeedsBesidesItsSpecialPlant() {
		StaticSnapshot generator = StaticSnapshot.capture(Arrays.<Class<?>>asList(Generator.class, Generator.Category.class));
		try {
			for (long seed = 1; seed <= 30; seed++) {
				Level level = freshLevel();
				Random.pushGenerator(seed);
				try {
					new RegrowthBomb().explode(CENTER);
				} finally {
					Random.popGenerator();
				}
				int plants = level.plants.size;
				assertTrue(plants == 3 || plants == 4, "seed " + seed + ": 2-3 seeds plus one special plant, got " + plants);
			}
		} finally {
			generator.restore();
		}
	}

	@Test
	void theFlashbangShocksAndStunsEverythingWithinTwoTiles() throws Exception {
		RecordingRat rat = ratAt(new RecordingRat(), CENTER + 2);

		explodeUpToTheScene(bomb("FlashBangBomb"), CENTER);

		assertEquals(2, rat.hits.size(), "the blast, then the electricity");
		int electric = rat.hits.get(1).dmg;
		assertTrue(electric >= Math.round((4 + DEPTH) / 4f) && electric <= Math.round((12 + 3 * DEPTH) / 4f),
				"the electricity adds a quarter of bomb damage, got " + electric);
		Paralysis stun = rat.buff(Paralysis.class);
		assertNotNull(stun, "and stuns");
		assertEquals(Paralysis.DURATION, stun.cooldown(), 0.001f, "for 10 turns");
	}

	@Test
	void theSmokeBombFillsTwoTilesWithASmokeScreen() throws Exception {
		bomb("SmokeBomb").explode(CENTER);

		Blob smoke = Dungeon.level.blobs.get(SmokeScreen.class);
		assertNotNull(smoke, "a smoke screen is released");
		assertTrue(smoke.cur[CENTER + 2] > 0, "two tiles out");
		assertFalse(smoke.cur[CENTER + 3] > 0, "but not three");
		assertEquals(1000, smoke.volume, "1000 smoke in all");
	}

	@Test
	void everyAlchemyBombDescriptionShowsItsDamage() throws Exception {
		Dungeon.hero = null;
		for (String name : new String[]{"ArcaneBomb", "Firebomb", "FrostBomb", "HolyBomb", "Noisemaker",
				"ShrapnelBomb", "WoollyBomb", "SmokeBomb", "FlashBangBomb"}) {
			String desc = bomb(name).desc();
			assertTrue(desc.contains("_5-15"), name + ": " + desc);
		}
	}

	@Test
	void theRecipesMakeTheRenamedBombs() {
		assertEquals(PKG + "SmokeBomb", Bomb.EnhanceBomb.validIngredients.get(PotionOfInvisibility.class).getName());
		assertEquals(PKG + "FlashBangBomb", Bomb.EnhanceBomb.validIngredients.get(ScrollOfRecharging.class).getName());
	}

	@Test
	void oldSavesLoadTheRenamedBombs() throws IOException {
		assertEquals(PKG + "SmokeBomb", loadOld(PKG + "Flashbang").getClass().getName(),
				"a flashbang made from invisibility is a smoke bomb now");
		assertEquals(PKG + "FlashBangBomb", loadOld(PKG + "ShockBomb").getClass().getName(),
				"a shock bomb made from recharging is a flashbang now");
	}

	private static Bundlable loadOld(String className) throws IOException {
		Bundle bomb = new Bundle();
		bomb.put("__className", className);
		bomb.put("quantity", 2);
		Bundle save = new Bundle();
		save.put("bomb", bomb);
		Bundlable restored = SaveRoundtrip.writeRead(save).get("bomb");
		assertNotNull(restored, className + " resolves");
		assertEquals(2, ((Item) restored).quantity(), "and keeps its quantity");
		return restored;
	}

	@Test
	void theCatalogStillCountsAnOldSavesSeenBombs() throws Exception {
		StaticSnapshot statics = StaticSnapshot.capture(Arrays.<Class<?>>asList(Catalog.class, Badges.class, Journal.class));
		try {
			Bundle save = new Bundle();
			save.put("catalog_items", new String[]{PKG + "Flashbang", PKG + "ShockBomb"});
			Catalog.restore(SaveRoundtrip.writeRead(save));

			assertTrue(Catalog.isSeen(itemClass("SmokeBomb")), "an old seen flashbang counts as the smoke bomb");
			assertTrue(Catalog.isSeen(itemClass("FlashBangBomb")), "an old seen shock bomb counts as the flashbang");
		} finally {
			statics.restore();
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Item> itemClass(String simpleName) throws ClassNotFoundException {
		return (Class<? extends Item>) Class.forName(PKG + simpleName);
	}

	@Test
	void everyBombThePacksNameExists() throws IOException {
		// tests run from core/src/main/assets
		Path marketplace = Paths.get("../../../../marketplace");
		assertTrue(Files.isDirectory(marketplace), "missing " + marketplace.toAbsolutePath());
		Pattern type = Pattern.compile("\"type\"\\s*:\\s*\"bombs\\.(\\w+)\"");
		List<String> named = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		List<Path> files;
		try (Stream<Path> walk = Files.walk(marketplace)) {
			files = walk.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());
		}
		for (Path file : files) {
			Matcher m = type.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
			while (m.find()) {
				named.add(m.group(1));
				try {
					Class.forName(PKG + m.group(1));
				} catch (ClassNotFoundException e) {
					missing.add(marketplace.relativize(file) + ": bombs." + m.group(1));
				}
			}
		}
		assertTrue(named.size() >= 4, "fixture: the packs name bombs; found " + named);
		assertTrue(missing.isEmpty(), "pack items that no longer exist: " + missing);
	}
}
