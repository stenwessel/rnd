package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedGraph

interface GeneralizedVpnSolver<V> {

    val graph: WeightedGraph<V>
    val demandTree: WeightedGraph<V>
    val terminals: Set<V>

    fun computeSolution(): VpnResult<V>

    data class VpnResult<V>(val cost: Double)
}
