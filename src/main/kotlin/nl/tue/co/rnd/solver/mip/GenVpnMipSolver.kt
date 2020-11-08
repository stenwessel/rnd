package nl.tue.co.rnd.solver.mip

import gurobi.GRB
import gurobi.GRBEnv
import gurobi.GRBLinExpr
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.graph.findPathInTree
import nl.tue.co.rnd.problem.GenVpnInstance
import nl.tue.co.rnd.problem.GenericRndSolution
import nl.tue.co.rnd.problem.RndMipSolution
import nl.tue.co.rnd.util.product

class GenVpnMipSolver<V>(env: GRBEnv = GRBEnv(), silent: Boolean = false) : RndCompactMipSolver<V, GenVpnInstance<V>, RndMipSolution<V, GenVpnInstance<V>>>(env, silent) {

    override fun buildDualDemandPolytope(instance: GenVpnInstance<V>, terminalPairs: Sequence<Pair<V, V>>,
                                         modelWithVars: Model<V>) {
        val (model, x, f) = modelWithVars

        val omega = instance.graph.edges.product(instance.demandTree.edges)
                .associateWith { (uv, e) ->
                    model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "omega[{${e.first},${e.second}},u=${uv.first},v=${uv.second}]")
                }

        val treePaths = findPathsInDemandTree(terminalPairs, instance.demandTree)

        for (uv in instance.graph.edges) {
            // Objective
            val objRhs = GRBLinExpr()
            instance.demandTree.edges.forEach { e -> objRhs.addTerm(e.weight, omega[uv to e]) }

            model.addConstr(x[uv], GRB.GREATER_EQUAL, objRhs, "dualObj[uv={${uv.first},${uv.second}}]")

            // Dual demand constraints
            for ((i, j) in terminalPairs) {
                val capLhs = GRBLinExpr()
                treePaths[i to j]?.forEach { e -> capLhs.addTerm(1.0, omega[uv to e]) } ?: error("Path is undefined")

                val capRhs = GRBLinExpr()
                capRhs.addTerm(1.0, f[FlowIndex(uv.first, uv.second, i, j)])
                capRhs.addTerm(1.0, f[FlowIndex(uv.second, uv.first, i, j)])

                model.addConstr(capLhs, GRB.GREATER_EQUAL, capRhs, "dualDemand[uv={${uv.first},${uv.second}},i=$i,j=$j]")
            }
        }
    }

    private fun findPathsInDemandTree(terminalPairs: Sequence<Pair<V, V>>, demandTree: WeightedGraph<V>): Map<Pair<V, V>, Set<WeightedEdge<V>>> {
        // Do this the lazy way
        return terminalPairs.map {
            val (i, j) = it
            it to findPathInTree(i, j, demandTree)
        }.toMap()
    }

    override fun constructSolution(instance: GenVpnInstance<V>, modelWithVars: Model<V>): RndMipSolution<V, GenVpnInstance<V>> {
        return RndMipSolution(modelWithVars.model[GRB.DoubleAttr.ObjVal], modelWithVars.model)
    }
}
