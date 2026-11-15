package com.qsr.customspd.modding;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.mock.audio.MockAudio;
import com.badlogic.gdx.files.FileHandle;
import com.qsr.customspd.Assets;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.levels.CustomLevel;
import com.qsr.customspd.levels.SewerLevel;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Random;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A pack that ships a file under music/ replaces the base track, the same way it replaces
 * sprites: the first enabled pack with the file wins, and a track no pack ships plays the base
 * file exactly as before. Music makes its players through Gdx.audio, which the test swaps for
 * one that records the file each player is made from, so no audio is decoded and any bytes do
 * for a pack's file.
 *
 * The packs live in a throwaway mod folder, set up as in SpecialStairsPackArtTest.
 */
class PackMusicTest {

	private static final String PACK = "Music Test Pack";
	private static final String OTHER_PACK = "Other Music Test Pack";

	private static final String LAYOUT = "{\"width\": 3, \"height\": 3, \"region\": 1,"
			+ " \"music\": {\"tracks\": [\"%s\"], \"probs\": [1.0]},"
			+ " \"map\": [4, 4, 4, 4, 1, 4, 4, 4, 4]}";

	private final List<FileHandle> opened = new ArrayList<>();

	private Path root;
	private Files.FileType priorType;
	private String priorPath;
	private com.badlogic.gdx.Audio priorAudio;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		priorType = (Files.FileType) field(FileUtils.class, "defaultFileType").get(null);
		priorPath = FileUtils.defaultPath;
		Path home = Paths.get(System.getProperty("user.home")).toAbsolutePath();
		Path temp = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath();
		// the mod folder is set relative to user.home, which only works on the same drive
		root = java.nio.file.Files.createTempDirectory(
				home.getRoot().equals(temp.getRoot()) ? temp : home, "cpdu-music-packs");
		FileUtils.setDefaultFileProperties(Files.FileType.External, home.relativize(root.toAbsolutePath()) + File.separator);
		// saving the (empty) mod list there also drops ModManager's cached list from the real location
		ModManager.INSTANCE.disableMod(PACK);

		priorAudio = Gdx.audio;
		Gdx.audio = new MockAudio() {
			@Override
			public com.badlogic.gdx.audio.Music newMusic(FileHandle file) {
				opened.add(file);
				return super.newMusic(file);
			}
		};

