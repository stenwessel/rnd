package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class FloydWarshallTest {

    @Test
    fun computeShortestPathsOnPath() {
        val graph = WeightedGraph(setOf(1, 2, 3), setOf(WeightedEdge(1, 2, 2.0), WeightedEdge(2, 3, 3.0)))

        val (distance, _) = FloydWarshall(graph).computeShortestPaths()

        for (pair in graph.vertices.product(graph.vertices)) {
            assertEquals(distance[pair.first to pair.second], distance[pair.second to pair.first], "Distances are not symmetric.")
        }

        assertEquals(2.0, distance[1 to 2])
        assertEquals(5.0, distance[1 to 3])
        assertEquals(3.0, distance[2 to 3])
        assertEquals(0.0, distance[1 to 1])
        assertEquals(0.0, distance[2 to 2])
        assertEquals(0.0, distance[3 to 3])
    }

    @Test
    fun computeShortestPathsOnPath2() {
        val graph = WeightedGraph(setOf(1, 2, 3, 4, 5, 6), setOf(WeightedEdge(1, 2, 1.0), WeightedEdge(2, 3, 2.0),
                                                              WeightedEdge(3, 4, 3.0), WeightedEdge(4, 5, 6.0),
                                                              WeightedEdge(5, 6, 5.0), WeightedEdge(1, 6, 4.0)
        ))

        val (distance, _) = FloydWarshall(graph).computeShortestPaths()

        for (pair in graph.vertices.product(graph.vertices)) {
            assertEquals(distance[pair.first to pair.second], distance[pair.second to pair.first], "Distances are not symmetric.")
        }

        assertEquals(0.0, distance[1 to 1])
        assertEquals(1.0, distance[1 to 2])
        assertEquals(3.0, distance[1 to 3])
        assertEquals(6.0, distance[1 to 4])
        assertEquals(9.0, distance[1 to 5])
        assertEquals(4.0, distance[1 to 6])
    }

}
