package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph

interface HierarchicalHubbing<V> {

    val graph: WeightedGraph<V>
    val demandTree: WeightedGraph<V>
    val terminals: Set<V>

    fun computeSolution(): HubbingResult<V>

    data class HubbingResult<V>(val cost: Double, val mappings: Set<Map<V, V>>, val edgeMapping: Map<WeightedEdge<V>, List<V>> = emptyMap(),
                                val routingTemplate: Map<Pair<V, V>, List<V>> = emptyMap(),
                                val boughtCapacity: Map<WeightedEdge<V>, Double> = emptyMap())
}
