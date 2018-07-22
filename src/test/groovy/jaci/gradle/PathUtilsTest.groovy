package jaci.gradle

import spock.lang.Specification

class PathUtilsTest extends Specification {

    def "combine"() {
        when:
        // tests mostly join
        def path = PathUtils.combine("/myroot/", "relative/")

        then:
        path.equals("/myroot/relative")
    }

    def "combine upone"() {
        when:
        // tests normalize, for ..
        def path = PathUtils.combine("/some/deep/directory", "../directory2")

        then:
        path.equals("/some/deep/directory2")
    }

    def "combine to root"() {
        when:
        // tests mostly join for root
        def path = PathUtils.combine("/some/deep/directory", "/root")

        then:
        path.equals("/root")
    }

}
