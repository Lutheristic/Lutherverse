package com.qsr.customspd.windows;

import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Window;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 52aa38780: a journal entry's details are a titled message that a tap anywhere closes,
 * through a blocker the size of the UI camera.
 */
class WndJournalItemTest {

	private static Window build() throws Exception {
		Class<?> cls = Class.forName("com.qsr.customspd.windows.WndJournalItem");
		assertTrue(WndTitledMessage.class.isAssignableFrom(cls), "a titled message");
		Constructor<?> c = cls.getConstructor(Image.class, String.class, String.class);
		return (Window) c.newInstance(new Image(), "title", "message");
	}

	private static PointerArea blocker(Window wnd) throws Exception {
		//Window's own blocker only closes on taps outside the chrome; the journal item adds a second one
		Object windowBlocker = WndJournalCatalogTest.field(Window.class, "blocker").get(wnd);
		PointerArea found = null;
		for (Gizmo g : WndJournalCatalogTest.members(wnd)) {
			if (g instanceof PointerArea && g != windowBlocker
					&& ((PointerArea) g).width() == PixelScene.uiCamera.width
					&& ((PointerArea) g).height() == PixelScene.uiCamera.height) {
				found = (PointerArea) g;
			}
		}
		return found;
	}

	@Test
	void aBlockerCoversTheWholeUiCamera() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20261115L)) {
			run.enterFirstLevel();
			WndJournalCatalogTest.setUpUi();

			Window wnd = build();
			PointerArea blocker = blocker(wnd);
			assertNotNull(blocker, "a PointerArea sized to the UI camera");
			assertSame(PixelScene.uiCamera, blocker.camera);
		}
	}

	@Test
	void aTapAnywhereClosesTheWindow() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20261115L)) {
			run.enterFirstLevel();
			WndJournalCatalogTest.setUpUi();

			Group parent = new Group();
			Window wnd = build();
			parent.add(wnd);
			assertSame(parent, wnd.parent);

			PointerArea blocker = blocker(wnd);
			assertNotNull(blocker, "a PointerArea sized to the UI camera");
			Method click = PointerArea.class.getDeclaredMethod("onClick", PointerEvent.class);
			click.setAccessible(true);
			click.invoke(blocker, (Object) null);

			assertNull(wnd.parent, "the window hid itself");
			assertEquals(0, WndJournalCatalogTest.members(parent).stream().filter(g -> g == wnd).count());
		}
	}
}
