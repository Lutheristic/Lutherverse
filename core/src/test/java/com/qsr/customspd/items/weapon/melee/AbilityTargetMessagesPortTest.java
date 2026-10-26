package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.quest.Pickaxe;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 969610dfd (v2.4.0, "adjusted some weapon ability error messages"): the weapon abilities'
 * one "That target can't be reached." becomes "That target isn't in range." or, where the cell is
 * taken, "That location is occupied." The sneak now checks range before occupancy, so an occupied
 * cell out of range reads as out of range. The old key is gone from code and from every bundle.
 */
class AbilityTargetMessagesPortTest {

	private static final int W = 12;

	private static final String OUT_OF_RANGE = GLog.WARNING + "That target isn't in range.";
	private static final String OCCUPIED = GLog.WARNING + "That location is occupied.";

	private static final String OLD_KEY = "ability_bad_position";
	private static final String RANGE_KEY = "items.weapon.melee.meleeweapon.ability_target_range=";
	private static final String OCCUPIED_KEY = "items.weapon.melee.meleeweapon.ability_occupied=";

	private HeadlessLevel level;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void listen() {
		log.clear();
		GLog.update.add(logListener);
	}

	@AfterEach
	void tearDown() {
		GLog.update.remove(logListener);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Hero heroAt(int cell, MeleeWeapon weapon) {
		level = HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, cell);
		hero.belongings.weapon = weapon;
		Buff.affect(hero, MeleeWeapon.Charger.class);
		return hero;
	}

	private Rat ratAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.HP = rat.HT = 1000;
		StubSprite.attach(rat);
		level.mobs.add(rat);
		level.heroFOV[cell] = true;
		return rat;
	}

	@Test
	void sneakOntoAnOccupiedCellSaysTheCellIsOccupied() {
		int start = 4 * W + 2;
		Dagger dagger = new Dagger();
		Hero hero = heroAt(start, dagger);
		ratAt(start + 3);
		int charges = hero.buff(MeleeWeapon.Charger.class).charges;

		dagger.duelistAbility(hero, start + 3);

		assertEquals(start, hero.pos);
		assertEquals(charges, hero.buff(MeleeWeapon.Charger.class).charges, "a refused sneak costs no charge");
		assertEquals(1, log.size(), "log was " + log);
		assertEquals(OCCUPIED, log.get(0));
	}

	@Test
	void sneakChecksRangeBeforeOccupancy() {
		int start = 4 * W + 2;
		Dagger dagger = new Dagger();
		Hero hero = heroAt(start, dagger);
		ratAt(start + 6); // occupied and one past the dagger's range of 5

		dagger.duelistAbility(hero, start + 6);

		assertEquals(start, hero.pos);
		assertEquals(1, log.size(), "log was " + log);
		assertEquals(OUT_OF_RANGE, log.get(0));
	}

	@Test
	void attacksOutOfReachSayTheTargetIsOutOfRange() {
		int start = 4 * W + 2;
		Sword sword = new Sword();
		Hero hero = heroAt(start, sword);
		Rat rat = ratAt(start + 2);

		sword.duelistAbility(hero, rat.pos);
		assertEquals(rat.HT, rat.HP, "a sword cannot reach two cells");
		assertEquals(OUT_OF_RANGE, log.get(log.size() - 1));

		MeleeWeapon pickaxe = new Pickaxe(); // its override is protected in another package
		hero.belongings.weapon = pickaxe;
		log.clear();
		pickaxe.duelistAbility(hero, rat.pos);
		assertEquals(rat.HT, rat.HP, "nor can a pickaxe");
		assertEquals(OUT_OF_RANGE, log.get(log.size() - 1));
	}

	@Test
	void aLungePastItsReachSaysTheTargetIsOutOfRange() {
		int start = 4 * W + 2;
		Rapier rapier = new Rapier();
		Hero hero = heroAt(start, rapier);
		Rat rat = ratAt(start + 4);

		rapier.duelistAbility(hero, rat.pos);

		assertEquals(start, hero.pos);
		assertEquals(OUT_OF_RANGE, log.get(log.size() - 1));
	}

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	@Test
	void noCodeOrBundleUsesTheOldKey() throws IOException {
		Path java = Paths.get("..", "java");
		assertTrue(Files.isDirectory(java.resolve("com/qsr/customspd/items/weapon/melee")),
				"fixture: core/src/main/java not found from " + new File(".").getAbsolutePath());
		List<Path> users;
		try (Stream<Path> files = Files.walk(java)) {
			users = files.filter(p -> p.toString().endsWith(".java")).filter(p -> {
				try {
					return new String(Files.readAllBytes(p), StandardCharsets.UTF_8).contains(OLD_KEY);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
		}
		assertTrue(users.isEmpty(), "still referencing " + OLD_KEY + ": " + users);

		File[] bundles = new File("messages/items").listFiles((d, n) -> n.startsWith("items") && n.endsWith(".properties"));
		assertTrue(bundles != null && bundles.length > 1, "fixture: item bundles not found");
		for (File bundle : bundles) {
			String text = new String(Files.readAllBytes(bundle.toPath()), StandardCharsets.UTF_8);
			assertFalse(text.contains(OLD_KEY), bundle.getName() + " still has " + OLD_KEY);
			assertTrue(text.contains("\n" + RANGE_KEY), bundle.getName() + " lacks " + RANGE_KEY);
			assertTrue(text.contains("\n" + OCCUPIED_KEY), bundle.getName() + " lacks " + OCCUPIED_KEY);
		}
	}
}
