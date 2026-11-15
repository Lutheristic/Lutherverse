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

package com.qsr.customspd.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.blobs.Foliage;
import com.qsr.customspd.actors.blobs.SacrificialFire;
import com.qsr.customspd.actors.blobs.WaterOfAwareness;
import com.qsr.customspd.actors.blobs.WaterOfHealth;
import com.qsr.customspd.actors.mobs.DemonSpawner;
import com.qsr.customspd.actors.mobs.Statue;
import com.qsr.customspd.actors.mobs.npcs.Blacksmith;
import com.qsr.customspd.actors.mobs.npcs.Ghost;
import com.qsr.customspd.actors.mobs.npcs.Imp;
import com.qsr.customspd.actors.mobs.npcs.ImpShopkeeper;
import com.qsr.customspd.actors.mobs.npcs.RatKing;
import com.qsr.customspd.actors.mobs.npcs.Shopkeeper;
import com.qsr.customspd.actors.mobs.npcs.Wandmaker;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.keys.Key;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.rooms.special.WeakFloorRoom;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.modding.LevelScheme;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.sprites.BlacksmithSprite;
import com.qsr.customspd.sprites.GhostSprite;
import com.qsr.customspd.sprites.ImpSprite;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.sprites.RatKingSprite;
import com.qsr.customspd.sprites.ShopkeeperSprite;
import com.qsr.customspd.sprites.SpawnerSprite;
import com.qsr.customspd.sprites.StatueSprite;
import com.qsr.customspd.sprites.WandmakerSprite;
import com.qsr.customspd.ui.IconUtils;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Notes {
	
	//floors are keyed by name; a floor that is not in the current layout has no depth
	public static final int UNKNOWN_DEPTH = Integer.MIN_VALUE;

	public static int depthOf( String levelName ){
		if (levelName == null || Dungeon.layout == null) return UNKNOWN_DEPTH;
		LevelScheme scheme = Dungeon.layout.getDungeon().get(levelName);
		if (scheme == null) return UNKNOWN_DEPTH;
		return scheme.getDepth();
	}

	//deepest floor first, floors outside the layout last, then by name
	public static int compareFloors( String a, String b ){
		int result = Integer.compare(depthOf(b), depthOf(a));
		if (result != 0) return result;
		if (a == null) return b == null ? 0 : 1;
		if (b == null) return -1;
		return a.compareTo(b);
	}

	public static abstract class Record implements Comparable<Record>, Bundlable {
		
		protected String levelName;

		public String levelName(){
			return levelName;
		}

		public int depth(){
			return depthOf(levelName());
		}

		public Image icon() { return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_STAIRS)); }

		public Visual secondIcon() { return null; }

		public int quantity() { return 1; }

		protected abstract int order();

		public abstract String title();
		
		public abstract String desc();
		
		@Override
		public abstract boolean equals(Object obj);

		@Override
		public int compareTo( Record another ) {
			return compareFloors(levelName(), another.levelName());
		}
		
		private static final String LEVEL_NAME = "levelName";
		
		@Override
		public void restoreFromBundle( Bundle bundle ) {
			levelName = bundle.getString(LEVEL_NAME);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			bundle.put(LEVEL_NAME, levelName );
		}
	}
	
	public enum Landmark {
		CHASM_FLOOR,
		WATER_FLOOR,
		GRASS_FLOOR,
		DARK_FLOOR,
		LARGE_FLOOR,
		TRAPS_FLOOR,
		SECRETS_FLOOR,

		SHOP,
		ALCHEMY,
		GARDEN,
		DISTANT_WELL,
		WELL_OF_HEALTH,
		WELL_OF_AWARENESS,
		SACRIFICIAL_FIRE,
		STATUE,

		GHOST,
		RAT_KING,
		WANDMAKER,
		TROLL,
		IMP,

		DEMON_SPAWNER;
	}
	
	public static class LandmarkRecord extends Record {
		
		protected Landmark landmark;
		
		public LandmarkRecord() {}
		
		public LandmarkRecord(Landmark landmark, String levelName ) {
			this.landmark = landmark;
			this.levelName = levelName;
		}
		
		@Override
		public Image icon(){
			switch (landmark){
				default:
					return super.icon();

				case CHASM_FLOOR:
					return IconUtils.getLarge(Level.Feeling.CHASM);
				case WATER_FLOOR:
					return IconUtils.getLarge(Level.Feeling.WATER);
				case GRASS_FLOOR:
					return IconUtils.getLarge(Level.Feeling.GRASS);
				case DARK_FLOOR:
					return IconUtils.getLarge(Level.Feeling.DARK);
				case LARGE_FLOOR:
					return IconUtils.getLarge(Level.Feeling.LARGE);
				case TRAPS_FLOOR:
					return IconUtils.getLarge(Level.Feeling.TRAPS);
				case SECRETS_FLOOR:
					return IconUtils.getLarge(Level.Feeling.SECRETS);

				case SHOP:
					if (isImpShop())    return new Image(new ImpSprite());
					else                return new Image(new ShopkeeperSprite());
				case ALCHEMY:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_ALCHEMY));
				case GARDEN:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_GRASS));
				case DISTANT_WELL:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_DISTANT_WELL));
				case WELL_OF_HEALTH:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_WELL_HEALTH));
				case WELL_OF_AWARENESS:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_WELL_AWARENESS));
				case SACRIFICIAL_FIRE:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_SACRIFICE_ALTAR));
				case STATUE:
					return new Image(new StatueSprite());

				case GHOST:
					return new Image(new GhostSprite());
				case RAT_KING:
					return new Image(new RatKingSprite());
				case WANDMAKER:
					return new Image(new WandmakerSprite());
				case TROLL:
					return new Image(new BlacksmithSprite());
				case IMP:
					return new Image(new ImpSprite());

				case DEMON_SPAWNER:
					return new Image(new SpawnerSprite());
			}
		}

		@Override
		public String title() {
			switch (landmark) {
				default:            return Messages.get(Landmark.class, landmark.name());
				case CHASM_FLOOR:   return Messages.get(Level.Feeling.class, "chasm_title");
				case WATER_FLOOR:   return Messages.get(Level.Feeling.class, "water_title");
				case GRASS_FLOOR:   return Messages.get(Level.Feeling.class, "grass_title");
				case DARK_FLOOR:    return Messages.get(Level.Feeling.class, "dark_title");
				case LARGE_FLOOR:   return Messages.get(Level.Feeling.class, "large_title");
				case TRAPS_FLOOR:   return Messages.get(Level.Feeling.class, "traps_title");
				case SECRETS_FLOOR: return Messages.get(Level.Feeling.class, "secrets_title");
			}
		}

		@Override
		public String desc() {
			switch (landmark) {
				default:            return "";

				case CHASM_FLOOR:   return Messages.get(Level.Feeling.class, "chasm_desc");
				case WATER_FLOOR:   return Messages.get(Level.Feeling.class, "water_desc");
				case GRASS_FLOOR:   return Messages.get(Level.Feeling.class, "grass_desc");
				case DARK_FLOOR:    return Messages.get(Level.Feeling.class, "dark_desc");
				case LARGE_FLOOR:   return Messages.get(Level.Feeling.class, "large_desc");
				case TRAPS_FLOOR:   return Messages.get(Level.Feeling.class, "traps_desc");
				case SECRETS_FLOOR: return Messages.get(Level.Feeling.class, "secrets_desc");

				case SHOP:
					if (isImpShop())    return Messages.get(ImpShopkeeper.class, "desc");
					else                return Messages.get(Shopkeeper.class, "desc");
				case ALCHEMY:           return Messages.get(Level.class, "alchemy_desc");
				case GARDEN:            return Messages.get(Foliage.class, "desc");
				case DISTANT_WELL:      return Messages.get(WeakFloorRoom.HiddenWell.class, "desc");
				case WELL_OF_HEALTH:    return Messages.get(WaterOfHealth.class, "desc");
				case WELL_OF_AWARENESS: return Messages.get(WaterOfAwareness.class, "desc");
				case SACRIFICIAL_FIRE:  return Messages.get(SacrificialFire.class, "desc");
				case STATUE:            return Messages.get(Statue.class, "desc");

				case GHOST:         return Messages.get(Ghost.class, "desc");
				case RAT_KING:      return Messages.get(RatKing.class, "desc");
				case WANDMAKER:     return Messages.get(Wandmaker.class, "desc");
				case TROLL:         return Messages.get(Blacksmith.class, "desc");
				case IMP:           return Messages.get(Imp.class, "desc");

				case DEMON_SPAWNER: return Messages.get(DemonSpawner.class, "desc");
			}
		}

		//upstream checks depth 20; here the imp shop is any floor built as an imp shop level
		private boolean isImpShop(){
			if (Dungeon.layout == null) return false;
			LevelScheme s = Dungeon.layout.getDungeon().get(levelName());
			if (s == null) return false;
			String l = s.getLayout();
			return "CityBossLevel".equals(l) || "LastShopLevel".equals(l);
		}

		@Override
		protected int order(){
			return landmark.ordinal();
		}
		
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof LandmarkRecord)
					&& landmark == ((LandmarkRecord) obj).landmark
					&& levelName().equals(((LandmarkRecord) obj).levelName());
		}
		
		private static final String LANDMARK	= "landmark";
		
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			landmark = Landmark.valueOf(bundle.getString(LANDMARK));
		}
		
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( LANDMARK, landmark.name() );
		}
	}
	
	public static class KeyRecord extends Record {
		
		protected Key key;
		
		public KeyRecord() {}
		
		public KeyRecord( Key key ){
			this.key = key;
		}
		
		@Override
		public String levelName() {
			return key.levelName;
		}
		
		@Override
		public Image icon() {
			return new ItemSprite(key);
		}

		@Override
		public Visual secondIcon() {
			if (quantity() > 1){
				BitmapText text = new BitmapText(Integer.toString(quantity()), PixelScene.pixelFont);
				text.measure();
				return text;
			} else {
				return null;
			}
		}

		@Override
		public String title() {
			return key.title();
		}

		@Override
		public String desc() {
			return key.desc();
		}
		
		public Class<? extends Key> type(){
			return key.getClass();
		}

		@Override
		protected int order() {
			return 1000 + Generator.Category.order(key);
		}
		
		@Override
		public int quantity(){
			return key.quantity();
		}
		
		public void quantity(int num){
			key.quantity(num);
		}
		
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof KeyRecord)
					&& key.isSimilar(((KeyRecord) obj).key);
		}
		
		private static final String KEY	= "key";
		
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			key = (Key) bundle.get(KEY);
		}
		
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( KEY, key );
		}
	}
	
	public enum CustomType {
		TEXT,
		DEPTH,
		ITEM,
	}

	public static class CustomRecord extends Record {

		protected CustomType type;

		protected int ID = -1;
		protected Class itemClass;

		protected String title;
		protected String body;

		public CustomRecord() {}

		public CustomRecord(String title, String desc) {
			type = CustomType.TEXT;
			this.title = title;
			body = desc;
		}

		//floor notes are keyed by floor name, their depth comes from the layout
		public CustomRecord(String levelName, String title, String desc) {
			type = CustomType.DEPTH;
			this.levelName = levelName;
			this.title = title;
			body = desc;
		}

		public CustomRecord(Item item, String title, String desc) {
			type = CustomType.ITEM;
			itemClass = item.getClass();
			this.title = title;
			body = desc;
		}

		public void assignID(){
			if (ID == -1) {
				ID = nextCustomID++;
			}
		}

		public int ID(){
			return ID;
		}

		//upstream returns 0 for text and item notes, but 0 is a real floor here
		@Override
		public int depth() {
			if (type == CustomType.DEPTH){
				return depthOf(levelName);
			} else {
				return UNKNOWN_DEPTH;
			}
		}

		@Override
		public Image icon() {
			switch (type){
				case TEXT: default:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_SCROLL_COLOR));
				case DEPTH:
					return new Image(Asset.getAssetFilePath(GeneralAsset.ICON_STAIRS));
				case ITEM:
					Item i = (Item) Reflection.newInstance(itemClass);
					return new ItemSprite(i);
			}
		}

		@Override
		public Visual secondIcon() {
			switch (type){
				case TEXT: default:
					return null;
				case DEPTH:
					int depth = depth();
					BitmapText text = new BitmapText(depth == UNKNOWN_DEPTH ? "?" : Integer.toString(depth), PixelScene.pixelFont);
					text.measure();
					return text;
				case ITEM:
					Item item = (Item) Reflection.newInstance(itemClass);
					if (item.isIdentified() && item.icon != GeneralAsset.SOMETHING) {
						return new Image(Asset.getAssetFilePath(item.icon));
					}
					return null;
			}
		}

		@Override
		protected int order() {
			return 2000 + ID;
		}

		public void editText(String title, String desc){
			this.title = title;
			this.body = desc;
		}

		@Override
		public String title() {
			return title;
		}

		@Override
		public String desc() {
			return body;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof CustomRecord && ((CustomRecord) obj).ID == ID;
		}

		private static final String TYPE        = "type";
		private static final String ID_NUMBER   = "id_number";

		private static final String ITEM_CLASS   = "item_class";

		private static final String TITLE       = "title";
		private static final String BODY        = "body";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TYPE, type);
			bundle.put(ID_NUMBER, ID);
			if (itemClass != null) bundle.put(ITEM_CLASS, itemClass);
			bundle.put(TITLE, title);
			bundle.put(BODY, body);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			type = bundle.getEnum(TYPE, CustomType.class);
			ID = bundle.getInt(ID_NUMBER);

			if (bundle.contains(ITEM_CLASS)) itemClass = bundle.getClass(ITEM_CLASS);

			title = bundle.getString(TITLE);
			body = bundle.getString(BODY);
		}
	}
	
	private static ArrayList<Record> records;
	
	public static void reset() {
		records = new ArrayList<>();
	}
	
	private static final String RECORDS	        = "records";
	private static final String NEXT_CUSTOM_ID	= "next_custom_id";

	protected static int nextCustomID = 0;

	public static void storeInBundle( Bundle bundle ) {
		bundle.put( RECORDS, records );
		bundle.put( NEXT_CUSTOM_ID, nextCustomID );
	}
	
	public static void restoreFromBundle( Bundle bundle ) {
		records = new ArrayList<>();
		nextCustomID = bundle.getInt( NEXT_CUSTOM_ID );
		for (Bundlable rec : bundle.getCollection( RECORDS ) ) {
			records.add( (Record) rec );
		}
	}
	
	public static boolean add( Landmark landmark ) {
		LandmarkRecord l = new LandmarkRecord( landmark, Dungeon.levelName );
		if (!records.contains(l)) {
			boolean result = records.add(new LandmarkRecord(landmark, Dungeon.levelName));
			Collections.sort(records, comparator);
			return result;
		}
		return false;
	}
	
	public static boolean contains( Landmark landmark ){
		return records.contains(new LandmarkRecord(landmark, Dungeon.levelName));
	}

	public static boolean remove( Landmark landmark ) {
		return records.remove( new LandmarkRecord(landmark, Dungeon.levelName) );
	}
	
	public static boolean add( Key key ){
		KeyRecord k = new KeyRecord(key);
		if (!records.contains(k)){
			boolean result = records.add(k);
			Collections.sort(records, comparator);
			return result;
		} else {
			k = (KeyRecord) records.get(records.indexOf(k));
			k.quantity(k.quantity() + key.quantity());
			return true;
		}
	}
	
	public static boolean remove( Key key ){
		KeyRecord k = new KeyRecord( key );
		if (records.contains(k)){
			Catalog.countUses(key.getClass(), key.quantity());
			k = (KeyRecord) records.get(records.indexOf(k));
			k.quantity(k.quantity() - key.quantity());
			if (k.quantity() <= 0){
				records.remove(k);
			}
			return true;
		}
		return false;
	}
	
	public static int keyCount( Key key ){
		KeyRecord k = new KeyRecord( key );
		if (records.contains(k)){
			k = (KeyRecord) records.get(records.indexOf(k));
			return k.quantity();
		} else {
			return 0;
		}
	}
	
	public static boolean add( CustomRecord rec ){
		rec.assignID();
		if (!records.contains(rec)){
			boolean result = records.add(rec);
			Collections.sort(records, comparator);
			return result;
		}
		return false;
	}

	public static boolean remove( CustomRecord rec ){
		if (records.contains(rec)){
			records.remove(rec);
			return true;
		}
		return false;
	}
	
	public static <T extends Record> ArrayList<T> getRecords( Class<T> recordType ){
		ArrayList<T> filtered = new ArrayList<>();
		for (Record rec : records){
			if (recordType.isInstance(rec)){
				filtered.add((T)rec);
			}
		}
		return filtered;
	}
	
	public static ArrayList<Record> getRecords( String levelName ){
		ArrayList<Record> filtered = new ArrayList<>();
		for (Record rec : records){
			if (!(rec instanceof CustomRecord) && levelName != null && levelName.equals(rec.levelName())){
				filtered.add(rec);
			}
		}

		Collections.sort(filtered, comparator);

		return filtered;
	}
	
	public static CustomRecord findCustomRecord( int ID ){
		for (Record rec : records){
			if (rec instanceof CustomRecord && ((CustomRecord) rec).ID == ID) {
				return (CustomRecord) rec;
			}
		}
		return null;
	}

	public static CustomRecord findCustomRecord( Class itemClass ){
		for (Record rec : records){
			if (rec instanceof CustomRecord && ((CustomRecord) rec).itemClass == itemClass) {
				return (CustomRecord) rec;
			}
		}
		return null;
	}

	public static int customRecordLimit(){
		return 5;
	}

	private static final Comparator<Record> comparator = new Comparator<Record>() {
		@Override
		public int compare(Record r1, Record r2) {
			return r1.order() - r2.order();
		}
	};
	
}
