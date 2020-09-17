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
        // Start with the terminals (leaves)
        val subtrees = terminals.map { Subtree(it, emptySet(), demandTree.neighbors(it).first()) }.toMutableList()
        val currentTrees = subtrees.toMutableSet()

        while (currentTrees.size > 1) {
            currentTrees.groupBy { it.parent }.forEach { (newRoot, trees) ->
                if (trees.size <= 1) return@forEach

                val newChildren = trees.map { it.root }.toSet()
                val newParent = (demandTree.neighbors(newRoot!!) - newChildren).firstOrNull()
                val newSubtree = Subtree(newRoot, newChildren, newParent)

                subtrees.add(newSubtree)
                currentTrees.add(newSubtree)
                currentTrees.removeAll(trees)
            }
        }

        return subtrees
    }
}
