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
        fun petersen(sampleWeight: () -> Double = { 1.0 }): WeightedGraph<Int> {
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

        private fun modifiedPetersen(innerWeight: Double, outerWeight: Double, choices: String, demandRootWeight : Double, demandInnerWeight : Double, demandLeafWeight : Double): Triple<WeightedGraph<Int>, WeightedGraph<Int>, Set<Int>> {
            val petersen = petersen()

            val root = 11
            val vertices = (1..11).toMutableSet()
            val edges = mutableSetOf(
                    WeightedEdge(1, 2, innerWeight),
                    WeightedEdge(2, 3, innerWeight),
                    WeightedEdge(3, 4, innerWeight),
                    WeightedEdge(4, 5, innerWeight),
                    WeightedEdge(5, 1, innerWeight),
                    WeightedEdge(1, 6, innerWeight),
                    WeightedEdge(2, 7, innerWeight),
                    WeightedEdge(3, 8, innerWeight),
                    WeightedEdge(4, 9, innerWeight),
                    WeightedEdge(5, 10, innerWeight),
                    WeightedEdge(6, 8, innerWeight),
                    WeightedEdge(8, 10, innerWeight),
                    WeightedEdge(10, 7, innerWeight),
                    WeightedEdge(7, 9, innerWeight),
                    WeightedEdge(9, 6, innerWeight),
                    WeightedEdge(1, 11, outerWeight),
                    WeightedEdge(2, 11, outerWeight),
                    WeightedEdge(3, 11, outerWeight),
                    WeightedEdge(4, 11, outerWeight),
                    WeightedEdge(5, 11, outerWeight),
                    WeightedEdge(6, 11, outerWeight),
                    WeightedEdge(7, 11, outerWeight),
                    WeightedEdge(8, 11, outerWeight),
                    WeightedEdge(9, 11, outerWeight),
                    WeightedEdge(10, 11, outerWeight),
            )
//            val vertexGroups1 = petersen.edges.toList().mapIndexed{ j, e -> setOf(e.first) + petersen.neighbors(e.first) - setOf(e.second) }
//            val vertexGroups2 = petersen.edges.toList().mapIndexed{ j, e -> petersen.neighbors(e.second) + setOf(e.second) - setOf(e.first)  }
//            val vertexGroups = vertexGroups1 + vertexGroups2
            val vertexGroups = petersen.edges.toList().mapIndexed{ j, e -> if (choices.get(j) == '1') setOf(e.first) + petersen.neighbors(e.first) - setOf(e.second) else petersen.neighbors(e.second) + setOf(e.second) - setOf(e.first) }

            var currentVertex = root + 1
            var currentInternalNode = -2

            val terminals = mutableSetOf<Int>()
            val demandTreeVertices = mutableSetOf(0, -1)
            val demandTreeEdges = mutableSetOf<WeightedEdge<Int>>()

            demandTreeEdges += WeightedEdge(0, -1, demandRootWeight)

            for (group in vertexGroups) {
                demandTreeVertices += currentInternalNode

                for (vertex in group) {
                    vertices += currentVertex
                    terminals += currentVertex
                    demandTreeVertices += currentVertex
                    edges += WeightedEdge(vertex, currentVertex, 0.0)
                    demandTreeEdges += WeightedEdge(currentVertex, currentInternalNode, demandLeafWeight)

                    currentVertex++
                }

                demandTreeEdges += WeightedEdge(-1, currentInternalNode, demandInnerWeight)
                currentInternalNode--
            }

            val modifiedPetersen = WeightedGraph(vertices, edges)
            val demandTree = WeightedGraph(demandTreeVertices, demandTreeEdges)

            return Triple(modifiedPetersen, demandTree, terminals)
        }
    }

    @Test
    fun laurasPotentialCounterexample() {
        val random = Random(1988)

        val env = GRBEnv(true)
        env.start()
        for (i in 1 until 0b100000000000000) {
            val k = random.nextInt(0b100000000000000)
            val choices = k.toString(radix = 2).padStart(15, '0')
            println(choices)
            val (graph, demandTree, terminals) = modifiedPetersen(1.0, 2.0, choices, 3.0, 1.0, 1.0)

//            val enum = EnumerateHH(graph, demandTree, terminals).computeSolution().cost
//            println(enum)
            val dp = DynamicProgramHH(graph, demandTree, terminals, backtrack = true).computeSolution()

            println("Banaan ${dp.cost}")

            val mipSolver = CompactMipVpnSolver(graph, demandTree, terminals, env)

            // Hot start
            for ((e, u) in mipSolver.problem.u) {
                u[GRB.DoubleAttr.Start] = dp.boughtCapacity[e]!!
            }

            for ((_, f) in mipSolver.problem.fMin) {
                f[GRB.DoubleAttr.Start] = 0.0
            }

            for ((_, f) in mipSolver.problem.fPlus) {
                f[GRB.DoubleAttr.Start] = 0.0
            }

            for ((i, j) in mipSolver.terminalSequence) {
                val path = dp.routingTemplate[i to j] ?: continue
                path.asSequence()
                        .zipWithNext()
                        .forEach { (u, v) ->
                            // Find the edge for uv
                            val e = graph.pairToEdge[u to v]
                            if (e != null) {
                                mipSolver.problem.fMin[Triple(e, i, j)]?.set(GRB.DoubleAttr.Start, 1.0)
                            }
                            else {
                                val e = graph.pairToEdge[v to u]
                                mipSolver.problem.fPlus[Triple(e, i, j)]?.set(GRB.DoubleAttr.Start, 1.0)
                            }
                        }
            }

            for ((index, omega) in mipSolver.problem.omega) {
                val (uv, e) = index
                val path = dp.edgeMapping[e]?.zipWithNext() ?: continue
                omega[GRB.DoubleAttr.Start] = when {
                    uv.first to uv.second in path || uv.second to uv.first in path -> 1.0
                    else -> 0.0
                }
            }

//            mipSolver.problem.model.write("banaan.mps")
//            mipSolver.problem.model.write("banaan.mst")

            val mip = mipSolver.computeSolution().cost

//            mipSolver.problem.model.write("banaan.sol")
            return


//            assertEquals(enum, dp, "Enum and DP do not match for choices $choices")
            assertEquals(dp.cost, round(mip), "MIP does not match for choices $choices")

        }
//        val terminalGroups = petersonEdges.map
    }

    @Test
    fun convertMst() {
        val file = File("cplex.mst")
        file.writeText("""<?xml version = "1.0" encoding="UTF-8" standalone="yes"?>
<CPLEXSolutions version="1.2">
 <CPLEXSolution version="1.2">
  <header
    problemName="banaan.mps"
    solutionName="m1"
    solutionIndex="0"
    MIPStartEffortLevel="0"
    writeLevel="2"/>
  <variables>""".trimIndent())

        File("banaan.mst").forEachLine {
            if (it.startsWith('#')) return@forEachLine

            val (name, value) = it.trim().split(' ')

            file.appendText("<variable name=\"$name\" index=\"0\" value=\"$value\"/>\n")
        }

        file.appendText("""</variables>
 </CPLEXSolution>
</CPLEXSolutions>""")
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
