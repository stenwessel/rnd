package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph

interface HierarchicalHubbing<V> {

    val graph: WeightedGraph<V>
    val demandTree: WeightedGraph<V>
    val terminals: Set<V>

    fun computeSolution(): HubbingResult<V>

    data class HubbingResult<V>(val cost: Double, val mappings: Set<Map<V, V>>)
}
