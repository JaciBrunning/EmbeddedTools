import org.gradle.api.*

import org.gradle.nativeplatform.toolchain.*

import org.gradle.api.*
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal 
import org.gradle.nativeplatform.platform.internal.ArchitectureInternal
import org.gradle.nativeplatform.toolchain.internal.gcc.*
import org.gradle.nativeplatform.toolchain.internal.*
import org.gradle.process.internal.ExecActionFactory
import org.gradle.nativeplatform.toolchain.internal.gcc.version.CompilerMetaDataProviderFactory
import org.gradle.internal.operations.BuildOperationProcessor
import org.gradle.internal.reflect.Instantiator
import org.gradle.nativeplatform.toolchain.GccPlatformToolChain

public class CrossGcc extends GccToolChain {
    public CrossGcc(Instantiator instantiator, String name, BuildOperationProcessor buildOperationProcessor, OperatingSystem operatingSystem, FileResolver fileResolver, ExecActionFactory execActionFactory, CompilerMetaDataProviderFactory metaDataProviderFactory) {
        super(instantiator, name, buildOperationProcessor, operatingSystem, fileResolver, execActionFactory, metaDataProviderFactory)
    }

    @Override
    protected String getTypeName() {
        return "CrossGcc";
    }

}