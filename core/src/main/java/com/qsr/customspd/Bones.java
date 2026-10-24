/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.qsr.customspd;

import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.artifacts.Artifact;
import com.qsr.customspd.items.remains.RemainsItem;
import com.qsr.customspd.items.weapon.missiles.MissileWeapon;
import com.qsr.customspd.modding.LevelScheme;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Bones {

	private static final String BONES_FILE	= "bones.dat";
	
	private static final String LEVEL	= "level";
	private static final String ITEM	= "item";
	private static final String HERO_CLASS	= "hero_class";

	private static String level = "surface";

	private static Item item;
	private static HeroClass heroClass;

	public static void leave() {

		//remains will usually drop on the floor the hero died on
		// but are capped at 5 floors above the lowest depth reached (even when ascending)
		level = cappedLevel(Math.max(Dungeon.depth, Statistics.deepestFloor-5));

		//daily runs do not interact with remains
		if (Dungeon.daily) {
			level = "surface";
			return;
		}

		item = pickItem(Dungeon.hero);
		heroClass = Dungeon.hero.heroClass;

		Bundle bundle = new Bundle();
		bundle.put( LEVEL, level);
		bundle.put( ITEM, item );
		bundle.put( HERO_CLASS, heroClass );

		try {
			FileUtils.bundleToFile( BONES_FILE, bundle );
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
	}

	//CPDU has no Dungeon.branch: remains are keyed by levelName, so the capped depth is mapped
	// back onto a level of the layout (the current one when it is not capped)
	private static String cappedLevel(int depth){
		if (depth == Dungeon.depth) {
			return Dungeon.levelName;
		}
		String name = String.valueOf(depth);
		LevelScheme scheme = Dungeon.layout.getDungeon().get(name);
		if (scheme != null && scheme.getDepth() == depth) {
			return name;
		}
		for (Map.Entry<String, LevelScheme> entry : Dungeon.layout.getDungeon().entrySet()) {
			if (entry.getValue().getDepth() == depth) {
				return entry.getKey();
			}
		}
		return Dungeon.levelName;
	}

	private static Item pickItem(Hero hero){
		Item item = null;

		//seeded runs don't leave items
		//This is to prevent using specific seeds to transport items to regular runs
		if (!Dungeon.customSeedText.isEmpty()){
			return null;
		}

		if (Random.Int(3) != 0) {
			switch (Random.Int(7)) {
				case 0:
					item = hero.belongings.weapon;
					//if the hero has two weapons (champion), pick the stronger one
					if (hero.belongings.secondWep != null &&
							(item == null || hero.belongings.secondWep.trueLevel() > item.trueLevel())){
						item = hero.belongings.secondWep;
						break;
					}
					break;
				case 1:
					item = hero.belongings.armor;
					break;
				case 2:
					item = hero.belongings.artifact;
					break;
				case 3:
					item = hero.belongings.misc;
					break;
				case 4:
					item = hero.belongings.ring;
					break;
				case 5: case 6:
					item = Dungeon.quickslot.randomNonePlaceholder();
					break;
			}
			if (item == null || !item.bones) {
				return pickItem(hero);
			}
		} else {

			Iterator<Item> iterator = hero.belongings.backpack.iterator();
			Item curItem;
			ArrayList<Item> items = new ArrayList<>();
			while (iterator.hasNext()){
				curItem = iterator.next();
				if (curItem.bones) {
					items.add(curItem);
				}
			}

			//if there are few items, there is an increasingly high chance of leaving nothing
			if (Random.Int(3) < items.size()) {
				item = Random.element(items);
				if (item.stackable){
					item.quantity(Random.NormalIntRange(1, (item.quantity() + 1) / 2));
					if (item.quantity() > 3){
						item.quantity(3);
					}
				}
			} else {
				item = null;
			}
		}
		
		return item;
	}

	public static ArrayList<Item> get() {
		//daily runs do not interact with remains
		if (Dungeon.daily){
			return null;
		}

		if (level.equals("surface")) {

			try {
				Bundle bundle = FileUtils.bundleFromFile(BONES_FILE);

				level = bundle.getString( LEVEL );
				if (!level.equals("surface") && !level.equals("0")) {
					if (bundle.contains(ITEM)) {
						item = (Item) bundle.get(ITEM);
					} else {
						item = null;
					}
					if (bundle.contains(HERO_CLASS)){
						heroClass = bundle.getEnum(HERO_CLASS, HeroClass.class);
					} else {
						heroClass = null;
					}
				}

				return get();

			} catch (IOException e) {
				return null;
			}

		} else {
			if (level.equals(Dungeon.levelName) && Dungeon.layout.getBones()) {

				//challenged or seeded runs don't get items from prior runs
				if (Dungeon.challenges != 0 || !Dungeon.customSeedText.isEmpty()){
					item = null;
				}

				Bundle emptyBones = new Bundle();
				emptyBones.put(LEVEL, 0);
				try {
					FileUtils.bundleToFile( BONES_FILE, emptyBones );
				} catch (IOException e) {
					ShatteredPixelDungeon.reportException(e);
				}
				level = "0";

				//Enforces artifact uniqueness
				if (item instanceof Artifact){
					if (Generator.removeArtifact(((Artifact)item).getClass())) {
						
						//generates a new artifact of the same type, always +0
						Artifact artifact = Reflection.newInstance(((Artifact)item).getClass());
						
						if (artifact != null){
							artifact.cursed = true;
							artifact.cursedKnown = true;
						}

						item = artifact;
						
					} else {
						item = new Gold(item.value());
					}
				}

				if (item != null) {
					if (item.isUpgradable() && !(item instanceof MissileWeapon)) {
						item.cursed = true;
						item.cursedKnown = true;
					}

					if (item.isUpgradable()) {
						//caps at +3
						if (item.level() > 3) {
							item.degrade(item.level() - 3);
						}
						//thrown weapons are always IDed, otherwise set unknown
						item.levelKnown = item instanceof MissileWeapon;
					}

					item.reset();
				}

				ArrayList<Item> result = new ArrayList<>();

				if (heroClass != null) {
					result.add(RemainsItem.get(heroClass));
					if (Dungeon.bossLevel()){
						Statistics.qualifiedForBossRemainsBadge = true;
					}
				}

				if (item != null) {
					result.add(item);
				}

				return result.isEmpty() ? null : result;
			} else {
				return null;
			}
		}
	}
}
