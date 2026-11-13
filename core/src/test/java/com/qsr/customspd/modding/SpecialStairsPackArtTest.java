package com.qsr.customspd.modding;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.qsr.customspd.assets.tiles.PrisonTile;
import com.qsr.customspd.assets.tiles.SewersTile;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Region tile sheets draw each tile from the enabled packs first, then the base art. Marketplace
 * packs such as Hollow Dungeon restyle a region's stairs (entrance.png) but ship no special stairs
 * (entrance_sp.png); the special stairs then use the pack's stairs rather than the base art.
 *
 * The packs live in a throwaway mod folder. ModManager keeps its mods in external storage, which
 * headless resolves against the user's home folder, so for the test the default file location is
 * pointed at a temp folder by a path relative to home, and put back afterwards.
 */
class SpecialStairsPackArtTest {

	private static final String PACK = "Stairs Test Pack";
	private static final String OTHER_PACK = "Special Stairs Test Pack";

	private Path root;
	private Files.FileType priorType;
	private String priorPath;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void pointModStorageAtATempFolder() throws Exception {
		priorType = defaultFileType();
		priorPath = FileUtils.defaultPath;
		root = java.nio.file.Files.createTempDirectory("cpdu-stairs-packs");
		Path home = Paths.get(System.getProperty("user.home")).toAbsolutePath();
		FileUtils.setDefaultFileProperties(Files.FileType.External, home.relativize(root.toAbsolutePath()) + File.separator);
		assertTrue(modStorage().file().getCanonicalFile().equals(root.resolve(ModManager.STORAGE).toFile().getCanonicalFile()),
				"test setup: mod storage is the temp folder, was " + modStorage().file());
		// saving the (empty) mod list there also drops ModManager's cached list from the real location
		ModManager.INSTANCE.disableMod(PACK);
	}

	@AfterEach
	void putModStorageBack() throws Exception {
		try {
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
	void withNoPackTheSpecialStairsUseTheBaseArt() {
		assertEquals(PrisonTile.ENTRANCE_SP.getPath(), ModManager.INSTANCE.getModdedTileFilePath(PrisonTile.ENTRANCE_SP));
	}

	@Test
	void aPackWithOnlyStairsGivesTheSpecialStairsThoseStairs() throws Exception {
		File stairs = packFile(PACK, PrisonTile.ENTRANCE.getPath());
		ModManager.INSTANCE.enableMod(PACK);

		String path = ModManager.INSTANCE.getModdedTileFilePath(PrisonTile.ENTRANCE_SP);
		assertEquals(ModManager.INSTANCE.getModdedAssetFilePath(PrisonTile.ENTRANCE.getPath()), path);
		assertOpens(stairs, path);

		// the plain stairs, other tiles and other regions are unchanged
		assertOpens(stairs, ModManager.INSTANCE.getModdedTileFilePath(PrisonTile.ENTRANCE));
		assertEquals(PrisonTile.FLOOR.getPath(), ModManager.INSTANCE.getModdedTileFilePath(PrisonTile.FLOOR));
		assertEquals(SewersTile.ENTRANCE_SP.getPath(), ModManager.INSTANCE.getModdedTileFilePath(SewersTile.ENTRANCE_SP));
	}

	@Test
	void aPackWithItsOwnSpecialStairsKeepsThem() throws Exception {
		packFile(PACK, PrisonTile.ENTRANCE.getPath());
		File special = packFile(PACK, PrisonTile.ENTRANCE_SP.getPath());
		ModManager.INSTANCE.enableMod(PACK);

		assertOpens(special, ModManager.INSTANCE.getModdedTileFilePath(PrisonTile.ENTRANCE_SP));
	}

	@Test
	void specialStairsFromAnyEnabledPackWinOverAnotherPacksStairs() throws Exception {
		packFile(PACK, PrisonTile.ENTRANCE.getPath());
		File special = packFile(OTHER_PACK, PrisonTile.ENTRANCE_SP.getPath());
		ModManager.INSTANCE.enableMod(OTHER_PACK);
		ModManager.INSTANCE.enableMod(PACK); // first in the list, so its files are looked up first

		assertOpens(special, ModManager.INSTANCE.getModdedTileFilePath(PrisonTile.ENTRANCE_SP));
	}

	/** Where the tile map compilers open a looked-up path: external storage, as in DesktopTileMapCompiler. */
	private static void assertOpens(File expected, String path) throws IOException {
		FileHandle handle = FileUtils.getFileHandle(Files.FileType.External, FileUtils.defaultPath, path);
		assertTrue(handle.exists(), path + " opens a file");
		assertEquals(expected.getCanonicalFile(), handle.file().getCanonicalFile(), path);
	}

	private File packFile(String pack, String assetPath) throws IOException {
		File file = root.resolve(ModManager.STORAGE).resolve(pack).resolve(assetPath).toFile();
		assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs(), "made " + file.getParent());
		// any bytes do: the lookup only asks whether the pack has the file
		java.nio.file.Files.write(file.toPath(), new byte[]{1, 2, 3});
		return file;
	}

	private static FileHandle modStorage() {
		return FileUtils.getFileHandle(Files.FileType.External, FileUtils.defaultPath, ModManager.STORAGE);
	}

	private static Files.FileType defaultFileType() throws Exception {
		Field f = FileUtils.class.getDeclaredField("defaultFileType");
		f.setAccessible(true);
		return (Files.FileType) f.get(null);
	}
}
