package nl.tue.co.rnd.graph.alg

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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
        val (costCompactMip) = CompactMipVpnSolver(graph, demandTree, terminals).computeSolution()

        assertEquals(costEnum, costDp)
        assertEquals(costEnum, costMip)
        assertEquals(costEnum, costCompactMip)
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
        val dp = DynamicProgramHH(graph, demandTree, terminals, backtrack = true).computeSolution()
        val (costDp, _) = dp

        val (costMip) = MipVpnSolver(graph, demandTree, terminals).computeSolution()
        val (costCompactMip) = CompactMipVpnSolver(graph, demandTree, terminals).computeSolution()

        assertEquals(costEnum, costDp)
        assertEquals(costEnum, costMip)
        assertEquals(costEnum, costCompactMip)
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
        val dp = DynamicProgramHH(graph, demandTree, terminals, backtrack = true).computeSolution()
        val (costDp, _) = dp

        val (costMip) = MipVpnSolver(graph, demandTree, terminals).computeSolution()
        val (costCompactMip) = CompactMipVpnSolver(graph, demandTree, terminals).computeSolution()

        assertEquals(costEnum, costDp)
        assertEquals(costEnum, costMip)
        assertEquals(costEnum, costCompactMip)
    }

    @Test
    @ExperimentalTime
    fun largeLodiRing() {
        val type1Terminals = setOf(1, 2, 3, 15, 20, 30, 31, 32, 33, 42, 45, 55, 56, 65, 75, 80, 90)
        val type2Terminals = setOf(4, 5, 10, 25, 34, 35, 40, 41, 50, 60, 70, 85, 95)

        val random = Random(1985)

        while (true) {
            val (graph, demandTree, terminals) = createLodiRingInstance(100, type1Terminals, type2Terminals, random)

            val costDp: Double
            val costEnum: Double
            val costCompactMip: Double

            val dpTime = measureTime {
                val result = DynamicProgramHH(graph, demandTree, terminals).computeSolution()
                costDp = result.cost
            }

            println("DP: ${dpTime.inSeconds} s with solution $costDp")

            val enumTime = measureTime {
                val result = EnumerateHH(graph, demandTree, terminals).computeSolution()
                costEnum = result.cost
            }

            println("Enum: ${enumTime.inSeconds} s with solution $costEnum")

            val compactMipTime = measureTime {
                val result = CompactMipVpnSolver(graph, demandTree, terminals).computeSolution()
                costCompactMip = result.cost
            }

            println("MIP: ${compactMipTime.inSeconds} s with solution $costCompactMip")

            if (costDp != costEnum || costDp != round(costCompactMip)) {
                println("Hahaha!!")
                println("Bridge capacity: ${demandTree.edges.find { it.first < 0 && it.second < 0 }!!.weight}")
                println("Sides of the tree:")
                for (terminal in terminals) {
                    println("$terminal: ${demandTree.incidentEdges(terminal)[0].second}")
                }
                assert(false)
                return
            }
        }

    }

    private fun createLodiRingInstance(nodes: Int, type1Terminals: Set<Int>, type2Terminals: Set<Int>, random: Random): Triple<WeightedGraph<Int>, WeightedGraph<Int>, Set<Int>> {
        val ring = WeightedGraph(
                (1..nodes).toSet(),
                (1..nodes).asSequence()
                        .windowed(2, partialWindows = true)
                        .map { WeightedEdge(it[0], it.getOrElse(1) { 1 }, 1.0) }
                        .toSet()
        )

        val terminals = type1Terminals + type2Terminals

        val tree = WeightedGraph(
                terminals + setOf(-1, -2),
                (type1Terminals.map { WeightedEdge(it, if (random.nextBoolean()) -1 else -2, 401.0) } + type2Terminals.map { WeightedEdge(it, if (random.nextBoolean()) -1 else -2, 700.0) } + WeightedEdge(-1, -2, round(random.nextDouble() * 300.0))).toSet()
        )

        return Triple(ring, tree, terminals)
    }
}
