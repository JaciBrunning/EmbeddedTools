package jaci.gradle

import groovy.transform.CompileStatic

@CompileStatic
class RefcountList<T> {
    List<T> list
    int refcount

    RefcountList() {
        list = Collections.synchronizedList(new ArrayList<T>())
        refcount = 0
    }

    void use() {
        refcount++
    }

    void release() {
        refcount--
        if (refcount <= 0)
            list.clear()
    }

    List<T> get() {
        return list
    }

    void clear() {
        list.clear()
    }

    int put(T obj) {
        list << obj
        return list.size() - 1
    }

    T get(int index) {
        return list.get(index)
    }
}
