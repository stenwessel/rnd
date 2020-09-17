package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph

class FloydWarshall<V>(val graph: WeightedGraph<V>) {

    fun computeShortestPaths(): Map<Pair<V, V>, Double> {
        val distance: MutableMap<Pair<V, V>, Double> = graph.vertices.product(graph.vertices)
                .map { it to Double.POSITIVE_INFINITY }.toMap().toMutableMap()

        for (edge in graph.edges) {
            distance[edge.first to edge.second] = edge.weight
            distance[edge.second to edge.first] = edge.weight
        }

        for (vertex in graph.vertices) {
            distance[vertex to vertex] = 0.0
        }

        for (k in graph.vertices) {
            for (i in graph.vertices) {
                for (j in graph.vertices) {
                    if (distance[i to j]!! > distance[i to k]!! + distance[k to j]!!) {
                        distance[i to j] = distance[i to k]!! + distance[k to j]!!
                        distance[j to i] = distance[i to k]!! + distance[k to j]!!
                    }
                }
            }
        }

        return distance
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
