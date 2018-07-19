package jaci.gradle.deploy.cache

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.context.DeployContext
import org.apache.log4j.Logger

import java.nio.file.Files
import java.security.MessageDigest

@CompileStatic
@InheritConstructors
class Md5FileCacheMethod extends AbstractCacheMethod {
    private Logger log = Logger.getLogger(Md5SumCacheMethod)
    private int csI = 0

    @Override
    boolean compatible(DeployContext context) {
        return true
    }

    private Object getRemoteCache(DeployContext ctx) {
        def remote_cache = ctx.execute("cat cache.md5 2> /dev/null || echo '{}'").result
        return new JsonSlurper().parseText(remote_cache)
    }

    Map<String, String> localChecksumsMap(Map<String, File> files) {
        def md = MessageDigest.getInstance("MD5")
        return files.collectEntries { String name, File file ->
            md.reset()
            md.update(Files.readAllBytes(file.toPath()))
            [(name): md.digest().encodeHex().toString()]
        } as Map<String, String>
    }

    @Override
    Set<String> needsUpdate(DeployContext context, Map<String, File> files) {
        context.logger?.silent(true)
        def cs = csI++
        log.debug("Comparing File Checksum $cs...")

        def remote_md5 = getRemoteCache(context)

        if (log.isDebugEnabled()) {
            log.debug("Remote Cache $cs:")
            log.debug(new JsonBuilder(remote_md5).toString())
        }

        def local_md5 = localChecksumsMap(files)

        if (log.isDebugEnabled()) {
            log.debug("Local JSON Cache $cs:")
            log.debug(new JsonBuilder(local_md5).toString())
        }

        def needs_update = files.keySet().findAll { String name ->
            remote_md5[name] == null || remote_md5[name] != local_md5[name]
        }

        if (needs_update.size() > 0) {
            context.execute("echo '${new JsonBuilder(local_md5).toString()}' > cache.md5")
        }
        context.logger?.silent(false)
        return needs_update
    }
}
