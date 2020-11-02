package nl.tue.co.rnd.problem

import nl.tue.co.rnd.graph.WeightedGraph

interface RndInstance<V> {

    val graph: WeightedGraph<V>

    val terminals: Set<V>
}
