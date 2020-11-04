package nl.tue.co.rnd.solver.enum

import nl.tue.co.rnd.graph.alg.FloydWarshall
import nl.tue.co.rnd.graph.alg.minByWithValue
import nl.tue.co.rnd.problem.CappedHoseCycleInstance
import nl.tue.co.rnd.problem.GenericRndSolution
import nl.tue.co.rnd.solver.RndSolver

class CappedHoseCycleEnumSolver : RndSolver<Int, CappedHoseCycleInstance<Int>, GenericRndSolution<Int, CappedHoseCycleInstance<Int>>> {

    override fun computeSolution(instance: CappedHoseCycleInstance<Int>): GenericRndSolution<Int, CappedHoseCycleInstance<Int>> {
        val terminals = instance.orderedTerminals
        val graph = instance.graph

        val (distance, shortestPath) = FloydWarshall(graph).computeShortestPaths()

        var minCost = Double.POSITIVE_INFINITY
        for (h1 in graph.vertices) {
            for (h2 in graph.vertices) {
                for (h3 in graph.vertices) {
                    for (h4 in graph.vertices) {
                        val b1 = instance.terminalCapacity(1)
                        val b2 = instance.terminalCapacity(2)
                        val b3 = instance.terminalCapacity(3)
                        val b4 = instance.terminalCapacity(4)
                        val d13 = instance.connectionCapacity(1, 3)
                        val d34 = instance.connectionCapacity(3, 4)
                        val d42 = instance.connectionCapacity(4, 2)
                        val d21 = instance.connectionCapacity(2, 1)
                        val cost = b1 * distance[1 to h1]!! + b2 * distance[2 to h2]!! + b3 * distance[3 to h3]!! + b4 * distance[4 to h4]!! + d13 * distance[h1 to h3]!! + d34 * distance[h3 to h4]!! + d42 * distance[h4 to h2]!! + d21 * distance[h2 to h1]!!
                        if (cost < minCost) {
                            minCost = cost
                        }
                        if (cost == 38.0) {
                            println("h1=$h1, h2=$h2, h3=$h3, h4=$h4")
                        }
                    }
                }
            }
        }

        return GenericRndSolution(minCost)
    }

}


