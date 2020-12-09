package nl.tue.co.rnd.test.instances

import gurobi.GRB
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.CompactMipVpnSolver
import nl.tue.co.rnd.graph.alg.DynamicProgramHH
import nl.tue.co.rnd.graph.alg.EnumerateHH
import org.junit.jupiter.api.Test

internal class RingProofCounterexample {

    @Test
    fun counterexample() {
        val max = 3
        require(max >= 2)

        val left = (1..max).toSet()
        val right = (-max..-1).toSet()
        val terminals = left + right

        val edges = mutableSetOf(
                WeightedEdge(-1, 1, 2.0),
                WeightedEdge(-max, max, 3.0),
                WeightedEdge(1, -2, 1.0),
                WeightedEdge(-1, 2, 1.0),
        )
        (2..max).asSequence().zipWithNext().forEach { (i, j) ->
            edges += WeightedEdge(i, j, 0.0)
            edges += WeightedEdge(-i, -j, 0.0)
        }

        val ring = WeightedGraph(terminals, edges)

        val rPlus = max + 1
        val rMin = -max - 1
        val treeEdges = setOf(WeightedEdge(rPlus, rMin, 1.0)) + terminals.map { WeightedEdge(it, if (it < 0) rMin else rPlus, 1.0) }
        val tree = WeightedGraph(terminals + setOf(max + 1, -max - 1), treeEdges)

        val enumerate = EnumerateHH(ring, tree, terminals).computeSolution()
        val dp = DynamicProgramHH(ring, tree, terminals, backtrack = true).computeSolution()
        println(dp.cost)

        val compactMipVpnSolver = CompactMipVpnSolver(ring, tree, terminals)
        val mip = compactMipVpnSolver.computeSolution()

        val fMin = compactMipVpnSolver.problem.fMin
        val fPlus = compactMipVpnSolver.problem.fPlus
        for ((i, j) in compactMipVpnSolver.terminalSequence) {
            val edges = ring.edges.filter { fMin[Triple(it, i, j)]?.get(GRB.DoubleAttr.X) == 1.0 || fPlus[Triple(it, i, j)]?.get(GRB.DoubleAttr.X) == 1.0 }
            println("$i : $j -> ${edges.joinToString()}")
        }

        for (mapping in enumerate.mappings) {
            println("${enumerate.cost}: ${mapping[rMin]}, ${mapping[rPlus]}")
            assert(mapping[rMin]!! < -1)
            assert(mapping[rPlus]!! > 1)
        }
    }

    @Test
    fun counterexample2TerminalsOnOneSide() {
        val terminals = (1..7).toSet()
        val ring = WeightedGraph(terminals, setOf(
                WeightedEdge(1, 5, 3.0),
                WeightedEdge(5, 6, 0.0),
                WeightedEdge(6, 7, 0.0),
                WeightedEdge(7, 2, 4.0),
                WeightedEdge(2, 4, 1.0),
                WeightedEdge(4, 3, 1.0),
                WeightedEdge(3, 1, 1.0),
        ))

        val tree = WeightedGraph(terminals + setOf(-1, -2), setOf(
                WeightedEdge(-1, -2, 1.0),
                WeightedEdge(-1, 1, 1.0),
                WeightedEdge(-1, 2, 1.0),
                WeightedEdge(-2, 3, 1.0),
                WeightedEdge(-2, 4, 1.0),
                WeightedEdge(-2, 5, 1.0),
                WeightedEdge(-2, 6, 1.0),
                WeightedEdge(-2, 7, 1.0),
        ))

        val enumerate = EnumerateHH(ring, tree, terminals).computeSolution()
        println("Cost: ${enumerate.cost}")
        for (mapping in enumerate.mappings) {
            println("Green root: ${mapping[-1]}, Orange root: ${mapping[-2]}")
        }
    }

