package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedGraph

typealias Path<V> = List<V>

class FloydWarshall<V>(val graph: WeightedGraph<V>) {

    fun computeShortestPaths(): Pair<Map<Pair<V, V>, Double>, Map<Pair<V, V>, Path<V>>> {
        val distance: MutableMap<Pair<V, V>, Double> = graph.vertices.product(graph.vertices)
                .map { it to Double.POSITIVE_INFINITY }.toMap().toMutableMap()

        val next: MutableMap<Pair<V, V>, V?> = graph.vertices.product(graph.vertices)
                .map { it to null }.toMap().toMutableMap()

        for (edge in graph.edges) {
            distance[edge.first to edge.second] = edge.weight
            distance[edge.second to edge.first] = edge.weight
            next[edge.first to edge.second] = edge.second
            next[edge.second to edge.first] = edge.first
        }

        for (vertex in graph.vertices) {
            distance[vertex to vertex] = 0.0
            next[vertex to vertex] = vertex
        }

        for (k in graph.vertices) {
            for (i in graph.vertices) {
                for (j in graph.vertices) {
                    if (distance[i to j]!! > distance[i to k]!! + distance[k to j]!!) {
                        distance[i to j] = distance[i to k]!! + distance[k to j]!!
                        distance[j to i] = distance[i to k]!! + distance[k to j]!!

                        next[i to j] = next[i to k]
                        next[j to i] = next[j to k]
                    }
                }
            }
        }

        val paths = graph.vertices.product(graph.vertices)
                .map { it to path(it.first, it.second, next) }
                .toMap()

        return distance to paths
    }

    private fun path(u: V, v: V, next: Map<Pair<V, V>, V?>): Path<V> {
        if (next[u to v] == null) return emptyList()

        val path = mutableListOf(u)
        var currentVertex = u
        while (currentVertex != v) {
            currentVertex = next[currentVertex to v] ?: error("Path is broken")
            path += currentVertex
        }

        return path
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

fun <T, U, V> Iterable<T>.product(other1: Iterable<U>, other2: Iterable<V>): Sequence<Triple<T, U, V>> {
    return sequence {
        for (u in this@product) {
            for (v in other1) {
                for (w in other2) {
                    yield(Triple(u, v, w))
                }
            }
        }
    }
}

