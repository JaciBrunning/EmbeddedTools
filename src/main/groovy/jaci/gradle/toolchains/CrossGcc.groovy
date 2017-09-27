package jaci.gradle.toolchains

import org.gradle.api.*

import org.gradle.nativeplatform.toolchain.*
import org.gradle.nativeplatform.toolchain.internal.*

import org.gradle.api.internal.file.FileResolver
import org.gradle.nativeplatform.toolchain.internal.gcc.AbstractGccCompatibleToolChain
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory
import org.gradle.nativeplatform.plugins.NativeComponentPlugin
import org.gradle.nativeplatform.toolchain.internal.gcc.version.CompilerMetaDataProviderFactory
import org.gradle.process.internal.ExecActionFactory

public class CrossGcc extends AbstractGccCompatibleToolChain {
    public CrossGcc(Instantiator instantiator, String name, 
            BuildOperationExecutor buildOperationExecutor, OperatingSystem operatingSystem, 
            FileResolver fileResolver, ExecActionFactory execActionFactory, 
            CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory, 
            CompilerMetaDataProviderFactory metaDataProviderFactory, WorkerLeaseService workerLeaseService) {

        super(name, buildOperationExecutor, operatingSystem, fileResolver, 
                execActionFactory, compilerOutputFileNamingSchemeFactory, 
                metaDataProviderFactory.gcc(), instantiator, workerLeaseService)

        // platformConfigs and configInsertLocation are properties in AbstractGccCompatibleToolChain,
        // and are used to keep track of what platforms this toolchain can build for.
        // In the AbstractGccCompatibleToolChain constructor, it automatically adds the x86 and 
        // x64 platform configurations, which is fine for local native builds, but not for cross
        // compilers. There is no way that allows these to be unset, and so we have to manually
        // edit the fields. See https://github.com/gradle/gradle/issues/2909
        def plCn = AbstractGccCompatibleToolChain.class.getDeclaredField("platformConfigs")
        plCn.setAccessible(true)
        plCn.get(this).clear()
        plCn.setAccessible(false)

        def cil = AbstractGccCompatibleToolChain.class.getDeclaredField("configInsertLocation")
        cil.setAccessible(true)
        cil.set(this, 0)
        cil.setAccessible(false)
    }

    @Override
    protected String getTypeName() {
        return "CrossGcc"
    }

}