package com.qsr.customspd.ui;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Badges;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.effects.BadgeBanner;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.windows.WndBadge;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyBindings;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Scene;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 3dbcfef05 sizes the badge grid by the space each badge gets and closes a badge window
 * on any tap, and 09b2338b5 opens badge windows in front of the scene rather than in its first
 * free slot.
 */
class BadgesUiPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private BitmapText.Font priorFont;
	private Camera priorMain;
	private int priorZoom;
	private Object priorGlobal;
	private Scene scene;

	@BeforeEach
	void setUp() throws Exception {
		run = HeadlessRun.start(20261115L);
		priorInstance = Game.instance;
		priorFont = PixelScene.pixelFont;
		priorMain = Camera.main;
		priorZoom = PixelScene.defaultZoom;
		priorGlobal = field(Badges.class, "global").get(null);
		// PixelScene.align divides by this; the game sets it from the display at startup
		PixelScene.defaultZoom = 1;
		new Game(null, null);
		PixelScene.uiCamera = new Camera(0, 0, 400, 400, 1);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
		KeyBindings.setAllBindings(SPDAction.getDefaults());
		KeyBindings.setAllControllerBindings(SPDAction.getControllerDefaults());
		scene = new Scene();
		field(Game.class, "scene").set(Game.instance, scene);
		if (Camera.main == null) Camera.main = new Camera(0, 0, 400, 400, 1);
		PixelScene.pixelFont = new BitmapText.Font(
				TextureCache.create("test-badges-ui-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
				4, BitmapText.Font.LATIN_FULL);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			field(Badges.class, "global").set(null, priorGlobal);
			BadgeBanner.showing.clear();
			PixelScene.pixelFont = priorFont;
			Camera.main = priorMain;
			PixelScene.defaultZoom = priorZoom;
			Game.instance = priorInstance;
		} finally {
			run.close();
		}
	}

	private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	@SuppressWarnings("unchecked")
	private static List<Gizmo> members(Group g) throws ReflectiveOperationException {
		List<Gizmo> m = (List<Gizmo>) field(Group.class, "members").get(g);
		return m == null ? new ArrayList<Gizmo>() : m;
	}

	private static void setGlobalBadges(Badges.Badge... unlocked) throws Exception {
		HashSet<Badges.Badge> set = new HashSet<>();
		for (Badges.Badge b : unlocked) set.add(b);
		field(Badges.class, "global").set(null, set);
		Badges.loadGlobal();
	}

	@SuppressWarnings("unchecked")
	private static List<Button> badgeButtons(BadgesGrid grid) throws Exception {
		return (List<Button>) field(BadgesGrid.class, "badgeButtons").get(grid);
	}

	/** A scene whose first member slot is empty, as after a window was hidden. */
	private void leaveAnEmptySlot() throws Exception {
		Gizmo gone = new Gizmo();
		scene.add(gone);
		scene.add(new Gizmo());
		scene.erase(gone);
		assertNull(members(scene).get(0), "the scene has an empty slot at the back");
	}

	private Gizmo lastMember() throws Exception {
		List<Gizmo> m = members(scene);
		return m.isEmpty() ? null : m.get(m.size() - 1);
	}

	@Test
	void gridColumnsFollowTheSpaceEachBadgeGets() throws Exception {
		setGlobalBadges();
		BadgesGrid grid = new BadgesGrid(true);
		grid.setRect(0, 0, 216, 110);

		List<Button> buttons = badgeButtons(grid);
		int n = buttons.size();
		assertTrue(n > 35, "every badge shows locked, got " + n);
		Set<Float> xs = new LinkedHashSet<>();
		for (Button b : buttons) xs.add(b.left());
		long expected = Math.round(216 / Math.sqrt(216 * 110 / (double) n));
		assertEquals(expected, xs.size(), "columns follow the area each of the " + n + " badges gets");
	}

	@Test
	void tappingABadgeOpensItsWindowInFront() throws Exception {
		setGlobalBadges(Badges.Badge.GAMES_PLAYED_1);

		BadgesGrid grid = new BadgesGrid(true);
		grid.setRect(0, 0, 216, 110);
		leaveAnEmptySlot();
		Method click = Button.class.getDeclaredMethod("onClick");
		click.setAccessible(true);
		click.invoke(badgeButtons(grid).get(0));
		Gizmo front = lastMember();
		assertTrue(front instanceof WndBadge, "a grid badge opens its window in front, got "
				+ (front == null ? "nothing" : front.getClass().getSimpleName()));
		((WndBadge) front).hide();

		scene.clear();
		BadgesList list = new BadgesList(true);
		list.camera = PixelScene.uiCamera;
		list.setRect(0, 0, 120, 100);
		List<?> items = (List<?>) field(BadgesList.class, "items").get(list);
		assertEquals(1, items.size(), "the list shows the one unlocked badge");
		Component item = (Component) items.get(0);
		leaveAnEmptySlot();
		Method itemClick = item.getClass().getDeclaredMethod("onClick", float.class, float.class);
		itemClick.setAccessible(true);
		assertTrue((Boolean) itemClick.invoke(item, item.left() + 1, item.top() + 1), "the tap lands on the badge");
		front = lastMember();
		assertTrue(front instanceof WndBadge, "a listed badge opens its window in front, got "
				+ (front == null ? "nothing" : front.getClass().getSimpleName()));
		((WndBadge) front).hide();
	}

	@Test
	void aTapOutsideABadgeWindowClosesIt() throws Exception {
		WndBadge wnd = new WndBadge(Badges.Badge.GAMES_PLAYED_1, true);
		scene.add(wnd);

		List<PointerArea> fullScreen = new ArrayList<>();
		for (Gizmo g : members(wnd)) {
			if (g instanceof PointerArea) {
				PointerArea p = (PointerArea) g;
				if (p.x == 0 && p.y == 0 && p.width == PixelScene.uiCamera.width
						&& p.height == PixelScene.uiCamera.height && p.camera == PixelScene.uiCamera) {
					fullScreen.add(p);
				}
			}
		}
		assertFalse(fullScreen.isEmpty(), "the badge window has a full-screen tap area");

		// a tap on the window itself: only a blocker that closes on any tap reacts to it
		Camera c = wnd.camera;
		assertNotNull(c, "the window has its camera");
		int sx = (int) (c.x + c.screenWidth() / 2);
		int sy = (int) (c.y + c.screenHeight() / 2);
		Method onClick = PointerArea.class.getDeclaredMethod("onClick", PointerEvent.class);
		onClick.setAccessible(true);
		for (PointerArea p : fullScreen) {
			if (wnd.parent == null) break;
			onClick.invoke(p, new PointerEvent(sx, sy, 0, PointerEvent.Type.UP));
		}
		assertNull(wnd.parent, "a tap closes the badge window");
		assertFalse(members(scene).contains(wnd), "the scene no longer holds it");
	}
}
