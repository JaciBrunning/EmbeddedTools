package jaci.gradle

class PathUtils {
    static String combine(String root, String relative) {
        normalize(relative == null ? root : join(root, relative))
    }

    static String join(String root, String relative) {
        if (relative.startsWith("/")) return relative;
        if (root.charAt(root.length() - 1) != "/") root += "/"
        root += relative;
    }

    static String normalize(String filepath) {
        def strings = filepath.split("/") as List
        def s = [] as Stack
        strings.forEach { str ->
            if (str.trim().equals("..")) {
                s.pop()
            } else s.push(str)
        }
        return s.join("/")
    }
}
