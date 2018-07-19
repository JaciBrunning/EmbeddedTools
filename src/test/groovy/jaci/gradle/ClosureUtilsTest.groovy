package jaci.gradle

import spock.lang.Specification

class ClosureUtilsTest extends Specification {

    def "delegateCall delegate"() {
        def delegate = Mock(DelegateSubject)
        def closure = { callDelegate(it) }

        when:
        ClosureUtils.delegateCall(delegate, closure)

        then:
        1 * delegate.callDelegate(delegate)
    }

    def "delegateCall args"() {
        def delegate = Mock(DelegateSubject)
        def closure = { a,b,c -> callDelegate(b); return c }

        when:
        def ret = ClosureUtils.delegateCall(delegate, closure, 12.0, 24.0)

        then:
        1 * delegate.callDelegate(12.0)
        ret == 24.0
    }

    static interface DelegateSubject {
        void callDelegate(Object arg)
    }

}
