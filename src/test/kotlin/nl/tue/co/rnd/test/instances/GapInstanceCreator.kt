package nl.tue.co.rnd.test.instances

import nl.tue.co.rnd.graph.GenVPNInstance
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph

class GapInstanceCreator {
    fun construct(numberOfTerminals: Int): GenVPNInstance<Int> {
        val graph = constructGraph(numberOfTerminals)
        val tree = constructStar(numberOfTerminals)
        return GenVPNInstance(graph, tree, (1..numberOfTerminals).toSet())
    }

    private fun constructGraph(numberOfTerminals: Int): WeightedGraph<Int> {
        val terminals = (1..numberOfTerminals).asSequence()
        val edges = terminals.map { i -> WeightedEdge(0, -i, 1.0) } + terminals.flatMap { i ->
            terminals.filter { j -> j != i }.map { j -> WeightedEdge(i, -j, 1.0) }
        }

        return WeightedGraph((-numberOfTerminals..numberOfTerminals).toSet(), edges.toSet())
    }

    private fun constructStar(numberOfTerminals: Int): WeightedGraph<Int> {
        return WeightedGraph(
                (0..numberOfTerminals).toSet(),
                (1..numberOfTerminals).asSequence().map { WeightedEdge(0, it, 1.0) }.toSet()
        )
    }
}