		Music.INSTANCE.end();
		Music.INSTANCE.setTrackResolver(null);
		// the game's own startup step, so a test fails if it stops pointing music at the packs
		ShatteredPixelDungeon.setUpMusic();
		Music.INSTANCE.enable(true);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			Music.INSTANCE.end();
			Music.INSTANCE.setTrackResolver(null);
			Gdx.audio = priorAudio;
			ModManager.INSTANCE.disableMod(PACK);
			ModManager.INSTANCE.disableMod(OTHER_PACK);
		} finally {
			// disableMod drops the cached mod list, so the next lookup reads the real one again
			FileUtils.setDefaultFileProperties(priorType, priorPath);
			try (Stream<Path> files = java.nio.file.Files.walk(root)) {
				files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
		}
	}

	@Test
	void withNoPackEveryTrackPlaysTheBaseFile() throws Exception {
		List<String> tracks = allTracks();
		assertTrue(tracks.size() >= 20, "found the game's music, was " + tracks);

		for (String track : tracks) {
			Music.INSTANCE.play(track, true);
			assertBase(track);
		}

		new SewerLevel().playLevelMusic();
		assertBase(Assets.Music.SEWERS_1);
		assertEquals(tracks.size() + 1, opened.size(), "one player per track played");
	}

	@Test
	void aPacksFileReplacesThatTrackAndNoOther() throws Exception {
		File sewers = packFile(PACK, Assets.Music.SEWERS_1);
		ModManager.INSTANCE.enableMod(PACK);

		// the region list always starts on its first entry
		new SewerLevel().playLevelMusic();
		assertOpens(sewers);

		Music.INSTANCE.play(Assets.Music.SEWERS_2, true);
		assertBase(Assets.Music.SEWERS_2);
		Music.INSTANCE.play(Assets.Music.SEWERS_BOSS, true);
		assertBase(Assets.Music.SEWERS_BOSS);
		Music.INSTANCE.play(Assets.Music.THEME_1, true);
		assertBase(Assets.Music.THEME_1);

		Music.INSTANCE.play(Assets.Music.SEWERS_1, true);
		assertOpens(sewers);
	}

	@Test
	void unreadableModStorageStillPlaysTheBaseFile() throws Exception {
		packFile(PACK, Assets.Music.SEWERS_1);
		ModManager.INSTANCE.enableMod(PACK);
		// the enabled list is read again on the next lookup, and this one can't be parsed
		File enabled = root.resolve("enabled_mods.dat").toFile();
		java.nio.file.Files.write(enabled.toPath(), "not a mod list".getBytes("UTF-8"));
		try {
			Music.INSTANCE.play(Assets.Music.SEWERS_1, true);
			assertBase(Assets.Music.SEWERS_1);
		} finally {
			assertTrue(enabled.delete(), "cleared the broken list");
		}
	}

	@Test
	void theFirstEnabledPackWinsAsItDoesForSprites() throws Exception {
		File mine = packFile(PACK, Assets.Music.SEWERS_1);
		File other = packFile(OTHER_PACK, Assets.Music.SEWERS_1);

		ModManager.INSTANCE.enableMod(OTHER_PACK);
		ModManager.INSTANCE.enableMod(PACK); // first in the list, so its files are looked up first
		Music.INSTANCE.play(Assets.Music.SEWERS_1, true);
		assertOpens(mine);
		assertSameFileAsSprites(Assets.Music.SEWERS_1);

		ModManager.INSTANCE.disableMod(OTHER_PACK);
		ModManager.INSTANCE.enableMod(OTHER_PACK); // now the other pack comes first
		Music.INSTANCE.play(Assets.Music.SEWERS_2, true);
		Music.INSTANCE.play(Assets.Music.SEWERS_1, true);
		assertOpens(other);
		assertSameFileAsSprites(Assets.Music.SEWERS_1);
	}

	@Test
	void aLayoutsMusicBlockPlaysThePacksFile() throws Exception {
		File theme = packFile(PACK, Assets.Music.THEME_2);
		File ownTrack = packFile(PACK, "music/test_pack_own.ogg");
		ModManager.INSTANCE.enableMod(PACK);

		layoutLevel(Assets.Music.THEME_2).playLevelMusic();
		assertOpens(theme);

		// a track only the pack ships plays too
		layoutLevel("music/test_pack_own.ogg").playLevelMusic();
		assertOpens(ownTrack);

		layoutLevel(Assets.Music.CAVES_BOSS).playLevelMusic();
		assertBase(Assets.Music.CAVES_BOSS);
	}

	@Test
	void picksFromAListDrawTheSameRandomsWithAPack() throws Exception {
		File sewers = packFile(PACK, Assets.Music.SEWERS_1);
		packFile(PACK, Assets.Music.SEWERS_3);
		ModManager.INSTANCE.enableMod(PACK);

		long seed = 20231114L;
		List<String> queued;
		long next;
		Random.pushGenerator(seed);
		try {
			new SewerLevel().playLevelMusic();
			queued = new ArrayList<>(queue());
			next = Random.Long();
		} finally {
			Random.popGenerator();
		}

		List<String> expected = new ArrayList<>();
		long expectedNext;
		Random.pushGenerator(seed);
		try {
			for (int i = 0; i < SewerLevel.SEWER_TRACK_LIST.length; i++) {
				if (Random.Float() < SewerLevel.SEWER_TRACK_CHANCES[i]) {
					expected.add(SewerLevel.SEWER_TRACK_LIST[i]);
				}
			}
			expectedNext = Random.Long();
		} finally {
			Random.popGenerator();
		}

		assertOpens(sewers);
		assertEquals(Assets.Music.SEWERS_1, expected.remove(0), "the first pick plays now");
		assertEquals(expected, queued, "the rest wait in the same order");
		assertEquals(expectedNext, next, "one draw per list entry, nothing more");
	}

	private void assertOpens(File expected) throws IOException {
		FileHandle last = last();
		assertTrue(last.exists(), last + " exists");
		assertEquals(expected.getCanonicalFile(), last.file().getCanonicalFile(), "the pack's file plays");
	}

	private void assertBase(String track) {
		assertEquals(Gdx.files.internal(track), last(), track + " plays the base file");
		assertEquals(Files.FileType.Internal, last().type(), track);
	}

	/** Sprites draw from the path ModManager looks up, opened by TextureCache in the same order as getAssetFileHandle. */
	private void assertSameFileAsSprites(String track) throws IOException {
		FileHandle sprites = ModManager.INSTANCE.getAssetFileHandle(ModManager.INSTANCE.getModdedAssetFilePath(track));
		assertEquals(sprites.file().getCanonicalFile(), last().file().getCanonicalFile(), track);
	}

	private FileHandle last() {
		assertFalse(opened.isEmpty(), "music opened a file");
		return opened.get(opened.size() - 1);
	}

	private static CustomLevel layoutLevel(String track) {
		CustomLevelLayout layout = Json.Default.decodeFromString(CustomLevelLayout.Companion.serializer(),
				String.format(LAYOUT, track));
		return new CustomLevel(layout, "c1");
	}

	private static List<String> allTracks() throws IllegalAccessException {
		List<String> tracks = new ArrayList<>();
		for (Field f : Assets.Music.class.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
				tracks.add((String) f.get(null));
			}
		}
		return tracks;
	}

	@SuppressWarnings("unchecked")
	private static List<String> queue() throws Exception {
		return (List<String>) field(Music.class, "trackQueue").get(Music.INSTANCE);
	}

	private File packFile(String pack, String assetPath) throws IOException {
		File file = root.resolve(ModManager.STORAGE).resolve(pack).resolve(assetPath).toFile();
		assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs(), "made " + file.getParent());
		java.nio.file.Files.write(file.toPath(), new byte[]{1, 2, 3});
		return file;
	}

	private static Field field(Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}
}
