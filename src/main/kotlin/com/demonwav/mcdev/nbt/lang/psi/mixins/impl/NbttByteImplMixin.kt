/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.psi.mixins.impl

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttByteMixin
import com.demonwav.mcdev.nbt.tags.TagByte
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttByteImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttByteMixin {

    override fun getByteTag(): TagByte {
        if (text == "false") {
            return TagByte(0)
        }
        if (text == "true") {
            return TagByte(1)
        }

        return TagByte(text.trim().replace(bRegex, "").toByte())
    }

    companion object {
        private val bRegex = "[bB]".toRegex()
    }
}
