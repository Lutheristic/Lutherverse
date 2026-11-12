package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.bombs.FlashBangBomb;
import com.qsr.customspd.items.bombs.SmokeBomb;
import com.qsr.customspd.modding.ExtraItemSpawn;
import com.qsr.customspd.modding.HeroConfig;
import com.qsr.customspd.test.HeadlessRun;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A pack installed before the v2.5.3 bomb renames still names bombs.Flashbang and
 * bombs.ShockBomb. Its items resolve through the same aliases old saves use, so starting a hero
 * or dropping loot from such a pack gives the renamed bombs instead of crashing.
 */
class RenamedPackItemTest {

	private static final long SEED = 20261112L;

	/** A rat whose loot is a pack config entry. */
	private static Rat ratDropping(String json) {
		final ExtraItemSpawn spawn = Json.Default.decodeFromString(ExtraItemSpawn.Companion.serializer(), json);
		return new Rat() {
			{
				loot = spawn;
			}
		};
	}

	@Test
	void aHeroKitNamingTheOldBombsStillGetsThem() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			ShatteredPixelDungeon.registerBundleAliases();
			Hero hero = Dungeon.hero;
			assertNull(hero.belongings.getItem(SmokeBomb.class), "fixture: the warrior starts without bombs");
			assertNull(hero.belongings.getItem(FlashBangBomb.class), "fixture: the warrior starts without bombs");

			HeroConfig config = Json.Default.decodeFromString(HeroConfig.Companion.serializer(),
					"{\"items\":[{\"type\":\"bombs.Flashbang\",\"quantity\":2},{\"type\":\"bombs.ShockBomb\",\"quantity\":3}]}");
			HeroClass.setUp(config, hero);

			SmokeBomb smoke = hero.belongings.getItem(SmokeBomb.class);
			FlashBangBomb flash = hero.belongings.getItem(FlashBangBomb.class);
			assertNotNull(smoke, "bombs.Flashbang is now the smoke bomb");
			assertNotNull(flash, "bombs.ShockBomb is now the flashbang");
			assertEquals(2, smoke.quantity());
			assertEquals(3, flash.quantity());
		}
	}

	@Test
	void lootNamingTheOldBombsStillDropsThem() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			ShatteredPixelDungeon.registerBundleAliases();

			Item smoke = ratDropping("{\"type\":\"bombs.Flashbang\"}").createLoot();
			Item flash = ratDropping("{\"type\":\"bombs.ShockBomb\",\"quantity\":2}").createLoot();

			assertTrue(smoke instanceof SmokeBomb, "bombs.Flashbang drops a smoke bomb, got " + smoke);
			assertTrue(flash instanceof FlashBangBomb, "bombs.ShockBomb drops a flashbang, got " + flash);
			assertEquals(2, flash.quantity());
		}
	}
}
