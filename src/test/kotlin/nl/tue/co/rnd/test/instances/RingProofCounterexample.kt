package nl.tue.co.rnd.test.instances

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

        for (mapping in enumerate.mappings) {
            println("${enumerate.cost}: ${mapping[rMin]}, ${mapping[rPlus]}")
            assert(mapping[rMin]!! < -1)
            assert(mapping[rPlus]!! > 1)
        }
    }
}
