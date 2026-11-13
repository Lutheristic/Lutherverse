package com.qsr.customspd.tools.contentaudit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EntityGraphTest {
    private static ContentClass cc(String name, String pkg, String sup, boolean abs,
                                   String sprite, String image) {
        return new ContentClass(name, pkg, sup, abs, sprite, image);
    }

    @Test
    void enumeratesConcreteMobsAndResolvesInheritedSprite() {
        List<ContentClass> all = List.of(
                cc("Mob", "com.qsr.customspd.actors.mobs", "Char", true, null, null),
                cc("Rat", "com.qsr.customspd.actors.mobs", "Mob", false, "RatSprite", null),
                cc("Albino", "com.qsr.customspd.actors.mobs", "Rat", false, null, null));
        List<EntityGraph.Entity> es = EntityGraph.build(all);
        assertEquals(2, es.size()); // Mob is abstract+base, excluded
        EntityGraph.Entity albino = es.stream()
                .filter(e -> e.cls().simpleName().equals("Albino")).findFirst().orElseThrow();
        assertEquals("Mob", albino.kind());
        assertEquals("RatSprite", albino.resolvedSpriteClass()); // inherited from Rat
    }

    @Test
    void enumeratesItemsAndResolvesInheritedImage() {
        List<ContentClass> all = List.of(
                cc("Item", "com.qsr.customspd.items", "Object", true, null, null),
                cc("Food", "com.qsr.customspd.items.food", "Item", true, null, null),
                cc("SupplyRation", "com.qsr.customspd.items.food", "Food", false, null, "SUPPLY_RATION"));
        List<EntityGraph.Entity> es = EntityGraph.build(all);
        assertEquals(1, es.size()); // Item, Food are abstract/base
        assertEquals("Item", es.get(0).kind());
        assertEquals("SUPPLY_RATION", es.get(0).resolvedImageAsset());
    }

    @Test
    void excludesClassesWhoseAncestryNeverReachesMobOrItem() {
        List<ContentClass> all = List.of(
                cc("Helper", "com.qsr.customspd.utils", "Object", false, null, null));
        assertTrue(EntityGraph.build(all).isEmpty());
    }

    @Test
    void guardsAgainstDuplicateSimpleNamesInAncestryChain() {
        // Two unrelated classes named "Helper" live in different packages, both
        // extending Mob directly with different sprites.
        List<ContentClass> all = List.of(
                cc("Mob", "com.qsr.customspd.actors.mobs", "Char", true, null, null),
                cc("Helper", "com.qsr.customspd.actors.mobs.variant1", "Mob", false, "VariantASprite", null),
                cc("Helper", "com.qsr.customspd.actors.mobs.variant2", "Mob", false, "VariantBSprite", null),
                cc("Elf", "com.qsr.customspd.actors.mobs", "Helper", false, null, null));
        List<EntityGraph.Entity> es = EntityGraph.build(all);
        // Elf's ancestry passes through the ambiguous "Helper" name, which could mean
        // either variant; it must not be silently resolved to whichever one happened
        // to win the name collision.
        assertTrue(es.stream().noneMatch(e -> e.cls().simpleName().equals("Elf")),
                () -> "Elf's ancestry is ambiguous and must be excluded, not guessed: " + es);
        // Both Helper classes are still audited on their own terms (their own
        // ancestry to Mob is unambiguous).
        assertEquals(2, es.stream().filter(e -> e.cls().simpleName().equals("Helper")).count());
    }

    @Test
    void reportsTheAmbiguousNameAndWhatItExcludes() {
        // Excluding Elf above is only safe if it is visible: the ambiguity must be
        // reported with both declarations and the class it drops from the audit.
        List<ContentClass> all = List.of(
                cc("Mob", "com.qsr.customspd.actors.mobs", "Char", true, null, null),
                cc("Helper", "com.qsr.customspd.actors.mobs.variant1", "Mob", false, "VariantASprite", null),
                cc("Helper", "com.qsr.customspd.actors.mobs.variant2", "Mob", false, "VariantBSprite", null),
                cc("Elf", "com.qsr.customspd.actors.mobs", "Helper", false, null, null),
                cc("Rat", "com.qsr.customspd.actors.mobs", "Mob", false, "RatSprite", null));
        List<EntityGraph.Ambiguity> amb = EntityGraph.ambiguities(all);
        assertEquals(List.of(new EntityGraph.Ambiguity("Helper",
                        List.of("com.qsr.customspd.actors.mobs.variant1.Helper",
                                "com.qsr.customspd.actors.mobs.variant2.Helper"),
                        List.of("com.qsr.customspd.actors.mobs.Elf"))),
                amb);
    }

    private static ContentClass nested(String name, String pkg, String outer, String sup, boolean abs,
                                       String sprite, String image) {
        String simple = sup.substring(sup.lastIndexOf('.') + 1);
        return new ContentClass(name, pkg, simple, abs, sprite, image, outer, sup);
    }

    private static ContentClass top(String name, String pkg, String sup, String image) {
        String simple = sup.substring(sup.lastIndexOf('.') + 1);
        return new ContentClass(name, pkg, simple, false, null, image, null, sup);
    }

    private static final String MOBS = "com.qsr.customspd.actors.mobs";

    @Test
    void followsParentsThatAreNestedClasses() {
        List<ContentClass> all = List.of(
                cc("Mob", MOBS, "Char", true, null, null),
                cc("Elemental", MOBS, "Mob", true, null, null),
                nested("FireElemental", MOBS, "Elemental", "Elemental", false, "FireSprite", null),
                // a sibling named without its outer class, as Java allows inside Elemental
                nested("Newborn", MOBS, "Elemental", "FireElemental", false, null, null),
                top("Spark", MOBS, "Elemental.Newborn", null),
                top("Ember", MOBS, MOBS + ".Elemental.FireElemental", null));
        List<EntityGraph.Entity> es = EntityGraph.build(all);

        for (String name : List.of("Spark", "Ember")) {
            EntityGraph.Entity e = es.stream().filter(x -> x.cls().simpleName().equals(name))
                    .findFirst().orElseThrow(() -> new AssertionError(name + " was not scanned: " + es));
            assertEquals("Mob", e.kind());
            assertEquals("FireSprite", e.resolvedSpriteClass(), name);
        }
        assertTrue(es.stream().noneMatch(e -> e.cls().isNested()),
                () -> "nested classes are followed, not audited themselves: " + es);
    }

    @Test
    void aMemberClassWinsOverATopLevelClassOfTheSameName() {
        List<ContentClass> all = List.of(
                cc("Mob", MOBS, "Char", true, null, null),
                cc("Item", "com.qsr.customspd.items", "Object", true, null, null),
                cc("Seed", "com.qsr.customspd.items", "Item", false, null, "SEED"),
                cc("Lasher", MOBS, "Mob", false, "LasherSprite", null),
                nested("Seed", MOBS, "Lasher", "Mob", false, "SeedlingSprite", null),
                nested("Sprout", MOBS, "Lasher", "Seed", false, null, null),
                top("Weed", MOBS, "Lasher.Sprout", null));
        EntityGraph.Entity weed = EntityGraph.build(all).stream()
                .filter(e -> e.cls().simpleName().equals("Weed")).findFirst().orElseThrow();
        assertEquals("Mob", weed.kind(), "Sprout's Seed is Lasher.Seed, not the top-level item");
        assertEquals("SeedlingSprite", weed.resolvedSpriteClass());
    }

    @Test
    void anAmbiguousNestedPathIsNotGuessed() {
        List<ContentClass> all = List.of(
                cc("Mob", MOBS, "Char", true, null, null),
                cc("Outer", MOBS + ".a", "Mob", true, null, null),
                cc("Outer", MOBS + ".b", "Mob", true, null, null),
                nested("Inner", MOBS + ".a", "Outer", "Mob", false, "ASprite", null),
                nested("Inner", MOBS + ".b", "Outer", "Mob", false, "BSprite", null),
                top("Child", MOBS, "Outer.Inner", null),
                top("ChildOfA", MOBS + ".a", "Outer.Inner", null));
        List<EntityGraph.Entity> es = EntityGraph.build(all);
        assertTrue(es.stream().noneMatch(e -> e.cls().simpleName().equals("Child")),
                () -> "Outer.Inner from a third package could be either class: " + es);
        EntityGraph.Entity a = es.stream().filter(e -> e.cls().simpleName().equals("ChildOfA"))
                .findFirst().orElseThrow();
        assertEquals("ASprite", a.resolvedSpriteClass(), "the same package's Outer.Inner is the one meant");

        EntityGraph.Ambiguity inner = EntityGraph.ambiguities(all).stream()
                .filter(x -> x.simpleName().equals("Outer.Inner")).findFirst()
                .orElseThrow(() -> new AssertionError("the dropped child must be reported: "
                        + EntityGraph.ambiguities(all)));
        assertEquals(List.of(MOBS + ".a.Outer.Inner", MOBS + ".b.Outer.Inner"), inner.declarations());
        assertEquals(List.of(MOBS + ".Child"), inner.excluded());
    }

    private static ContentClass importing(String name, String pkg, String sup, List<String> imports) {
        return new ContentClass(name, pkg, sup, false, null, null, null, sup, imports);
    }

    @Test
    void aSimpleNameResolvesThroughTheFilesImports() {
        String bombs = "com.qsr.customspd.items.bombs";
        List<ContentClass> all = List.of(
                cc("Item", "com.qsr.customspd.items", "Object", true, null, null),
                cc("Bomb", bombs, "Item", false, null, "BOMB"),
                nested("MagicalBomb", bombs, "Bomb", "Bomb", true, null, "MAGIC_BOMB"),
                // a top-level class of the same simple name must not be taken for the import
                cc("MagicalBomb", "com.qsr.customspd.items.spells", "Item", false, null, "SPELL"),
                importing("Fizz", "com.qsr.customspd.items.stones", "MagicalBomb",
                        List.of(bombs + ".Bomb.MagicalBomb")),
                importing("Pop", "com.qsr.customspd.items.stones", "MagicalBomb",
                        List.of("com.qsr.customspd.items.Item", bombs + ".Bomb.*")),
                // the same package's class wins over an on-demand import
                importing("Pip", "com.qsr.customspd.items.spells", "MagicalBomb",
                        List.of(bombs + ".Bomb.*")),
                // imported from outside the scan, so it is not the scanned MagicalBomb either
                importing("Wisp", "com.qsr.customspd.items.stones", "MagicalBomb",
                        List.of("com.watabou.noosa.MagicalBomb")));
        List<EntityGraph.Entity> es = EntityGraph.build(all);

        assertEquals("MAGIC_BOMB", image(es, "Fizz"));
        assertEquals("MAGIC_BOMB", image(es, "Pop"));
        assertEquals("SPELL", image(es, "Pip"));
        assertTrue(es.stream().noneMatch(e -> e.cls().simpleName().equals("Wisp")),
                () -> "Wisp's parent is outside the scan: " + es);
    }

    private static String image(List<EntityGraph.Entity> es, String name) {
        return es.stream().filter(e -> e.cls().simpleName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError(name + " was not scanned: " + es))
                .resolvedImageAsset();
    }

    @Test
    void reportsNoAmbiguityWhenSimpleNamesAreUnique() {
        List<ContentClass> all = List.of(
                cc("Mob", "com.qsr.customspd.actors.mobs", "Char", true, null, null),
                cc("Rat", "com.qsr.customspd.actors.mobs", "Mob", false, "RatSprite", null));
        assertTrue(EntityGraph.ambiguities(all).isEmpty());
    }
}
