package com.github.apkelly.drool

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.classes
import com.lemonappdev.konsist.api.ext.list.interfaces
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import java.io.File
import kotlin.test.Test

class ArchitectureTest {
    @Test
    fun `Dribl code does not depend on Drool`() {
        productionScope.files
            .withPackage("$DRIBL_PACKAGE..")
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.isInPackage(DROOL_PACKAGE)
                }
            }
    }

    @Test
    fun `feature domain does not depend on data or UI`() {
        featureNames.forEach { feature ->
            productionScope.files
                .withPackage("$DROOL_PACKAGE.$feature.domain..")
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.isInPackage("$DROOL_PACKAGE.$feature.data") ||
                            import.name.isInPackage("$DROOL_PACKAGE.$feature.ui") ||
                            import.name.isInPackage("$DROOL_PACKAGE.data")
                    }
                }
        }
    }

    @Test
    fun `feature UI does not depend on data or Dribl`() {
        featureNames.forEach { feature ->
            productionScope.files
                .withPackage("$DROOL_PACKAGE.$feature.ui..")
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.isInPackage("$DROOL_PACKAGE.$feature.data") ||
                            import.name.isInPackage("$DROOL_PACKAGE.data") ||
                            import.name.isInPackage(DRIBL_PACKAGE)
                    }
                }
        }
    }

    @Test
    fun `features do not depend directly on other features`() {
        featureNames.forEach { feature ->
            val otherFeaturePackages = featureNames
                .filterNot { it == feature }
                .map { "$DROOL_PACKAGE.$it" }

            productionScope.files
                .withPackage("$DROOL_PACKAGE.$feature..")
                .assertFalse { file ->
                    file.imports.any { import ->
                        otherFeaturePackages.any { packageName ->
                            import.name.isInPackage(packageName)
                        }
                    }
                }
        }
    }

    @Test
    fun `use cases reside in feature domain usecase packages`() {
        productionScope
            .classes()
            .withNameEndingWith("UseCase")
            .assertTrue { declaration ->
                featureNames.any { feature ->
                    declaration.resideInPackage("$DROOL_PACKAGE.$feature.domain.usecase..")
                }
            }
    }

    @Test
    fun `repository interfaces reside in feature domain repository packages`() {
        productionScope
            .interfaces()
            .withNameEndingWith("Repository")
            .assertTrue { declaration ->
                featureNames.any { feature ->
                    declaration.resideInPackage("$DROOL_PACKAGE.$feature.domain.repository..")
                }
            }
    }

    @Test
    fun `repository implementations reside in feature data packages`() {
        productionScope
            .classes()
            .withNameEndingWith("RepositoryImpl")
            .assertTrue { declaration ->
                featureNames.any { feature ->
                    declaration.resideInPackage("$DROOL_PACKAGE.$feature.data..")
                }
            }
    }

    @Test
    fun `UI models reside in feature UI model packages`() {
        productionScope
            .classes()
            .withNameEndingWith("UiModel")
            .assertTrue { declaration ->
                featureNames.any { feature ->
                    declaration.resideInPackage("$DROOL_PACKAGE.$feature.ui.model..")
                }
            }
    }

    private fun String.isInPackage(
        packageName: String,
    ): Boolean = this == packageName || startsWith("$packageName.")

    private companion object {
        const val DRIBL_PACKAGE = "com.github.apkelly.dribl"
        const val DROOL_PACKAGE = "com.github.apkelly.drool"

        val featureNames = listOf(
            "auth",
            "discover",
            "home",
            "match",
            "profile",
            "schedule",
            "team",
        )

        val productionScope = Konsist.scopeFromDirectory(productionSourceDirectory())

        private fun productionSourceDirectory(): String = listOf(
            "src/commonMain/kotlin",
            "../src/commonMain/kotlin",
        ).first { File(it).isDirectory }
    }
}
