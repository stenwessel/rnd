package nl.tue.co.rnd.test.instances

import gurobi.GRB
import gurobi.GRBEnv
import nl.tue.co.rnd.graph.alg.CompactMipVpnSolver
import nl.tue.co.rnd.graph.alg.DynamicProgramHH
import nl.tue.co.rnd.graph.random.RandomDemandTreeGenerator
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
internal class RealisticTest {

    @Test
    fun randooom() {
        val ndReader = NdReader()
        val generator = RandomDemandTreeGenerator()
        val env = GRBEnv(true)

        val file = File("RealisticTestHotStart-Stress.csv")
        file.appendText("agree,seed,instance,nodecount,itercount,timeDp,timeMip,dp,mip\n")

        var count = 0

        while (true) {
            for (problem in SymInstances.values()) {
                println(count)

                val seed = 1985 + count++
                val random = Random(seed)

                val instance = ndReader.readFromFile(problem.filePath)
                val demandTree = generator.generateFromRegularVPN(random, instance)
                val terminals = instance.terminalCapacity.keys

                val dpSolver = DynamicProgramHH(instance.graph, demandTree, terminals, backtrack = true)
                val mipSolver = CompactMipVpnSolver(instance.graph, demandTree, terminals, env)

                val timedDp = measureTimedValue { dpSolver.computeSolution() }

                // Hot start
                for ((e, u) in mipSolver.problem.u) {
                    u[GRB.DoubleAttr.Start] = timedDp.value.boughtCapacity[e]!!
                }

                for ((_, f) in mipSolver.problem.fMin) {
                    f[GRB.DoubleAttr.Start] = 0.0
                }

                for ((_, f) in mipSolver.problem.fPlus) {
                    f[GRB.DoubleAttr.Start] = 0.0
                }

                for ((i, j) in mipSolver.terminalSequence) {
                    val path = timedDp.value.routingTemplate[i to j] ?: continue
                    path.asSequence()
                            .zipWithNext()
                            .forEach { (u, v) ->
                                // Find the edge for uv
                                val e = instance.graph.pairToEdge[u to v]
                                if (e != null) {
                                    mipSolver.problem.fMin[Triple(e, i, j)]?.set(GRB.DoubleAttr.Start, 1.0)
                                }
                                else {
                                    val e = instance.graph.pairToEdge[v to u]
                                    mipSolver.problem.fPlus[Triple(e, i, j)]?.set(GRB.DoubleAttr.Start, 1.0)
                                }
                            }
                }

                for ((index, omega) in mipSolver.problem.omega) {
                    val (uv, e) = index
                    val path = timedDp.value.edgeMapping[e]?.zipWithNext() ?: continue
                    omega[GRB.DoubleAttr.Start] = when {
                        uv.first to uv.second in path || uv.second to uv.first in path -> 1.0
                        else -> 0.0
                    }
                }

                val timedMip = measureTimedValue { mipSolver.computeSolution() }

                val rMip = timedMip.value.cost
                val agree = abs(rMip - timedDp.value.cost) <= 1E-6
                if (!agree) {
                    println("OMGOMGOMGOMG IT IS FALSE!")
                }

                val model = timedMip.value.model

                file.appendText("$agree,$seed,${problem.name},${model[GRB.DoubleAttr.NodeCount]},${model[GRB.DoubleAttr.IterCount]},${timedDp.duration.toLongMilliseconds()},${timedMip.duration.toLongMilliseconds()},${timedDp.value.cost},${rMip}\n")
            }
        }
    }
}
