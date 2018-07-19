package jaci.gradle.deploy.artifact

import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails

class FileTreeArtifactTest extends AbstractArtifactTest {

    FileTreeArtifact artifact
    def ctx = Mock(DeployContext)

    def setup() {
        artifact = new FileTreeArtifact(name, project)
    }

    @Override
    AbstractArtifact getArtifact() {
        return artifact
    }

    def "deploy (no files)"() {

        when:
        artifact.deploy(ctx)
        then:
        0 * ctx.put(_, _)
    }

    def "deploy"() {
        def dirEntries = ["mydir", "mydir/subdir"].collect { p ->
            Mock(FileVisitDetails) {
                getFile() >> new File(p)
                isDirectory() >> true
                getPath() >> p
            }
        }
        def dirString = dirEntries.collect { it.getPath() }.join(' ')
        def fileEntries = ["test", "mydir/test", "mydir/subdir/test"].collect { p ->
            Mock(FileVisitDetails) {
                getFile() >> new File(p)
                isDirectory() >> false
                getPath() >> p
            }
        }
        def fileMap = fileEntries.collectEntries { [(it.getPath()), it.getFile()] }

        def fileTree = Mock(FileTree) {
            visit(_) >> { cb ->
                (dirEntries + fileEntries).each { cb.first().call(it) }
                null
            }
        }

        artifact.setFiles(fileTree)

        when:
        artifact.deploy(ctx)
        then:
        1 * ctx.execute("mkdir -p ${dirString}")
        1 * ctx.put(fileMap, null)
    }

    def "deploy cache"() {
        def fileTree = Mock(FileTree)

        def cache = Mock(CacheMethod)
        def resolver = Mock(Resolver) {
            resolve(_) >> cache
        }

        artifact.setCacheResolver(resolver)
        artifact.setFiles(fileTree)

        when:
        artifact.deploy(ctx)
        then:
        1 * ctx.put(_, cache)
        0 * ctx.put(_, _)
    }
}
