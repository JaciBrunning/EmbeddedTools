package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext

import java.nio.file.Files
import java.security.MessageDigest

@CompileStatic
class CacheMethodMd5Sum implements CacheMethod {
    @Override
    boolean compatible(DeployContext context) {
        context.logger().silent(true)
        def sum = context.executeMaybe("echo test | md5sum 2> /dev/null")
        context.logger().silent(false)

        return !sum.empty && sum.split(" ").first().equalsIgnoreCase("d8e8fca2dc0f896fd7cb4cb0031ba249")
    }

    @Override
    Set<String> needsUpdate(DeployContext context, Map<String, File> files) {
        def md = MessageDigest.getInstance("MD5")
        context.logger().silent(true)
        def needs_update = files.findAll { String name, File file ->
            md.reset()
            md.update(Files.readAllBytes(file.toPath()))
            def local = md.digest().encodeHex().toString()
            def remote = context.executeMaybe("md5sum ${name} 2> /dev/null || true").split(" ")[0] ?: ""
            local != remote
        }.keySet()
        context.logger().silent(false)
        return needs_update
    }
}
