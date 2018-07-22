package jaci.gradle.deploy.cache

import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.context.DeployContext

class Md5SumCacheMethodTest extends AbstractCacheMethodTestSpec {

    Md5SumCacheMethod cacheMethod

    def setup() {
        cacheMethod = new Md5SumCacheMethod(name)
    }

    def "compatible if md5sum found"() {
        def ctx = Mock(DeployContext) {
            execute(_) >> new CommandDeployResult(null, "d8e8fca2dc0f896fd7cb4cb0031ba249 somefile.txt", 0)
        }

        expect:
        cacheMethod.compatible(ctx)
    }

    def "not compatible if md5sum incorrect"() {
        def ctx = Mock(DeployContext) {
            execute(_) >> new CommandDeployResult(null, "n0t_th3_ch3cksum somefile.txt", 0)
        }

        expect:
        !cacheMethod.compatible(ctx)
    }

    def "local checksums correct"() {
        def map = [
            "hit": File.createTempFile('cacheHit', 'et_test'),
            "miss": File.createTempFile('cacheMiss', 'et_test')
        ]
        map["hit"].write("aaaaaaa")
        map["miss"].write("bbbbbbb")

        def expect = "5d793fc5b00a2348c3fb9ab59e5ca98a *hit\ne1faffe9c3c801f2f8c3fbe7cb032cb2 *miss"

        when:
        def r = cacheMethod.localChecksumsText(map)
        then:
        r == expect
    }

    def "cache hit / miss"() {
        def map = [
            "hit": File.createTempFile('cacheHit', 'et_test'),
            "miss": File.createTempFile('cacheMiss', 'et_test')
        ]

        def md5sumOutput = "hit: OK\nmiss: FAILED"

        def ctx = Mock(DeployContext) {
            execute(_) >> new CommandDeployResult(null, md5sumOutput, 0)
        }

        when:
        def needsUpdate = cacheMethod.needsUpdate(ctx, map)
        then:
        needsUpdate == ["miss"] as Set<String>
    }

}
