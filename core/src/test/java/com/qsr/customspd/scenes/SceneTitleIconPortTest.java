package com.qsr.customspd.scenes;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.windows.IconTitle;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The main scenes' titles, built for real with no display, as AlchemySceneUiPortTest does.
 * Upstream 80abc8376 gave each title an icon and pinned the changes panels to y 20, the right
 * panel's half coming from 72f6ee799.
 */
class SceneTitleIconPortTest {

	private HeadlessRun run;
	private PixelScene scene;
	private Map<Object, SmartTexture> textures;
	private Map<Object, SmartTexture> earlierTextures;

	@BeforeEach
	void start() throws Exception {
		run = HeadlessRun.start(20261026L);
		// StartScene empties the texture cache, and deleting a texture needs GL. Start each test
		// with nothing cached, whatever earlier test classes loaded, and put it all back after.
		@SuppressWarnings("unchecked")
		Map<Object, SmartTexture> cache = (Map<Object, SmartTexture>) read(null, TextureCache.class, "all");
		textures = cache;
		earlierTextures = new LinkedHashMap<>(textures);
		textures.clear();
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
	}

	@AfterEach
	void tearDown() {
		try {
			if (scene != null) scene.destroy();
		} finally {
			if (textures != null) {
				textures.clear();
				textures.putAll(earlierTextures);
			}
			run.close();
		}
	}

	/** 1500x1000 at the default zoom is a 375x250 camera, wide enough for both changes panels. */
	private PixelScene create(PixelScene s) {
		return create(s, 1500, 1000);
	}

	private PixelScene create(PixelScene s, int width, int height) {
		Game.width = Game.dispWidth = width;
		Game.height = Game.dispHeight = height;
		scene = s;
		scene.create();
		return scene;
	}

	private static Object read(Object owner, Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	private static <T> List<T> all(Group group, Class<T> type) throws Exception {
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) read(group, Group.class, "members");
		List<T> found = new ArrayList<>();
		for (Gizmo g : members) {
			if (type.isInstance(g)) found.add(type.cast(g));
		}
		return found;
	}

	private void assertIconTitle(GeneralAsset icon) throws Exception {
		List<IconTitle> titles = all(scene, IconTitle.class);
		assertEquals(1, titles.size(), "the scene title is an icon title");
		IconTitle title = titles.get(0);
		Image image = (Image) read(title, IconTitle.class, "imIcon");
		assertSame(TextureCache.get(Asset.getAssetFilePath(icon)), image.texture, icon.name());
		assertEquals((Camera.main.width - title.reqWidth()) / 2f, title.left(), 0.5f, "centred on its own width");
		assertEquals((20 - title.height()) / 2f, title.top(), 0.5f, "centred in the top 20");
	}

	@Test
	void theChangesTitleCarriesTheChangesIcon() throws Exception {
		create(new ChangesScene());
		assertIconTitle(GeneralAsset.ICON_CHANGES);
	}

	@Test
	void theNewsTitleCarriesTheNewsIcon() throws Exception {
		create(new NewsScene());
		assertIconTitle(GeneralAsset.ICON_NEWS);
	}

	@Test
	void theRankingsTitleCarriesTheRankingsIcon() throws Exception {
		create(new RankingsScene());
		assertIconTitle(GeneralAsset.ICON_RANKINGS);
	}

	@Test
	void theGamesInProgressTitleCarriesTheEnterIcon() throws Exception {
		create(new StartScene());
		assertIconTitle(GeneralAsset.ICON_ENTER);
	}

	@Test
	void bothChangesPanelsStartAt20OnAWideScreen() throws Exception {
		create(new ChangesScene());
		NinePatch right = (NinePatch) read(scene, ChangesScene.class, "rightPanel");
		assertNotNull(right, "test setup: wide enough for the right panel");
		List<NinePatch> panels = all(scene, NinePatch.class);
		assertEquals(2, panels.size(), "test setup: the list panel and the right panel");
		for (NinePatch panel : panels) {
			assertEquals(20, panel.y, panel == right ? "right panel" : "list panel");
		}
	}

	@Test
	void theChangesPanelStartsAt20OnANarrowScreen() throws Exception {
		create(new ChangesScene(), 500, 800);
		assertTrue(Camera.main.width < 300, "test setup: too narrow for the right panel");
		List<NinePatch> panels = all(scene, NinePatch.class);
		assertEquals(1, panels.size(), "test setup: only the list panel");
		assertEquals(20, panels.get(0).y, "list panel");
	}
}
