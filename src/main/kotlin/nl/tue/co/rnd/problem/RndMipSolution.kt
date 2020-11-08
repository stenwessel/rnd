package nl.tue.co.rnd.problem

import gurobi.GRBModel

class RndMipSolution<V, P : RndInstance<V>>(cost: Double, val model: GRBModel) : GenericRndSolution<V, P>(cost) {
    operator fun component2() = model
}
