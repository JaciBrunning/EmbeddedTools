package jaci.gradle

import spock.lang.Specification

class WorkerStorageTest extends Specification {

    def "obtain empty"() {
        when:
        def ws1 = WorkerStorage.obtain()
        def ws2 = WorkerStorage.obtain()

        then:
        ws1.empty
        ws2.empty
    }

    def "obtain unique"() {
        when:
        def ws1 = WorkerStorage.obtain()
        def ws2 = WorkerStorage.obtain()
        ws1.put(11)

        then:
        !ws1.equals(ws2)
    }

    def "put method"() {
        when:
        def ws = WorkerStorage.obtain() as WorkerStorage<Integer>;
        def idx = ws.put(12)

        then:
        idx == 0
        ws.get(idx) == 12
    }

}
