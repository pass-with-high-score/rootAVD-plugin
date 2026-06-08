package com.github.rootavd.plugin

import com.intellij.openapi.project.Project
import com.intellij.ui.components.*
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

class RootAVDPanel(private val project: Project) {
    private val panel = JBPanel<JBPanel<*>>(BorderLayout())
    
    private val logArea = JBTextArea(15, 60).apply {
        isEditable = false
        background = Color(30, 30, 30)
        foreground = Color(0, 255, 65)
        font = Font("Monospaced", Font.PLAIN, 12)
        margin = JBUI.insets(10)
    }
    
    private val manager = RootAVDManager(project)
    private val scanner = AVDScanner()
    private val emulator = EmulatorManager()
    private val engine = RootEngine(project) { text ->
        SwingUtilities.invokeLater {
            logArea.append(text)
            logArea.caretPosition = logArea.document.length
        }
    }
    
    private val avdComboBox = ComboBox<AVDInfo>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is AVDInfo) {
                    text = value.name
                }
                return component
            }
        }
    }

    init {
        panel.border = JBUI.Borders.empty(10)

        // --- Header ---
        val headerLabel = JBLabel("RootAVD Manager v1.1").apply {
            font = JBUI.Fonts.label(18f).asBold()
            border = JBUI.Borders.emptyBottom(15)
        }
        panel.add(headerLabel, BorderLayout.NORTH)

        val centerPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            insets = JBUI.insets(0, 0, 10, 0)
        }

        // 1. Device Selection
        val selectionPanel = JPanel(BorderLayout(10, 0)).apply {
            border = createTitled(" 1. Device Selection ")
        }
        selectionPanel.add(avdComboBox, BorderLayout.CENTER)
        val refreshButton = JButton("Refresh List")
        refreshButton.addActionListener { refreshAVDs() }
        selectionPanel.add(refreshButton, BorderLayout.EAST)
        
        gbc.gridy = 0
        centerPanel.add(selectionPanel, gbc)

        // 2. Main Actions
        val actionPanel = JPanel(GridLayout(2, 2, 10, 10)).apply {
            border = createTitled(" 2. Core Actions ")
        }
        
        val rootBtn = JButton("Root Now").apply {
            font = font.deriveFont(Font.BOLD)
        }
        rootBtn.addActionListener {
            val selected = avdComboBox.selectedItem as? AVDInfo
            if (selected != null) {
                logArea.append("\n[ROOT PROCESS STARTED]\n")
                Thread { engine.root(selected) }.start()
            }
        }

        val restoreBtn = JButton("Restore Backup")
        restoreBtn.addActionListener {
            val selected = avdComboBox.selectedItem as? AVDInfo
            if (selected != null) {
                Thread { engine.restore(selected) }.start()
            }
        }

        val coldBootBtn = JButton("Cold Boot")
        coldBootBtn.addActionListener {
            val selected = avdComboBox.selectedItem as? AVDInfo
            if (selected != null) {
                logArea.append("> Triggering Cold Boot for ${selected.name}...\n")
                logArea.append(emulator.coldBoot(selected.name) + "\n")
            }
        }

        val installMagiskBtn = JButton("Install Magisk App")
        installMagiskBtn.addActionListener {
            Thread { engine.installMagiskApp() }.start()
        }

        actionPanel.add(rootBtn)
        actionPanel.add(restoreBtn)
        actionPanel.add(coldBootBtn)
        actionPanel.add(installMagiskBtn)

        gbc.gridy = 1
        centerPanel.add(actionPanel, gbc)

        // 3. Utility Actions
        val utilPanel = JPanel(GridLayout(1, 2, 10, 0)).apply {
            border = createTitled(" 3. Utilities ")
        }
        val scanBtn = JButton("Scan Details")
        scanBtn.addActionListener {
            val selected = avdComboBox.selectedItem as? AVDInfo
            if (selected != null) {
                manager.ensureScriptsExtracted { /* silent */ }
                logArea.append("\n--- AVD INFO ---\nName: ${selected.name}\nPath: ${selected.ramdiskPath}\n----------------\n")
            }
        }
        val wipeBtn = JButton("Wipe Data")
        wipeBtn.addActionListener {
            val selected = avdComboBox.selectedItem as? AVDInfo
            if (selected != null) {
                logArea.append("> Wiping data for ${selected.name}...\n")
                logArea.append(emulator.wipeData(selected.name) + "\n")
            }
        }
        utilPanel.add(scanBtn)
        utilPanel.add(wipeBtn)

        gbc.gridy = 2
        centerPanel.add(utilPanel, gbc)

        // 4. Console
        val logContainer = JPanel(BorderLayout()).apply {
            border = createTitled(" Console Output ")
            add(JBScrollPane(logArea), BorderLayout.CENTER)
        }
        gbc.gridy = 3
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        centerPanel.add(logContainer, gbc)

        panel.add(centerPanel, BorderLayout.CENTER)
        refreshAVDs()
    }

    private fun createTitled(title: String) = BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(JBColor.border()), title,
        TitledBorder.LEFT, TitledBorder.TOP, JBUI.Fonts.label(12f).asBold()
    )

    private fun refreshAVDs() {
        val avds = scanner.getAvds()
        avdComboBox.model = DefaultComboBoxModel(avds.toTypedArray())
        logArea.append("> List refreshed. Found ${avds.size} devices.\n")
    }

    fun getContent(): JPanel = panel
}
