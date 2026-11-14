package com.nidaa.prayer.update

/**
 * Minimal semantic-version comparator used by the update checker.
 *
 * Supports the subset GitHub tags actually use for Nidaa:
 *  - an optional `v`/`V` prefix (`v1.0.0-beta7.95`),
 *  - one to three numeric core fields (`1.0` == `1.0.0`),
 *  - build metadata (`+5`, ignored),
 *  - dot-separated pre-release identifiers (`beta7.95`), with numeric
 *    identifiers compared numerically (so `beta7.95` > `beta7.9`).
 *
 * Anything else (empty, letters in the core, more than three core fields) is
 * rejected, and the caller treats an unparsable tag as "no update".
 */
class SemanticVersion private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String>
) : Comparable<SemanticVersion> {

    val isPreRelease: Boolean
        get() = preRelease.isNotEmpty()

    override fun compareTo(other: SemanticVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        patch.compareTo(other.patch).let { if (it != 0) return it }

        if (preRelease.isEmpty() || other.preRelease.isEmpty()) {
            return when {
                preRelease.isEmpty() && other.preRelease.isEmpty() -> 0
                preRelease.isEmpty() -> 1   // release > pre-release
                else -> -1
            }
        }

        val count = maxOf(preRelease.size, other.preRelease.size)
        for (index in 0 until count) {
            val left = preRelease.getOrNull(index) ?: return -1 // shorter pre-release sorts first
            val right = other.preRelease.getOrNull(index) ?: return 1
            val leftNumber = left.toLongOrNull()
            val rightNumber = right.toLongOrNull()
            val result = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1 // numeric identifiers sort before alphanumeric
                rightNumber != null -> 1
                else -> left.compareTo(right)
            }
            if (result != 0) return result
        }
        return 0
    }

    override fun equals(other: Any?): Boolean =
        other is SemanticVersion && compareTo(other) == 0

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + preRelease.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        append(major).append('.').append(minor).append('.').append(patch)
        if (preRelease.isNotEmpty()) append('-').append(preRelease.joinToString("."))
    }

    companion object {
        fun parse(raw: String?): SemanticVersion? {
            var value = raw?.trim().orEmpty()
            if (value.isEmpty()) return null
            if (value.first() == 'v' || value.first() == 'V') value = value.substring(1)
            value = value.substringBefore('+')
            if (value.isEmpty()) return null

            val coreText = value.substringBefore('-')
            val preRelease = if (value.contains('-')) {
                value.substringAfter('-').split('.').filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val parts = coreText.split('.')
            if (parts.isEmpty() || parts.size > 3) return null
            val numbers = parts.map { part ->
                if (part.isEmpty() || part.any { !it.isDigit() }) return null
                part.toIntOrNull() ?: return null
            }
            return SemanticVersion(
                major = numbers.getOrElse(0) { 0 },
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 },
                preRelease = preRelease
            )
        }
    }
}
