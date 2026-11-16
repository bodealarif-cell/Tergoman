package com.tarjuman.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenReaderAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ScreenReaderAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun extractVisibleScreenText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val lines = LinkedHashSet<String>()
        traverseNodes(rootNode, lines)
        return lines.joinToString("\n").trim()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, lines: LinkedHashSet<String>) {
        if (node == null) return
        if (node.packageName?.toString() == packageName) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && text.length > 1) {
            lines.add(text)
        }

        for (i in 0 until node.childCount) {
            traverseNodes(node.getChild(i), lines)
        }
    }
}
