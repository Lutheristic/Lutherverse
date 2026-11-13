package com.qsr.customspd.tiles;

import com.qsr.customspd.assets.tiles.CavesTile;
import com.qsr.customspd.assets.tiles.CityTile;
import com.qsr.customspd.assets.tiles.HallsTile;
import com.qsr.customspd.assets.tiles.PrisonTile;
import com.qsr.customspd.assets.tiles.SewersTile;
import com.qsr.customspd.assets.tiles.TileAsset;
import com.qsr.customspd.levels.Terrain;
import com.watabou.utils.SparseArray;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream draws each region from one tiles_*.png sheet. CPDU builds that sheet at startup from
 * one PNG per tile, listed in the region's tile enum, and a position no entry fills is drawn fully
 * transparent. So every terrain the tile sheet maps straight to a region tile needs an entry at
 * that position, in every region, with a real image behind it. Upstream 345b6fe79 added the
 * ENTRANCE_SP art at position 22 of every region sheet.
 */
class RegionTileArtTest {

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	private static final File ASSETS = new File(".");

	private static Map<String, TileAsset[]> regions() {
		Map<String, TileAsset[]> regions = new LinkedHashMap<>();
		regions.put("sewers", SewersTile.values());
		regions.put("prison", PrisonTile.values());
		regions.put("caves", CavesTile.values());
		regions.put("city", CityTile.values());
		regions.put("halls", HallsTile.values());
		return regions;
	}

	/** Terrain id to region sheet position, for every terrain drawn without stitching. */
	private static Map<Integer, Integer> directTiles() {
		Map<Integer, Integer> tiles = new LinkedHashMap<>();
		for (SparseArray<Integer> visuals : new SparseArray[]{DungeonTileSheet.directVisuals, DungeonTileSheet.directFlatVisuals}) {
			for (int terrain : visuals.keyArray()) {
				tiles.put(terrain, visuals.get(terrain));
			}
		}
		return tiles;
	}

	@Test
	void everyDirectlyDrawnTerrainHasArtInEveryRegion() throws Exception {
		assertTrue(new File(ASSETS, SewersTile.FLOOR.getPath()).isFile(),
				"fixture sanity: working directory must be core/src/main/assets, was " + ASSETS.getAbsolutePath());

		Map<Integer, Integer> tiles = directTiles();
		assertEquals(DungeonTileSheet.ENTRANCE_SP, tiles.get(Terrain.ENTRANCE_SP),
				"fixture sanity: ENTRANCE_SP is drawn straight from the region sheet");

		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, TileAsset[]> region : regions().entrySet()) {
			Map<Integer, TileAsset> byPos = new LinkedHashMap<>();
			for (TileAsset asset : region.getValue()) byPos.put(asset.getPos(), asset);

			for (Map.Entry<Integer, Integer> tile : tiles.entrySet()) {
				String where = region.getKey() + ": terrain " + tile.getKey() + " -> tile " + tile.getValue();
				TileAsset asset = byPos.get(tile.getValue());
				if (asset == null) {
					problems.add(where + " has no tile entry");
					continue;
				}
				File png = new File(ASSETS, asset.getPath());
				if (!png.isFile()) {
					problems.add(where + " -> missing file " + asset.getPath());
					continue;
				}
				BufferedImage img = ImageIO.read(png);
				if (img == null || img.getWidth() != 16 || img.getHeight() != 16) {
					problems.add(where + " -> " + asset.getPath() + " is not a 16x16 image");
					continue;
				}
				boolean opaquePixel = false;
				for (int y = 0; y < 16 && !opaquePixel; y++) {
					for (int x = 0; x < 16 && !opaquePixel; x++) {
						opaquePixel = (img.getRGB(x, y) >>> 24) != 0;
					}
				}
				if (!opaquePixel) problems.add(where + " -> " + asset.getPath() + " is fully transparent");
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}
}
