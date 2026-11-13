package com.qsr.customspd.tools.contentaudit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Builds the extends-ancestry graph, enumerates concrete Mob/Item entities,
 *  and resolves sprite/image touchpoints through inheritance.
 *
 *  Only top-level classes are audited as entities. Nested classes are followed as
 *  parents: {@code extends Bomb.MagicalBomb}, a fully qualified parent, a simple name
 *  the file imports, or a simple name that a nested class uses for a sibling all
 *  resolve to the nested class. */
public final class EntityGraph {
    private static final Set<String> BASE_TYPES = Set.of(
            "Mob", "Item", "MeleeWeapon", "MissileWeapon", "Armor", "Wand",
            "Ring", "Artifact", "Potion", "Scroll", "Food");

    private EntityGraph() {}

    public record Entity(String kind, ContentClass cls,
                         String resolvedSpriteClass, String resolvedImageAsset) {}

    /** A simple name (or Outer.Inner path) declared by more than one parsed class, with the
     *  package-qualified declarations and the concrete classes {@link #build} drops because
     *  their ancestry passes through it. */
    public record Ambiguity(String simpleName, List<String> declarations, List<String> excluded) {}

    public static List<Ambiguity> ambiguities(List<ContentClass> all) {
        // keyed by the name other classes write: the simple name, or Outer.Inner for a nested class
        Map<String, List<String>> decls = new TreeMap<>();
        for (ContentClass c : all) {
            decls.computeIfAbsent(c.path(), k -> new ArrayList<>()).add(c.qualifiedName());
        }
        decls.values().removeIf(d -> d.size() < 2);
        if (decls.isEmpty()) return List.of();

        Index index = new Index(all);
        Map<String, List<String>> excluded = new TreeMap<>();
        for (ContentClass c : all) {
            if (!isCandidate(c)) continue;
            ContentClass cur = c;
            int guard = 0;
            while (cur != null && cur.superName() != null && guard++ < 50) {
                ContentClass next = index.parent(cur);
                if (next == null) {
                    if (decls.containsKey(cur.superName())) {
                        excluded.computeIfAbsent(cur.superName(), k -> new ArrayList<>()).add(qualified(c));
                    }
                    break;
                }
                cur = next;
            }
        }

        List<Ambiguity> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : decls.entrySet()) {
            List<String> d = new ArrayList<>(e.getValue());
            List<String> x = new ArrayList<>(excluded.getOrDefault(e.getKey(), List.of()));
            Collections.sort(d);
            Collections.sort(x);
            out.add(new Ambiguity(e.getKey(), d, x));
        }
        return out;
    }

    public static List<Entity> build(List<ContentClass> all) {
        Index index = new Index(all);

        List<Entity> out = new ArrayList<>();
        for (ContentClass c : all) {
            if (!isCandidate(c)) continue;
            String root = ancestryRoot(c, index); // "Mob", "Item", or null
            if (root == null) continue;
            out.add(new Entity(root, c,
                    resolve(c, index, /*sprite=*/true),
                    resolve(c, index, /*sprite=*/false)));
        }
        return out;
    }

    private static boolean isCandidate(ContentClass c) {
        return !c.isNested() && !c.isAbstract() && !BASE_TYPES.contains(c.simpleName());
    }

    /** Lookup tables for resolving a class's parent. */
    private static final class Index {
        // top-level classes by simple name; a duplicated name maps to nothing
        private final Map<String, ContentClass> byName = new HashMap<>();
        // every class by package + path
        private final Map<String, ContentClass> byQualified = new HashMap<>();
        // nested classes by Outer.Inner path, across packages
        private final Map<String, List<ContentClass>> byPath = new HashMap<>();

        Index(List<ContentClass> all) {
            Set<String> seen = new HashSet<>();
            for (ContentClass c : all) {
                byQualified.put(c.qualifiedName(), c);
                if (c.isNested()) {
                    byPath.computeIfAbsent(c.path(), k -> new ArrayList<>()).add(c);
                    continue;
                }
                if (!seen.add(c.simpleName())) {
                    // Duplicate simple name across packages: an unqualified `extends X`
                    // elsewhere can't tell which one it means, so don't silently guess.
                    // ambiguities() reports what this drops.
                    byName.remove(c.simpleName());
                    continue;
                }
                byName.put(c.simpleName(), c);
            }
        }

        /** The class {@code c} extends, or null if it is outside the scan or ambiguous. */
        ContentClass parent(ContentClass c) {
            String written = c.superName();
            if (written == null) return null;
            if (written.indexOf('.') < 0) {
                // inside a class, a member class of an enclosing class wins over a top-level one
                for (String outer = c.outerName(); outer != null; outer = enclosing(outer)) {
                    ContentClass member = byQualified.get(qualify(c.packageName(), outer + "." + written));
                    if (member != null) return member;
                }
                for (String imp : c.imports()) {
                    boolean isStatic = imp.startsWith("static ");
                    String name = isStatic ? imp.substring("static ".length()) : imp;
                    if (!name.endsWith("." + written)) continue;
                    ContentClass imported = byQualified.get(name);
                    // a type import names the class even when it lies outside the scan
                    if (imported != null || !isStatic) return imported;
                }
                ContentClass samePackage = byQualified.get(qualify(c.packageName(), written));
                if (samePackage != null) return samePackage;
                ContentClass onDemand = null;
                for (String imp : c.imports()) {
                    if (!imp.endsWith(".*")) continue;
                    String scope = imp.startsWith("static ") ? imp.substring("static ".length()) : imp;
                    ContentClass hit = byQualified.get(scope.substring(0, scope.length() - 1) + written);
                    if (hit == null) continue;
                    if (onDemand != null) return null; // two on-demand imports supply it: ambiguous
                    onDemand = hit;
                }
                if (onDemand != null) return onDemand;
                return byName.get(written);
            }
            ContentClass exact = byQualified.get(written);
            if (exact != null) return exact;
            ContentClass samePackage = byQualified.get(qualify(c.packageName(), written));
            if (samePackage != null) return samePackage;
            List<ContentClass> nested = byPath.get(written);
            return nested != null && nested.size() == 1 ? nested.get(0) : null;
        }

        private static String enclosing(String outer) {
            int dot = outer.lastIndexOf('.');
            return dot < 0 ? null : outer.substring(0, dot);
        }

        private static String qualify(String pkg, String path) {
            return pkg.isEmpty() ? path : pkg + "." + path;
        }
    }

    private static String qualified(ContentClass c) {
        return c.packageName().isEmpty() ? c.simpleName() : c.packageName() + "." + c.simpleName();
    }

    /** Walks up superclasses; returns "Mob" or "Item" if the ancestry reaches one, else null. */
    private static String ancestryRoot(ContentClass c, Index index) {
        ContentClass cur = c;
        int guard = 0;
        while (cur != null && guard++ < 50) {
            if (!cur.isNested() && "Mob".equals(cur.simpleName())) return "Mob";
            if (!cur.isNested() && "Item".equals(cur.simpleName())) return "Item";
            cur = index.parent(cur);
        }
        return null;
    }

    /** Nearest assigned spriteClass (sprite=true) or imageAsset (sprite=false) in the ancestry. */
    private static String resolve(ContentClass c, Index index, boolean sprite) {
        ContentClass cur = c;
        int guard = 0;
        while (cur != null && guard++ < 50) {
            String v = sprite ? cur.spriteClass() : cur.imageAsset();
            if (v != null) return v;
            cur = index.parent(cur);
        }
        return null;
    }
}
