package com.qsr.customspd.items.remains;

import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 85ed79cd1 redrew the seal shard as several shards and made its text plural; the
 * wording here is v2.5.4's, which also fixed that commit's "shards ... looks".
 */
class SealShardsTextPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void theSealShardTextIsPlural() {
		SealShard shard = new SealShard();

		assertEquals("seal shards", shard.name());
		assertEquals("These tiny shards of red wax look like they came from the seal of a fallen Warrior."
				+ " You can still feel a bit of willpower clinging to the seal, you can use it to give yourself"
				+ " a bit of shielding. Doing this will destroy the seal shards however.", shard.desc());
	}
}
