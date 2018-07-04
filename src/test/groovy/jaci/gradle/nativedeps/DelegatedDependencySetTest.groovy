package jaci.gradle.nativedeps

import org.gradle.api.file.FileCollection
import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.platform.NativePlatform
import spock.lang.Specification
import spock.lang.Subject

class DelegatedDependencySetTest extends Specification {

    def ext = new DependencySpecExtension()

    def flavor = Mock(Flavor)
    def nativePlat = Mock(NativePlatform)
    def buildType = Mock(BuildType)

    def bin = Mock(NativeBinarySpec)

    def depset = Mock(ETNativeDepSet) {
        getIncludeRoots() >> Mock(FileCollection)
        getLinkFiles() >> Mock(FileCollection)
        getRuntimeFiles() >> Mock(FileCollection)
        getSourceFiles() >> Mock(FileCollection)

        appliesTo(*_) >> true
        getName() >> "test"
    }

    @Subject def dds = new DelegatedDependencySet("test", bin, ext)

    def "not found"() {
        when:
        dds.get()
        then:
        thrown(DelegatedDependencySet.MissingDependencyException)
    }

    def "found"() {
        ext.sets.add(depset)

        when:
        def ret = dds.get()
        then:
        ret == depset
    }

    def "filecollection functions"() {
        ext.sets.add(depset)

        when:
        def fi = dds.includeRoots
        def fl = dds.linkFiles
        def fr = dds.runtimeFiles
        def fs = dds.sourceFiles

        then:
        fi == depset.includeRoots
        fl == depset.linkFiles
        fr == depset.runtimeFiles
        fs == depset.sourceRoots
    }

}
