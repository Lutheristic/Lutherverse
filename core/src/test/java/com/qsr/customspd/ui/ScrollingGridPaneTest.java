package com.qsr.customspd.ui;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.input.KeyBindings;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layout of upstream's journal grid (ScrollingGridPane at its v2.5.4 end state): rows of 17px
 * cells with 1px gaps, small groups sharing a row behind a separator, and separators dropped on
 * clear().
 */
class ScrollingGridPaneTest {

	private static final int ITEM_SIZE = 17;

	private HeadlessRun run;
	private ScrollingGridPane grid;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20240611L);
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
		grid = new ScrollingGridPane();
		grid.camera = PixelScene.uiCamera;
	}

	@AfterEach
	void tearDown() {
		if (grid != null) grid.destroy();
		if (run != null) run.close();
	}

	private static ScrollingGridPane.GridItem cell() {
		return new ScrollingGridPane.GridItem(new ItemSprite(GeneralAsset.WORN_SHORTSWORD, null));
	}

	private List<ScrollingGridPane.GridItem> addCells(int n) {
		List<ScrollingGridPane.GridItem> cells = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			ScrollingGridPane.GridItem c = cell();
			grid.addItem(c);
			cells.add(c);
		}
		return cells;
	}

	@SuppressWarnings("unchecked")
	private static List<Gizmo> members(Group g) throws ReflectiveOperationException {
		Field f = Group.class.getDeclaredField("members");
		f.setAccessible(true);
		List<Gizmo> m = (List<Gizmo>) f.get(g);
		return m == null ? new ArrayList<Gizmo>() : m;
	}

	private List<ColorBlock> separatorsInContent() throws ReflectiveOperationException {
		List<ColorBlock> out = new ArrayList<>();
		for (Gizmo g : members(grid.content)) {
			if (g instanceof ColorBlock) out.add((ColorBlock) g);
		}
		return out;
	}

	private ScrollingGridPane.GridHeader lastHeader() throws ReflectiveOperationException {
		ScrollingGridPane.GridHeader h = null;
		for (Gizmo g : members(grid.content)) {
			if (g instanceof ScrollingGridPane.GridHeader) h = (ScrollingGridPane.GridHeader) g;
		}
		return h;
	}

	@Test
	void cellsFillRowsLeftToRightAndWrap() throws Exception {
		grid.addHeader("Header");
		ScrollingGridPane.GridHeader header = lastHeader();
		List<ScrollingGridPane.GridItem> cells = addCells(9);
		grid.setRect(0, 0, 100, 200);

		// Headless text has zero height, so a plain header is just its +2 padding.
		assertEquals(2, header.height(), "plain header height");

		float[] rowOne = {0, 18, 36, 54, 72};
		for (int i = 0; i < rowOne.length; i++) {
			assertEquals(rowOne[i], cells.get(i).left(), "row 1 cell " + i + " x");
			assertEquals(header.height() + 1, cells.get(i).top(), "row 1 cell " + i + " y");
		}
		ScrollingGridPane.GridItem rowTwoFirst = cells.get(5);
		assertEquals(0, rowTwoFirst.left(), "row 2 starts at the left edge");
		assertEquals(header.height() + 1 + 18, rowTwoFirst.top(), "row 2 y");
		assertEquals(ITEM_SIZE, rowTwoFirst.width(), "cell width");
		assertEquals(ITEM_SIZE, rowTwoFirst.height(), "cell height");

		ScrollingGridPane.GridItem last = cells.get(8);
		assertEquals(54, last.left(), "last cell x");
		assertEquals(last.top() + ITEM_SIZE + 1, grid.content.height(),
				"content ends at the bottom of the last row (cell plus its 1px gap)");
	}

	@Test
	void aHeaderAfterAPartRowStartsANewRowWhenTheNextGroupDoesNotFit() throws Exception {
		grid.addHeader("First");
		ScrollingGridPane.GridHeader first = lastHeader();
		List<ScrollingGridPane.GridItem> firstCells = addCells(4);
		grid.addHeader("Second");
		ScrollingGridPane.GridHeader second = lastHeader();
		List<ScrollingGridPane.GridItem> secondCells = addCells(5);
		grid.setRect(0, 0, 100, 200);

		assertEquals(54, firstCells.get(3).left(), "first group fills part of its row");
		assertEquals(0, second.left(), "second header starts at the left edge");
		assertEquals(firstCells.get(0).top() + ITEM_SIZE + 2, second.top(), "second header is on a new row");
		assertEquals(first.height() + 1 + ITEM_SIZE + 2, second.top(), "second header y");
		assertEquals(0, secondCells.get(0).left(), "second group starts at the left edge");
		assertEquals(second.top() + second.height() + 1, secondCells.get(0).top(), "second group sits under its header");
		assertEquals(0, separatorsInContent().size(), "no separator when groups do not share a row");
	}

	@Test
	void smallGroupsShareARowBehindOneSeparatorAndClearDropsIt() throws Exception {
		grid.addHeader("First");
		List<ScrollingGridPane.GridItem> firstCells = addCells(3);
		grid.addHeader("Second");
		ScrollingGridPane.GridHeader second = lastHeader();
		List<ScrollingGridPane.GridItem> secondCells = addCells(3);
		grid.setRect(0, 0, 216, 200);

		assertEquals(36, firstCells.get(2).left());
		float secondLeft = secondCells.get(0).left();
		assertEquals(54, secondLeft, "second group continues on the same row");
		assertEquals(firstCells.get(0).top(), secondCells.get(0).top(), "both groups on one row");
		assertEquals(54, second.left(), "second header sits above its group");
		assertEquals(0, second.top(), "second header shares the first header's row");

		List<ColorBlock> seps = separatorsInContent();
		assertEquals(1, seps.size(), "exactly one separator");
		ColorBlock sep = seps.get(0);
		assertEquals(1, sep.width(), "separator is 1px wide");
		assertEquals(secondLeft - 1, sep.x, "separator sits just left of the second group");
		assertEquals(second.height() + 1 + ITEM_SIZE, sep.height(), "separator spans header and cells");

		// laying out the same grid again reuses the separator instead of adding another
		grid.setRect(0, 0, 216, 200);
		assertEquals(1, separatorsInContent().size(), "relayout reuses the separator");

		// clear() forgets the separator along with the content (c8ab57632)
		grid.clear();
		Field f = ScrollingGridPane.class.getDeclaredField("separators");
		f.setAccessible(true);
		assertTrue(((List<?>) f.get(grid)).isEmpty(), "separator list emptied by clear");

		// refilling straight after clear() draws a separator that is in the content again,
		// rather than reusing one clear() already took out of the content
		grid.addHeader("First");
		addCells(3);
		grid.addHeader("Second");
		addCells(3);
		grid.setRect(0, 0, 216, 200);
		assertEquals(1, separatorsInContent().size(), "a refilled grid shows its separator");

		// and clear() followed by layout leaves no separators at all
		grid.clear();
		grid.setRect(0, 0, 216, 200);
		assertEquals(0, separatorsInContent().size(), "no separators after clear");
		assertTrue(((List<?>) f.get(grid)).isEmpty(), "separator list still empty after layout");
	}

	@Test
	void secondIconSitsTopRight() throws Exception {
		grid.addHeader("Header");
		addCells(2);
		ScrollingGridPane.GridItem item = cell();
		Image second = new ItemSprite(GeneralAsset.SOMETHING, null);
		second.scale.set(0.5f);
		item.addSecondIcon(second);
		grid.addItem(item);
		grid.setRect(0, 0, 100, 200);

		assertEquals(36, item.left());
		assertEquals(item.left() + ITEM_SIZE - second.width(), second.x, "second icon x");
		assertEquals(item.top(), second.y, "second icon y");
		assertTrue(second.width() < ITEM_SIZE, "second icon is narrower than the cell");
	}
}
