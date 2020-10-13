package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.*
import nl.tue.co.rnd.graph.alg.HierarchicalHubbing.HubbingResult

class DynamicProgramHH<V>(override val graph: WeightedGraph<V>, override val demandTree: WeightedGraph<V>, override val terminals: Set<V>, private val backtrack: Boolean = false) : HierarchicalHubbing<V> {

    override fun computeSolution(): HubbingResult<V> {
        val subtrees = buildSubtrees()
        val rootToSubtree = subtrees.associateBy { it.root }
        val (distance, shortestPath) = FloydWarshall(graph).computeShortestPaths()

        val cost = mutableMapOf<Pair<Subtree<V>, V>, Double>()

        for (tree in subtrees) {
            for (v in graph.vertices) {
                cost[tree to v] = when {
                    tree.children.isEmpty() && tree.root == v -> 0.0
                    tree.children.isEmpty() && tree.root != v -> Double.POSITIVE_INFINITY
                    else -> tree.children.sumByDouble { child ->
                        graph.vertices.minOf { w ->
                            cost[rootToSubtree[child] to w]!! + demandTree.weightOf(Edge(tree.root, child))!! * distance[v to w]!!
                        }
                    }
                }
            }
        }

        val rootSubtree = subtrees.last()
        val (rootMapping, minCost) = graph.vertices.minByWithValue { v -> cost[rootSubtree to v] ?: Double.POSITIVE_INFINITY }!!

        if (!backtrack) return HubbingResult(minCost, emptySet())

        val mapping = mutableMapOf(rootSubtree.root to rootMapping)
        val capacity = graph.edges.associate { (it.first to it.second) to (it to 0.0) }.toMutableMap()

        // Backtrack to find the optimal mapping
        backtrack(cost, rootToSubtree, distance, shortestPath, rootSubtree, rootMapping, mapping, capacity)

        // Find the actual solution paths
        val terminalsSequence = sequence {
            for (i in terminals) {
                for (j in terminals) {
                    if (i != j) yield(i to j)
                }
            }
        }

        val paths = terminalsSequence.asSequence()
                .map { ij ->
                    val (i, j) = ij
                    val nonSimplePath = findPathInTree(i, j, demandTree).asSequence()
                            .windowed(2, partialWindows = true)
                            .flatMap { edges ->
                                val from: V
                                val to: V
                                if (edges.size == 1) { // Last edge in path
                                    val (e1) = edges
                                    from = if (e1.first == j) e1.second else e1.first
                                    to = if (from == e1.first) e1.second else e1.first
                                }
                                else {
                                    val (e1, e2) =  edges
                                    from = if (e1.first == e2.first || e1.first == e2.second) e1.second else e1.first
                                    to = if (from == e1.first) e1.second else e1.first
                                }

                                val hz = mapping[from]!!
                                val hw = mapping[to]!!

                                shortestPath[hz to hw]!!
                            }
                            .toList()

                    val path = removeCycles(nonSimplePath)
                    ij to path
                }.toMap()

        val edgeMapping = demandTree.edges.associateWith { shortestPath[mapping[it.first] to mapping[it.second]]!! }

        return HubbingResult(minCost, setOf(mapping), edgeMapping, paths, capacity.values.toMap())
    }

    private fun backtrack(cost: Map<Pair<Subtree<V>, V>, Double>,
                          rootToSubtree: Map<V, Subtree<V>>,
                          distance: Map<Pair<V, V>, Double>,
                          shortestPath: Map<Pair<V, V>, List<V>>,
                          currentSubtree: Subtree<V>,
                          currentV: V,
                          mapping: MutableMap<V, V>,
                          capacity: MutableMap<Pair<V, V>, Pair<WeightedEdge<V>, Double>>) {
        for (child in currentSubtree.children) {
            val be = demandTree.weightOf(Edge(currentSubtree.root, child))!!

            val childMapping = graph.vertices.minByOrNull { w -> cost[rootToSubtree[child] to w]!! + be * distance[currentV to w]!! }!!
            mapping[child] = childMapping

            // Update capacities
            val mappedPath = shortestPath[currentV to childMapping]!!
            mappedPath.asSequence()
                    .windowed(2, partialWindows = false)
                    .forEach { (u, v) ->
                        if (capacity.containsKey(u to v)) {
                            val (e, cap) = capacity[u to v]!!
                            capacity[u to v] = e to cap + be
                        }
                        else {
                            val (e, cap) = capacity[v to u]!!
                            capacity[v to u] = e to cap + be
                        }
                    }

            backtrack(cost, rootToSubtree, distance, shortestPath, rootToSubtree[child]!!, childMapping, mapping, capacity)
        }
    }

    private fun buildSubtrees(): List<Subtree<V>> {
        // Choose an arbitrary root (that is not a leaf)
        val root = demandTree.vertices.find { it !in terminals } ?: error("Demand tree is not rootable from a non-leaf vertex.")

        // The discovered subtrees must be stored bottom-up, hence they must be stored from the start of the collection.
        // We use an ArrayDeque to do this efficiently
        val subtrees = ArrayDeque<Subtree<V>>()
        subtrees.addFirst(Subtree(root, demandTree.neighbors(root), parent = null))

        // Current trees that we are considering
        val currentTrees = ArrayDeque(subtrees)

        while (currentTrees.isNotEmpty()) {
            val currentTree = currentTrees.removeFirst()
            for (newRoot in currentTree.children) {
                val newSubtree = Subtree(newRoot, (demandTree.neighbors(newRoot) - currentTree.root), currentTree.root)
                subtrees.addFirst(newSubtree)
                currentTrees.addLast(newSubtree)
            }
        }

        return subtrees
    }

    private fun removeCycles(path: List<V>): List<V> {
        val seenAt = mutableMapOf<V, Int>()
        val cycleIndices = mutableSetOf<Int>()
        for ((i, v) in path.withIndex()) {
            if (v in seenAt) {
                val start = seenAt[v]!! + 1
                cycleIndices.addAll(start..i)

                for (j in start until i) {
                    seenAt.remove(path[j])
                }
            }

            seenAt[v] = i
        }

        return path.filterIndexed { i, _ -> i !in cycleIndices }
    }
}


fun <T, R : Comparable<R>> Iterable<T>.minByWithValue(selector: (T) -> R): Pair<T, R>? {
    return this.asSequence()
            .map { it to selector(it) }
            .minByOrNull { (_, s) -> s }
}
