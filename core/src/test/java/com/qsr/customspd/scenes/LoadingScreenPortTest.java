package com.qsr.customspd.scenes;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.GamesInProgress;
import com.qsr.customspd.Statistics;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.levels.features.LevelTransition;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.modding.ModManager;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.StyledButton;
import com.badlogic.gdx.Input;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Random;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loading screen, built for real with no display. GL calls go to a stub that does nothing,
 * and the loading thread is never started: a thread that has not run counts as finished loading.
 */
class LoadingScreenPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private GL20 priorGl;
	private GL20 priorGl20;
	private InterlevelScene scene;

	private static final String PACK = "Loading Screen Test Pack";
	private Path packRoot;
	private Files.FileType priorFileType;
	private String priorFilePath;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		run.enterFirstLevel();
		run.saveAll();
		priorInstance = Game.instance;
		priorGl = Gdx.gl;
		priorGl20 = Gdx.gl20;
		new Game(null, null);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		Game.width = Game.dispWidth = 1500;
		Game.height = Game.dispHeight = 1000;
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(),
				new Class<?>[]{GL20.class}, (proxy, method, args) -> {
					Class<?> r = method.getReturnType();
					if (r == int.class) return 0;
					if (r == boolean.class) return false;
					if (r == float.class) return 0f;
					if (r == long.class) return 0L;
					if (r == String.class) return "";
					return null;
				});
		InterlevelScene.lastRegion = -1;
		InterlevelScene.curTransition = null;
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			if (scene != null) scene.destroy();
		} finally {
			Gdx.gl = priorGl;
			Gdx.gl20 = priorGl20;
			Game.instance = priorInstance;
			try {
				if (packRoot != null) dropPack();
			} finally {
				run.close();
			}
		}
	}

	/**
	 * Enables a pack whose dungeon.json is the given text. As in SpecialStairsPackArtTest, mod
	 * storage is pointed at a throwaway folder by a path relative to home, and put back afterwards.
	 */
	private void enablePack(String dungeonJson) throws Exception {
		priorFileType = (Files.FileType) field(FileUtils.class, "defaultFileType").get(null);
		priorFilePath = FileUtils.defaultPath;
		packRoot = java.nio.file.Files.createTempDirectory("cpdu-loading-pack");
		Path home = Paths.get(System.getProperty("user.home")).toAbsolutePath();
		FileUtils.setDefaultFileProperties(Files.FileType.External, home.relativize(packRoot.toAbsolutePath()) + File.separator);
		Path layout = packRoot.resolve(ModManager.STORAGE).resolve(PACK).resolve("dungeon").resolve("dungeon.json");
		java.nio.file.Files.createDirectories(layout.getParent());
		java.nio.file.Files.write(layout, dungeonJson.getBytes(StandardCharsets.UTF_8));
		ModManager.INSTANCE.enableMod(PACK);
	}

	private void dropPack() throws Exception {
		try {
			// also drops ModManager's cached mod list, so the next lookup reads the real one again
			ModManager.INSTANCE.disableMod(PACK);
		} finally {
			FileUtils.setDefaultFileProperties(priorFileType, priorFilePath);
			try (Stream<Path> files = java.nio.file.Files.walk(packRoot)) {
				files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
			packRoot = null;
		}
	}

	/** The save in the run's slot is on the given level, as the title screen reads it. */
	private static void savedOn(String levelName, int depth) {
		GamesInProgress.Info info = GamesInProgress.check(GamesInProgress.curSlot);
		info.levelName = levelName;
		info.depth = depth;
	}

	private static Field field(Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	/** Builds the loading screen for a mode without starting its loading thread. */
	private InterlevelScene load(InterlevelScene.Mode mode) throws Exception {
		if (scene != null) scene.destroy();
		InterlevelScene.mode = mode;
		field(InterlevelScene.class, "thread").set(null, new Thread());
		scene = new InterlevelScene();
		scene.create();
		return scene;
	}

	/** The first image the scene adds, which is its backdrop. */
	private static Image backdrop(Group scene) throws Exception {
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) field(Group.class, "members").get(scene);
		for (Gizmo g : members) {
			if (g instanceof Image) return (Image) g;
		}
		throw new AssertionError("the loading screen has no backdrop");
	}

	private static void assertSplash(GeneralAsset splash, Group scene) throws Exception {
		assertSame(TextureCache.get(Asset.getAssetFilePath(splash)), backdrop(scene).texture,
				"the backdrop is " + splash);
	}

	private static LevelTransition toFloor(String levelName) {
		LevelTransition t = new LevelTransition();
		t.departLevel = Dungeon.levelName;
		t.destLevel = levelName;
		t.destType = LevelTransition.Type.REGULAR_ENTRANCE;
		return t;
	}

	/** The region intro the loading screen shows, or null if it shows none. */
	private static String story(InterlevelScene scene) throws Exception {
		RenderedTextBlock text = (RenderedTextBlock) field(InterlevelScene.class, "storyMessage").get(scene);
		return text == null ? null : text.text();
	}

	/** Walks down the regular stairs from one floor to the next. */
	private InterlevelScene walkDown(String from, String to, int deepest) throws Exception {
		onFloor(from);
		Statistics.deepestFloor = deepest;
		InterlevelScene.curTransition = toFloor(to);
		return load(InterlevelScene.Mode.DESCEND);
	}

	/** Lets the fade in run out; the loading thread was never started, so loading counts as done. */
	private static void finishLoading(InterlevelScene scene) {
		Game.elapsed = 5f;
		scene.update();
		Game.elapsed = 0f;
	}

	private static Button continueButton(InterlevelScene scene) throws Exception {
		Button button = (Button) field(InterlevelScene.class, "btnContinue").get(scene);
		assertNotNull(button, "test setup: the story is up");
		return button;
	}

	private static void onFloor(String levelName) {
		Dungeon.levelName = levelName;
		Dungeon.depth = Dungeon.layout.getDungeon().get(levelName).getDepth();
	}

	// upstream afe3f71cb, with the region taken from the level being loaded
	@Test
	void aNewGameShowsTheSewers() throws Exception {
		Dungeon.hero = null;
		load(InterlevelScene.Mode.DESCEND);

		assertEquals(1, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.SEWERS_SPLASH, scene);
	}

	@Test
	void continuingOnFloorOneShowsTheSewers() throws Exception {
		load(InterlevelScene.Mode.CONTINUE);

		assertEquals(1, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.SEWERS_SPLASH, scene);
	}

	@Test
	void fallingFromTheSewerBossShowsThePrison() throws Exception {
		onFloor("5");
		load(InterlevelScene.Mode.FALL);

		assertEquals(2, InterlevelScene.lastRegion, "the chasm under floor 5 leads to floor 6");
		assertSplash(GeneralAsset.PRISON_SPLASH, scene);
	}

	@Test
	void returningShowsTheRegionOfTheReturnFloor() throws Exception {
		onFloor("3");
		InterlevelScene.returnLevel = "12";
		load(InterlevelScene.Mode.RETURN);

		assertEquals(3, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.CAVES_SPLASH, scene);
	}

	@Test
	void resurrectingShowsTheCurrentRegion() throws Exception {
		onFloor("17");
		load(InterlevelScene.Mode.RESURRECT);

		assertEquals(4, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.CITY_SPLASH, scene);
	}

	@Test
	void descendingStillTakesTheRegionFromTheTransition() throws Exception {
		onFloor("20");
		InterlevelScene.curTransition = toFloor("21");
		load(InterlevelScene.Mode.DESCEND);

		assertEquals(5, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.HALLS_SPLASH, scene);
	}

	// upstream 4ed89d588: the splash picks its focus from a generator of its own
	@Test
	void theSplashDrawsNothingFromTheGeneratorUnderIt() throws Exception {
		Random.pushGenerator(4242L);
		long control = Random.Long();
		Random.popGenerator();

		Dungeon.hero = null;
		Random.pushGenerator(4242L);
		try {
			load(InterlevelScene.Mode.DESCEND);
			assertEquals(control, Random.Long(), "the loading screen moved the generator under it");
		} finally {
			Random.popGenerator();
		}
	}

	// upstream 92905ae81: the region intro moves onto the loading screen
	@Test
	void aNewGameShowsTheSewersStory() throws Exception {
		Dungeon.hero = null;
		load(InterlevelScene.Mode.DESCEND);

		assertEquals(Document.INTROS.pageBody(1), story(scene));
	}

	@Test
	void walkingDownToANewRegionShowsItsStory() throws Exception {
		walkDown("5", "6", 5);

		assertEquals(Document.INTROS.pageBody(2), story(scene));
	}

	@Test
	void theStoryShowsEveryRunEvenWhenAlreadyRead() throws Exception {
		Document.INTROS.readPage(3);
		walkDown("10", "11", 10);

		assertEquals(Document.INTROS.pageBody(3), story(scene));
	}

	@Test
	void fallingIntoANewRegionShowsNoStory() throws Exception {
		onFloor("5");
		Statistics.deepestFloor = 5;
		load(InterlevelScene.Mode.FALL);

		assertNull(story(scene));
	}

	@Test
	void revisitingARegionsFirstFloorShowsNoStory() throws Exception {
		walkDown("5", "6", 8);

		assertNull(story(scene));
	}

	@Test
	void aNewFloorInsideARegionShowsNoStory() throws Exception {
		walkDown("6", "7", 6);

		assertNull(story(scene));
	}

	@Test
	void continuingShowsNoStory() throws Exception {
		load(InterlevelScene.Mode.CONTINUE);

		assertNull(story(scene));
	}

	// on the shipped layout a new region starts every five floors, so the story shows where v2.5.4 shows it
	@Test
	void walkingDownTheShippedLayoutShowsTheStoryOnUpstreamsFloors() throws Exception {
		List<Integer> shown = new ArrayList<>();
		Hero hero = Dungeon.hero;
		Dungeon.hero = null;
		if (story(load(InterlevelScene.Mode.DESCEND)) != null) shown.add(1);
		Dungeon.hero = hero;

		for (int floor = 2; floor <= 26; floor++) {
			if (story(walkDown(Integer.toString(floor - 1), Integer.toString(floor), floor - 1)) != null) {
				shown.add(floor);
			}
		}

		assertEquals(Arrays.asList(1, 6, 11, 16, 21), shown);
	}

	@Test
	void fallingThroughTheShippedLayoutShowsNoStory() throws Exception {
		for (int floor = 1; floor <= 25; floor++) {
			onFloor(Integer.toString(floor));
			Statistics.deepestFloor = floor;
			assertNull(story(load(InterlevelScene.Mode.FALL)), "falling from floor " + floor);
		}
	}

	// Town and FiveExit start above floor 1 on a floor that is already in the first region
	@Test
	void aPackStartInTheFirstRegionShowsTheSewersStoryOnce() throws Exception {
		useTownLayout();
		Hero hero = Dungeon.hero;
		Dungeon.hero = null;
		load(InterlevelScene.Mode.DESCEND);
		assertEquals(Document.INTROS.pageBody(1), story(scene), "a new game shows the sewers story");
		Dungeon.hero = hero;

		walkDown("Town", "1", 0);

		assertEquals(1, InterlevelScene.lastRegion, "test setup: floor 1 is in the sewers");
		assertNull(story(scene), "floor 1 is not a new region");
	}

	@Test
	void aRegionStartingOffTheFiveFloorGridShowsItsStory() throws Exception {
		useTownLayout();
		walkDown("2", "3", 2);
		assertNull(story(scene), "test setup: floor 3 is still in the sewers");

		walkDown("3", "4", 3);

		assertEquals(2, InterlevelScene.lastRegion, "test setup: floor 4 is in the prison");
		assertEquals(Document.INTROS.pageBody(2), story(scene));
	}

	// upstream 400eebcac: the final floor is past the five regions and has no intro
	@Test
	void walkingDownToTheLastFloorShowsNoStory() throws Exception {
		walkDown("25", "26", 25);

		assertEquals(6, InterlevelScene.lastRegion, "floor 26 is region 6, as upstream counts it");
		assertNull(story(scene));
	}

	/** Pack floors the region table does not know: p0/p30/p31 by name, c5/c6 with a custom region 0. */
	private static void usePackLayout() {
		String custom = "\"custom_layout\":{\"width\":1,\"height\":1,\"region\":0,"
				+ "\"music\":{\"tracks\":[],\"probs\":[]},\"map\":[4]}";
		String json = "{\"start\":\"p30\",\"dungeon\":{"
				+ "\"p0\":{\"depth\":0,\"type\":\"regular\",\"layout\":\"PackLevel\","
				+ "\"entrances\":[],\"exits\":[\"p30\"],\"chasm\":\"p30\",\"passage\":\"p0\"},"
				+ "\"p30\":{\"depth\":30,\"type\":\"regular\",\"layout\":\"PackLevel\","
				+ "\"entrances\":[],\"exits\":[\"p31\"],\"chasm\":\"p31\",\"passage\":\"p30\"},"
				+ "\"p31\":{\"depth\":31,\"type\":\"regular\",\"layout\":\"PackLevel\","
				+ "\"entrances\":[\"p30\"],\"exits\":[],\"chasm\":\"p31\",\"passage\":\"p30\"},"
				+ "\"c5\":{\"depth\":5,\"type\":\"custom\",\"entrances\":[],\"exits\":[\"c6\"],"
				+ "\"chasm\":\"c6\",\"passage\":\"c5\"," + custom + "},"
				+ "\"c6\":{\"depth\":6,\"type\":\"custom\",\"entrances\":[\"c5\"],\"exits\":[],"
				+ "\"chasm\":\"c6\",\"passage\":\"c5\"," + custom + "}"
				+ "}}";
		Dungeon.layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), json);
	}

	private static String customLayout(int region) {
		return "\"custom_layout\":{\"width\":1,\"height\":1,\"region\":" + region + ","
				+ "\"music\":{\"tracks\":[],\"probs\":[]},\"map\":[4]}";
	}

	private static String floor(String name, int depth, String layout, String from, String to) {
		return "\"" + name + "\":{\"depth\":" + depth + ",\"type\":\"regular\",\"layout\":\"" + layout + "\","
				+ "\"entrances\":[\"" + from + "\"],\"exits\":[\"" + to + "\"],\"chasm\":\"" + to + "\",\"passage\":\"" + from + "\"}";
	}

	/**
	 * A start floor at depth 0 that names the first region and leads down to the sewers, as the
	 * Town pack has. Here the prison starts at floor 4, off the five floor grid.
	 */
	static final String TOWN_LAYOUT = "{\"merge\":true,\"start\":\"Town\",\"dungeon\":{"
			+ "\"Town\":{\"depth\":0,\"type\":\"custom\",\"entrances\":[],\"exits\":[\"1\"],"
			+ "\"chasm\":\"1\",\"passage\":\"Town\"," + customLayout(1) + "},"
			+ floor("1", 1, "SewerLevel", "Town", "2") + ","
			+ floor("2", 2, "SewerLevel", "1", "3") + ","
			+ floor("3", 3, "SewerLevel", "2", "4") + ","
			+ floor("4", 4, "PrisonLevel", "3", "5")
			+ "}}";

	private static void useTownLayout() {
		Dungeon.layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), TOWN_LAYOUT);
	}

	/** A pack start floor at depth 0 that names the third region. */
	static final String GROTTO_LAYOUT = "{\"merge\":true,\"start\":\"Grotto\",\"dungeon\":{"
			+ "\"Grotto\":{\"depth\":0,\"type\":\"custom\",\"entrances\":[],\"exits\":[\"1\"],"
			+ "\"chasm\":\"1\",\"passage\":\"Grotto\"," + customLayout(3) + "}"
			+ "}}";

	// a cold start has not read the run's layout when the loading screen is built
	@Test
	void continuingAPackRunFromAColdStartShowsItsRegion() throws Exception {
		enablePack(TOWN_LAYOUT);
		savedOn("Town", 0);
		// the game has just launched, and the continue button clears the hero
		Dungeon.layout = null;
		Dungeon.level = null;
		Dungeon.hero = null;

		Random.pushGenerator(4242L);
		long control = Random.Long();
		Random.popGenerator();

		Random.pushGenerator(4242L);
		try {
			load(InterlevelScene.Mode.CONTINUE);
			assertEquals(control, Random.Long(), "reading the layout moved the generator under it");
		} finally {
			Random.popGenerator();
		}

		assertEquals(1, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.SEWERS_SPLASH, scene);
		assertNull(Dungeon.layout, "the layout is left for loading the game to read");
	}

	@Test
	void continuingFromAColdStartOntoAFloorInAnotherRegionShowsThatRegion() throws Exception {
		enablePack(GROTTO_LAYOUT);
		savedOn("Grotto", 0);
		Dungeon.layout = null;
		Dungeon.level = null;
		Dungeon.hero = null;

		load(InterlevelScene.Mode.CONTINUE);

		assertEquals(3, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.CAVES_SPLASH, scene);
	}

	// a layout left from an earlier run need not be the one the continued run loads
	@Test
	void continuingLooksTheLevelUpInTheLayoutTheRunLoads() throws Exception {
		enablePack(GROTTO_LAYOUT);
		savedOn("Grotto", 0);
		Dungeon.hero = null;

		load(InterlevelScene.Mode.CONTINUE);

		assertEquals(3, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.CAVES_SPLASH, scene);
	}

	@Test
	void aDepthZeroFloorTheTableDoesNotKnowShowsTheSewers() throws Exception {
		usePackLayout();
		onFloor("p0");
		load(InterlevelScene.Mode.RESURRECT);

		assertEquals(1, InterlevelScene.lastRegion);
		assertSplash(GeneralAsset.SEWERS_SPLASH, scene);
	}

	// pack floors can land outside the five regions that have an intro
	@Test
	void walkingDownToADeepFloorTheTableDoesNotKnowShowsNoStory() throws Exception {
		usePackLayout();
		walkDown("p30", "p31", 30);

		assertEquals(7, InterlevelScene.lastRegion, "test setup: floor 31 falls back to the depth rule");
		assertNull(story(scene));
		assertSplash(GeneralAsset.HALLS_SPLASH, scene);
	}

	@Test
	void walkingDownToAPackRegionBelowTheFirstShowsNoStory() throws Exception {
		usePackLayout();
		walkDown("c5", "c6", 5);

		assertEquals(0, InterlevelScene.lastRegion, "test setup: the custom layout names region 0");
		assertNull(story(scene));
		assertSplash(GeneralAsset.HALLS_SPLASH, scene);
	}

	// upstream 0368b61d8: going on past the story marks the intro read, and nothing else does
	@Test
	void continuingPastTheStoryMarksTheIntroRead() throws Exception {
		assertFalse(Document.INTROS.isPageRead(2), "test setup: the prison intro is unread");
		walkDown("5", "6", 5);
		assertFalse(Document.INTROS.isPageRead(2), "showing the story does not mark it read");

		finishLoading(scene);
		assertTrue(continueButton(scene).active, "test setup: loading is done");
		assertFalse(Document.INTROS.isPageRead(2), "showing the story does not mark it read");

		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(continueButton(scene));

		assertTrue(Document.INTROS.isPageRead(2));
	}

	@Test
	void aKeyPastTheStoryAlsoMarksTheIntroRead() throws Exception {
		Dungeon.hero = null;
		load(InterlevelScene.Mode.DESCEND);
		finishLoading(scene);
		assertTrue(continueButton(scene).active, "test setup: loading is done");
		assertFalse(Document.INTROS.isPageRead(1), "showing the story does not mark it read");

		KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.SPACE, true));
		KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.SPACE, false));
		KeyEvent.processKeyEvents();

		assertTrue(Document.INTROS.isPageRead(1));
	}

	// upstream 6fe93bda2
	@Test
	void theContinueButtonTextComesFromTheMessages() throws Exception {
		Messages.setup(Languages.ENGLISH);
		Dungeon.hero = null;
		load(InterlevelScene.Mode.DESCEND);

		assertEquals("Continue", Messages.get(InterlevelScene.class, "continue"), "the English text exists");
		assertEquals(Messages.get(InterlevelScene.class, "continue"), ((StyledButton) continueButton(scene)).text());
	}

	// upstream 4eccfda92: the falling text shakes while levelgen runs on the loading thread
	@Test
	void theFallingTextShakeDrawsNothingFromTheLevelgenGenerator() throws Exception {
		onFloor("5");
		load(InterlevelScene.Mode.FALL);

		Random.pushGenerator(4242L);
		long control = Random.Long();
		Random.popGenerator();

		// the seeded generator levelgen pushes for the floor being built
		Random.pushGenerator(4242L);
		try {
			for (int i = 0; i < 5; i++) scene.update();
			assertEquals(control, Random.Long(), "the shake moved the levelgen generator");
		} finally {
			Random.popGenerator();
		}
	}
}
