package jaci.gradle.deploy.artifact

import jaci.gradle.Resolver
import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.context.DeployContext

class FileArtifactTest extends AbstractArtifactTest {

    FileArtifact artifact
    def ctx = Mock(DeployContext)

    def setup() {
        artifact = new FileArtifact(name, project)
    }

    @Override
    AbstractArtifact getArtifact() {
        return artifact
    }

    def "deploy (no file)"() {

        when:
        artifact.deploy(ctx)
        then:
        0* ctx.put(_, _, _)
    }

    def "deploy (no filename)"() {
        def file = Mock(File) {
            getName() >> "filename"
        }

        artifact.setFile(file)

        when:
        artifact.deploy(ctx)
        then:
        1 * ctx.put(file, file.getName(), null)
        0 * ctx.put(_, _, _)
    }

    def "deploy (filename)"() {
        def file = Mock(File) {
            getName() >> "filename"
        }

        artifact.setFile(file)
        artifact.setFilename("othername")

        when:
        artifact.deploy(ctx)
        then:
        1 * ctx.put(file, "othername", null)
        0 * ctx.put(_, _, _)
    }

    def "deploy cache"() {
        def file = Mock(File)
        def cache = Mock(CacheMethod)
        def resolver = Mock(Resolver) {
            resolve(_) >> cache
        }

        artifact.setCacheResolver(resolver)
        artifact.setFile(file)

        when:
        artifact.deploy(ctx)
        then:
        1 * ctx.put(file, file.getName(), cache)
        0 * ctx.put(_, _, _)
    }
}
