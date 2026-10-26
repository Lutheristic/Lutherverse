package com.qsr.customspd.actors.hero.abilities;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.hero.abilities.duelist.Challenge;
import com.qsr.customspd.actors.hero.abilities.duelist.Feint;
import com.qsr.customspd.actors.hero.abilities.rogue.SmokeBomb;
import com.qsr.customspd.actors.hero.abilities.warrior.HeroicLeap;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.ClassArmor;
import com.qsr.customspd.items.armor.DuelistArmor;
import com.qsr.customspd.items.armor.RogueArmor;
import com.qsr.customspd.items.armor.WarriorArmor;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 70f5b796e (v2.1.1, "big consistency pass on rooted"): a rooted hero cannot leap, blink
 * or step away with an armor ability, and the refusal shakes the screen, though a smoke bomb on her
 * own cell still works. Upstream 00c45dc7f lets Challenge reach across hazards and land a flying
 * hero on them, and lets Feint step onto any cell that is not solid; e4b704e6a stops Challenge
 * reaching through magical fire.
 */
class RootedArmorAbilitiesPortTest {

	private static final int W = 16;
	private static final int H = 10;
	private static final int START = 4 * W + 2;

	private HeadlessLevel level;
	private Hero hero;
	private Camera prevCamera;
	private int shakes;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevCamera = Camera.main;
		shakes = 0;
		Camera.main = new Camera(0, 0, 100, 100, 1f) {
			@Override
			public void shake(float magnitude, float duration) {
				shakes++;
			}
		};
		log.clear();
		GLog.update.add(logListener);

