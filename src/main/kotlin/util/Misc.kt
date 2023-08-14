package network.warzone.api.util

import java.util.*

/**
 * The first character of a String is uppercased. The reamining characters are lowercased.
 *
 * Example:
 * <b>Input: hELlO</b>
 * <b>Output: Hello</b>
 *
 */
fun String.capitalizeFirst(): String {
    if (this.isEmpty()) return ""
    return this[0].uppercaseChar() + this.substring(1).lowercase(Locale.getDefault())
}