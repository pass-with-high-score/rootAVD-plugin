package com.github.rootavd.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RootAVDToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val rootAVDPanel = RootAVDPanel(project)
        val content = ContentFactory.getInstance().createContent(rootAVDPanel.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
