package jaci.gradle.deploy.cache

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext

import java.nio.file.Files
import java.security.MessageDigest

@CompileStatic
class CacheMethodMd5File implements CacheMethod {
    @Override
    boolean compatible(DeployContext context) {
        return true
    }

    @Override
    Set<String> needsUpdate(DeployContext context, Map<String, File> files) {
        context.logger().silent(true)
        def remote_cache = context.execute("cat cache.md5 2> /dev/null || echo '{}'")
        def remote_md5 = new JsonSlurper().parseText(remote_cache)

        def md = MessageDigest.getInstance("MD5")
        def local_md5 = files.collectEntries { String name, File file ->
            md.reset()
            md.update(Files.readAllBytes(file.toPath()))
            [(name): md.digest().encodeHex().toString()]
        }

        def needs_update = files.keySet().findAll { String name -> remote_md5[name] == null || remote_md5[name] != local_md5[name] }
        if (needs_update.size() > 0) {
            context.execute("echo '${new JsonBuilder(local_md5).toString()}' > cache.md5")
        }
        context.logger().silent(false)
        return needs_update
    }
}
