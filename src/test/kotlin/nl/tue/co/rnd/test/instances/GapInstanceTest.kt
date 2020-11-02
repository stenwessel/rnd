package nl.tue.co.rnd.test.instances

import nl.tue.co.rnd.graph.alg.CompactMipVpnSolver
import nl.tue.co.rnd.graph.alg.DynamicProgramHH
import nl.tue.co.rnd.graph.alg.EnumerateHH
import org.junit.jupiter.api.Test

internal class GapInstanceTest {
    @Test
    fun gapInstance() {
        val instance = GapInstanceCreator().construct(10)

        val mipSolver = CompactMipVpnSolver(instance.graph, instance.demandTree, instance.terminals)
        val dpSolver = DynamicProgramHH(instance.graph, instance.demandTree, instance.terminals)
        val enumSolver = EnumerateHH(instance.graph, instance.demandTree, instance.terminals)

        val (mip, ) = mipSolver.computeSolution()
        val dp = dpSolver.computeSolution().cost
        val enum = enumSolver.computeSolution().cost

        println("DP: $dp, Enum: $enum, Mip: $mip")
    }
}
