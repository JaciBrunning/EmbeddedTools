package jaci.gradle.nativedeps

import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ETNativeDepSetTest extends Specification {

    def project = ProjectBuilder.builder().build()

    def platform = Mock(NativePlatform)
    def flavor = Mock(Flavor)
    def buildType = Mock(BuildType)

    ETNativeDepSet appliesSet(NativePlatform platform, Flavor flavor, BuildType type) {
        return new ETNativeDepSet(
                project, "test",
                null, null, null,
                null, null, null,
                null,
                platform, flavor, type
        )
    }

    def "appliesTo exactly"() {
        def nds = appliesSet(platform, flavor, buildType)

        when:
        def applies = nds.appliesTo(flavor, buildType, platform)

        then:
        applies
    }

    // Build Type and Flavor are permitted null for wildcard match
    def "appliesTo null pass"() {
        def nds = appliesSet(platform, null, null)

        when:
        def applies = nds.appliesTo(flavor, buildType, platform)

        then:
        applies
    }

    // Platform may not be wildcard matched
    def "appliesTo null fail"() {
        def nds = appliesSet(null, flavor, buildType)

        when:
        def applies = nds.appliesTo(flavor, buildType, platform)

        then:
        !applies
    }

    def "appliesTo no platform"() {
        def nds = appliesSet(platform, flavor, buildType)

        when:
        def applies = nds.appliesTo(flavor, buildType, Mock(NativePlatform))

        then:
        !applies
    }

    def "appliesTo no buildtype"() {
        def nds = appliesSet(platform, flavor, buildType)

        when:
        def applies = nds.appliesTo(flavor, Mock(BuildType), platform)

        then:
        !applies
    }

    def "appliesTo no flavor"() {
        def nds = appliesSet(platform, flavor, buildType)

        when:
        def applies = nds.appliesTo(Mock(Flavor), buildType, platform)

        then:
        !applies
    }

}