    @Test
    fun counterexample2TerminalsOnOneSideReally() {
        val terminals = (1..9).toSet()
        val ring = WeightedGraph(terminals, setOf(
                WeightedEdge(1, 5, 2.0),
                WeightedEdge(5, 6, 1.0),
                WeightedEdge(6, 7, 0.0),
                WeightedEdge(7, 8, 0.0),
                WeightedEdge(8, 9, 1.0),
                WeightedEdge(9, 2, 4.0),
                WeightedEdge(2, 4, 1.0),
                WeightedEdge(4, 3, 1.0),
                WeightedEdge(3, 1, 1.0),
        ))

        val tree = WeightedGraph(terminals + setOf(-1, -2), setOf(
                WeightedEdge(-1, -2, 1.0),
                WeightedEdge(-1, 1, 1.0),
                WeightedEdge(-1, 2, 1.0),
                WeightedEdge(-2, 3, 1.0),
                WeightedEdge(-2, 4, 1.0),
                WeightedEdge(-2, 5, 1.0),
                WeightedEdge(-2, 6, 1.0),
                WeightedEdge(-2, 7, 1.0),
                WeightedEdge(-2, 8, 1.0),
                WeightedEdge(-2, 9, 1.0),
        ))

        val enumerate = EnumerateHH(ring, tree, terminals).computeSolution()
        println("Cost: ${enumerate.cost}")
        for (mapping in enumerate.mappings) {
            println("Green root: ${mapping[-1]}, Orange root: ${mapping[-2]}")
        }
    }

    @Test
    fun counterexample2TerminalsOnOneSide2() {
        val terminals = (1..5).toSet()
        val ring = WeightedGraph(terminals, setOf(
                WeightedEdge(1, 3, 1.0),
                WeightedEdge(3, 4, 1.0),
                WeightedEdge(4, 2, 1.0),
                WeightedEdge(2, 5, 2.0),
                WeightedEdge(5, 1, 2.0),
        ))

        val tree = WeightedGraph(terminals + setOf(-1, -2), setOf(
                WeightedEdge(-1, -2, 1.0),
                WeightedEdge(-1, 1, 1.0),
                WeightedEdge(-1, 2, 1.0),
                WeightedEdge(-2, 3, 1.0),
                WeightedEdge(-2, 4, 1.0),
                WeightedEdge(-2, 5, 1.0),
        ))

        val enumerate = EnumerateHH(ring, tree, terminals).computeSolution()
        println("Cost: ${enumerate.cost}")
        for (mapping in enumerate.mappings) {
            println("Green root: ${mapping[-1]}, Orange root: ${mapping[-2]}")
        }
    }

    @Test
    fun counterexample2TerminalsOnOneSideDavid() {
        val terminals = (1..5).toSet()
        val ring = WeightedGraph(terminals, setOf(
                WeightedEdge(1, 2, 1.0),
                WeightedEdge(2, 3, 1.0),
                WeightedEdge(3, 4, 1.0),
                WeightedEdge(4, 5, 1.0),
                WeightedEdge(5, 1, 100.0),
        ))

        val tree = WeightedGraph(terminals + setOf(-1, -2), setOf(
                WeightedEdge(-1, -2, 1.0),
                WeightedEdge(-1, 1, 1.0),
                WeightedEdge(-1, 2, 1.0),
                WeightedEdge(-2, 3, 1.0),
                WeightedEdge(-2, 4, 1.0),
                WeightedEdge(-2, 5, 1.0),
        ))

        val enumerate = EnumerateHH(ring, tree, terminals).computeSolution()
        println("Cost: ${enumerate.cost}")
        for (mapping in enumerate.mappings) {
            println("Green root: ${mapping[-1]}, Orange root: ${mapping[-2]}")
        }
    }

    @Test
    fun counterexampleLaura() {
        val terminals = setOf('A', 'B') + ('a'..'z')
        val ring = WeightedGraph(terminals, setOf(
                WeightedEdge('A', 'a', 1.5),
                WeightedEdge('B', 'b', 0.1),
                WeightedEdge('a', 'b', 1.5),
                WeightedEdge('A', 'c', 0.4),
                WeightedEdge('B', 'z', 3.2),
        ) + ('c'..'z').zipWithNext { i, j -> WeightedEdge(i, j, 0.0) })

        val tree = WeightedGraph(
                terminals + setOf('1', '2'),
                setOf(
                        WeightedEdge('1', '2', 1.0),
                        WeightedEdge('A', '1', 1.0),
                        WeightedEdge('B', '1', 1.0),
                ) + ('a'..'z').map { WeightedEdge(it, '2', 1.0) })

        val enumerate = EnumerateHH(ring, tree, terminals).computeSolution()
        println("Cost: ${enumerate.cost}")
        for (mapping in enumerate.mappings) {
            println("Blue root: ${mapping['1']}, Red root: ${mapping['2']}")
        }
    }
}
