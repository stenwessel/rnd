package nl.tue.co.rnd.test.instances

import gurobi.GRB
import gurobi.GRBEnv
import nl.tue.co.rnd.graph.alg.CompactMipVpnSolver
import nl.tue.co.rnd.graph.alg.DynamicProgramHH
import nl.tue.co.rnd.graph.alg.EnumerateHH
import nl.tue.co.rnd.graph.random.RandomDemandTreeGenerator
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.round
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

        val file = File("RealisticTestHotStart.csv")
        file.appendText("agree,seed,instance,nodecount,itercount,timeDp,timeMip,dp,mip\n")

        var i = 0

        while (true) {
            for (problem in SymInstances.values()) {
                println(i)

                val seed = 1985 + i++
                val random = Random(seed)

                val instance = ndReader.readFromFile(problem.filePath)
                val demandTree = generator.generateFromRegularVPN(random, instance)
                val terminals = instance.terminalCapacity.keys

                val dpSolver = DynamicProgramHH(instance.graph, demandTree, terminals)
                val mipSolver = CompactMipVpnSolver(instance.graph, demandTree, terminals, env)

                // Hot start
                for ((e, u) in mipSolver.problem.u) {
                    u[GRB.DoubleAttr.Start] = 0.0
                }



                val timedDpCost = measureTimedValue { dpSolver.computeSolution().cost }
                val timedMip = measureTimedValue { mipSolver.computeSolution() }

                val rMip = timedMip.value.cost
                val agree = abs(rMip - timedDpCost.value) <= 1E-6
                if (!agree) {
                    println("OMGOMGOMGOMG IT IS FALSE!")
                }

                val model = timedMip.value.model

                file.appendText("$agree,$seed,${problem.name},${model[GRB.DoubleAttr.NodeCount]},${model[GRB.DoubleAttr.IterCount]},${timedDpCost.duration.toLongMilliseconds()},${timedMip.duration.toLongMilliseconds()},${timedDpCost.value},${rMip}\n")
            }
        }
    }
}
