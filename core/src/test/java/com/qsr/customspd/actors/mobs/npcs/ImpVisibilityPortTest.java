package com.qsr.customspd.actors.mobs.npcs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.sprites.ImpSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.RenderThreadCalls;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.CharHealthIndicator;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 00b4300f0 (v2.5.0): the imp is easier to see. Its spawn tile
 * becomes plain floor and its sprite is drawn at half alpha instead of 0.4. Its quest opens with a
 * shared intro before the golem or monk request.
 */
class ImpVisibilityPortTest {

	private Hero prevHero;
	private int prevDepth;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevHero = Dungeon.hero;
		prevDepth = Dungeon.depth;
		Imp.Quest.reset();
		// the quest start adds a journal landmark; the journal is only set up by a run
		Notes.reset();
	}

	@AfterEach
	void tearDown() {
		Imp.Quest.reset();
		Notes.reset();
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		Dungeon.depth = prevDepth;
	}

	@Test
	void theImpSpawnsOnPlainFloor() {
		HeadlessLevel level = HeadlessLevel.install(12, 12);
		for (int i = 0; i < level.length(); i++) {
			if (level.map[i] == Terrain.EMPTY) level.map[i] = Terrain.HIGH_GRASS;
		}
		level.buildFlagMaps();
		Dungeon.depth = 17;

		Random.pushGenerator(20240808L);
		try {
			Imp.Quest.spawn(level);
		} finally {
			Random.popGenerator();
		}

		Imp imp = null;
		for (Mob m : level.mobs) {
			if (m instanceof Imp) imp = (Imp) m;
		}
		assertNotNull(imp, "precondition: the imp spawned");
		assertEquals(Terrain.EMPTY, level.map[imp.pos], "the imp's tile is cleared to plain floor");
	}

	@Test
	void theImpSpriteIsHalfTransparent() throws Exception {
		HeadlessLevel.install(8, 8);
		Dungeon.hero = new Hero();
		Imp imp = HeadlessLevel.at(new Imp(), 3 * 8 + 3);
		HeadlessImpSprite sprite = allocate(HeadlessImpSprite.class);
		sprite.skipHealthBar();

		sprite.link(imp);

		assertEquals(0.5f, sprite.alpha(), 0.0001f);
	}

	@Test
	void theGolemQuestOpensWithTheIntro() throws Exception {
		Hero hero = placeHeroAndImpQuest(false);
		String told = startQuest();

		String intro = Messages.get(Imp.class, "intro");
		assertTrue(intro.startsWith("Are you an adventurer?"), "the intro has its own text: " + intro);
		assertEquals(intro + "\n\n" + Messages.get(Imp.class, "golems_1", Messages.titleCase(hero.name())), told);
		assertFalse(Messages.get(Imp.class, "golems_1").contains("adventurer"), "the request no longer repeats the intro");
	}

	@Test
	void theMonkQuestOpensWithTheIntro() throws Exception {
		Hero hero = placeHeroAndImpQuest(true);
		String told = startQuest();

		String intro = Messages.get(Imp.class, "intro");
		assertTrue(intro.startsWith("Are you an adventurer?"), "the intro has its own text: " + intro);
		assertEquals(intro + "\n\n" + Messages.get(Imp.class, "monks_1", Messages.titleCase(hero.name())), told);
		assertFalse(Messages.get(Imp.class, "monks_1").contains("adventurer"), "the request no longer repeats the intro");
	}

	// ------------------------------------------------------------------------------ helpers

	private Imp imp;

	private Hero placeHeroAndImpQuest(boolean monks) throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, 2 * 8 + 2);
		StubSprite.attach(hero);
		imp = HeadlessLevel.at(new Imp(), 2 * 8 + 3);
		Dungeon.level.mobs.add(imp);
		StubSprite.attach(imp);
		questField("alternative").setBoolean(null, monks);
		return hero;
	}

	/** Talks to the imp with no quest given yet and returns the text of the window it opens. */
	private String startQuest() throws Exception {
		List<Callback> posted;
		try (RenderThreadCalls calls = RenderThreadCalls.capture()) {
			imp.interact(Dungeon.hero);
			posted = calls.callbacks();
		}
		assertEquals(1, posted.size(), "talking to the imp opens one window");
		assertTrue(questField("given").getBoolean(null), "precondition: the quest is given");
		return (String) RenderThreadCalls.captured(posted.get(0), "text");
	}

	private static Field questField(String name) throws Exception {
		Field f = Imp.Quest.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	/** Skips the constructor, which loads the sprite's texture. */
	private static <T> T allocate(Class<T> type) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return type.cast(unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type));
	}

	/** An ImpSprite with no texture that links to its char without a camera or scene. */
	static class HeadlessImpSprite extends ImpSprite {
		void skipHealthBar() throws Exception {
			health = allocate(CharHealthIndicator.class);
		}

		@Override public void place(int cell) {}
		@Override public void turnTo(int from, int to) {}
	}
}
