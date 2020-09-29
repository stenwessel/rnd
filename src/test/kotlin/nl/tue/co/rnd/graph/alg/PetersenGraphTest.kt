package nl.tue.co.rnd.graph.alg

import gurobi.GRB
import gurobi.GRBEnv
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.test.assertEquals

internal class PetersenGraphTest {

    @Test
    fun unitCostUnitCapacityStarUnion() {
        val terminals = (1..10).toSet()

        val graph = WeightedGraph(
                (1..10).toSet(),
                setOf(
                        WeightedEdge(1, 2, 1.0),
                        WeightedEdge(2, 3, 1.0),
                        WeightedEdge(3, 4, 1.0),
                        WeightedEdge(4, 5, 1.0),
                        WeightedEdge(5, 1, 1.0),
                        WeightedEdge(1, 6, 1.0),
                        WeightedEdge(2, 7, 1.0),
                        WeightedEdge(3, 8, 1.0),
                        WeightedEdge(4, 9, 1.0),
                        WeightedEdge(5, 10, 1.0),
                        WeightedEdge(6, 8, 1.0),
                        WeightedEdge(8, 10, 1.0),
                        WeightedEdge(10, 7, 1.0),
                        WeightedEdge(7, 9, 1.0),
                        WeightedEdge(9, 6, 1.0),
                )
        )

        // Construct every unit capacity two star-union tree, by looping over all partitions.
        // Exclude the cases where all nodes are on one side (as this will introduce a leaf that is not a terminal)
        for (i in 1 until 0b1000000000) {
            val partition = i.toString(radix = 2).padStart(10, '0')
            print("$partition ")

            val demandTree = WeightedGraph(
                    (-2..10).toSet(),
                    partition.mapIndexed { j, side -> WeightedEdge(j + 1, side.toInt() - 48 - 2, 1.0) }.toSet() + WeightedEdge(-1, -2, 1.0)
            )

            val enum = EnumerateHH(graph, demandTree, terminals).computeSolution().cost
            println(enum)
            val dp = DynamicProgramHH(graph, demandTree, terminals).computeSolution().cost
            val mip = CompactMipVpnSolver(graph, demandTree, terminals).computeSolution().cost


            assertEquals(enum, dp, "Enum and DP do not match for partition $partition")
            assertEquals(enum, round(mip), "MIP does not match for partition $partition")
        }
    }

    @Test
    fun unitCostUnitCapacityStarUnionAllTerminalSets() {
        val graph = WeightedGraph(
                (1..10).toSet(),
                setOf(
                        WeightedEdge(1, 2, 1.0),
                        WeightedEdge(2, 3, 1.0),
                        WeightedEdge(3, 4, 1.0),
                        WeightedEdge(4, 5, 1.0),
                        WeightedEdge(5, 1, 1.0),
                        WeightedEdge(1, 6, 1.0),
                        WeightedEdge(2, 7, 1.0),
                        WeightedEdge(3, 8, 1.0),
                        WeightedEdge(4, 9, 1.0),
                        WeightedEdge(5, 10, 1.0),
                        WeightedEdge(6, 8, 1.0),
                        WeightedEdge(8, 10, 1.0),
                        WeightedEdge(10, 7, 1.0),
                        WeightedEdge(7, 9, 1.0),
                        WeightedEdge(9, 6, 1.0),
                )
        )

        val file = File("unitCostUnitCapacityStarUnionAllTerminalSets.csv")
        file.appendText("terminals,partition,bridgeWeight,enum,dp,mip,agree,nodecount,itercount\n")

        val env = GRBEnv(true)

        for (t in 3..0b1111111111) {
            val terminalsBitStr = t.toString(radix = 2).padStart(10, '0')
            val terminals = terminalsBitStr.mapIndexedNotNull { index, c -> if (c == '1') index + 1 else null }

            if (terminals.size < 2) continue

            val terminalsSet = terminals.toSet()

            for (i in 1 until 2.0.pow(terminals.size - 1).toInt()) {
                val partition = i.toString(radix = 2).padStart(terminals.size, '0')

                val maxWeight = min(partition.count { it == '0' }, partition.count { it == '1' })

                for (w in 1..maxWeight) {
                    val weight = w.toDouble()

                    val demandTree = WeightedGraph(
                            setOf(-1, -2) + terminals,
                            partition.mapIndexed { j, side -> WeightedEdge(terminals[j], side.toInt() - 48 - 2, 1.0) }.toSet() + WeightedEdge(-1, -2, weight)
                    )

                    val enum = EnumerateHH(graph, demandTree, terminalsSet).computeSolution().cost
                    val dp = DynamicProgramHH(graph, demandTree, terminalsSet).computeSolution().cost
                    val (mip, model) = CompactMipVpnSolver(graph, demandTree, terminalsSet, env).computeSolution()

                    val rMip = round(mip)

                    file.appendText("$terminalsBitStr,$partition,$weight,$enum,$dp,$rMip,${enum == dp && dp == rMip},${model[GRB.DoubleAttr.NodeCount]},${model[GRB.DoubleAttr.IterCount]}\n")

                }

            }
            println(terminalsBitStr)
        }
    }

    @Test
    fun interesting() {
        val interesting = listOf(
                142,
                220,
                333,
                367,
                381,
                642,
                656,
                690,
                803,
                881
        )

        for (i in interesting) {
            val partition = i.toString(radix = 2).padStart(10, '0')
            println("$partition")
        }
    }
}