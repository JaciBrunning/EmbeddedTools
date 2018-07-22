package jaci.gradle

import java.util.*

object PathUtils {

    @JvmStatic
    fun combine(root: String, relative: String?): String {
        return normalize(join(root, relative))
    }

    @JvmStatic
    fun join(root: String, relative: String?): String {
        if (relative == null)
            return root
        if (relative.startsWith("/"))
            return relative
        if (root[root.length - 1] != '/')
            return root + "/" + relative
        return root + relative
    }

    @JvmStatic
    fun normalize(filepath: String): String {
        val strings = filepath.split("/")
        val s = Stack<String>()
        for (str in strings) {
            if (str.trim().equals(".."))
                s.pop()
            else if (str.trim() != ".")
                s.push(str)
        }
        return s.joinToString("/")
    }

}