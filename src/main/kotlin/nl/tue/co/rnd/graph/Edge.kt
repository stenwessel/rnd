package nl.tue.co.rnd.graph

import kotlin.math.max
import kotlin.math.min

/**
 * Edge between vertices [V].
 */
open class Edge<V>(val first: V, val second: V) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge<*>

        if (first != other.first && first != other.second) return false
        if (second != other.second && second != other.first) return false

        return true
    }

    override fun hashCode(): Int {
        val firstHash = first?.hashCode() ?: 0
        val secondHash = second?.hashCode() ?: 0
        val minHash = min(firstHash, secondHash)
        val maxHash = max(firstHash, secondHash)

        var result = minHash
        result = 31 * result + maxHash
        return result
    }

    operator fun component1() = first

    operator fun component2() = second

    operator fun contains(element: Any) = element == first || element == second

    override fun toString() = "{$first, $second}"
}
