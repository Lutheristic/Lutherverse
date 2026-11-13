package com.qsr.customspd.tools.contentaudit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContentClassParserTest {
    @Test
    void extractsMobWithInheritedSpriteInInitializer() {
        String src = "package com.qsr.customspd.actors.mobs;\n"
                + "public class Rat extends Mob {\n"
                + "  { spriteClass = RatSprite.class; }\n"
                + "}\n";
        ContentClass c = ContentClassParser.parse(src);
        assertEquals("Rat", c.simpleName());
        assertEquals("com.qsr.customspd.actors.mobs", c.packageName());
        assertEquals("Mob", c.superSimpleName());
        assertFalse(c.isAbstract());
        assertEquals("RatSprite", c.spriteClass());
        assertNull(c.imageAsset());
    }

    @Test
    void extractsItemImageAsset() {
        String src = "package com.qsr.customspd.items.food;\n"
                + "public class SupplyRation extends Food {\n"
                + "  { image = GeneralAsset.SUPPLY_RATION; }\n"
                + "}\n";
        ContentClass c = ContentClassParser.parse(src);
        assertEquals("SupplyRation", c.simpleName());
        assertEquals("Food", c.superSimpleName());
        assertEquals("SUPPLY_RATION", c.imageAsset());
        assertNull(c.spriteClass());
    }

    @Test
    void marksAbstractClasses() {
        ContentClass c = ContentClassParser.parse("public abstract class Mob extends Char {}");
        assertEquals("Mob", c.simpleName());
        // abstract classes are enumerated but skipped by EntityGraph
        org.junit.jupiter.api.Assertions.assertTrue(c.isAbstract());
    }

    @Test
    void returnsNullForNoTopLevelClass() {
        assertNull(ContentClassParser.parse("package x;\n"));
    }

    @Test
    void skipsLeadingInterfaceToFindTheClass() {
        String src = "package com.qsr.customspd.actors.mobs;\n"
                + "public interface Flying {}\n"
                + "public class Bat extends Mob {}\n";
        ContentClass c = ContentClassParser.parse(src);
        assertEquals("Bat", c.simpleName());
        assertEquals("Mob", c.superSimpleName());
    }

    @Test
    void parseAllAddsNestedClassesWithTheirOuterPathAndWrittenParent() {
        String src = "package com.qsr.customspd.items.bombs;\n"
                + "public class Bomb extends Item {\n"
                + "  { image = GeneralAsset.BOMB; }\n"
                + "  public static abstract class MagicalBomb extends Bomb {\n"
                + "    { image = GeneralAsset.MAGIC_BOMB; }\n"
                + "    public static class Inner extends com.qsr.customspd.items.Item {}\n"
                + "  }\n"
                + "  void act() { class Local extends Bomb {} }\n"
                + "}\n";
        List<ContentClass> all = ContentClassParser.parseAll(src);
        assertEquals(3, all.size(), () -> "the local class is left out: " + all);

        ContentClass bomb = all.get(0);
        assertEquals("Bomb", bomb.simpleName());
        assertFalse(bomb.isNested());
        assertEquals("BOMB", bomb.imageAsset());

        ContentClass magical = all.get(1);
        assertEquals("MagicalBomb", magical.simpleName());
        assertEquals("Bomb", magical.outerName());
        assertEquals("Bomb.MagicalBomb", magical.path());
        assertEquals("com.qsr.customspd.items.bombs.Bomb.MagicalBomb", magical.qualifiedName());
        assertEquals("Bomb", magical.superName());
        assertTrue(magical.isAbstract());
        assertEquals("MAGIC_BOMB", magical.imageAsset());

        ContentClass inner = all.get(2);
        assertEquals("Bomb.MagicalBomb", inner.outerName());
        assertEquals("Item", inner.superSimpleName());
        assertEquals("com.qsr.customspd.items.Item", inner.superName());
        assertNull(inner.imageAsset());
    }

    @Test
    void aParentNamedThroughItsOuterClassKeepsTheOuterName() {
        ContentClass c = ContentClassParser.parse("public class ArcaneBomb extends Bomb.MagicalBomb {}");
        assertEquals("MagicalBomb", c.superSimpleName());
        assertEquals("Bomb.MagicalBomb", c.superName());
    }

    @Test
    void anImageAssignedOnlyInANestedClassIsNotTheOuterClasssOwn() {
        String src = "public class Honeypot extends Item {\n"
                + "  public static class ShatteredPot extends Item { { image = GeneralAsset.SHATTPOT; } }\n"
                + "}\n";
        assertNull(ContentClassParser.parse(src).imageAsset());
    }

    @Test
    void returnsNullForUnparseableSource() {
        assertNull(ContentClassParser.parse("class {"));
    }
}
