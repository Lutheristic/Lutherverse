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

package com.qsr.customspd.windows;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDAction;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.armor.ClassArmor;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.exotic.ExoticPotion;
import com.qsr.customspd.items.rings.Ring;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.exotic.ExoticScroll;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.journal.Catalog;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.sprites.ItemSprite;
import com.qsr.customspd.ui.QuickRecipe;
import com.qsr.customspd.ui.RedButton;
import com.qsr.customspd.ui.RenderedTextBlock;
import com.qsr.customspd.ui.ScrollPane;
import com.qsr.customspd.ui.ScrollingListPane;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class WndJournal extends WndTabbed {

	public static final int WIDTH_P     = 126;
	public static final int HEIGHT_P    = 180;

	public static final int WIDTH_L     = 200;
	public static final int HEIGHT_L    = 130;

	private static final int ITEM_HEIGHT	= 18;

	private GuideTab guideTab;
	private AlchemyTab alchemyTab;
	private NotesTab notesTab;
	private CatalogTab catalogTab;
	private LoreTab loreTab;

	public static int last_index = 0;

	public WndJournal(){

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		int height = PixelScene.landscape() ? HEIGHT_L : HEIGHT_P;

		resize(width, height);

		guideTab = new GuideTab();
		add(guideTab);
		guideTab.setRect(0, 0, width, height);
		guideTab.updateList();

		alchemyTab = new AlchemyTab();
		add(alchemyTab);
		alchemyTab.setRect(0, 0, width, height);

		notesTab = new NotesTab();
		add(notesTab);
		notesTab.setRect(0, 0, width, height);
		notesTab.updateList();

		catalogTab = new CatalogTab();
		add(catalogTab);
		catalogTab.setRect(0, 0, width, height);
		catalogTab.updateList();

		loreTab = new LoreTab();
		add(loreTab);
		loreTab.setRect(0, 0, width, height);
		loreTab.updateList();

		Tab[] tabs = {
				new IconTab( new ItemSprite(GeneralAsset.MASTERY, null) ) {
					protected void select( boolean value ) {
						super.select( value );
						guideTab.active = guideTab.visible = value;
						if (value) last_index = 0;
					}
				},
				new IconTab( new ItemSprite(GeneralAsset.ALCH_PAGE, null) ) {
					protected void select( boolean value ) {
						super.select( value );
						alchemyTab.active = alchemyTab.visible = value;
						if (value) last_index = 1;
					}
				},
				new IconTab( new Image(Asset.getAssetFilePath(GeneralAsset.ICON_STAIRS)) ) {
					protected void select( boolean value ) {
						super.select( value );
						notesTab.active = notesTab.visible = value;
						if (value) last_index = 2;
					}
				},
				new IconTab( new ItemSprite(GeneralAsset.WEAPON_HOLDER, null) ) {
					protected void select( boolean value ) {
						super.select( value );
						catalogTab.active = catalogTab.visible = value;
						if (value) last_index = 3;
					}
				},
				new IconTab( new ItemSprite(GeneralAsset.GUIDE_PAGE, null) ) {
					protected void select( boolean value ) {
						super.select( value );
						loreTab.active = loreTab.visible = value;
						if (value) last_index = 4;
					}
				}
		};

		for (Tab tab : tabs) {
			add( tab );
		}

		layoutTabs();

		select(last_index);
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (event.pressed && KeyBindings.getActionForKey( event ) == SPDAction.JOURNAL) {
			onBackPressed();
			return true;
		} else {
			return super.onSignal(event);
		}
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		guideTab.layout();
		alchemyTab.layout();
		notesTab.layout();
		catalogTab.layout();
		loreTab.layout();
	}

	public static class GuideTab extends Component {

		private ScrollingListPane list;

		@Override
		protected void createChildren() {
			list = new ScrollingListPane();
			add( list );
		}

		@Override
		protected void layout() {
			super.layout();
			list.setRect( 0, 0, width, height);
		}

		private void updateList(){
			list.addTitle(Document.ADVENTURERS_GUIDE.title());

			for (String page : Document.ADVENTURERS_GUIDE.pageNames()){
				boolean found = Document.ADVENTURERS_GUIDE.isPageFound(page);
				ScrollingListPane.ListItem item = new ScrollingListPane.ListItem(
						Document.ADVENTURERS_GUIDE.pageSprite(page),
						null,
						found ? Messages.titleCase(Document.ADVENTURERS_GUIDE.pageTitle(page)) : Messages.titleCase(Messages.get( this, "missing" ))
				){
					@Override
					public boolean onClick(float x, float y) {
						if (inside( x, y ) && found) {
							GameScene.show( new WndStory( Document.ADVENTURERS_GUIDE.pageSprite(page),
									Document.ADVENTURERS_GUIDE.pageTitle(page),
									Document.ADVENTURERS_GUIDE.pageBody(page) ));
							Document.ADVENTURERS_GUIDE.readPage(page);
							return true;
						} else {
							return false;
						}
					}
				};
				if (!found){
					item.hardlight(0x999999);
					item.hardlightIcon(0x999999);
				}
				list.addItem(item);
			}

			list.setRect(x, y, width, height);
		}

	}

	public static class AlchemyTab extends Component {

		private RedButton[] pageButtons;
		private static final int NUM_BUTTONS = 9;

		private static final Asset[] spriteIndexes = {
			GeneralAsset.SEED_HOLDER,
			GeneralAsset.STONE_HOLDER,
            GeneralAsset.FOOD_HOLDER,
            GeneralAsset.POTION_HOLDER,
            GeneralAsset.SCROLL_HOLDER,
            GeneralAsset.BOMB_HOLDER,
            GeneralAsset.MISSILE_HOLDER,
            GeneralAsset.ELIXIR_HOLDER,
            GeneralAsset.SPELL_HOLDER,
        };

		public static int currentPageIdx   = 0;

		private IconTitle title;
		private RenderedTextBlock body;

		private ScrollPane list;
		private ArrayList<QuickRecipe> recipes = new ArrayList<>();

		@Override
		protected void createChildren() {
			pageButtons = new RedButton[NUM_BUTTONS];
			for (int i = 0; i < NUM_BUTTONS; i++){
				final int idx = i;
				pageButtons[i] = new RedButton( "" ){
					@Override
					protected void onClick() {
						currentPageIdx = idx;
						updateList();
					}
				};
				if (Document.ALCHEMY_GUIDE.isPageFound(i)) {
					pageButtons[i].icon(new ItemSprite(spriteIndexes[i], null));
				} else {
					pageButtons[i].icon(new ItemSprite(GeneralAsset.SOMETHING, null));
					pageButtons[i].enable(false);
				}
				add( pageButtons[i] );
			}

			title = new IconTitle();
			title.icon( new ItemSprite(GeneralAsset.ALCH_PAGE));
			title.visible = false;

			body = PixelScene.renderTextBlock(6);

			list = new ScrollPane(new Component());
			add(list);
		}

		@Override
		protected void layout() {
			super.layout();

			if (width() >= 180){
				float buttonWidth = width()/pageButtons.length;
				for (int i = 0; i < NUM_BUTTONS; i++) {
					pageButtons[i].setRect(x + i*buttonWidth, y, buttonWidth, ITEM_HEIGHT);
					PixelScene.align(pageButtons[i]);
				}
			} else {
				//for first row
				float buttonWidth = width()/5;
				float y = 0;
				float x = 0;
				for (int i = 0; i < NUM_BUTTONS; i++) {
					pageButtons[i].setRect(this.x + x, this.y + y, buttonWidth, ITEM_HEIGHT);
					PixelScene.align(pageButtons[i]);
					x += buttonWidth;
					if (i == 4){
						y += ITEM_HEIGHT;
						x = 0;
						buttonWidth = width()/4;
					}
				}
			}

			list.setRect(x, pageButtons[NUM_BUTTONS-1].bottom() + 1, width,
					height - pageButtons[NUM_BUTTONS-1].bottom() + y - 1);

			updateList();
		}

		public void updateList() {

			if (currentPageIdx != -1 && !Document.ALCHEMY_GUIDE.isPageFound(currentPageIdx)){
				currentPageIdx = -1;
			}

			for (int i = 0; i < NUM_BUTTONS; i++) {
				if (i == currentPageIdx) {
					pageButtons[i].icon().color(TITLE_COLOR);
				} else {
					pageButtons[i].icon().resetColor();
				}
			}

			if (currentPageIdx == -1){
				return;
			}

			for (QuickRecipe r : recipes){
				if (r != null) {
					r.killAndErase();
					r.destroy();
				}
			}
			recipes.clear();

			Component content = list.content();

			content.clear();

			title.visible = true;
			title.label(Document.ALCHEMY_GUIDE.pageTitle(currentPageIdx));
			title.setRect(0, 0, width(), 10);
			content.add(title);

			body.maxWidth((int)width());
			body.text(Document.ALCHEMY_GUIDE.pageBody(currentPageIdx));
			body.setPos(0, title.bottom());
			content.add(body);

			Document.ALCHEMY_GUIDE.readPage(currentPageIdx);

			ArrayList<QuickRecipe> toAdd = QuickRecipe.getRecipes(currentPageIdx);

			float left;
			float top = body.bottom()+2;
			int w;
			ArrayList<QuickRecipe> toAddThisRow = new ArrayList<>();
			while (!toAdd.isEmpty()){
				if (toAdd.get(0) == null){
					toAdd.remove(0);
					top += 6;
				}

				w = 0;
				while(!toAdd.isEmpty() && toAdd.get(0) != null
						&& w + toAdd.get(0).width() <= width()){
					toAddThisRow.add(toAdd.remove(0));
					w += toAddThisRow.get(0).width();
				}

				float spacing = (width() - w)/(toAddThisRow.size() + 1);
				left = spacing;
				while (!toAddThisRow.isEmpty()){
					QuickRecipe r = toAddThisRow.remove(0);
					r.setPos(left, top);
					left += r.width() + spacing;
					if (!toAddThisRow.isEmpty()) {
						ColorBlock spacer = new ColorBlock(1, 16, 0xFF222222);
						spacer.y = top;
						spacer.x = left - spacing / 2 - 0.5f;
						PixelScene.align(spacer);
						content.add(spacer);
					}
					recipes.add(r);
					content.add(r);
				}

				if (!toAdd.isEmpty() && toAdd.get(0) == null){
					toAdd.remove(0);
				}

				if (!toAdd.isEmpty() && toAdd.get(0) != null) {
					ColorBlock spacer = new ColorBlock(width(), 1, 0xFF222222);
					spacer.y = top + 16;
					spacer.x = 0;
					content.add(spacer);
				}
				top += 17;
				toAddThisRow.clear();
			}
			top -= 1;
			content.setSize(width(), top);
			list.setSize(list.width(), list.height());
			list.scrollTo(0, 0);
		}
	}

	private static class NotesTab extends Component {

		private ScrollingListPane list;

		@Override
		protected void createChildren() {
			list = new ScrollingListPane();
			add( list );
		}

		@Override
		protected void layout() {
			super.layout();
			list.setRect( x, y, width, height);
		}

		private void updateList(){
			//Keys
			ArrayList<Notes.KeyRecord> keys = Notes.getRecords(Notes.KeyRecord.class);
			if (!keys.isEmpty()){
				list.addTitle(Messages.get(this, "keys"));

				for(Notes.Record rec : keys){
					ScrollingListPane.ListItem item = new ScrollingListPane.ListItem( new Image(Asset.getAssetFilePath(GeneralAsset.ICON_STAIRS)),
							rec.levelName(),
							Messages.titleCase(rec.desc()));
					if (Dungeon.levelName.equals(rec.levelName())) item.hardlight(TITLE_COLOR);
					list.addItem(item);
				}
			}

			//Landmarks
			ArrayList<Notes.LandmarkRecord> landmarks = Notes.getRecords(Notes.LandmarkRecord.class);
			if (!landmarks.isEmpty()){

				list.addTitle(Messages.get(this, "landmarks"));

				for (Notes.Record rec : landmarks) {
					ScrollingListPane.ListItem item = new ScrollingListPane.ListItem( new Image(Asset.getAssetFilePath(GeneralAsset.ICON_STAIRS)),
							rec.levelName(),
							Messages.titleCase(rec.desc()));
					if (Dungeon.levelName.equals(rec.levelName())) item.hardlight(TITLE_COLOR);
					list.addItem(item);
				}

			}

			list.setRect(x, y, width, height);
		}

	}

	private static class CatalogTab extends Component{

		private RedButton[] itemButtons;
		private static final int NUM_BUTTONS = 2;

		private static int currentItemIdx   = 0;

		//sprite locations
		private static final int EQUIP_IDX  = 0;
		private static final int CONSUM_IDX = 1;

		private static final Asset spriteIndexes[] = {
            GeneralAsset.WEAPON_HOLDER,
            GeneralAsset.POTION_HOLDER,
        };

		private ScrollingListPane list;

		@Override
		protected void createChildren() {
			itemButtons = new RedButton[NUM_BUTTONS];
			for (int i = 0; i < NUM_BUTTONS; i++){
				final int idx = i;
				itemButtons[i] = new RedButton( "" ){
					@Override
					protected void onClick() {
						currentItemIdx = idx;
						updateList();
					}
				};
				itemButtons[i].icon(new ItemSprite(spriteIndexes[i], null));
				add( itemButtons[i] );
			}

			list = new ScrollingListPane();
			add( list );
		}

		@Override
		protected void layout() {
			super.layout();

			int perRow = NUM_BUTTONS;
			float buttonWidth = width()/perRow;

			for (int i = 0; i < NUM_BUTTONS; i++) {
				itemButtons[i].setRect((i%perRow) * (buttonWidth), (i/perRow) * (ITEM_HEIGHT ),
						buttonWidth, ITEM_HEIGHT);
				PixelScene.align(itemButtons[i]);
			}

			list.setRect(0, itemButtons[NUM_BUTTONS-1].bottom() + 1, width,
					height - itemButtons[NUM_BUTTONS-1].bottom() - 1);
		}

		private void updateList() {

			list.clear();

			for (int i = 0; i < NUM_BUTTONS; i++){
				if (i == currentItemIdx){
					itemButtons[i].icon().color(TITLE_COLOR);
				} else {
					itemButtons[i].icon().resetColor();
				}
			}

			list.scrollTo( 0, 0 );

			ArrayList<Catalog> catalogs;
			String title;
			if (currentItemIdx == EQUIP_IDX) {
				catalogs = Catalog.equipmentCatalogs;
				title = Messages.get(this, "title_equipment");
			} else {
				catalogs = Catalog.consumableCatalogs;
				title = Messages.get(this, "title_consumables");
			}

			int totalItems = 0;
			int totalSeen = 0;
			for (Catalog catalog : catalogs){
				totalItems += catalog.totalItems();
				totalSeen += catalog.totalSeen();
			}
			list.addTitle(title + " (" + totalSeen + "/" + totalItems + ")");

			for (Catalog catalog : catalogs){
				list.addTitle(Messages.titleCase(catalog.title()) + " (" + catalog.totalSeen() + "/" + catalog.totalItems() + ")");
				addListItems(catalog);
			}

			list.setRect(x, itemButtons[NUM_BUTTONS-1].bottom() + 1, width,
					height - itemButtons[NUM_BUTTONS-1].bottom() - 1);
		}

		//rings, potions and scrolls (and their exotic versions) are only known once identified in this run
		private static boolean isKnown(Catalog catalog, Class<?> cls){
			switch (catalog){
				case RINGS:
					return Ring.getKnown().contains(cls);
				case POTIONS:
					return Potion.getKnown().contains(cls);
				case SCROLLS:
					return Scroll.getKnown().contains(cls);
				case EXOTIC_POTIONS:
					return Potion.getKnown().contains(ExoticPotion.exoToReg.get(cls));
				case EXOTIC_SCROLLS:
					return Scroll.getKnown().contains(ExoticScroll.exoToReg.get(cls));
				default:
					return true;
			}
		}

		private void addListItems(Catalog catalog) {

			ArrayList<Class<?>> itemClasses = new ArrayList<>(catalog.items());
			final HashMap<Class<?>, Boolean> known = new HashMap<>();
			for (Class<?> cls : itemClasses) known.put(cls, isKnown(catalog, cls));

			Collections.sort(itemClasses, new Comparator<Class<?>>() {
				@Override
				public int compare(Class<?> a, Class<?> b) {
					int result = 0;

					//specifically known items appear first, then seen items, then unknown items.
					if (known.get(a) && Catalog.isSeen(a)) result -= 2;
					if (known.get(b) && Catalog.isSeen(b)) result += 2;
					if (Catalog.isSeen(a))                 result --;
					if (Catalog.isSeen(b))                 result ++;

					return result;
				}
			});

			for (Class<?> itemClass : itemClasses) {
				boolean itemSeen = Catalog.isSeen(itemClass);

				if (Weapon.Enchantment.class.isAssignableFrom(itemClass)){
					Weapon.Enchantment ench = (Weapon.Enchantment) Reflection.newInstance(itemClass);
					addEffectItem(new ItemSprite(GeneralAsset.WORN_SHORTSWORD, itemSeen ? ench.glowing() : null),
							itemSeen, ench.name(), ench.desc());
					continue;
				} else if (Armor.Glyph.class.isAssignableFrom(itemClass)){
					Armor.Glyph glyph = (Armor.Glyph) Reflection.newInstance(itemClass);
					addEffectItem(new ItemSprite(GeneralAsset.ARMOR_CLOTH, itemSeen ? glyph.glowing() : null),
							itemSeen, glyph.name(), glyph.desc());
					continue;
				}

				Item item = (Item) Reflection.newInstance(itemClass);
				boolean itemIDed = known.get(itemClass);
				if ( itemSeen && !itemIDed ){
					if (item instanceof Ring){
						((Ring) item).anonymize();
					} else if (item instanceof Potion){
						((Potion) item).anonymize();
					} else if (item instanceof Scroll){
						((Scroll) item).anonymize();
					}
				}
				ItemSprite sprite = new ItemSprite(item);
				if (!itemSeen) sprite.lightness(0);
				ScrollingListPane.ListItem listItem = new ScrollingListPane.ListItem(
						sprite,
						null,
						itemSeen ? Messages.titleCase(item.trueName()) : "???"
				){
					@Override
					public boolean onClick(float x, float y) {
						if (inside( x, y ) && itemSeen) {
							if (item instanceof ClassArmor){
								GameScene.show(new WndTitledMessage(new Image(icon),
										Messages.titleCase(item.trueName()), item.desc()));
							} else {
								GameScene.show(new WndTitledMessage(new Image(icon),
										Messages.titleCase(item.trueName()), item.info()));
							}
							return true;
						} else {
							return false;
						}
					}
				};

				if (!itemSeen) {
					listItem.hardlight( 0x999999 );
				} else if (!itemIDed) {
					listItem.hardlight( 0xCCCCCC );
				}

				list.addItem(listItem);

			}
		}

		private void addEffectItem(ItemSprite sprite, boolean seen, String name, String desc){
			if (!seen) sprite.lightness(0);
			EffectListItem listItem = new EffectListItem(sprite, seen, Messages.titleCase(name), desc);
			if (!seen) listItem.hardlight( 0x999999 );
			list.addItem(listItem);
		}

		private static class EffectListItem extends ScrollingListPane.ListItem {

			private final boolean seen;
			private final String title;
			private final String desc;

			EffectListItem(ItemSprite sprite, boolean seen, String title, String desc){
				super(sprite, null, seen ? title : "???");
				this.seen = seen;
				this.title = title;
				this.desc = desc;
			}

			@Override
			public boolean onClick(float x, float y) {
				if (inside( x, y ) && seen) {
					GameScene.show(new WndTitledMessage(new Image(icon), title, desc));
					return true;
				} else {
					return false;
				}
			}
		}

	}

	public static class LoreTab extends Component{

		private ScrollingListPane list;

		@Override
		protected void createChildren() {
			list = new ScrollingListPane();
			add( list );
		}

		@Override
		protected void layout() {
			super.layout();
			list.setRect( 0, 0, width, height);
		}

		private void updateList(){
			list.addTitle(Messages.get(this, "title"));

			for (Document doc : Document.values()){
				if (!doc.isLoreDoc()) continue;

				boolean found = doc.anyPagesFound();
				ScrollingListPane.ListItem item = new ScrollingListPane.ListItem(
						doc.pageSprite(),
						null,
						found ? Messages.titleCase(doc.title()) : "???"
				){
					@Override
					public boolean onClick(float x, float y) {
						if (inside( x, y ) && found) {
							ShatteredPixelDungeon.scene().addToFront( new WndDocument( doc ));
							return true;
						} else {
							return false;
						}
					}
				};
				if (!found){
					item.hardlight(0x999999);
					item.hardlightIcon(0x999999);
				}
				list.addItem(item);
			}

			list.setRect(x, y, width, height);
		}

	}

}
