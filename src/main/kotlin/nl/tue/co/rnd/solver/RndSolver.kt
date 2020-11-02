package nl.tue.co.rnd.solver

import nl.tue.co.rnd.problem.RndInstance
import nl.tue.co.rnd.problem.RndSolution

interface RndSolver<V, P : RndInstance<V>, out S : RndSolution<V, P>> {

    fun computeSolution(instance: P): S
}
