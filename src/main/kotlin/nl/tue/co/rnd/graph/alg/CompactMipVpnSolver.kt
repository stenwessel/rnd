package nl.tue.co.rnd.graph.alg

import gurobi.*
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.GeneralizedVpnSolver.VpnResult
import nl.tue.co.rnd.graph.findPathInTree

class CompactMipVpnSolver<V>(override val graph: WeightedGraph<V>, override val demandTree: WeightedGraph<V>,
                             override val terminals: Set<V>, private val env: GRBEnv = GRBEnv()) : GeneralizedVpnSolver<V> {

    val problem by lazy { buildProblem() }

    val terminalSequence by lazy {
        val terminalsList = terminals.toList()
        sequence {
            for (i in terminalsList.indices) {
                for (j in (i + 1) until terminalsList.size) {
                    yield(terminalsList[i] to terminalsList[j])
                }
            }
        }
    }

    override fun computeSolution(): VpnResult<V> {
        val (model, _, _, _) = problem

        model.set(GRB.IntParam.OutputFlag, 0)

        model.optimize()

        return VpnResult(model.get(GRB.DoubleAttr.ObjVal), model)
    }

    private fun buildProblem(): Problem<V> {
        env.start()

        val model = GRBModel(env)

        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE)

        val x = graph.edges.associateWith { e ->
            model.addVar(0.0, GRB.INFINITY, e.weight, GRB.CONTINUOUS, "x[{${e.first},${e.second}}]")
        }

        val flowPlus = graph.edges.product(terminalSequence.asIterable())
                .map { Triple(it.first, it.second.first, it.second.second) }
                .associateWith { (e, i, j) ->
                    model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "f+[{${e.first},${e.second}},i=$i,j=$j]")
                }

        val flowMin = graph.edges.product(terminalSequence.asIterable())
                .map { Triple(it.first, it.second.first, it.second.second) }
                .associateWith { (e, i, j) ->
                    model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "f-[{${e.first},${e.second}},i=$i,j=$j]")
                }

        val omega = graph.edges.product(demandTree.edges)
                .associateWith { (uv, e) ->
                    model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "omega_{${e.first},${e.second}}^{${uv.first},${uv.second}}")
                }

        // Flow constraints
        terminalSequence.forEach { (i, j) ->
            for (v in graph.vertices) {
                val lhs = GRBLinExpr()
                graph.incidentEdges(v).forEach { e ->
                    val triple = Triple(e, i, j)

                    lhs.addTerm(if (e.first == v) 1.0 else -1.0, flowPlus[triple])
                    lhs.addTerm(if (e.first == v) -1.0 else 1.0 , flowMin[triple])
                }

                val rhs = when (v) {
                    i -> -1.0
                    j -> 1.0
                    else -> 0.0
                }

                model.addConstr(lhs, GRB.EQUAL, rhs, "pathVtxDegreeFlow[v=$v,i=$i,j=$j]")
            }
        }

        val treePaths = findPathsInDemandTree(terminalSequence)

        // Dual subproblem constraints
        for (uv in graph.edges) {
            // Objective
            val objRhs = GRBLinExpr()
            demandTree.edges.forEach { e -> objRhs.addTerm(e.weight, omega[uv to e]) }

            model.addConstr(x[uv], GRB.GREATER_EQUAL, objRhs, "dualObj[uv={${uv.first},${uv.second}}]")

            // Dual demand constraints
            for ((i, j) in terminalSequence) {
                val capLhs = GRBLinExpr()
                treePaths[i to j]!!.forEach { e -> capLhs.addTerm(1.0, omega[uv to e]) }

                val capRhs = GRBLinExpr()
                capRhs.addTerm(1.0, flowPlus[Triple(uv, i, j)])
                capRhs.addTerm(1.0, flowMin[Triple(uv, i, j)])

                model.addConstr(capLhs, GRB.GREATER_EQUAL, capRhs, "dualDemand[uv={${uv.first},${uv.second}},i=$i,j=$j]")
            }
        }

        return Problem(model, x, flowMin, flowPlus, omega)
    }

    private fun findPathsInDemandTree(terminalSequence: Sequence<Pair<V, V>>): Map<Pair<V, V>, Set<WeightedEdge<V>>> {
        // Do this the lazy way
        return terminalSequence.map {
            val (i, j) = it
            it to findPathInTree(i, j, demandTree)
        }.toMap()
    }

    data class Problem<V>(val model: GRBModel,
                                  val u: Map<WeightedEdge<V>, GRBVar>,
                                  val fMin:  Map<Triple<WeightedEdge<V>, V, V>, GRBVar>,
                                  val fPlus:  Map<Triple<WeightedEdge<V>, V, V>, GRBVar>,
                                  val omega:  Map<Pair<WeightedEdge<V>, WeightedEdge<V>>, GRBVar>)

}
