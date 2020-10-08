package nl.tue.co.rnd.graph

fun <V> findPathInTree(i: V, j: V, tree: WeightedGraph<V>): Set<WeightedEdge<V>> {
    val visited = mutableMapOf<V, Set<WeightedEdge<V>>>(i to emptySet())

    val boundary = ArrayDeque<WeightedEdge<V>>()
    boundary.addAll(tree.incidentEdges(i))

    while (boundary.isNotEmpty()) {
        val currentEdge = boundary.removeFirst()

        val discovered = if (currentEdge.first !in visited) currentEdge.first else currentEdge.second
        val from = if (currentEdge.first !in visited) currentEdge.second else currentEdge.first

        visited[discovered] = visited[from]!! + currentEdge

        if (discovered == j) {
            return visited[discovered]!!
        }

        for (newEdge in tree.incidentEdges(discovered)) {
            if (newEdge == currentEdge) continue

            boundary.addLast(newEdge)
        }
    }

    return emptySet()
}
