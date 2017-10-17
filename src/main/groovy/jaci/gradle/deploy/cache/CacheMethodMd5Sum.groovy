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

        def checksums_text = files.collect { String name, File file ->
            md.reset()
            md.update(Files.readAllBytes(file.toPath()))
            def local = md.digest().encodeHex().toString()
            "${local}  ${name}"
        }.join("\n")
        def result = context.executeMaybe("echo '${checksums_text}' > _tmp.et.md5 && md5sum -b -c _tmp.et.md5 2> /dev/null; rm _tmp.et.md5")
        def upToDate = result.split('\n').collect { String s ->
            s.split(':')
        }.findAll { String[] ls ->
            ls.last().trim().equalsIgnoreCase('ok')
        }.collect { String[] ls ->
            ls.first()
        }

        context.logger().silent(false)
        return files.keySet().findAll { String name -> !upToDate.contains(name) }
    }
}
