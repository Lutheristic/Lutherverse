package com.qsr.customspd.tools.contentaudit;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.ArrayList;
import java.util.List;

/** Parses one Java source file into {@link ContentClass}es. */
public final class ContentClassParser {
    private ContentClassParser() {}

    /** The file's top-level class, or null. */
    public static ContentClass parse(String source) {
        CompilationUnit cu = compile(source);
        if (cu == null) return null;
        ClassOrInterfaceDeclaration type = topLevel(cu);
        return type == null ? null : toContentClass(cu, type, null);
    }

    /** The file's top-level class followed by every class nested in it, at any depth.
     *  Local classes inside methods are left out: nothing outside the method can extend them. */
    public static List<ContentClass> parseAll(String source) {
        List<ContentClass> out = new ArrayList<>();
        CompilationUnit cu = compile(source);
        if (cu == null) return out;
        ClassOrInterfaceDeclaration top = topLevel(cu);
        if (top == null) return out;
        out.add(toContentClass(cu, top, null));
        for (ClassOrInterfaceDeclaration type : top.findAll(ClassOrInterfaceDeclaration.class)) {
            if (type == top || type.isInterface() || type.isLocalClassDeclaration()) continue;
            String outer = outerPath(type);
            if (outer == null) continue; // nested in something other than a type
            out.add(toContentClass(cu, type, outer));
        }
        return out;
    }

    private static CompilationUnit compile(String source) {
        try {
            return StaticJavaParser.parse(source);
        } catch (RuntimeException e) {
            return null; // unparseable source is not content; callers skip nulls
        }
    }

    private static ClassOrInterfaceDeclaration topLevel(CompilationUnit cu) {
        return cu.getTypes().stream()
                .filter(t -> t instanceof ClassOrInterfaceDeclaration)
                .map(t -> (ClassOrInterfaceDeclaration) t)
                .filter(t -> !t.isInterface()).findFirst().orElse(null);
    }

    private static ContentClass toContentClass(CompilationUnit cu, ClassOrInterfaceDeclaration type,
                                               String outer) {
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        String sup = null;
        String supWritten = null;
        if (!type.getExtendedTypes().isEmpty()) {
            sup = type.getExtendedTypes(0).getNameAsString();
            supWritten = type.getExtendedTypes(0).getNameWithScope();
        }

        String spriteClass = assignedSimpleName(type, "spriteClass", /*classExpr=*/true);
        String imageAsset = assignedSimpleName(type, "image", /*classExpr=*/false);

        return new ContentClass(type.getNameAsString(), pkg, sup, type.isAbstract(),
                spriteClass, imageAsset, outer, supWritten);
    }

    /** "Outer" or "Outer.Mid" for a nested class; null if an enclosing node is not a type. */
    private static String outerPath(ClassOrInterfaceDeclaration type) {
        StringBuilder path = new StringBuilder();
        Node cur = type.getParentNode().orElse(null);
        while (cur != null && !(cur instanceof CompilationUnit)) {
            if (!(cur instanceof TypeDeclaration<?> t)) return null;
            path.insert(0, path.length() == 0 ? t.getNameAsString() : t.getNameAsString() + ".");
            cur = cur.getParentNode().orElse(null);
        }
        return path.length() == 0 ? null : path.toString();
    }

    private static Node enclosingType(Node node) {
        Node cur = node.getParentNode().orElse(null);
        while (cur != null && !(cur instanceof TypeDeclaration)) cur = cur.getParentNode().orElse(null);
        return cur;
    }

    /** Finds {@code <field> = X.class} (classExpr) or {@code <field> = Enum.MEMBER}
     *  (field access) in the class body, outside its nested classes, and returns
     *  X / MEMBER, or null. */
    private static String assignedSimpleName(ClassOrInterfaceDeclaration type,
                                             String field, boolean classExpr) {
        for (AssignExpr assign : type.findAll(AssignExpr.class)) {
            if (enclosingType(assign) != type) continue;
            if (!(assign.getTarget() instanceof NameExpr target)) continue;
            if (!target.getNameAsString().equals(field)) continue;
            Expression value = assign.getValue();
            if (classExpr && value instanceof ClassExpr ce) {
                return ce.getType().asString();
            }
            if (!classExpr && value instanceof FieldAccessExpr fa) {
                return fa.getNameAsString(); // e.g. GeneralAsset.SUPPLY_RATION -> SUPPLY_RATION
            }
        }
        return null;
    }
}
