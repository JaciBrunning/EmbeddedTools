package jaci.gradle.deploy.cache

import jaci.gradle.deploy.DeployContext

import java.nio.file.Files
import java.security.MessageDigest

class CacheMethodMd5Sum implements CacheMethod {
    @Override
    boolean compatible(DeployContext context) {
        context.logger().silent(true)
        def sum = context.executeMaybe("echo test | md5sum 2> /dev/null")
        context.logger().silent(false)

        return !sum.empty && sum.split(" ").first().equalsIgnoreCase("d8e8fca2dc0f896fd7cb4cb0031ba249")
    }

    @Override
    boolean needsUpdate(DeployContext context, File localFile, String file) {
        context.logger().silent(true)
        def remote_md5 = context.executeMaybe("md5sum ${file} 2> /dev/null || true").split(" ")[0] ?: ""
        context.logger().silent(false)

        def md = MessageDigest.getInstance("MD5")
        md.update(Files.readAllBytes(localFile.toPath()))
        def local_md5 = md.digest().encodeHex().toString()

        return !remote_md5.equalsIgnoreCase(local_md5)
    }
}
