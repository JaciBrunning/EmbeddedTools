package jaci.gradle.toolchains

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.internal.registry.LanguageTransformContainer
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.plugins.NativeComponentPlugin
import org.gradle.nativeplatform.toolchain.internal.ToolType
import org.gradle.platform.base.BinaryContainer

class ToolchainsPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.getPluginManager().apply(NativeComponentPlugin.class)
        
        project.ext.defineTools = { target, prefix, suffix ->
            target.cppCompiler.executable       = prefix + target.cppCompiler.executable + suffix
            target.cCompiler.executable         = prefix + target.cCompiler.executable + suffix
            target.linker.executable            = prefix + target.linker.executable + suffix
            target.assembler.executable         = prefix + target.assembler.executable + suffix
            target.staticLibArchiver.executable = prefix + target.staticLibArchiver.executable + suffix
        }
    }

    public static def LANG_TOOLS_MAP = [
        "cpp" : ToolType.CPP_COMPILER,
        "c" : ToolType.C_COMPILER,
        "objcpp" : ToolType.OBJECTIVECPP_COMPILER,
        "objc" : ToolType.OBJECTIVEC_COMPILER,
        "rc" : ToolType.WINDOW_RESOURCES_COMPILER,
        "asm" : ToolType.ASSEMBLER
    ]

    static class ToolchainRules extends RuleSource {
        @Mutate
        void configureOptionalBuildables(BinaryContainer binaries, LanguageTransformContainer languageTransforms) {
            binaries.withType(NativeBinarySpec) { bin ->
                def toolProvider = bin.toolChain.select(bin.targetPlatform)
                try {
                    def treg = toolProvider.class.getDeclaredField("toolRegistry")
                    treg.setAccessible(true)
                    def toolRegistry = treg.get(toolProvider)
                    treg.setAccessible(false)

                    for (transform in languageTransforms) {
                        if (transform.getSourceSetType().isInstance(bin.inputs[0])) {
                            def tool_type_required = LANG_TOOLS_MAP[transform.languageName]
                            toolRegistry.tools.findAll {
                                it.toolType == tool_type_required && !bin.toolChain.locate(it).isAvailable()
                            }.forEach {
                                println "No ${it.toolType.toolName} for Binary (${bin.displayName}) with Toolchain ${bin.toolChain.name} on Platform ${bin.targetPlatform.name}. Skipping build."
                                bin.buildable = false
                            }
                        }
                    }
                } catch (all) { }
            }
        }
    }
}