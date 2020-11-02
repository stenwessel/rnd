package nl.tue.co.rnd.problem

data class GenericRndSolution<V, out P : RndInstance<V>>(val cost: Double) : RndSolution<V, P>
