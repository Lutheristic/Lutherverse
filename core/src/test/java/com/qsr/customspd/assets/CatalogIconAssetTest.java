package com.qsr.customspd.assets;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream draws the catalog tab icon from icons.png (Icons.CATALOG, 80,48,13,16 at v2.5.4).
 * CPDU ships one PNG per icon, so the cut lives at interfaces/icons/catalog.png. A missing or
 * empty file only shows up when the journal opens, so pin the path, size, pixel count and a
 * checksum of the decoded pixels, so a wrong crop or recoloured art fails too.
 */
class CatalogIconAssetTest {

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	private static final File ASSETS = new File(".");

	@Test
	void catalogIconIsTheThirteenBySixteenUpstreamCut() throws Exception {
		assertTrue(new File(ASSETS, GeneralAsset.STAR.getPath()).isFile(),
				"fixture sanity: working directory must be core/src/main/assets, was " + ASSETS.getAbsolutePath());

		GeneralAsset icon = GeneralAsset.valueOf("ICON_CATALOG");
		assertEquals("interfaces/icons/catalog.png", icon.getPath());

		File png = new File(ASSETS, icon.getPath());
		assertTrue(png.isFile(), "missing file " + icon.getPath());

		BufferedImage img = ImageIO.read(png);
		assertNotNull(img, icon.getPath() + " is not a readable image");
		assertEquals(13, img.getWidth(), "width");
		assertEquals(16, img.getHeight(), "height");

		int opaque = 0;
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				if ((img.getRGB(x, y) >>> 24) != 0) opaque++;
			}
		}
		assertEquals(200, opaque, "pixels with alpha > 0");

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
		// CRC32 of the big-endian ARGB words of the v2.5.4 icons.png crop at 80,48,13,16.
		assertEquals(0x9f75f00dL, crc.getValue(), "decoded ARGB checksum");
	}
}
