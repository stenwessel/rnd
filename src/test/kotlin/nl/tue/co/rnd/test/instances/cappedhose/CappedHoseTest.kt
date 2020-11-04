package nl.tue.co.rnd.test.instances.cappedhose

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.PetersenGraphTest
import nl.tue.co.rnd.problem.CappedHoseCycleInstance
import nl.tue.co.rnd.solver.dp.CappedHoseCycleDpSolver
import nl.tue.co.rnd.solver.enum.CappedHoseCycleEnumSolver
import nl.tue.co.rnd.solver.mip.CappedHoseMipSolver
import nl.tue.co.rnd.test.instances.GapInstanceCreator
import nl.tue.co.rnd.util.circular
import org.junit.jupiter.api.Test
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.assertEquals

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

        val instance = CappedHoseCycleInstance(graph, listOf(1, 2, 3, 4).circular(), terminals.associateWith { 1.0 }, listOf(1.0, 1.0, 1.0, 1.0))

        val mipSolver = CappedHoseMipSolver<Int>()
        val (mip) = mipSolver.computeSolution(instance)

        val dpSolver = CappedHoseCycleDpSolver<Int>()
        val (dp) = dpSolver.computeSolution(instance)

        assertEquals(4.0, mip)
        assertEquals(mip, dp)
    }

    @Test
    fun petersen() {
        val graph = PetersenGraphTest.petersen { Random.nextInt(1, 11).toDouble() }
        val terminals = listOf(1, 7, 3, 6, 2).circular()
        val instance = CappedHoseCycleInstance(graph, terminals, terminals.zip(listOf(2.0, 2.0, 3.0, 4.0, 4.0)).toMap(), listOf(2.0, 2.0, 2.0, 2.0, 2.0))

        val mipSolver = CappedHoseMipSolver<Int>()
        val (mip) = mipSolver.computeSolution(instance)

        val dpSolver = CappedHoseCycleDpSolver<Int>()
        val (dp) = dpSolver.computeSolution(instance)

        assertEquals(mip, dp)
    }

    @Test
    fun randomRealInstances() {

    }

    @Test
    fun gapInstance() {
        val seed = 1984
        val random = Random(seed)
        val n = 4
        val graph = GapInstanceCreator().constructGraph(n)

        val terminals = listOf(1, 3, 4, 2).circular()
//        val connectionCapacity = List(n) { 1.0 }.circular()
        val connectionCapacity = List(n) { random.nextInt(1, 11).toDouble() }.circular()
        val terminalCapacity = terminals.withIndex().associate { (i, v) ->
            val prev = connectionCapacity[i-1].toInt()
            val next = connectionCapacity[i].toInt()
//            v to 1.0
            v to random.nextInt(max(prev, next), prev + next + 1).toDouble()
        }

        val instance = CappedHoseCycleInstance(graph, terminals, terminalCapacity, connectionCapacity)

        val mipSolver = CappedHoseMipSolver<Int>()
        val (mip) = mipSolver.computeSolution(instance)

        val dpSolver = CappedHoseCycleDpSolver<Int>()
        val (dp) = dpSolver.computeSolution(instance)

        val enumSolver = CappedHoseCycleEnumSolver()
        val (enum) = enumSolver.computeSolution(instance)

        assertEquals(mip, dp)
    }
}
