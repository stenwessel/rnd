package nl.tue.co.rnd.test.instances

import gurobi.GRB
import gurobi.GRBEnv
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.CompactMipVpnSolver
import nl.tue.co.rnd.graph.alg.DynamicProgramHH
import nl.tue.co.rnd.graph.alg.EnumerateHH
import org.junit.jupiter.api.Test
import kotlin.math.min

internal class GapInstanceTest {

    companion object {
        private val ZERO = '0'.toInt()

        fun constructGraph(numberOfTerminals: Int, demandTree: WeightedGraph<Int>): WeightedGraph<Int> {
            val terminals = (1..numberOfTerminals).asSequence()
            val edges = terminals.flatMap { i ->
                terminals.filter { j -> j != i }.map { j -> WeightedEdge(i, -j, 1.0) }
            } + demandTree.edges.asSequence()
                    .map {
                        val first = when (it.first) {
                            -1 -> Int.MAX_VALUE
                            -2 -> Int.MIN_VALUE
                            else -> -it.first
                        }
                        val second = when (it.second) {
                            -1 -> Int.MAX_VALUE
                            -2 -> Int.MIN_VALUE
                            else -> -it.second
                        }
                        WeightedEdge(first, second, it.weight)
                    }

            return WeightedGraph((-numberOfTerminals..numberOfTerminals).toSet() - 0 + setOf(Int.MIN_VALUE, Int.MAX_VALUE), edges.toSet())
        }
    }

    @Test
    fun originalGapInstance() {
        val instance = GapInstanceCreator().construct(3)

        val mipSolver = CompactMipVpnSolver(instance.graph, instance.demandTree, instance.terminals)
        val dpSolver = DynamicProgramHH(instance.graph, instance.demandTree, instance.terminals)
        val enumSolver = EnumerateHH(instance.graph, instance.demandTree, instance.terminals)

        val (mip, model) = mipSolver.computeSolution()
        val dp = dpSolver.computeSolution().cost
        val enum = enumSolver.computeSolution().cost

        val lp = model.relax()
        lp.optimize()
        val relax = lp[GRB.DoubleAttr.ObjVal]
        val gap = mip / relax

        println("DP: $dp, Enum: $enum, Mip: $mip, Relax: $relax, Gap: $gap")
    }

    /**
     * Test the gap instance from the ACM _The VPN Conjecture is True_ paper, on n terminals, using all unit-capacity
     * two-star union options
     */
    @Test
    fun gapInstance() {
        val n = 6
        val terminals = (1..n).toSet()

        val env = GRBEnv(false)

        for (i in 1 until (0b1 shl (n-1))) {
            val partition = i.toString(radix = 2).padStart(n, '0')
            val maxWeight = min(partition.count { it == '0' }, partition.count { it == '1' }) + 1

            // Ony one terminal on one side: this is essentially a star
            if (maxWeight == 1) continue

            for (w in 1..maxWeight) {
                val weight = w.toDouble()
                println("$partition, b(r): $weight")

                val demandTree = WeightedGraph(
                        (-2..n).toSet() - 0,
                        partition.mapIndexed { j, side -> WeightedEdge(j + 1, side.toInt() - ZERO - 2, 1.0) }.toSet() + WeightedEdge(-1, -2, weight)
                )

                val graph = GapInstanceCreator().constructGraph(n)
//                val graph = constructGraph(n, demandTree)

                val mipSolver = CompactMipVpnSolver(graph, demandTree, terminals, env)
                val dpSolver = DynamicProgramHH(graph, demandTree, terminals)
                val enumSolver = EnumerateHH(graph, demandTree, terminals)

                val (mip, model) = mipSolver.computeSolution()
                val dp = dpSolver.computeSolution().cost
                val enum = enumSolver.computeSolution().cost

                val lp = model.relax()
                lp.optimize()
                val relax = lp[GRB.DoubleAttr.ObjVal]
                val gap = mip / relax

                println("DP: $dp, Enum: $enum, Mip: $mip, Relax: $relax, Gap: $gap")
            }
        }

    }
}
