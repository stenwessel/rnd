package nl.tue.co.rnd.problem

open class GenericRndSolution<V, out P : RndInstance<V>>(val cost: Double) : RndSolution<V, P> {
    operator fun component1() = cost
}
