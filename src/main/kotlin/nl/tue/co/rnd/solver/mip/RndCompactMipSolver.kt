package nl.tue.co.rnd.solver.mip

import gurobi.*
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.problem.RndInstance
import nl.tue.co.rnd.problem.RndSolution
import nl.tue.co.rnd.solver.RndSolver
import nl.tue.co.rnd.util.pairs
import nl.tue.co.rnd.util.product

abstract class RndCompactMipSolver<V, P : RndInstance<V>, out S : RndSolution<V, P>>(private val env: GRBEnv = GRBEnv(),
                                                                                 private val silent: Boolean = false) :
        RndSolver<V, P, S> {

    override fun computeSolution(instance: P): S {
        val modelWithVars = buildModel(instance)

        if (silent) {
            modelWithVars.model[GRB.IntParam.OutputFlag] = 0
        }

        modelWithVars.model.optimize()

        return constructSolution(instance, modelWithVars)
    }

    private fun buildModel(instance: P): Model<V> {
        val terminalPairs = instance.terminals.pairs()

        env.start()

        val model = GRBModel(env)
        model[GRB.IntAttr.ModelSense] = GRB.MINIMIZE

        val x = instance.graph.edges.associateWith { e ->
            model.addVar(0.0, GRB.INFINITY, e.weight, GRB.CONTINUOUS, "x[u=${e.first},v=${e.second}]")
        }

        val f = instance.graph.edges.product(terminalPairs.asIterable()).asSequence()
                .flatMap { (e, ij) -> sequenceOf(
                        FlowIndex(e.first, e.second, ij.first, ij.second),
                        FlowIndex(e.second, e.first, ij.first, ij.second),
                ) }
                .associateWith { (u, v, i, j) ->
                    model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "f[u=$u,v=$v,i=$i,j=$j]")
                }

        // Flow constraints
        for ((i, j) in terminalPairs) {
            for (u in instance.graph.vertices) {
                val lhs = GRBLinExpr()
                for (v in instance.graph.neighbors(u)) {
                    lhs.addTerm(1.0, f[FlowIndex(u, v, i, j)])
                    lhs.addTerm(-1.0, f[FlowIndex(v, u, i, j)])
                }

                val rhs = when (u) {
                    i -> 1.0
                    j -> -1.0
                    else -> 0.0
                }

                model.addConstr(lhs, GRB.EQUAL, rhs, "flowConstraint[u=$u,i=$i,j=$j]")
            }
        }

        val modelWithVars = Model(model, x, f)

        buildDualDemandPolytope(instance, terminalPairs, modelWithVars)

        return modelWithVars
    }

    protected abstract fun buildDualDemandPolytope(instance: P, terminalPairs: Sequence<Pair<V, V>>,
                                                   modelWithVars: Model<V>)

    protected abstract fun constructSolution(instance: P, modelWithVars: Model<V>): S


    data class FlowIndex<V>(val u: V, val v: V, val i: V, val j: V)

    data class Model<V>(val model: GRBModel, val x: Map<WeightedEdge<V>, GRBVar>, val f: Map<FlowIndex<V>, GRBVar>)
}
