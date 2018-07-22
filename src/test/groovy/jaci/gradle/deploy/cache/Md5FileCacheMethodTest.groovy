package jaci.gradle.deploy.cache

import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.context.DeployContext

class Md5FileCacheMethodTest extends AbstractCacheMethodTestSpec {
    Md5FileCacheMethod cacheMethod

    void setup() {
        cacheMethod = new Md5FileCacheMethod(name)
    }

    def "compatible"() {
        expect:
        cacheMethod.compatible(null)
    }

    def "miss by default"() {
        def map = [
                "a": File.createTempFile('tmp', 'et_test')
        ]
        def ctx = Mock(DeployContext) {
            execute(_) >> new CommandDeployResult(null, '{}', 0)
        }

        when:
        def needsUpdate = cacheMethod.needsUpdate(ctx, map)
        then:
        needsUpdate == ["a"] as Set<String>
    }

    def "local checksums correct"() {
        def map = [
                "hit": File.createTempFile('cacheHit', 'et_test'),
                "miss": File.createTempFile('cacheMiss', 'et_test')
        ]
        map["hit"].write("aaaaaaa")
        map["miss"].write("bbbbbbb")

        when:
        def r = cacheMethod.localChecksumsMap(map)
        then:
        r["hit"] == "5d793fc5b00a2348c3fb9ab59e5ca98a"
        r["miss"] == "e1faffe9c3c801f2f8c3fbe7cb032cb2"
    }

    def "cache hit / miss"() {
        def map = [
                "hit": File.createTempFile('cacheHit', 'et_test'),
                "miss": File.createTempFile('cacheMiss', 'et_test')
        ]

        map["hit"].write("aaaaaaa")
        map["miss"].write("bbbbbbb")

        def json = "{\"hit\": \"5d793fc5b00a2348c3fb9ab59e5ca98a\", \"miss\": \"n0t_corr3ct\"}"

        def ctx = Mock(DeployContext) {
            execute(_) >> new CommandDeployResult(null, json, 0)
        }

        when:
        def needsUpdate = cacheMethod.needsUpdate(ctx, map)
        then:
        needsUpdate == ["miss"] as Set<String>
    }
}
