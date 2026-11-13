package com.qsr.customspd.tools.contentaudit;

import java.util.List;

/** A parsed content class. {@code spriteClass} and {@code imageAsset} are the
 *  simple names assigned in this class's own body (null if it does not assign one;
 *  EntityGraph resolves inherited values). {@code outerName} is the enclosing class
 *  path of a nested class ("Bomb", or "A.B" two levels down), null at top level, and
 *  {@code superName} is the parent as written ("Mob", "Bomb.MagicalBomb", or fully
 *  qualified), where {@code superSimpleName} keeps only its last segment. {@code imports}
 *  are the file's imports as written ("a.B", "a.*", "static a.B.C"). */
public record ContentClass(
        String simpleName,
        String packageName,
        String superSimpleName,
        boolean isAbstract,
        String spriteClass,
        String imageAsset,
        String outerName,
        String superName,
        List<String> imports) {

    /** A top-level class whose parent is written as a simple name. */
    public ContentClass(String simpleName, String packageName, String superSimpleName,
                        boolean isAbstract, String spriteClass, String imageAsset) {
        this(simpleName, packageName, superSimpleName, isAbstract, spriteClass, imageAsset,
                null, superSimpleName);
    }

    public ContentClass(String simpleName, String packageName, String superSimpleName,
                        boolean isAbstract, String spriteClass, String imageAsset,
                        String outerName, String superName) {
        this(simpleName, packageName, superSimpleName, isAbstract, spriteClass, imageAsset,
                outerName, superName, List.of());
    }

    public boolean isNested() {
        return outerName != null;
    }

    /** Outer.Inner for a nested class, the simple name otherwise. */
    public String path() {
        return outerName == null ? simpleName : outerName + "." + simpleName;
    }

    public String qualifiedName() {
        return packageName.isEmpty() ? path() : packageName + "." + path();
    }
}
