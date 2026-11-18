package com.example.time.detector

/**
 * Sanitized, pure Kotlin representation of an accessibility node hierarchy.
 *
 * STRICT PRIVACY REQUIREMENT:
 * This model deliberately excludes any text, content descriptions, input values,
 * user identifiers, or message contents. It captures only structural layout metadata
 * required for UI signature detection and diagnostics.
 */
data class UiNodeSnapshot(
    val id: String?,
    val className: String?,
    val isVisibleToUser: Boolean,
    val isSelected: Boolean,
    val isClickable: Boolean,
    val bounds: RectData = RectData.EMPTY,
    val children: List<UiNodeSnapshot> = emptyList(),
    val depth: Int = 0
) {
    val nodeCount: Int
        get() = 1 + children.sumOf { it.nodeCount }

    val maxDepth: Int
        get() {
            if (children.isEmpty()) return depth
            return children.maxOf { it.maxDepth }
        }

    fun findFirst(predicate: (UiNodeSnapshot) -> Boolean): UiNodeSnapshot? {
        if (predicate(this)) return this
        for (child in children) {
            val found = child.findFirst(predicate)
            if (found != null) return found
        }
        return null
    }

    fun findAll(predicate: (UiNodeSnapshot) -> Boolean): List<UiNodeSnapshot> {
        val result = mutableListOf<UiNodeSnapshot>()
        if (predicate(this)) {
            result.add(this)
        }
        for (child in children) {
            result.addAll(child.findAll(predicate))
        }
        return result
    }

    fun findById(targetId: String): UiNodeSnapshot? {
        return findFirst { node ->
            InstagramSignatures.matchesId(node.id, targetId)
        }
    }

    fun hasId(targetId: String): Boolean {
        return findById(targetId) != null
    }

    /**
     * Serializes this node hierarchy into a clean, sanitized JSON representation.
     * Guaranteed free of any personal or readable text.
     */
    fun toJson(indent: Int = 2): String {
        val sb = StringBuilder()
        buildJson(sb, 0, indent)
        return sb.toString()
    }

    private fun buildJson(sb: StringBuilder, currentIndentLevel: Int, indentSize: Int) {
        val baseIndent = " ".repeat(currentIndentLevel * indentSize)
        val fieldIndent = " ".repeat((currentIndentLevel + 1) * indentSize)

        sb.append("{\n")
        sb.append(fieldIndent).append("\"id\": ").append(id?.let { "\"$it\"" } ?: "null").append(",\n")
        sb.append(fieldIndent).append("\"className\": ").append(className?.let { "\"$it\"" } ?: "null").append(",\n")
        sb.append(fieldIndent).append("\"isVisibleToUser\": ").append(isVisibleToUser).append(",\n")
        sb.append(fieldIndent).append("\"isSelected\": ").append(isSelected).append(",\n")
        sb.append(fieldIndent).append("\"isClickable\": ").append(isClickable).append(",\n")
        sb.append(fieldIndent).append("\"depth\": ").append(depth).append(",\n")
        sb.append(fieldIndent).append("\"bounds\": {")
            .append("\"left\": ").append(bounds.left).append(", ")
            .append("\"top\": ").append(bounds.top).append(", ")
            .append("\"right\": ").append(bounds.right).append(", ")
            .append("\"bottom\": ").append(bounds.bottom)
            .append("},\n")

        sb.append(fieldIndent).append("\"children\": [")
        if (children.isEmpty()) {
            sb.append("]\n")
        } else {
            sb.append("\n")
            for (i in children.indices) {
                sb.append(" ".repeat((currentIndentLevel + 2) * indentSize))
                children[i].buildJson(sb, currentIndentLevel + 2, indentSize)
                if (i < children.size - 1) {
                    sb.append(",")
                }
                sb.append("\n")
            }
            sb.append(fieldIndent).append("]\n")
        }
        sb.append(baseIndent).append("}")
    }
}

/**
 * Geometric bounding rectangle for a view node.
 */
data class RectData(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val isEmpty: Boolean get() = left >= right || top >= bottom
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)

    companion object {
        val EMPTY = RectData(0, 0, 0, 0)
    }
}

