package nl.tue.co.rnd.test.instances.cappedhose

import gurobi.GRB
import gurobi.GRBEnv
import gurobi.GRBLinExpr
import gurobi.GRBModel
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.PetersenGraphTest
import nl.tue.co.rnd.problem.CappedHoseCycleInstance
import nl.tue.co.rnd.solver.dp.CappedHoseCycleDpSolver
import nl.tue.co.rnd.solver.enum.CappedHoseCycleEnumSolver
import nl.tue.co.rnd.solver.mip.CappedHoseMipSolver
import nl.tue.co.rnd.test.instances.GapInstanceCreator
import nl.tue.co.rnd.util.CircularList
import nl.tue.co.rnd.util.circular
import org.junit.jupiter.api.Test
import kotlin.math.*
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.assertEquals
import kotlin.test.assertFails

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
        val n = 3
        val graph = GapInstanceCreator().constructGraph(n)

        val terminals = (1..n).toList().circular()
        val connectionCapacity = List(n) { 2.0 }.circular()
//        val connectionCapacity = List(n) { random.nextInt(1, 11).toDouble() }.circular()
        val terminalCapacity = terminals.withIndex().associate { (i, v) ->
            val prev = connectionCapacity[i-1].toInt()
            val next = connectionCapacity[i].toInt()
            v to 2.0
//            v to random.nextInt(max(prev, next), prev + next + 1).toDouble()
        }

        val instance = CappedHoseCycleInstance(graph, terminals, terminalCapacity, connectionCapacity)

        val mipSolver = CappedHoseMipSolver<Int>()
        val (mip, model) = mipSolver.computeSolution(instance)

        val lp = model.relax()
        lp.optimize()

//        val dpSolver = CappedHoseCycleDpSolver<Int>()
//        val (dp) = dpSolver.computeSolution(instance)

