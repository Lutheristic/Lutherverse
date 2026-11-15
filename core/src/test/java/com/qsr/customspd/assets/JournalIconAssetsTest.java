package com.qsr.customspd.assets;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The notes tab and its landmark records draw icons that upstream (v2.5.4) cuts from icons.png.
 * CPDU ships one PNG per icon, so each cut needs its own GeneralAsset entry and file. A missing or
 * empty file only shows when the journal opens, so pin the names, paths, sizes and that each has
 * visible pixels. The rename icons became the scroll icons upstream (40afe2a22), and the journal
 * button art was recut (75532abf0).
 */
class JournalIconAssetsTest {

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	private static final File ASSETS = new File(".");

	private static BufferedImage load(GeneralAsset asset) throws Exception {
		File png = new File(ASSETS, asset.getPath());
		assertTrue(png.isFile(), "missing file " + asset.getPath());
		BufferedImage img = ImageIO.read(png);
		assertNotNull(img, asset.getPath() + " is not a readable image");
		return img;
	}

	private static void assertIcon(String name, String path, int w, int h) throws Exception {
		GeneralAsset asset = GeneralAsset.valueOf(name);
		assertEquals(path, asset.getPath(), name + " path");
		BufferedImage img = load(asset);
		assertEquals(w, img.getWidth(), name + " width");
		assertEquals(h, img.getHeight(), name + " height");

		boolean opaque = false;
		for (int y = 0; y < img.getHeight() && !opaque; y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				if ((img.getRGB(x, y) >>> 24) != 0) {
					opaque = true;
					break;
				}
			}
		}
		assertTrue(opaque, name + " has no visible pixel");
	}

	@Test
	void journalIconsResolveWithUpstreamSizes() throws Exception {
		assertTrue(new File(ASSETS, GeneralAsset.STAR.getPath()).isFile(),
				"fixture sanity: working directory must be core/src/main/assets, was " + ASSETS.getAbsolutePath());

		assertIcon("ICON_JOURNAL", "interfaces/icons/journal.png", 17, 15);
		assertIcon("ICON_GRASS", "interfaces/icons/grass.png", 16, 16);
		assertIcon("ICON_WELL_HEALTH", "interfaces/icons/well_health.png", 16, 16);
		assertIcon("ICON_WELL_AWARENESS", "interfaces/icons/well_awareness.png", 16, 16);
		assertIcon("ICON_SACRIFICE_ALTAR", "interfaces/icons/sacrifice_altar.png", 16, 16);
		assertIcon("ICON_SCROLL_COLOR", "interfaces/icons/scroll_color.png", 15, 14);
		assertIcon("ICON_SCROLL_GREY", "interfaces/icons/scroll_grey.png", 15, 14);
	}

	@Test
	void renameIconsAreGone() {
		assertThrows(IllegalArgumentException.class, () -> GeneralAsset.valueOf("ICON_RENAME_ON"));
		assertThrows(IllegalArgumentException.class, () -> GeneralAsset.valueOf("ICON_RENAME_OFF"));
		assertTrue(!new File(ASSETS, "interfaces/icons/rename_on.png").exists(), "rename_on.png still ships");
		assertTrue(!new File(ASSETS, "interfaces/icons/rename_off.png").exists(), "rename_off.png still ships");
	}

	@Test
	void journalButtonArtIsRecut() throws Exception {
		assertIcon("JOURNAL_ICON", "interfaces/journal_icon.png", 11, 6);
		assertIcon("MENU_BUTTON", "interfaces/menu_button.png", 12, 11);
	}
}
