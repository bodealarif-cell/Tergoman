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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // يمكن التقاط تحديد النص التلقائي هنا مستقبلاً
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * استخراج كل النصوص الظاهرة حالياً في التطبيق المفتوح على الشاشة
     */
    fun extractVisibleScreenText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        traverseNodes(rootNode, sb)
        return sb.toString().trim()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return

        // تجاهل نافذة تطبيق ترجمان نفسه حتى لا يترجم نفسه
        if (node.packageName?.toString() == packageName) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && text.length > 1) {
            sb.append(text).append("\n")
        }

        for (i in 0 until node.childCount) {
            traverseNodes(node.getChild(i), sb)
        }
    }
}
