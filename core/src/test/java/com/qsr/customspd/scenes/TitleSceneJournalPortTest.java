package com.qsr.customspd.scenes;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.modding.TileMapCompilationManager;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.StyledButton;
import com.qsr.customspd.windows.WndMessage;
import com.qsr.customspd.windows.WndMods;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobKt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 53139c9e5 and fbf3bfc73: the title's badges button became the Journal button, with the
 * journal icon, and the landscape rows moved so Journal sits beside Rankings. CPDU keeps Mods in
 * upstream's Support slot and Guides in its News slot, and makes the Journal wait for tile
 * compilation as Play and Changes do.
 */
class TitleSceneJournalPortTest {

	private static final float EPS = 0.001f;

	private HeadlessRun run;
	private Game priorInstance;
	private TitleScene scene;
	private Map<Object, SmartTexture> textures;
	private Map<Object, SmartTexture> earlierTextures;
	private Field lastJob;
	private Object earlierJob;

	@BeforeEach
	void start() throws Exception {
		run = HeadlessRun.start(20261115L);
		@SuppressWarnings("unchecked")
		Map<Object, SmartTexture> cache = (Map<Object, SmartTexture>) read(null, TextureCache.class, "all");
		textures = cache;
		earlierTextures = new LinkedHashMap<>(textures);
		textures.clear();
		priorInstance = Game.instance;
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
		lastJob = TileMapCompilationManager.class.getDeclaredField("lastJob");
		lastJob.setAccessible(true);
		earlierJob = lastJob.get(null);
		lastJob.set(null, null);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			Object job = lastJob.get(null);
			if (job instanceof Job && job != earlierJob) ((Job) job).cancel(null);
			lastJob.set(null, earlierJob);
			if (scene != null) {
				TitleScene s = scene;
				scene = null;
				s.destroy();
			}
			Game.instance = priorInstance;
		} finally {
			textures.clear();
			textures.putAll(earlierTextures);
			run.close();
		}
	}

	// helpers

	private static Object read(Object owner, Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	@SuppressWarnings("unchecked")
	private static List<Gizmo> members(Group group) throws Exception {
		List<Gizmo> m = (List<Gizmo>) read(group, Group.class, "members");
		return m == null ? new ArrayList<Gizmo>() : m;
	}

	private static Object requestedScene() throws Exception {
		return read(null, Game.class, "sceneClass");
	}

	private static void clearRequestedScene() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		f.set(null, null);
	}

	private static void click(Button button) throws Exception {
		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(button);
	}

	private void createTitle(int width, int height) throws Exception {
		Game.width = Game.dispWidth = width;
		Game.height = Game.dispHeight = height;
		scene = new TitleScene();
		Field current = Game.class.getDeclaredField("scene");
		current.setAccessible(true);
		current.set(Game.instance, scene);
		scene.create();
	}

	/**
	 * The title's own menu buttons in the order the scene adds them: Play, Mods, Rankings,
	 * Journal, Guides, Changes, Settings, About.
	 */
	private List<StyledButton> menu() throws Exception {
		List<StyledButton> found = new ArrayList<>();
		for (Gizmo g : members(scene)) {
			if (g instanceof StyledButton) found.add((StyledButton) g);
		}
		assertEquals(8, found.size(), "test setup: the title has eight menu buttons");
		assertEquals(Messages.get(TitleScene.class, "enter"), found.get(0).text(), "test setup: Play comes first");
		assertEquals(Messages.get(TitleScene.class, "mods"), found.get(1).text(), "test setup: Mods second");
		assertEquals(Messages.get(TitleScene.class, "rankings"), found.get(2).text(), "test setup: Rankings third");
		assertEquals(Messages.get(TitleScene.class, "guides"), found.get(4).text(), "test setup: Guides fifth");
		assertEquals(Messages.get(TitleScene.class, "changes"), found.get(5).text(), "test setup: Changes sixth");
		assertEquals(Messages.get(TitleScene.class, "settings"), found.get(6).text(), "test setup: Settings seventh");
		assertEquals(Messages.get(TitleScene.class, "about"), found.get(7).text(), "test setup: About last");
		return found;
	}

	private static List<WndMessage> messages(Group group) throws Exception {
		List<WndMessage> found = new ArrayList<>();
		for (Gizmo g : members(group)) {
			if (g instanceof WndMessage) found.add((WndMessage) g);
		}
		return found;
	}

	private static List<String> texts(Group group) throws Exception {
		List<String> found = new ArrayList<>();
		for (Gizmo g : members(group)) {
			if (g instanceof RenderedTextBlock) found.add(((RenderedTextBlock) g).text());
			else if (g instanceof Group) found.addAll(texts((Group) g));
		}
		return found;
	}

	private static Properties englishScenes() throws Exception {
		Properties p = new Properties();
		InputStream in = new FileInputStream("messages/scenes/scenes.properties");
		try {
			p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} finally {
			in.close();
		}
		return p;
	}

	// tests

	@Test
	void theJournalButtonLeadsToTheJournal() throws Exception {
		createTitle(1440, 810);
		StyledButton journal = menu().get(3);

		assertEquals("Journal", journal.text(), "the button reads Journal");
		assertSame(TextureCache.get(Asset.getAssetFilePath(GeneralAsset.ICON_JOURNAL)), journal.icon().texture,
				"the button shows the journal icon");

		clearRequestedScene();
		click(journal);
		assertSame(JournalScene.class, requestedScene(), "the button opens the journal scene");
	}

	@Test
	void theJournalWaitsForTileCompilation() throws Exception {
		createTitle(1440, 810);
		StyledButton journal = menu().get(3);
		lastJob.set(null, JobKt.Job(null));
		assertTrue(TileMapCompilationManager.INSTANCE.isBusy(), "test setup: tile compilation reports busy");
		assertEquals(0, messages(scene).size(), "test setup: no message is up yet");

		clearRequestedScene();
		click(journal);

		assertEquals(1, messages(scene).size(), "the journal shows the wait message while tiles compile");
		assertEquals(Arrays.asList(Messages.get(WndMods.class, "wait")), texts(messages(scene).get(0)),
				"the message is the tile compilation wait text");
		assertNull(requestedScene(), "the journal does not open while tiles compile");
	}

	@Test
	void landscapeRowsMatchUpstream() throws Exception {
		createTitle(1440, 810);
		assertTrue(PixelScene.landscape(), "test setup: a wide screen lays the title out in landscape");
		List<StyledButton> b = menu();
		StyledButton play = b.get(0), mods = b.get(1), rankings = b.get(2), journal = b.get(3),
				guides = b.get(4), changes = b.get(5), settings = b.get(6), about = b.get(7);
		float gap = rankings.top() - play.bottom();

		assertEquals(play.right() + 2, mods.left(), EPS, "Mods sits at Play's right");
		assertEquals(play.top(), mods.top(), EPS, "Mods shares Play's row");

		assertEquals(rankings.right() + 2, journal.left(), EPS, "Journal sits at Rankings' right");
		assertEquals(rankings.top(), journal.top(), EPS, "Journal shares Rankings' row");
		assertEquals(journal.right() + 2, guides.left(), EPS, "Guides sits at Journal's right");
		assertEquals(journal.top(), guides.top(), EPS, "Guides shares Journal's row");

		assertEquals(rankings.left(), settings.left(), EPS, "Settings starts the third row under Rankings");
		assertEquals(rankings.bottom() + gap, settings.top(), EPS, "Settings is one gap under Rankings");
		assertEquals(settings.right() + 2, changes.left(), EPS, "Changes sits at Settings' right");
		assertEquals(changes.right() + 2, about.left(), EPS, "About sits at Changes' right");
		assertEquals(settings.top(), changes.top(), EPS, "Changes shares Settings' row");
		assertEquals(settings.top(), about.top(), EPS, "About shares Settings' row");
	}

	@Test
	void portraitRowsAreUnchanged() throws Exception {
		createTitle(540, 960);
		assertFalse(PixelScene.landscape(), "test setup: a narrow screen lays the title out in portrait");
		List<StyledButton> b = menu();
		StyledButton play = b.get(0), mods = b.get(1), rankings = b.get(2), journal = b.get(3),
				guides = b.get(4), changes = b.get(5), settings = b.get(6), about = b.get(7);
		float gap = mods.top() - play.bottom();
		assertTrue(gap >= 2, "test setup: the portrait gap is at least 2");

		assertEquals(play.left(), mods.left(), EPS, "Mods is under Play");
		assertEquals(play.width(), mods.width(), EPS, "Mods is as wide as Play");

		assertEquals(play.left(), rankings.left(), EPS, "Rankings starts the pairs under Mods");
		assertEquals(mods.bottom() + gap, rankings.top(), EPS, "Rankings is one gap under Mods");
		assertEquals(play.width() / 2 - 1, rankings.width(), EPS, "Rankings takes half of Play's width");
		assertEquals(rankings.right() + 2, journal.left(), EPS, "Journal pairs with Rankings");
		assertEquals(rankings.top(), journal.top(), EPS, "Journal shares Rankings' row");

		assertEquals(rankings.left(), guides.left(), EPS, "Guides is under Rankings");
		assertEquals(rankings.bottom() + gap, guides.top(), EPS, "Guides is one gap under Rankings");
		assertEquals(guides.right() + 2, changes.left(), EPS, "Changes pairs with Guides");
		assertEquals(guides.top(), changes.top(), EPS, "Changes shares Guides' row");

		assertEquals(guides.left(), settings.left(), EPS, "Settings is under Guides");
		assertEquals(guides.bottom() + gap, settings.top(), EPS, "Settings is one gap under Guides");
		assertEquals(settings.right() + 2, about.left(), EPS, "About pairs with Settings");
		assertEquals(settings.top(), about.top(), EPS, "About shares Settings' row");
	}

	@Test
	void theTitleKeysAreInEnglish() throws Exception {
		Properties scenes = englishScenes();
		assertFalse(scenes.containsKey("scenes.titlescene.badges"), "the old badges key is gone");
		assertEquals("Journal", Messages.get(TitleScene.class, "journal"), "the journal label is in English");
	}
}
