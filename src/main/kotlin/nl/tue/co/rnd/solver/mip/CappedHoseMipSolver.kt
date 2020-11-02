package nl.tue.co.rnd.solver.mip

import gurobi.GRB
import gurobi.GRBEnv
import gurobi.GRBLinExpr
import nl.tue.co.rnd.problem.CappedHoseInstance
import nl.tue.co.rnd.problem.GenericRndSolution
import nl.tue.co.rnd.util.product

class CappedHoseMipSolver<V>(env: GRBEnv = GRBEnv(), silent: Boolean = false) :
        RndCompactMipSolver<V, CappedHoseInstance<V>, GenericRndSolution<V, CappedHoseInstance<V>>>(env, silent) {

    override fun buildDualDemandPolytope(instance: CappedHoseInstance<V>, terminalPairs: Sequence<Pair<V, V>>,
                                         modelWithVars: Model<V>) {
        val (model, x, f) = modelWithVars

        val psi = instance.graph.edges.product(terminalPairs.asIterable()).asSequence()
                .map { (e, ij) -> Triple(e, ij.first, ij.second) }
                .associateWith { (e, i, j) ->
                    model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "psi[u=${e.first},v=${e.second},i=$i,j=$j]")
                }

        val omega = instance.graph.edges.product(instance.terminals)
                .associateWith { (e, i) ->
                    model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "omega[u=${e.first},v=${e.second},i=$i]")
                }

        for (e in instance.graph.edges) {
            val rhs = GRBLinExpr()

            for ((i, j) in terminalPairs) {
                rhs.addTerm(instance.connectionCapacity(i, j), psi[Triple(e, i, j)])
            }

            for (i in instance.terminals) {
                rhs.addTerm(instance.terminalCapacity(i), omega[e to i])
            }

            model.addConstr(x[e], GRB.GREATER_EQUAL, rhs, "sufficientCapacity[u=${e.first},v=${e.second}]")

            // Add polytope constraints
            for ((i, j) in terminalPairs) {
                val lhs = GRBLinExpr()
                lhs.addTerm(1.0, psi[Triple(e, i, j)])
                lhs.addTerm(1.0, omega[e to i])
                lhs.addTerm(1.0, omega[e to j])

                val dualrhs = GRBLinExpr()
                dualrhs.addTerm(1.0, f[FlowIndex(e.first, e.second, i, j)])
                dualrhs.addTerm(1.0, f[FlowIndex(e.second, e.first, i, j)])

                model.addConstr(lhs, GRB.GREATER_EQUAL, dualrhs, "dualDemandPolytope[u=${e.first},v=${e.second},i=$i,j=$j]")
            }
        }
    }

    override fun constructSolution(instance: CappedHoseInstance<V>, modelWithVars: Model<V>): GenericRndSolution<V, CappedHoseInstance<V>> {
        return GenericRndSolution(modelWithVars.model[GRB.DoubleAttr.ObjVal])
    }
}
