package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.HierarchicalHubbing.HubbingResult

class EnumerateHH<V>(override val graph: WeightedGraph<V>, override val demandTree: WeightedGraph<V>, override val terminals: Set<V>) : HierarchicalHubbing<V> {

    override fun computeSolution(): HubbingResult<V> {
        val mapping: MutableMap<V, V> = terminals.zip(terminals).toMap(HashMap())

        val treeInternals = demandTree.vertices - terminals
        val (distanceInGraph, _) = FloydWarshall(graph).computeShortestPaths()

        return recursiveAssign(treeInternals, mapping, distanceInGraph)
    }

    private fun recursiveAssign(treeInternals: Set<V>, mapping: MutableMap<V, V>, distanceInGraph: Map<Pair<V, V>, Double>): HubbingResult<V> {
        if (treeInternals.isEmpty()) {
            return HubbingResult(hubbingCost(demandTree, distanceInGraph, mapping), emptySet())
        }

        val i = treeInternals.first()

        var bestCost = Double.POSITIVE_INFINITY
        val bestMappings = mutableSetOf<Map<V, V>>()
        for (j in graph.vertices) {
            mapping[i] = j
            val (cost, _) = recursiveAssign(treeInternals - i, mapping, distanceInGraph)
            if (cost <= bestCost) {
                if (cost < bestCost) {
                    bestMappings.clear()
                }

                bestCost = cost
                bestMappings.add(mapping.toMap())
            }
        }

        return HubbingResult(bestCost, bestMappings)
    }

    private fun hubbingCost(demandTree: WeightedGraph<V>, distanceInGraph: Map<Pair<V, V>, Double>, mapping: Map<V, V>): Double {
        return demandTree.edges.sumByDouble {
            val hu = mapping[it.first] ?: error("Edge vertex not in map!")
            val hv = mapping[it.second] ?: error("Edge vertex not in map!")
            val d = distanceInGraph[hu to hv] ?: error("Distance not defined!")

            it.weight * d
        }
    }
}
