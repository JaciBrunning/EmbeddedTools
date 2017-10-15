package jaci.gradle.deploy.cache

import jaci.gradle.deploy.DeployContext

import java.nio.file.Files
import java.security.MessageDigest

class CacheMethodMd5File implements CacheMethod {
    @Override
    boolean compatible(DeployContext context) {
        return true
    }

    @Override
    boolean needsUpdate(DeployContext context, File localFile, String file) {
        context.logger().silent(true)
        def remote_md5 = context.executeMaybe("cat ${file}.md5 2> /dev/null || true")

        def md = MessageDigest.getInstance("MD5")
        md.update(Files.readAllBytes(localFile.toPath()))
        def local_md5 = md.digest().encodeHex().toString()
        def needsUpdate = !remote_md5.equalsIgnoreCase(local_md5)

        if (needsUpdate) {
            context.executeMaybe("echo ${local_md5} > ${file}.md5")
        }
        context.logger().silent(false)

        return needsUpdate
    }
}
