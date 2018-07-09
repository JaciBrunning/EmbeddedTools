package jaci.gradle

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class RequestResultPair<A,B> {
    A request
    B result

    @Override
    public int hashCode() {
        return request.hashCode()
    }
}
