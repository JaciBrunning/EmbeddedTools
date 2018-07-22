package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.context.DeployContext
import org.apache.log4j.Logger

import java.nio.file.Files
import java.security.MessageDigest

@CompileStatic
@InheritConstructors
class Md5SumCacheMethod extends AbstractCacheMethod {
    private Logger log = Logger.getLogger(Md5SumCacheMethod)
    private int csI = 0

    @Override
    boolean compatible(DeployContext context) {
        context.logger?.silent(true)
        def sum = context.execute("echo test | md5sum 2> /dev/null").result
        context.logger?.silent(false)

        return !sum.empty && sum.split(" ").first().equalsIgnoreCase("d8e8fca2dc0f896fd7cb4cb0031ba249")
    }

    String localChecksumsText(Map<String, File> files) {
        def md = MessageDigest.getInstance("MD5")
        return files.collect { String name, File file ->
            md.reset()
            md.update(Files.readAllBytes(file.toPath()))
            def local = md.digest().encodeHex().toString()
            "${local} *${name}"
        }.join("\n")
    }

    @Override
    Set<String> needsUpdate(DeployContext context, Map<String, File> files) {
        context.logger?.silent(true)

        def cs = csI++

        log.debug("Comparing Checksums $cs...")
        def localChecksums = localChecksumsText(files)

        if (log.isDebugEnabled()) {
            log.debug("Local Checksums $cs:")
            log.debug(localChecksums)
        }

        def result = context.execute("echo '${localChecksums}' > _tmp.et.md5 && md5sum -c _tmp.et.md5 2> /dev/null; rm _tmp.et.md5").result

        if (log.isDebugEnabled()) {
            log.debug("Remote Checksums $cs:")
            log.debug(result)
        }

        def upToDate = result.split('\n').collect { String s ->
            s.split(':')
        }.findAll { String[] ls ->
            ls.last().trim().equalsIgnoreCase('ok')
        }.collect { String[] ls ->
            ls.first()
        }

        context.logger?.silent(false)
        return files.keySet().findAll { String name -> !upToDate.contains(name) }
    }
}
