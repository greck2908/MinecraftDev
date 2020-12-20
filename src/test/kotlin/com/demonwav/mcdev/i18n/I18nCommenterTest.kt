/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.actionSystem.IdeActions
import org.intellij.lang.annotations.Language

class I18nCommenterTest : BaseMinecraftTest(PlatformType.MCP) {
    private val fileName: String
        get() = getTestName(true)

    // Dirty hack :(
    override fun getTestDataPath() = project.baseDir.path

    private fun doTest(actionId: String, @Language("I18n") before: String, @Language("I18n") after: String) {
        buildProject {
            i18n("$fileName.lang", before, configure = true)
            i18n("${fileName}_after.lang", after, configure = false)
        }

        myFixture.performEditorAction(actionId)
        myFixture.checkResultByFile("${fileName}_after.lang", true)
    }

    fun testSingleLineComment() = doTest(IdeActions.ACTION_COMMENT_LINE, """
        test.<caret>key1=value1
        test.key2=value2
    """, """
        #test.key1=value1
        test.k<caret>ey2=value2
    """)

    fun testMultiLineComment() = doTest(IdeActions.ACTION_COMMENT_LINE, """
        test.key1=value1
        test.<selection>key2=value2
        test</selection>.key3=value3
        test.key4=value4
    """, """
        test.key1=value1
        #test.<selection>key2=value2
        #test</selection>.key3=value3
        test.key4=value4
    """)

    fun testSingleLineUncomment() = doTest(IdeActions.ACTION_COMMENT_LINE, """
        test.key1=value1
        test.key2=value2
        #test<caret>.key3=value3
        #test.key4=value4
    """, """
        test.key1=value1
        test.key2=value2
        test.key3=value3
        #tes<caret>t.key4=value4
    """)

    fun testMultiLineUncomment() = doTest(IdeActions.ACTION_COMMENT_LINE, """
        #test.<selection>key1=value1
        #test.key2=</selection>value2
        #test.key3=value3
        test.key4=value4
    """, """
        test.<selection>key1=value1
        test.key2=</selection>value2
        #test.key3=value3
        test.key4=value4
    """)
}
