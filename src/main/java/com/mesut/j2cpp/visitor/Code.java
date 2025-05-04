package com.mesut.j2cpp.visitor;

import com.mesut.j2cpp.Config;
import com.mesut.j2cpp.ast.CType;
import com.mesut.j2cpp.util.TypeHelper;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.List;

public class CodeBuilder {
    private final StringBuilder sb = new StringBuilder();
    private int level = 0;
    private String indent = "";
    private final List<String> usings = new ArrayList<>();
    public boolean rust = false; // modo de saída Rust

    private void init() {
        // Java 11+: repete o indentador "    " pelo nível atual
        indent = "    ".repeat(Math.max(0, level));
    }

    public void up() {
        level++;
        init();
    }

    public void down() {
        level = Math.max(0, level - 1);
        init();
    }

    public void clear() {
        sb.setLength(0);
        level = 0;
        init();
    }

    private String str(ITypeBinding b) {
        if (rust) {
            return mapType(b);
        }

        if ("void".equals(b.getName())) return "void";

        if (b.isTypeVariable()) {
            return TypeHelper.getObjectType().toString();
        }

        CType ct = TypeVisitor.fromBinding(b);
        if (Config.full) {
            ct.typeNames.clear();
        }

        String s = ct.toString();
        for (String u : usings) {
            if (s.startsWith(u)) {
                return s.substring(u.length() + 2); // remove namespace usado
            }
        }
        return s;
    }

    public void write(ITypeBinding b) {
        write(str(b));
    }

    private String format(String s, Object... args) {
        if (args.length != 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof ITypeBinding type) {
                    args[i] = str(type);
                } else if (args[i] instanceof Type type) {
                    args[i] = mapType(type);
                }
            }
            return String.format(s, args);
        }
        return s;
    }

    public void write(String s, Object... args) {
        s = format(s, args);
        sb.append(s);
        if (s.trim().endsWith("{")) {
            up();
        }
    }

    public void line(String s, Object... args) {
        if (s.trim().endsWith("}")) {
            down();
        }
        sb.append(indent);
        write(s, args);
        sb.append("\n");
    }

    public String mapType(Type type) {
        return rust ? RustHelper.mapType(type) : type.toString();
    }

    public String mapType(ITypeBinding type) {
        return rust ? RustHelper.mapType(type) : type.getName();
    }

    public String ptr(ITypeBinding b) {
        String s = str(b);
        if (!b.isPrimitive()) {
            s += "*";
        }
        return s;
    }

    public String ptr(CType b) {
        return b + "*";
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    // Getters and setters for `usings` if needed
    public List<String> getUsings() {
        return usings;
    }
}
