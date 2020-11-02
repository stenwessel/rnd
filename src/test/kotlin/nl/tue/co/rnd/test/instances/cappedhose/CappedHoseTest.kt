package nl.tue.co.rnd.test.instances.cappedhose

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.problem.CappedHoseInstance
import nl.tue.co.rnd.problem.UPair
import nl.tue.co.rnd.solver.mip.CappedHoseMipSolver
import org.junit.jupiter.api.Test

class CappedHoseTest {

    @Test
    fun smallExample() {
        val terminals = setOf(1, 2, 3, 4)
        val graph = WeightedGraph(terminals, setOf(
                WeightedEdge(1, 2, 1.0),
                WeightedEdge(2, 3, 1.0),
                WeightedEdge(3, 4, 1.0),
                WeightedEdge(4, 1, 1.0),
        ))

        val instance = CappedHoseInstance(graph, terminals.associateWith { 1.0 }, graph.edges.associate { UPair(it.first, it.second) to 1.0 })

        val mipSolver = CappedHoseMipSolver<Int>()
        mipSolver.computeSolution(instance)
    }
}
