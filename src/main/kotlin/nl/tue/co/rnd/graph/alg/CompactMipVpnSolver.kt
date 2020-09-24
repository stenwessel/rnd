package nl.tue.co.rnd.graph.alg

import gurobi.*
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.GeneralizedVpnSolver.VpnResult

class CompactMipVpnSolver<V>(override val graph: WeightedGraph<V>, override val demandTree: WeightedGraph<V>,
                             override val terminals: Set<V>) : GeneralizedVpnSolver<V> {

    override fun computeSolution(): VpnResult<V> {
        val terminalsList = terminals.toList()
        val terminalSequence = sequence {
            for (i in terminalsList.indices) {
                for (j in (i + 1) until terminalsList.size) {
                    yield(terminalsList[i] to terminalsList[j])
                }
            }
        }

        val env = GRBEnv(true)
        env.start()

        val (model, _, _, _) = buildModel(env, terminalSequence)

        model.optimize()

        return VpnResult(model.get(GRB.DoubleAttr.ObjVal))
    }

    private fun buildModel(env: GRBEnv, terminalSequence: Sequence<Pair<V, V>>): Problem<V> {
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
                    model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "ω_{${e.first},${e.second}}^{${uv.first},${uv.second}}")
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

            model.addConstr(x[uv], GRB.GREATER_EQUAL, objRhs, "dualObj[uv={${uv.first}, ${uv.second}}]")

            // Dual demand constraints
            for ((i, j) in terminalSequence) {
                val capLhs = GRBLinExpr()
                treePaths[i to j]!!.forEach { e -> capLhs.addTerm(1.0, omega[uv to e]) }

                val capRhs = GRBLinExpr()
                capRhs.addTerm(1.0, flowPlus[Triple(uv, i, j)])
                capRhs.addTerm(1.0, flowMin[Triple(uv, i, j)])

                model.addConstr(capLhs, GRB.GREATER_EQUAL, capRhs, "dualDemand[uv={${uv.first}, ${uv.second}},i=$i,j=$j]")
            }
        }

        return Problem(model, x, flowMin, flowPlus)
    }

    private fun findPathsInDemandTree(terminalSequence: Sequence<Pair<V, V>>): Map<Pair<V, V>, Set<WeightedEdge<V>>> {
        // Do this the lazy way
        return terminalSequence.map {
            val (i, j) = it

            val visited = mutableMapOf<V, Set<WeightedEdge<V>>>(i to emptySet())
            val boundary = ArrayDeque<WeightedEdge<V>>()
            boundary.addAll(demandTree.incidentEdges(i))

            while (boundary.isNotEmpty()) {
                val currentEdge = boundary.removeFirst()

                val discovered = if (currentEdge.first !in visited) currentEdge.first else currentEdge.second
                val from = if (currentEdge.first !in visited) currentEdge.second else currentEdge.first

                visited[discovered] = visited[from]!! + currentEdge

                if (discovered == j) {
                    return@map it to visited[discovered]!!
                }

                for (newEdge in demandTree.incidentEdges(discovered)) {
                    if (newEdge == currentEdge) continue

                    boundary.addLast(newEdge)
                }
            }

            it to emptySet()
        }.toMap()
    }

    private data class Problem<V>(val model: GRBModel,
                                  val u: Map<WeightedEdge<V>, GRBVar>,
                                  val fMin:  Map<Triple<WeightedEdge<V>, V, V>, GRBVar>,
                                  val fPlus:  Map<Triple<WeightedEdge<V>, V, V>, GRBVar>)

}
