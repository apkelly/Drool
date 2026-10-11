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
            .withPackage("$DriblPackage..")
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.isInPackage(DroolPackage)
                }
            }
    }

    @Test
    fun `feature domain does not depend on data or UI`() {
        featureNames.forEach { feature ->
            productionScope.files
                .withPackage("$DroolPackage.$feature.domain..")
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.isInPackage("$DroolPackage.$feature.data") ||
                            import.name.isInPackage("$DroolPackage.$feature.ui") ||
                            import.name.isInPackage("$DroolPackage.data")
                    }
                }
        }
    }

    @Test
    fun `feature UI does not depend on data or Dribl`() {
        featureNames.forEach { feature ->
            productionScope.files
                .withPackage("$DroolPackage.$feature.ui..")
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.isInPackage("$DroolPackage.$feature.data") ||
                            import.name.isInPackage("$DroolPackage.data") ||
                            import.name.isInPackage(DriblPackage)
                    }
                }
        }
    }

    @Test
    fun `features do not depend directly on other features`() {
        featureNames.forEach { feature ->
            val otherFeaturePackages = featureNames
                .filterNot { it == feature }
                .map { "$DroolPackage.$it" }

            productionScope.files
                .withPackage("$DroolPackage.$feature..")
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
                    declaration.resideInPackage("$DroolPackage.$feature.domain.usecase..")
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
                    declaration.resideInPackage("$DroolPackage.$feature.domain.repository..")
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
                    declaration.resideInPackage("$DroolPackage.$feature.data..")
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
                    declaration.resideInPackage("$DroolPackage.$feature.ui.model..")
                }
            }
    }

    private fun String.isInPackage(packageName: String): Boolean =
        this == packageName || startsWith("$packageName.")

    private companion object {
        const val DriblPackage = "com.github.apkelly.dribl"
        const val DroolPackage = "com.github.apkelly.drool"

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

        private fun productionSourceDirectory(): String =
            listOf(
                "src/commonMain/kotlin",
                "../src/commonMain/kotlin",
            ).first { File(it).isDirectory }
    }
}
