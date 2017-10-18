package jaci.gradle

/**
 * Temporary storage for ServiceWorkers is Isolation Mode NONE.
 */
class WorkerStorage<T> extends ArrayList<T> {

    int put(T obj) {
        this << obj
        size() - 1
    }

    static WorkerStorage obtain() {
        return Collections.synchronizedList(new WorkerStorage())
    }

}
