package com.qsr.customspd.scenes;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Rankings;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.StyledButton;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream aec4b0089: a player who already has finished runs skips the intro, both from the
 * welcome screen and on reaching hero select with the intro still set.
 */
class IntroSkipPortTest {

	private HeadlessRun run;
	private Game priorInstance;
	private PixelScene scene;

	@BeforeEach
	void start() {
		run = HeadlessRun.start(20261112L);
		priorInstance = Game.instance;
		// a bare Game takes the scene switch; HeadlessRun's snapshot puts its statics back
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
		Game.width = Game.dispWidth = 1500;
		Game.height = Game.dispHeight = 1000;
		SPDSettings.intro(true);
	}

	@AfterEach
	void tearDown() {
		try {
			if (scene != null) scene.destroy();
		} finally {
			Game.instance = priorInstance;
			run.close();
		}
	}

	private static Object requestedScene() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		return f.get(null);
	}

	private static void click(Button button) throws Exception {
		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(button);
	}

	private static List<StyledButton> buttons(Group group) throws Exception {
		Field f = Group.class.getDeclaredField("members");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) f.get(group);
		List<StyledButton> found = new ArrayList<>();
		for (Gizmo g : members) {
			if (g instanceof StyledButton) found.add((StyledButton) g);
		}
		return found;
	}

	/** Builds the welcome screen a first launch shows and presses its only button. */
	private void enterFromWelcome() throws Exception {
		scene = new WelcomeScene();
		scene.create();
		List<StyledButton> found = buttons(scene);
		assertEquals(1, found.size(), "test setup: the intro welcome screen has one button");
		click(found.get(0));
	}

	@Test
	void theWelcomeScreenSkipsTheIntroForAPlayerWithFinishedRuns() throws Exception {
		Rankings.INSTANCE.totalNumber = 3;

		enterFromWelcome();

		assertFalse(SPDSettings.intro(), "a player with finished runs must not be sent through the intro");
		assertSame(TitleScene.class, requestedScene(), "they go to the title screen, not hero select");
	}

	@Test
	void theWelcomeScreenStillStartsTheIntroForANewPlayer() throws Exception {
		Rankings.INSTANCE.totalNumber = 0;

		enterFromWelcome();

		assertTrue(SPDSettings.intro(), "control: a new player keeps the intro");
		assertSame(HeroSelectScene.class, requestedScene(), "control: a new player picks a hero first");
	}

	@Test
	void heroSelectClearsTheIntroForAPlayerWithFinishedRuns() {
		Rankings.INSTANCE.totalNumber = 3;
		scene = new HeroSelectScene();
		scene.create();
		assertTrue(SPDSettings.intro(), "test setup: the intro is still set when hero select opens");

		scene.update();

		assertFalse(SPDSettings.intro(), "hero select must drop the intro once finished runs exist");
	}

	@Test
	void heroSelectKeepsTheIntroForANewPlayer() {
		Rankings.INSTANCE.totalNumber = 0;
		scene = new HeroSelectScene();
		scene.create();

		scene.update();

		assertTrue(SPDSettings.intro(), "control: with no finished runs the intro stays");
	}
}
