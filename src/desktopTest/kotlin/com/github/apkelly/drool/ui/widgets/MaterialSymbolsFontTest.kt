package com.github.apkelly.drool.ui.widgets

import java.awt.Font
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaterialSymbolsFontTest {
    @Test
    fun bundledFontContainsEveryUsedSymbol() {
        val file = File(
            "src/commonMain/composeResources/font/material_symbols_outlined.ttf"
        )
        val font = Font.createFont(Font.TRUETYPE_FONT, file)

        assertTrue(file.length() < 10_000)
        assertEquals(
            MaterialSymbol.entries.size,
            MaterialSymbol.entries.map(MaterialSymbol::glyph).toSet().size,
        )
        MaterialSymbol.entries.forEach { symbol ->
            assertTrue(font.canDisplay(symbol.glyph.single()), symbol.name)
        }
    }
}
