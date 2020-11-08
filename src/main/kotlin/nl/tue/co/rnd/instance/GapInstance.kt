package nl.tue.co.rnd.instance

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph

object GapInstance {
    fun constructGraph(numberOfTerminals: Int): WeightedGraph<Int> {
        val terminals = (1..numberOfTerminals).asSequence()
        val edges = terminals.map { i -> WeightedEdge(0, -i, 1.0) } + terminals.flatMap { i ->
            terminals.filter { j -> j != i }.map { j -> WeightedEdge(i, -j, 1.0) }
        }

        return WeightedGraph((-numberOfTerminals..numberOfTerminals).toSet(), edges.toSet())
    }
}
