package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.Edge
import nl.tue.co.rnd.graph.Subtree
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.HierarchicalHubbing.HubbingResult

class DynamicProgramHH<V>(override val graph: WeightedGraph<V>, override val demandTree: WeightedGraph<V>, override val terminals: Set<V>) : HierarchicalHubbing<V> {

    override fun computeSolution(): HubbingResult<V> {
        val subtrees = buildSubtrees()
        val distance = FloydWarshall(graph).computeShortestPaths()

        val cost = mutableMapOf<Pair<Subtree<V>, V>, Double>()

        for (tree in subtrees) {
            for (v in graph.vertices) {
                cost[tree to v] = when {
                    tree.children.isEmpty() && tree.root == v -> 0.0
                    tree.children.isEmpty() && tree.root != v -> Double.POSITIVE_INFINITY
                    else -> tree.children.sumByDouble { child ->
                        graph.vertices.minOf { w ->
                            cost[subtrees.find { it.root == child } to w]!! + demandTree.weightOf(Edge(tree.root, child))!! * distance[v to w]!!
                        }
                    }
                }
            }
        }

        return HubbingResult(graph.vertices.minOf { v -> cost[subtrees.last() to v] ?: Double.POSITIVE_INFINITY }, emptySet())
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
}
