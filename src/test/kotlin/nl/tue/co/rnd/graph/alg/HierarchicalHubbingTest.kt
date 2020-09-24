package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class HierarchicalHubbingTest {

    @Test
    fun simpleStar() {
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

        val (costEnum, _) = EnumerateHH(graph, demandTree, terminals).computeSolution()
        val (costDp, _) = DynamicProgramHH(graph, demandTree, terminals).computeSolution()

        val (costMip) = MipVpnSolver(graph, demandTree, terminals).computeSolution()

        assertEquals(costEnum, costDp)
        assertEquals(costEnum, costMip)
    }

    @Test
    fun fromOlverNoteOnFigure1() {
        val graph = WeightedGraph(setOf(1, 2, 3, 4, 5, 6, -1, -2, -3), setOf(
                WeightedEdge(1, -1, 1.0),
                WeightedEdge(6, -1, 1.0),
                WeightedEdge(2, -1, 1.0),
                WeightedEdge(-1, -2, 1.0),
                WeightedEdge(-1, -3, 1.0),
                WeightedEdge(-2, 2, 1.0),
                WeightedEdge(-2, 3, 1.0),
                WeightedEdge(-2, 5, 1.0),
                WeightedEdge(-2, -3, 1.0),
                WeightedEdge(-3, 5, 1.0),
                WeightedEdge(-3, 4, 1.0),
                WeightedEdge(-3, 1, 1.0),
        ))

        val demandTree = WeightedGraph(setOf(1, 2, 3, 4, 5, 6, -1, -2, -3), setOf(
                WeightedEdge(1, -2, 1.0),
                WeightedEdge(2, -2, 3.0),
                WeightedEdge(3, -2, 2.0),
                WeightedEdge(4, -1, 4.0),
                WeightedEdge(5, -3, 4.0),
                WeightedEdge(6, -3, 4.0),
                WeightedEdge(-2, -1, 4.0),
                WeightedEdge(-3, -1, 4.0),
        ))

        val terminals = setOf(1, 2, 3, 4, 5, 6)

        val (costEnum, _) = EnumerateHH(graph, demandTree, terminals).computeSolution()
        val (costDp, _) = DynamicProgramHH(graph, demandTree, terminals).computeSolution()

        val (costMip) = MipVpnSolver(graph, demandTree, terminals).computeSolution()

        assertEquals(costEnum, costDp)
        assertEquals(costEnum, costMip)
    }

    @Test
    fun davidsTestCase() {
        val graph = WeightedGraph(setOf(1, 2, 3, 4, 5, 6), setOf(
                WeightedEdge(1, 2, 1.0),
                WeightedEdge(1, 4, 4.0),
                WeightedEdge(2, 3, 4.0),
                WeightedEdge(2, 4, 2.0),
                WeightedEdge(3, 4, 1.0),
                WeightedEdge(1, 5, 6.0),
                WeightedEdge(4, 5, 9.0),
                WeightedEdge(5, 6, 2.0),
                WeightedEdge(2, 6, 1.5),
        ))

        val demandTree = WeightedGraph(setOf(1, 2, 3, 4, 5), setOf(
                WeightedEdge(1, 4, 1.0),
                WeightedEdge(2, 4, 3.0),
                WeightedEdge(3, 5, 2.0),
                WeightedEdge(4, 5, 4.0),
        ))

        val terminals = setOf(1, 2, 3)

        val (costEnum, _) = EnumerateHH(graph, demandTree, terminals).computeSolution()
        val (costDp, _) = DynamicProgramHH(graph, demandTree, terminals).computeSolution()

        val (costMip) = MipVpnSolver(graph, demandTree, terminals).computeSolution()

        assertEquals(costEnum, costDp)
        assertEquals(costEnum, costMip)
    }
}
