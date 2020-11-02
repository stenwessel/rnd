package nl.tue.co.rnd.util

/**
 * List with circular indexing: the index is reduced modulo the size of the list.
 *
 * If `n` is the size of the list, the index `n` would normally be out of bounds, but now returns the element at
 * position `0`.
 */
class CircularList<out T>(private val list: List<T>) : List<T> by list {

    override fun get(index: Int): T = list[index.posmod()]

    private fun Int.posmod(): Int {
        val reduced = this % size
        return if (reduced >= 0) reduced else reduced + size
    }
}

fun <T> List<T>.circular() = CircularList(this)
