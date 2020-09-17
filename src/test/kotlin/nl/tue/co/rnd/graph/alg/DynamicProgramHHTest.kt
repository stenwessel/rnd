package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class DynamicProgramHHTest {

    @Test
    fun computeSolution() {
        val graph = WeightedGraph(setOf(1, 2, 3, 4), setOf(
                WeightedEdge(1, 2, 1.0),
                WeightedEdge(1, 4, 4.0),
                WeightedEdge(2, 3, 4.0),
                WeightedEdge(2, 4, 2.0),
                WeightedEdge(3, 4, 1.0),
        ))

        val demandTree = WeightedGraph(setOf(1, 2, 3, 4, 5), setOf(
                WeightedEdge(1, 5, 1.0),
                WeightedEdge(2, 5, 3.0),
                WeightedEdge(3, 5, 2.0),
                WeightedEdge(4, 5, 4.0),
        ))

        val terminals = setOf(1, 2, 3, 4)

        val (cost, mappings) = EnumerateHH(graph, demandTree, terminals).computeSolution()

        assertEquals(11.0, cost)
    }
}
