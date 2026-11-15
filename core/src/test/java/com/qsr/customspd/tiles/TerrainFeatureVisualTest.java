package com.qsr.customspd.tiles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.qsr.customspd.Assets;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.levels.traps.WornDartTrap;
import com.qsr.customspd.plants.Firebloom;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.RectF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Upstream 52aa38780 and b6f7e7462: the journal's bestiary draws traps and plants from the
 * terrain feature sheet with no level loaded and no tilemap built for one.
 */
class TerrainFeatureVisualTest {

	private HeadlessRun run;
	private GL20 priorGl;
	private GL20 priorGl20;
	private Level priorLevel;

	@BeforeEach
	void start() throws Exception {
		run = HeadlessRun.start(20261115L);
		priorGl = Gdx.gl;
		priorGl20 = Gdx.gl20;
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
		TextureCache.create(Assets.Environment.TERRAIN_FEATURES, 256, 128);
		priorLevel = Dungeon.level;
		Dungeon.level = null;
		setInstance(null);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			setInstance(null);
			Dungeon.level = priorLevel;
		} finally {
			Gdx.gl = priorGl;
			Gdx.gl20 = priorGl20;
			run.close();
		}
	}

	private static void setInstance(Object value) throws Exception {
		Field f = TerrainFeaturesTilemap.class.getDeclaredField("instance");
		f.setAccessible(true);
		f.set(null, value);
	}

	private static RectF expected(int cell) {
		return new TextureFilm(Assets.Environment.TERRAIN_FEATURES, DungeonTilemap.SIZE, DungeonTilemap.SIZE).get(cell);
	}

	private static Image visual(String name, Class<?> argType, Object arg) throws Exception {
		Method m = TerrainFeaturesTilemap.class.getMethod(name, argType);
		return (Image) m.invoke(null, arg);
	}

	private static void assertFrame(RectF want, Image img) {
		assertNotNull(img);
		RectF got = img.frame();
		assertEquals(want.left, got.left, 1e-6f);
		assertEquals(want.top, got.top, 1e-6f);
		assertEquals(want.right, got.right, 1e-6f);
		assertEquals(want.bottom, got.bottom, 1e-6f);
	}

	@Test
	void anActiveTrapUsesItsColourAndShape() throws Exception {
		WornDartTrap trap = new WornDartTrap();
		assertFrame(expected(trap.color + trap.shape * 16), visual("getTrapVisual", Trap.class, trap));
	}

	@Test
	void anInactiveTrapIsDrawnBlack() throws Exception {
		WornDartTrap trap = new WornDartTrap();
		trap.active = false;
		assertFrame(expected(Trap.BLACK + trap.shape * 16), visual("getTrapVisual", Trap.class, trap));
	}

	@Test
	void aPlantUsesTheSeventhRow() throws Exception {
		Firebloom plant = new Firebloom();
		assertFrame(expected(plant.image + 7 * 16),
				visual("getPlantVisual", com.qsr.customspd.plants.Plant.class, plant));
	}

	@Test
	void theTilemapBuildsWithNoLevel() {
		assertDoesNotThrow(() -> new TerrainFeaturesTilemap(null, null));
	}
}
