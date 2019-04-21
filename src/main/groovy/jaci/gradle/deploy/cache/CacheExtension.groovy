package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import jaci.gradle.Resolver
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator

@CompileStatic
class CacheExtension extends DefaultNamedDomainObjectSet<CacheMethod> implements Resolver<CacheMethod> {

    final Project project

    CacheExtension(Project project) {
        super(CacheMethod.class, DirectInstantiator.INSTANCE)
        this.project = project

        method("md5file", Md5FileCacheMethod, {})
        method("md5sum", Md5SumCacheMethod, {})
    }

    public <T extends AbstractCacheMethod> CacheMethod method(String name, Class<T> type, final Action<T> config) {
        AbstractCacheMethod cm = project.objects.newInstance(type, name)
        config.execute(cm);
        this << (cm)
        return cm
    }

    CacheMethod method(String name, final Action<? extends DefaultCacheMethod> config) {
        return method(name, DefaultCacheMethod, config)
    }

    CacheMethod resolve(Object cache) {
        if (project.hasProperty("deploy-dirty")) {
            return null
        } else if (cache == null || cache == false) {
            return null
        } else if (cache instanceof CacheMethod) {
            return (CacheMethod)cache
        } else if (cache instanceof String || cache instanceof GString) {
            return getByName(cache.toString())
        } else if (cache instanceof NeedsUpdateFunction) {
            def dcm = new DefaultCacheMethod("customCacheMethod")
            dcm.needsUpdate = (cache as NeedsUpdateFunction)
            return dcm
        } else if (cache instanceof Closure<Boolean>) {
            def dcm = new DefaultCacheMethod("customCacheMethod")
            dcm.needsUpdate = (cache as NeedsUpdateFunction)
            return dcm
        }

        throw new IllegalArgumentException("Unknown Cache Method Type: ${cache.class}.\nMust be one of:\n" +
                "- instance of CacheMethod\n" +
                "- The name (String) of a CacheMethod stored in deploy.cache\n" +
                "- A closure returning whether the file needs update (true) or not (false)\n" +
                "- Null or False for no caching")
    }

}
