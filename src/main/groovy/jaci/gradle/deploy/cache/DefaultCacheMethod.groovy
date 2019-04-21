package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.context.DeployContext

@CompileStatic
@InheritConstructors
class DefaultCacheMethod extends AbstractCacheMethod {
    public NeedsUpdateFunction needsUpdate = { DeployContext ctx, String filename, File localfile -> true } as NeedsUpdateFunction     // true if needs update
    public CompatibleFunction compatible = { true } as CompatibleFunction

    @Override
    boolean compatible(DeployContext context) {
        return compatible.check(context)
    }

    @Override
    Set<String> needsUpdate(DeployContext context, Map<String, File> files) {
        return files.findAll { String name, File file ->
            return needsUpdate.check(context, name, file)
        }.keySet()
    }
}
