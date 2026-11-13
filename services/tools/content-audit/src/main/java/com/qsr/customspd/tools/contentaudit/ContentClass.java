package com.qsr.customspd.tools.contentaudit;

/** A parsed content class. {@code spriteClass} and {@code imageAsset} are the
 *  simple names assigned in this class's own body (null if it does not assign one;
 *  EntityGraph resolves inherited values). {@code outerName} is the enclosing class
 *  path of a nested class ("Bomb", or "A.B" two levels down), null at top level, and
 *  {@code superName} is the parent as written ("Mob", "Bomb.MagicalBomb", or fully
 *  qualified), where {@code superSimpleName} keeps only its last segment. */
public record ContentClass(
        String simpleName,
        String packageName,
        String superSimpleName,
        boolean isAbstract,
        String spriteClass,
        String imageAsset,
        String outerName,
        String superName) {

    /** A top-level class whose parent is written as a simple name. */
    public ContentClass(String simpleName, String packageName, String superSimpleName,
                        boolean isAbstract, String spriteClass, String imageAsset) {
        this(simpleName, packageName, superSimpleName, isAbstract, spriteClass, imageAsset,
                null, superSimpleName);
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