//        val enumSolver = CappedHoseCycleEnumSolver()
//        val (enum) = enumSolver.computeSolution(instance)

        for (v in lp.vars) {
            println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
        }

        assertEquals(mip, lp[GRB.DoubleAttr.ObjVal])
    }


    @Test
    fun findAGap() {
        val terminals = CircularList((1..5).toList())
        val graph = WeightedGraph(terminals.toSet() + setOf(-1, -2), setOf(
                WeightedEdge(-1, -2, 1.0),
                WeightedEdge(-1, 1, 2.0),
                WeightedEdge(-1, 3, 2.0),
                WeightedEdge(-1, 5, 2.0),
                WeightedEdge(-2, 5, 2.0),
                WeightedEdge(-2, 2, 2.0),
                WeightedEdge(-2, 4, 2.0),
        ))

        val terminalCapacity = mapOf(
                1 to 1.0,
                2 to 2.0,
                3 to 1.0,
                4 to 2.0,
                5 to 1.0,
        )

        val connectionCapacity = listOf(1.0, 1.0, 1.0, 1.0, 1.0)

        val instance = CappedHoseCycleInstance(graph, terminals, terminalCapacity, connectionCapacity)

        val mipSolver = CappedHoseMipSolver<Int>()
        val (mip, model) = mipSolver.computeSolution(instance)

        val lp = model.relax()
        lp.optimize()

        println("Mip:")
        for (v in model.vars) {
            println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
        }

        println("\nLP:")
        for (v in lp.vars) {
            println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
        }

    }

    @Test
    fun arbitrarilyBadFractionalSolutionOfDualSubproblem() {
        val terminals = CircularList((1..5).toList())

        val b = mapOf(
                1 to 1.0,
                2 to 2.0,
                3 to 1.0,
                4 to 2.0,
                5 to 1.0,
        )

        val d = List(5) { 1.0 }

        val connections = terminals.mapIndexed { i, t -> t to terminals[i+1] }

        val env = GRBEnv()
        val model = GRBModel(env)

        val omega = terminals.associateWith { i ->
            model.addVar(0.0, GRB.INFINITY, b[i] ?: 0.0, GRB.CONTINUOUS, "omega[i=$i]")
        }

        val psi = connections.associateWith { (i, j) ->
                    model.addVar(0.0, GRB.INFINITY, d[terminals.indexOf(i)], GRB.CONTINUOUS, "psi[i=$i,j=$j]")
//                    model.addVar(0.0, 0.0, d[terminals.indexOf(i)], GRB.CONTINUOUS, "psi[i=$i,j=$j]")
                }

        for (ij in connections) {
            val (i, j) = ij
            val lhs = GRBLinExpr().apply {
                addTerm(1.0, omega[i])
                addTerm(1.0, omega[j])
                addTerm(1.0, psi[ij])
            }
            model.addConstr(lhs, GRB.GREATER_EQUAL, 1.0, null)
        }

        model.optimize()

        for (v in model.vars) {
            println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
        }
    }

    @Test
    fun findCounterExampleHalfIntegralityGeneralization() {
        val terminals = CircularList((1..5).toList())
        val connections = terminals.mapIndexed { i, t -> t to terminals[i+1] }

        val values = 1..6

        val env = GRBEnv()
        val model = GRBModel(env)

        model[GRB.IntParam.OutputFlag] = 0

        val omega = terminals.associateWith { i ->
            model.addVar(0.0, 2.0, 0.0, GRB.CONTINUOUS, "omega[i=$i]")
        }

        val psi = connections.associateWith { (i, j) ->
            model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "psi[i=$i,j=$j]")
        }

        for (ij in connections) {
            val (i, j) = ij
            val lhs = GRBLinExpr().apply {
                addTerm(0.5, omega[i])
                addTerm(0.5, omega[j])
                addTerm(1.0, psi[ij])
            }
            model.addConstr(lhs, GRB.GREATER_EQUAL, 1.0, null)
        }


        for (d12 in values) {
            psi[1 to 2]!![GRB.DoubleAttr.Obj] = d12.toDouble()
            for (d23 in values) {
                psi[2 to 3]!![GRB.DoubleAttr.Obj] = d23.toDouble()
                for (d34 in values) {
                    psi[3 to 4]!![GRB.DoubleAttr.Obj] = d34.toDouble()
                    for (d45 in values) {
                        psi[4 to 5]!![GRB.DoubleAttr.Obj] = d45.toDouble()
                        for (d51 in values) {
                            println("d = $d12, $d23, $d34, $d45, $d51")
                            psi[5 to 1]!![GRB.DoubleAttr.Obj] = d51.toDouble()
                            for (b1 in max(d51, d12)..(d51 + d12)) {
                                omega[1]!![GRB.DoubleAttr.Obj] = 0.5 * b1.toDouble()
                                for (b2 in max(d12, d23)..(d12 + d23)) {
                                    omega[2]!![GRB.DoubleAttr.Obj] = 0.5 * b2.toDouble()
                                    for (b3 in max(d23, d34)..(d23 + d34)) {
                                        omega[3]!![GRB.DoubleAttr.Obj] = 0.5 * b3.toDouble()
                                        for (b4 in max(d34, d45)..(d34 + d45)) {
                                            omega[4]!![GRB.DoubleAttr.Obj] = 0.5 * b4.toDouble()
                                            for (b5 in max(d45, d51)..(d45 + d51)) {
                                                omega[5]!![GRB.DoubleAttr.Obj] = 0.5 * b5.toDouble()
                                                model.optimize()
                                                val obj1 = model[GRB.DoubleAttr.ObjVal]

                                                val constr = model.addConstr(GRBLinExpr().apply { psi.forEach { (_, v) -> addTerm(1.0, v) } }, GRB.LESS_EQUAL, 1.0, null)

                                                psi.values.forEach { it[GRB.CharAttr.VType] = GRB.BINARY }
                                                omega.values.forEach { it[GRB.CharAttr.VType] = GRB.INTEGER }
                                                model.optimize()
                                                val obj2 = model[GRB.DoubleAttr.ObjVal]

                                                if (abs(obj1 - obj2) > 1.0E-6) {
                                                    println("Counterexample: $obj1 != $obj2\n b = $b1, $b2, $b3, $b4, $b5; d = $d12, $d23, $d34, $d45, $d51")
                                                    assert(false)
                                                }

                                                model.remove(constr)
                                                psi.values.forEach { it[GRB.CharAttr.VType] = GRB.CONTINUOUS }
                                                omega.values.forEach { it[GRB.CharAttr.VType] = GRB.CONTINUOUS }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun randomCycle() {
        val terminals = CircularList((1..9).toList())
        val connections = terminals.mapIndexed { i, t -> t to terminals[i+1] }

        val maxD = 20

        val values = 1 until (maxD*maxD*maxD)

        var seed = 1984

        val env = GRBEnv()
        val model = GRBModel(env)

        model[GRB.IntParam.OutputFlag] = 0

        val omega = terminals.associateWith { i ->
            model.addVar(0.0, 2.0, 0.0, GRB.CONTINUOUS, "omega[i=$i]")
        }

        val psi = connections.associateWith { (i, j) ->
            model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "psi[i=$i,j=$j]")
        }

        for (ij in connections) {
            val (i, j) = ij
            val lhs = GRBLinExpr().apply {
                addTerm(0.5, omega[i])
                addTerm(0.5, omega[j])
                addTerm(1.0, psi[ij])
            }
            model.addConstr(lhs, GRB.GREATER_EQUAL, 1.0, null)
        }

        while (true) {
            val random = Random(seed++)
            if (seed % 250 == 0) println("$seed")
            val d = connections.associateWith { val s = random.nextInt(values); maxD - floor(s.toDouble().pow(0.333333)) }
            val b = terminals.associateWith { i ->
                val d1 = (d[i to (i-1)] ?: 0.0) + (d[(i-1) to i] ?: 0.0)
                val d2 = (d[i to (i+1)] ?: 0.0) + (d[(i+1) to i] ?: 0.0)
                random.nextInt(max(d1, d2).toInt(), (d1 + d2 + 1).toInt()).toDouble()
            }

            omega.forEach { (i, v) -> v[GRB.DoubleAttr.Obj] = b[i]!! }
            psi.forEach { (ij, v) -> v[GRB.DoubleAttr.Obj] = d[ij]!! }

            model.optimize()
            val obj1 = model[GRB.DoubleAttr.ObjVal]

            if (seed ==1985) {
                for (v in model.vars) {
                    println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
                }
            }

            val constr = model.addConstr(GRBLinExpr().apply { psi.forEach { (_, v) -> addTerm(1.0, v) } }, GRB.LESS_EQUAL, 1.0, null)

            psi.values.forEach { it[GRB.CharAttr.VType] = GRB.BINARY }
            omega.values.forEach { it[GRB.CharAttr.VType] = GRB.INTEGER }
            model.optimize()
            val obj2 = model[GRB.DoubleAttr.ObjVal]

            if (seed ==1985) {
                for (v in model.vars) {
                    println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
                }
            }

            if (abs(obj1 - obj2) > 1.0E-6) {
                println("Counterexample: $obj1 != $obj2 (seed $seed)\n")
                assert(false)
            }

            model.remove(constr)
            psi.values.forEach { it[GRB.CharAttr.VType] = GRB.CONTINUOUS }
            omega.values.forEach { it[GRB.CharAttr.VType] = GRB.CONTINUOUS }
        }
    }

    @Test
    fun runIsolated() {
        val terminals = CircularList((1..5).toList())
        val connections = terminals.mapIndexed { i, t -> t to terminals[i+1] }

        val b = mapOf(
                1 to 2.0,
                2 to 2.0,
                3 to 1.0,
                4 to 1.0,
                5 to 2.0,
        )

        val d = List(5) { 1.0 }

        val env = GRBEnv()
        val model = GRBModel(env)

        model[GRB.IntParam.OutputFlag] = 0

        val omega = terminals.associateWith { i ->
            model.addVar(0.0, GRB.INFINITY, b[i] ?: 0.0, GRB.CONTINUOUS, "omega[i=$i]")
        }

        val psi = connections.associateWith { (i, j) ->
            model.addVar(0.0, GRB.INFINITY, d[terminals.indexOf(i)], GRB.CONTINUOUS, "psi[i=$i,j=$j]")
        }

        for (ij in connections) {
            val (i, j) = ij
            val lhs = GRBLinExpr().apply {
                addTerm(1.0, omega[i])
                addTerm(1.0, omega[j])
                addTerm(1.0, psi[ij])
            }
            model.addConstr(lhs, GRB.GREATER_EQUAL, 1.0, null)
        }

        model.optimize()

        println("ObjVal = ${model[GRB.DoubleAttr.ObjVal]}")
        for (v in model.vars) {
            println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
        }


//        psi.values.forEach { it[GRB.DoubleAttr.UB] = 0.0 }
        val constr = model.addConstr(GRBLinExpr().apply { psi.forEach { (_, v) -> addTerm(1.0, v) } }, GRB.LESS_EQUAL, 1.0, null)

        model.optimize()

        println("\n\nObjVal = ${model[GRB.DoubleAttr.ObjVal]}")
        for (v in model.vars) {
            println("${v[GRB.StringAttr.VarName]} = ${v[GRB.DoubleAttr.X]}")
        }
    }
}
