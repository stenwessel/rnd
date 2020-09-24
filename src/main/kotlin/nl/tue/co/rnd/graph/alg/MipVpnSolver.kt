package nl.tue.co.rnd.graph.alg

import gurobi.*
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.alg.GeneralizedVpnSolver.VpnResult

class MipVpnSolver<V>(override val graph: WeightedGraph<V>, override val demandTree: WeightedGraph<V>,
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

        val (masterProblem, u, fMin, fPlus) = buildMasterProblem(env, terminalSequence)
        val (subProblem, demand) = buildSubProblem(env, terminalSequence)

        var matricesAdded = 0

        solving@ while (true) {
            masterProblem.optimize()

            // Solve the subproblems, adding constr it a constraint is violated
            for (e in graph.edges) {
                demand.forEach { (ij, d) ->
                    val (i, j) = ij
                    val fMineij = fMin[Triple(e, i, j)]!!.get(GRB.DoubleAttr.X)
                    val fPluseij = fPlus[Triple(e, i, j)]!!.get(GRB.DoubleAttr.X)

                    d.set(GRB.DoubleAttr.Obj, fMineij + fPluseij)
                }

                subProblem.optimize()

                // Add lazy constr if constraint is violated
                if (subProblem.get(GRB.DoubleAttr.ObjVal) > u[e]!!.get(GRB.DoubleAttr.X)) {
                    matricesAdded++

                    for (f in graph.edges) {
                        val rhs = GRBLinExpr()
                        for ((ij, d) in demand) {
                            val (i, j) = ij

                            rhs.addTerm(d.get(GRB.DoubleAttr.X), fMin[Triple(f, i, j)])
                            rhs.addTerm(d.get(GRB.DoubleAttr.X), fPlus[Triple(f, i, j)])
                        }

                        masterProblem.addConstr(u[f], GRB.GREATER_EQUAL, rhs, null)
                    }

                    continue@solving
                }

            }

            break
        }

        return VpnResult(masterProblem.get(GRB.DoubleAttr.ObjVal))
    }

    private fun buildMasterProblem(env: GRBEnv, terminalSequence: Sequence<Pair<V, V>>): MasterProblem<V> {

        val model = GRBModel(env)

        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE)

        val u = graph.edges.associateWith { e ->
            model.addVar(0.0, GRB.INFINITY, e.weight, GRB.CONTINUOUS, "u[e={${e.first},${e.second}}]")
        }

        val flowPlus = graph.edges.product(terminalSequence.asIterable())
                .map { Triple(it.first, it.second.first, it.second.second) }
                .associateWith { (e, i, j) ->
                    model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "f+[e={${e.first},${e.second}},i=$i,j=$j]")
                }

        val flowMin = graph.edges.product(terminalSequence.asIterable())
                .map { Triple(it.first, it.second.first, it.second.second) }
                .associateWith { (e, i, j) ->
                    model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "f-[e={${e.first},${e.second}},i=$i,j=$j]")
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

        return MasterProblem(model, u, flowMin, flowPlus)
    }

    private fun buildSubProblem(env: GRBEnv, terminalSequence: Sequence<Pair<V, V>>): SubProblem<V> {
        val model = GRBModel(env)

        model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE)

        val demand = terminalSequence.associateWith { (i, j) ->
            // Initialize objective weights to 0, to be modified later (current value of p_e^{i,j} in master problem)
            model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "D[i=$i,j=$j]")
        }

        // Find the paths between the leaves in the demand tree
        val paths = findPathsInDemandTree(terminalSequence)

        for (f in demandTree.edges) {
            val lhs = GRBLinExpr()
            paths.filter { (_, p) -> f in p }.forEach { (ij, _) ->
                lhs.addTerm(1.0, demand[ij])
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, f.weight, "routedOnTreeEdge[f={${f.first}, ${f.second}}]")
        }

        return SubProblem(model, demand)
    }

    private fun findPathsInDemandTree(terminalSequence: Sequence<Pair<V, V>>): Map<Pair<V, V>, Set<WeightedEdge<V>>> {
        // TODO: now assumes a star
        return terminalSequence.map {
            val (i, j) = it

            it to (demandTree.incidentEdges(i) + demandTree.incidentEdges(j)).toSet()
        }.toMap()
    }

    private data class MasterProblem<V>(val model: GRBModel,
                                        val u: Map<WeightedEdge<V>, GRBVar>,
                                        val fMin:  Map<Triple<WeightedEdge<V>, V, V>, GRBVar>,
                                        val fPlus:  Map<Triple<WeightedEdge<V>, V, V>, GRBVar>)

    private data class SubProblem<V>(val model: GRBModel, val demand: Map<Pair<V, V>, GRBVar>)
}
