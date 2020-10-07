package nl.tue.co.rnd.graph.alg

import gurobi.GRB
import gurobi.GRBEnv
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.random.RandomDemandTreeGenerator
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random
import kotlin.test.assertEquals

internal class PetersenGraphTest {

    companion object {
        private fun petersen(sampleWeight: () -> Double = { 1.0 }): WeightedGraph<Int> {
            return WeightedGraph(
                    (1..10).toSet(),
                    setOf(
                            WeightedEdge(1, 2, sampleWeight()),
                            WeightedEdge(2, 3, sampleWeight()),
                            WeightedEdge(3, 4, sampleWeight()),
                            WeightedEdge(4, 5, sampleWeight()),
                            WeightedEdge(5, 1, sampleWeight()),
                            WeightedEdge(1, 6, sampleWeight()),
                            WeightedEdge(2, 7, sampleWeight()),
                            WeightedEdge(3, 8, sampleWeight()),
                            WeightedEdge(4, 9, sampleWeight()),
                            WeightedEdge(5, 10, sampleWeight()),
                            WeightedEdge(6, 8, sampleWeight()),
                            WeightedEdge(8, 10, sampleWeight()),
                            WeightedEdge(10, 7, sampleWeight()),
                            WeightedEdge(7, 9, sampleWeight()),
                            WeightedEdge(9, 6, sampleWeight()),
                    )
            )
        }
    }

    @Test
    fun unitCostUnitCapacityStarUnion() {
        val terminals = (1..10).toSet()

        val graph = petersen()

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
        val graph = petersen()

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
        val graph = petersen()
        val env = GRBEnv(true)

        val file = File("interesting.csv")
        file.appendText("terminals,partition,bridgeWeight,relax,mip\n")

        val interesting = listOf(
                "0110011110" to "010100",
                "0110011111" to "0101001",
                "0110011111" to "0101011",
                "0110111110" to "0100100",
                "0111011111" to "01101001",
                "1110011111" to "00101001",
                "1110011111" to "00101011",
                "1110101101" to "0101111",
                "1110110101" to "0100011",
                "1110111101" to "01010111",
                "1110111101" to "01011111",
                "1111011111" to "001001001",
                "1111101101" to "00100110",
                "1111101101" to "00110110",
                "1111101111" to "001001100",
                "1111111111" to "0010001110",
                "1111111111" to "0011011100",
                "1111111111" to "0101001101",
                "1111111111" to "0101101111",
                "1111111111" to "0101111101",
        )

        for ((terminalsBitStr, partition) in interesting) {
            val terminals = terminalsBitStr.mapIndexedNotNull { index, c -> if (c == '1') index + 1 else null }
            val terminalsSet = terminals.toSet()
            val weight = 1.0

            val demandTree = WeightedGraph(
                    setOf(-1, -2) + terminals,
                    partition.mapIndexed { j, side -> WeightedEdge(terminals[j], side.toInt() - 48 - 2, 1.0) }.toSet() + WeightedEdge(-1, -2, weight)
            )

            val (mip, model) = CompactMipVpnSolver(graph, demandTree, terminalsSet, env).computeSolution()
            val relax = model.relax()

            relax.optimize()

            file.appendText("$terminalsBitStr,$partition,$weight,${relax.get(GRB.DoubleAttr.ObjVal)},${round(mip)}\n")
        }
    }

    @Test
    fun randomTrees() {
        val file = File("RandomTreesPetersen.csv")
        file.appendText("agree;seed;nodecount;itercount;enum;dp;mip;terminals;graph;tree\n")

        val env = GRBEnv(true)

        val generator = RandomDemandTreeGenerator()
        var i = 0
        while (true) {
            if (i % 100 == 0) {
                println(i)
            }
            val seed = 1985 + i++
            val random = Random(seed)

            val graph = petersen { random.nextInt(1, 11).toDouble() }
            val demandTree = generator.generateWithRandomTerminals(random, (1..10).toSet())
            val terminalsSet = demandTree.vertices.filter { it >= 0 }.toSet()

            val enum = EnumerateHH(graph, demandTree, terminalsSet).computeSolution().cost
            val dp = DynamicProgramHH(graph, demandTree, terminalsSet).computeSolution().cost
            val (mip, model) = CompactMipVpnSolver(graph, demandTree, terminalsSet, env).computeSolution()

            val rMip = round(mip)

            val agree = enum == dp && dp == rMip
            if (!agree) {
                println("OMGOMGOMGOMG IT IS FALSE!")
            }

            file.appendText("$agree;$seed;${model[GRB.DoubleAttr.NodeCount]};${model[GRB.DoubleAttr.IterCount]};$enum;$dp;$rMip;${terminalsSet.joinToString(separator = ":")};${graph.edges.joinToString(separator = ":")};${demandTree.edges.joinToString(separator = ":")}\n")
        }
    }
}
