package nl.tue.co.rnd.problem

import nl.tue.co.rnd.graph.Edge
import nl.tue.co.rnd.graph.WeightedGraph

typealias UPair<V> = Edge<V>

open class CappedHoseInstance<V>(override val graph: WeightedGraph<V>,
                                 private val terminalCapacity: Map<V, Double>,
                                 private val connectionCapacity: Map<UPair<V>, Double>) : RndInstance<V> {

    override val terminals = terminalCapacity.keys

    fun terminalCapacity(i: V) = terminalCapacity[i] ?: 0.0

    fun connectionCapacity(ij: UPair<V>) = connectionCapacity[ij] ?: 0.0

    fun connectionCapacity(i: V, j: V) = connectionCapacity(UPair(i, j))
}
