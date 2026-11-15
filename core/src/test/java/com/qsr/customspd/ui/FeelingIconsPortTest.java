package com.qsr.customspd.ui;

import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v2.5.0 (6b9940e88) added a row of large stairs icons, one per level feeling, plus the distant
 * well icon, and redrew the plain stairs icon at 15x16. Upstream cuts them from icons.png; CPDU
 * ships one PNG per icon. Each file pins its size and a CRC32 of its pixels, taken from the
 * matching v2.5.4 icons.png rectangle.
 */
class FeelingIconsPortTest {

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	private static final File ASSETS = new File(".");

	private HeadlessRun run;
	private Map<Object, SmartTexture> textures;
	private Map<Object, SmartTexture> earlierTextures;

	@BeforeEach
	void start() throws Exception {
		run = HeadlessRun.start(20261115L);
		Field f = TextureCache.class.getDeclaredField("all");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<Object, SmartTexture> cache = (Map<Object, SmartTexture>) f.get(null);
		textures = cache;
		earlierTextures = new LinkedHashMap<>(textures);
		textures.clear();
	}

	@AfterEach
	void tearDown() {
		try {
			if (textures != null) {
				textures.clear();
				textures.putAll(earlierTextures);
			}
		} finally {
			run.close();
		}
	}

	/** The key the image's texture is cached under, so a wrong icon fails without loading the right one. */
	private Object textureKey(Image image) {
		for (Map.Entry<Object, SmartTexture> e : textures.entrySet()) {
			if (e.getValue() == image.texture) return e.getKey();
		}
		return null;
	}

	private void assertLarge(Level.Feeling feeling, GeneralAsset expected) {
		Image image = IconUtils.getLarge(feeling);
		assertNotNull(image, feeling.name());
		assertEquals(Asset.getAssetFilePath(expected), textureKey(image), feeling.name());
	}

	@Test
	void eachFeelingGetsItsLargeStairsIcon() {
		assertLarge(Level.Feeling.NONE, GeneralAsset.ICON_STAIRS);
		assertLarge(Level.Feeling.CHASM, GeneralAsset.ICON_STAIRS_CHASM);
		assertLarge(Level.Feeling.WATER, GeneralAsset.ICON_STAIRS_WATER);
		assertLarge(Level.Feeling.GRASS, GeneralAsset.ICON_STAIRS_GRASS);
		assertLarge(Level.Feeling.DARK, GeneralAsset.ICON_STAIRS_DARK);
		assertLarge(Level.Feeling.LARGE, GeneralAsset.ICON_STAIRS_LARGE);
		assertLarge(Level.Feeling.TRAPS, GeneralAsset.ICON_STAIRS_TRAPS);
		assertLarge(Level.Feeling.SECRETS, GeneralAsset.ICON_STAIRS_SECRETS);
	}

	/**
	 * CRC32 over the pixels in row order, each as 4 bytes A, R, G, B. The colour under a fully
	 * transparent pixel counts too, so a re-cut must keep the v2.5.4 bytes exactly.
	 */
	private static long pixelCrc(BufferedImage img) {
		CRC32 crc = new CRC32();
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int argb = img.getRGB(x, y);
				crc.update(argb >>> 24);
				crc.update(argb >>> 16);
				crc.update(argb >>> 8);
				crc.update(argb);
			}
		}
		return crc.getValue();
	}

	private static void assertIcon(GeneralAsset asset, String path, int w, int h, long crc) throws Exception {
		assertEquals(path, asset.getPath(), asset.name() + " path");
		File png = new File(ASSETS, path);
		assertTrue(png.isFile(), "missing file " + path);
		BufferedImage img = ImageIO.read(png);
		assertNotNull(img, path + " is not a readable image");
		assertEquals(w, img.getWidth(), asset.name() + " width");
		assertEquals(h, img.getHeight(), asset.name() + " height");
		assertEquals(Long.toHexString(crc), Long.toHexString(pixelCrc(img)),
				asset.name() + " pixels differ from the v2.5.4 icons.png rectangle");
	}

	@Test
	void iconsMatchTheV254Cuts() throws Exception {
		assertTrue(new File(ASSETS, GeneralAsset.STAR.getPath()).isFile(),
				"fixture sanity: working directory must be core/src/main/assets, was " + ASSETS.getAbsolutePath());

		assertIcon(GeneralAsset.ICON_STAIRS, "interfaces/icons/stairs.png", 15, 16, 0x2f613d3eL);
		assertIcon(GeneralAsset.ICON_STAIRS_CHASM, "interfaces/icons/stairs_chasm.png", 15, 16, 0x1d9bdd13L);
		assertIcon(GeneralAsset.ICON_STAIRS_WATER, "interfaces/icons/stairs_water.png", 15, 16, 0x2cf96e2dL);
		assertIcon(GeneralAsset.ICON_STAIRS_GRASS, "interfaces/icons/stairs_grass.png", 15, 16, 0xd04c9e3aL);
		assertIcon(GeneralAsset.ICON_STAIRS_DARK, "interfaces/icons/stairs_dark.png", 15, 16, 0x587a7039L);
		assertIcon(GeneralAsset.ICON_STAIRS_LARGE, "interfaces/icons/stairs_large.png", 15, 16, 0x67932be7L);
		assertIcon(GeneralAsset.ICON_STAIRS_TRAPS, "interfaces/icons/stairs_traps.png", 15, 16, 0xe900be75L);
		assertIcon(GeneralAsset.ICON_STAIRS_SECRETS, "interfaces/icons/stairs_secrets.png", 15, 16, 0xfb8f055cL);
		assertIcon(GeneralAsset.ICON_DISTANT_WELL, "interfaces/icons/distant_well.png", 16, 16, 0xee49dc2eL);
	}
}