		level = HeadlessLevel.install(W, H);
		hero = new Hero();
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, START);
		Arrays.fill(level.heroFOV, true);
		// as Hero.act sets it
		hero.fieldOfView = level.heroFOV;
	}

	@AfterEach
	void tearDown() {
		GLog.update.remove(logListener);
		Camera.main = prevCamera;
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Rat ratAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		StubSprite.attach(rat);
		level.mobs.add(rat);
		return rat;
	}

	/** Fills column x, wall to wall, with the given terrain. */
	private void column(int x, int terrain) {
		for (int y = 1; y < H - 1; y++) {
			Level.set(y * W + x, terrain, level);
		}
	}

	private static ClassArmor charged(ClassArmor armor) {
		armor.charge = 100;
		return armor;
	}

	private void activate(ArmorAbility ability, ClassArmor armor, int target) {
		ability.activate(armor, hero, target);
	}

	/**
	 * Runs an ability that goes through. Headless, it stops where it first needs the game scene,
	 * the puff of smoke at CellEmitter.get or the afterimage's GameScene.add; everything the
	 * ability decides happens before that. Returns whether it got there.
	 */
	private boolean activateUpToTheScene(ArmorAbility ability, ClassArmor armor, int target) {
		try {
			activate(ability, armor, target);
			return false;
		} catch (NullPointerException e) {
			for (StackTraceElement el : e.getStackTrace()) {
				if ((el.getClassName().endsWith(".CellEmitter") && el.getMethodName().equals("get"))
						|| (el.getClassName().endsWith(".GameScene") && el.getMethodName().equals("add"))) {
					return true;
				}
			}
			throw e;
		}
	}

	private static String warning(Class<?> cls, String key) {
		return GLog.WARNING + Messages.get(cls, key);
	}

	@Test
	void aRootedDuelistCannotCloseTheGap() {
		hero.heroClass = HeroClass.DUELIST;
		hero.talents.get(0).put(Talent.CLOSE_THE_GAP, 1);
		hero.rooted = true;
		Rat rat = ratAt(START + 6);
		ClassArmor armor = charged(new DuelistArmor());

		activate(new Challenge(), armor, rat.pos);

		assertEquals(START, hero.pos, "a rooted duelist does not blink");
		assertEquals(1, shakes);
		assertTrue(log.contains(warning(Challenge.class, "distant_target")), "log was " + log);
		assertEquals(100f, armor.charge, 0.0001f, "the refused challenge costs nothing");
		assertNull(hero.buff(Challenge.DuelParticipant.class));
	}

	@Test
	void aTargetAcrossAChasmCanBeChallenged() {
		hero.heroClass = HeroClass.DUELIST;
		column(4, Terrain.CHASM);
		Rat rat = ratAt(START + 4);

		activate(new Challenge(), charged(new DuelistArmor()), rat.pos);

		assertTrue(log.isEmpty(), "log was " + log);
		assertNotNull(hero.buff(Challenge.DuelParticipant.class), "the duel starts across the chasm");
		assertEquals(START, hero.pos);
	}

	@Test
	void aBlinkDoesNotLandADuelistWhoCannotFlyInAChasm() {
		hero.heroClass = HeroClass.DUELIST;
		hero.talents.get(0).put(Talent.CLOSE_THE_GAP, 1);
		column(3, Terrain.CHASM);
		column(4, Terrain.CHASM);
		Rat rat = ratAt(START + 7);

		activate(new Challenge(), charged(new DuelistArmor()), rat.pos);

		assertEquals(START, hero.pos, "the only cells in blink range nearer the target are chasm");
		assertTrue(log.contains(warning(Challenge.class, "distant_target")), "log was " + log);
		assertEquals(0, shakes, "only a rooted refusal shakes");
	}

	@Test
	void aFlyingDuelistBlinksOntoAChasm() {
		hero.heroClass = HeroClass.DUELIST;
		hero.talents.get(0).put(Talent.CLOSE_THE_GAP, 1);
		hero.flying = true;
		column(3, Terrain.CHASM);
		column(4, Terrain.CHASM);
		Rat rat = ratAt(START + 7);

		boolean blinked = activateUpToTheScene(new Challenge(), charged(new DuelistArmor()), rat.pos);

		assertEquals(START + 2, hero.pos, "a flier lands on the chasm nearest the target");
		assertTrue(log.isEmpty(), "log was " + log);
		assertTrue(blinked, "the blink goes on to its puff of smoke");
	}

	@Test
	void magicalFireBlocksAChallenge() {
		hero.heroClass = HeroClass.DUELIST;
		// magical fire leaves the floor's own terrain but marks its cells impassable
		for (int y = 1; y < H - 1; y++) {
			level.passable[y * W + 4] = false;
		}
		Rat rat = ratAt(START + 4);

		activate(new Challenge(), charged(new DuelistArmor()), rat.pos);

		assertTrue(log.contains(warning(Challenge.class, "unreachable_target")), "log was " + log);
		assertNull(hero.buff(Challenge.DuelParticipant.class));
	}

	@Test
	void aRootedDuelistCannotFeint() {
		hero.heroClass = HeroClass.DUELIST;
		hero.rooted = true;
		ClassArmor armor = charged(new DuelistArmor());

		activate(new Feint(), armor, START + 1);

		assertEquals(START, hero.pos);
		assertEquals(1, shakes);
		assertTrue(log.contains(warning(Feint.class, "bad_location")), "log was " + log);
		assertEquals(100f, armor.charge, 0.0001f);
	}

	@Test
	void feintRejectsASolidCell() {
		hero.heroClass = HeroClass.DUELIST;
		Level.set(START + 1, Terrain.WALL, level);
		ClassArmor armor = charged(new DuelistArmor());

		activate(new Feint(), armor, START + 1);

		assertEquals(START, hero.pos);
		assertTrue(log.contains(warning(Feint.class, "bad_location")), "log was " + log);
		assertEquals(100f, armor.charge, 0.0001f);
		assertEquals(0, shakes);
	}

	@Test
	void aDuelistCanFeintOntoAChasm() {
		hero.heroClass = HeroClass.DUELIST;
		// flying, so the step leaves her on this floor rather than falling through it
		hero.flying = true;
		Level.set(START + 1, Terrain.CHASM, level);

		boolean feinted = activateUpToTheScene(new Feint(), charged(new DuelistArmor()), START + 1);

		assertTrue(log.isEmpty(), "a chasm is not solid, so it is no bad location; log was " + log);
		assertEquals(START + 1, hero.pos);
		assertTrue(feinted, "the feint goes on to leave its afterimage");
	}

	@Test
	void aRootedRogueCanSmokeBombWhereSheStands() {
		hero.heroClass = HeroClass.ROGUE;
		hero.rooted = true;
		ClassArmor armor = charged(new RogueArmor());

		boolean bombed = activateUpToTheScene(new SmokeBomb(), armor, hero.pos);

		assertEquals(0, shakes, "roots only refuse a smoke bomb that would move her");
		assertTrue(log.isEmpty(), "log was " + log);
		assertTrue(armor.charge < 100f, "the bomb is paid for");
		assertEquals(START, hero.pos);
		assertTrue(bombed, "the bomb goes on to its puff of smoke");
	}

	@Test
	void aRootedRogueCannotSmokeBombAway() {
		hero.heroClass = HeroClass.ROGUE;
		hero.rooted = true;
		ClassArmor armor = charged(new RogueArmor());

		activate(new SmokeBomb(), armor, START + 3);

		assertEquals(START, hero.pos);
		assertEquals(1, shakes);
		assertEquals(100f, armor.charge, 0.0001f);
	}

	@Test
	void aRootedWarriorCannotHeroicLeap() {
		hero.heroClass = HeroClass.WARRIOR;
		hero.rooted = true;
		ClassArmor armor = charged(new WarriorArmor());

		activate(new HeroicLeap(), armor, START + 3);

		assertEquals(START, hero.pos);
		assertEquals(1, shakes);
		assertEquals(100f, armor.charge, 0.0001f);
	}
}
