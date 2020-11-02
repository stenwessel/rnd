package nl.tue.co.rnd.util

fun <T> Collection<T>.pairs(): Sequence<Pair<T, T>> {
    val list = this.toList()
    return sequence {
        for (i in list.indices) {
            for (j in (i + 1) until list.size) {
                yield(list[i] to list[j])
            }
        }
    }
}

fun <T, U> Iterable<T>.product(other: Iterable<U>): Sequence<Pair<T, U>> {
    return sequence {
        for (u in this@product) {
            for (v in other) {
                yield(u to v)
            }
        }
    }
}
