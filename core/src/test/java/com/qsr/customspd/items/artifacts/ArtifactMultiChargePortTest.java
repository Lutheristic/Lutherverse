package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 08fc0a23b (v2.4.0): when a turn or a recharge leaves an artifact holding two or more
 * whole charges of partial charge, all of them are converted at once instead of one per call.
 * The talisman also announces and clears its partial charge on the turn it fills.
 */
class ArtifactMultiChargePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		GameScene.updateItemDisplays = false;
	}

	private static Hero hero() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		hero.pos = 3 * 8 + 3;
		Dungeon.hero = hero;
		return hero;
	}

	private static <T extends Artifact> T wear(Hero hero, T artifact) {
		hero.belongings.artifact = artifact;
		artifact.activate(hero);
		return artifact;
	}

	@Test
	void cloakTurnConvertsEveryWholeCharge() {
		Hero hero = hero();
		CloakOfShadows cloak = wear(hero, new CloakOfShadows());
		cloak.charge = 0;
		cloak.partialCharge = 2.5f;
		cloak.passiveBuff.act();
		assertEquals(2, cloak.charge);
		assertEquals(0.5f, cloak.partialCharge, 0.05f);
	}

	@Test
	void roseTurnHealsTheGhostForEveryWholeCharge() throws Exception {
		Hero hero = hero();
		DriedRose rose = wear(hero, new DriedRose());
		DriedRose.GhostHero ghost = new DriedRose.GhostHero(rose);
		Field f = DriedRose.class.getDeclaredField("ghost");
		f.setAccessible(true);
		f.set(rose, ghost);
		ghost.HP = 1;
		rose.partialCharge = 2.5f;
		rose.passiveBuff.act();
		assertEquals(3, ghost.HP);
		assertEquals(0.5f, rose.partialCharge, 0.05f);
	}

	@Test
	void roseTurnConvertsEveryWholeCharge() {
		Hero hero = hero();
		DriedRose rose = wear(hero, new DriedRose());
		rose.charge = 0;
		rose.partialCharge = 2.5f;
		rose.passiveBuff.act();
		assertEquals(2, rose.charge);
		assertEquals(0.7f, rose.partialCharge, 0.05f);
	}

	@Test
	void chainsRechargeGivesEveryWholeCharge() {
		Hero hero = hero();
		EtherealChains chains = wear(hero, new EtherealChains());
		chains.charge = 0;
		chains.partialCharge = 0;
		chains.charge(hero, 4f);
		assertEquals(2, chains.charge);
		assertEquals(0f, chains.partialCharge, 0f);
	}

	@Test
	void chainsTurnConvertsEveryWholeCharge() {
		Hero hero = hero();
		EtherealChains chains = wear(hero, new EtherealChains());
		chains.charge = 0;
		chains.partialCharge = 2.5f;
		chains.passiveBuff.act();
		assertEquals(2, chains.charge);
		assertEquals(0.5f, chains.partialCharge, 0.05f);
	}

	@Test
	void hornRechargeGivesEveryWholeCharge() {
		Hero hero = hero();
		HornOfPlenty horn = wear(hero, new HornOfPlenty());
		horn.charge = 0;
		horn.partialCharge = 0;
		horn.charge(hero, 8f);
		assertEquals(2, horn.charge);
		assertEquals(0f, horn.partialCharge, 0f);
	}

	@Test
	void talismanTurnConvertsEveryWholeCharge() {
		Hero hero = hero();
		TalismanOfForesight talisman = wear(hero, new TalismanOfForesight());
		talisman.charge = 0;
		talisman.partialCharge = 2.5f;
		talisman.passiveBuff.act();
		assertEquals(2, talisman.charge);
		assertEquals(0.55f, talisman.partialCharge, 0.05f);
	}

	@Test
	void talismanAnnouncesAndClearsWhenATurnFillsIt() {
		Hero hero = hero();
		TalismanOfForesight talisman = wear(hero, new TalismanOfForesight());
		talisman.charge = talisman.chargeCap - 1;
		talisman.partialCharge = 0.99f;

		String full = GLog.POSITIVE + Messages.get(TalismanOfForesight.class, "full_charge");
		List<String> log = new ArrayList<>();
		Signal.Listener<String> listener = log::add;
		GLog.update.add(listener);
		try {
			talisman.passiveBuff.act();
		} finally {
			GLog.update.remove(listener);
		}

		assertEquals(talisman.chargeCap, talisman.charge);
		assertEquals(0f, talisman.partialCharge, 0f);
		int announcements = 0;
		for (String s : log) if (s.equals(full)) announcements++;
		assertEquals(1, announcements, "log was " + log);
	}

	@Test
	void hourglassTurnConvertsEveryWholeCharge() {
		Hero hero = hero();
		TimekeepersHourglass hourglass = wear(hero, new TimekeepersHourglass());
		hourglass.charge = 0;
		hourglass.partialCharge = 2.5f;
		hourglass.passiveBuff.act();
		assertEquals(2, hourglass.charge);
		assertEquals(0.5f, hourglass.partialCharge, 0.05f);
	}

	@Test
	void spellbookTurnConvertsEveryWholeChargeUpToTheCap() {
		Hero hero = hero();
		UnstableSpellbook book = wear(hero, new UnstableSpellbook());
		book.charge = book.chargeCap - 2;
		book.partialCharge = 2.5f;
		book.passiveBuff.act();
		assertEquals(book.chargeCap, book.charge);
		assertEquals(0f, book.partialCharge, 0f);
	}
}
