package com.qsr.customspd.scenes;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.ui.Button;
import com.qsr.customspd.ui.IconButton;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.StyledButton;
import com.qsr.customspd.ui.Window;
import com.qsr.customspd.windows.WndOptions;
import com.qsr.customspd.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Scene;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hero select's game options panel, built for real with no display. Each test names the
 * upstream change it pins.
 */
class GameOptionsPortTest {

	private static final long DAY = 24 * 60 * 60 * 1000L;

	private static final int SEED = 0, DAILY = 1, CHALLENGES = 2;

	private HeadlessRun run;
	private Game priorInstance;
	private HeroSelectScene scene;

	@BeforeEach
	void start() throws Exception {
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

	private static Object read(Object owner, Class<?> type, String name) throws Exception {
		Field f = type.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	@SuppressWarnings("unchecked")
	private static void setVictory(boolean won) throws Exception {
		Set<Badges.Badge> global = (Set<Badges.Badge>) read(null, Badges.class, "global");
		if (won) global.add(Badges.Badge.VICTORY);
		else global.remove(Badges.Badge.VICTORY);
	}

	private HeroSelectScene createScene() throws Exception {
		scene = new HeroSelectScene();
		scene.create();
		Field f = Game.class.getDeclaredField("scene");
		f.setAccessible(true);
		f.set(Game.instance, scene);
		return scene;
	}

	@SuppressWarnings("unchecked")
	private List<StyledButton> optionButtons() throws Exception {
		Object pane = read(scene, HeroSelectScene.class, "optionsPane");
		return (List<StyledButton>) read(pane, pane.getClass(), "buttons");
	}

	private static void click(Button button) throws Exception {
		Method onClick = Button.class.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(button);
	}

	/** The last window the scene put in front. */
	private static <T> T frontWindow(Scene scene, Class<T> type) throws Exception {
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) read(scene, Group.class, "members");
		List<T> found = new ArrayList<>();
		for (Gizmo g : members) {
			if (type.isInstance(g)) found.add(type.cast(g));
		}
		assertEquals(1, found.size(), "one " + type.getSimpleName() + " is open");
		return found.get(0);
	}

	private static int windowsOpen(Scene scene) throws Exception {
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) read(scene, Group.class, "members");
		int open = 0;
		for (Gizmo g : members) {
			if (g instanceof Window) open++;
		}
		return open;
	}

	private static String body(WndTitledMessage window) throws Exception {
		@SuppressWarnings("unchecked")
		List<Gizmo> members = (List<Gizmo>) read(window, Group.class, "members");
		for (Gizmo g : members) {
			if (g instanceof RenderedTextBlock) return ((RenderedTextBlock) g).text();
		}
		throw new AssertionError("the window has no message text");
	}

	/** Clicks a locked option and returns the text of the one window it opens. */
	private String lockedMessage(int option) throws Exception {
		assertEquals(0, windowsOpen(scene), "test setup: no window open yet");
		click(optionButtons().get(option));
		assertEquals(1, windowsOpen(scene), "exactly one window opens");
		WndTitledMessage window = frontWindow(scene, WndTitledMessage.class);
		String text = body(window);
		window.hide();
		return text;
	}

	// upstream 425c90135
	@Test
	void aClockSetBeforeTheEarliestDailyStartsTheEarliestDaily() throws Exception {
		setVictory(true);
		SPDSettings.lastDaily(0);
		// Dec 15 2023: after v1.4.0's minimum, before v2.3.2's Jan 30 2024
		Game.realTime = 19_706 * DAY + 5 * 60 * 60 * 1000L;
		createScene();
		assertEquals(3, optionButtons().size(), "test setup: a winner sees seed, daily and challenges");

		click(optionButtons().get(DAILY));
		WndOptions confirm = frontWindow(scene, WndOptions.class);
		Method onSelect = WndOptions.class.getDeclaredMethod("onSelect", int.class);
		onSelect.setAccessible(true);
		onSelect.invoke(confirm, 0);

		assertEquals(19_752 * DAY, SPDSettings.lastDaily(), "the daily is dated no earlier than Jan 30 2024");
		assertSame(InterlevelScene.class, read(null, Game.class, "sceneClass"), "test setup: the daily starts");
	}

	// upstream aedd3d04b
	@Test
	void aPlayerWithNoWinStillSeesTheSeedDailyAndChallengeButtons() throws Exception {
		setVictory(false);
		createScene();

		assertEquals(3, optionButtons().size(), "the options panel must not open empty before a first win");
	}

	// upstream aedd3d04b
	@Test
	void eachLockedOptionExplainsThatAWinUnlocksIt() throws Exception {
		setVictory(false);
		createScene();
		assertEquals(3, optionButtons().size(), "test setup: the three options are there");

		assertEquals(Messages.get(HeroSelectScene.class, "custom_seed_nowin"), lockedMessage(SEED));
		assertEquals(Messages.get(HeroSelectScene.class, "daily_nowin"), lockedMessage(DAILY));
		assertEquals(Messages.get(HeroSelectScene.class, "challenges_nowin"), lockedMessage(CHALLENGES));
		assertTrue(Messages.get(HeroSelectScene.class, "daily_nowin").contains("win at least one game"),
				"test setup: the English text exists");
		assertFalse(Dungeon.daily, "a locked daily button must not start a daily");
	}

	// upstream aedd3d04b
	@Test
	void aPlayerWithNoWinStartsWithNoChallengesOrCustomSeed() throws Exception {
		setVictory(false);
		SPDSettings.challenges(3);
		SPDSettings.customSeed("ABC-DEF-GHI");
		Dungeon.challenges = 3;

		createScene();

		assertEquals(0, SPDSettings.challenges(), "challenges set before a win are cleared");
		assertEquals(0, Dungeon.challenges);
		assertEquals("", SPDSettings.customSeed(), "a custom seed set before a win is cleared");
	}

	// upstream aedd3d04b
	@Test
	void aWinnerKeepsTheirChallengesAndCustomSeed() throws Exception {
		setVictory(true);
		SPDSettings.challenges(3);
		SPDSettings.customSeed("ABC-DEF-GHI");

		createScene();

		assertEquals(3, SPDSettings.challenges(), "control: a winner's challenges stay");
		assertEquals("ABC-DEF-GHI", SPDSettings.customSeed(), "control: a winner's seed stays");
	}

	// upstream aedd3d04b
	@Test
	void theOptionsButtonIsLeftOutDuringTheIntro() throws Exception {
		setVictory(false);
		SPDSettings.intro(true);
		createScene();
		IconButton options = (IconButton) read(scene, HeroSelectScene.class, "btnOptions");
		assertNull(options.parent, "the intro hero select has no options button");

		scene.destroy();
		SPDSettings.intro(false);
		createScene();
		options = (IconButton) read(scene, HeroSelectScene.class, "btnOptions");
		assertNotNull(options.parent, "control: after the intro the options button is there");
	}
}
