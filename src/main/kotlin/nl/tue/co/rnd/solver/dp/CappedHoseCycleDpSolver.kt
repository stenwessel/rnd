package nl.tue.co.rnd.solver.dp

import nl.tue.co.rnd.graph.alg.FloydWarshall
import nl.tue.co.rnd.graph.alg.minByWithValue
import nl.tue.co.rnd.problem.CappedHoseCycleInstance
import nl.tue.co.rnd.problem.GenericRndSolution
import nl.tue.co.rnd.solver.RndSolver

class CappedHoseCycleDpSolver<V> : RndSolver<V, CappedHoseCycleInstance<V>, GenericRndSolution<V, CappedHoseCycleInstance<V>>> {

    override fun computeSolution(instance: CappedHoseCycleInstance<V>): GenericRndSolution<V, CappedHoseCycleInstance<V>> {
        val terminals = instance.orderedTerminals
        val graph = instance.graph

        val (distance, shortestPath) = FloydWarshall(graph).computeShortestPaths()

        val table: MutableMap<Index<V>, Entry<V>> = mutableMapOf()

        for (intervalLength in 1..terminals.size) {
            for (i in 0..(terminals.size - intervalLength)) {
                val j = i + intervalLength
                val k = i + 1
                val vi = terminals[i]
                val vj = terminals[j]
                val bi = instance.terminalCapacity(vi)
                val dij = instance.connectionCapacity(vi, vj)

                for (hi in graph.vertices) {
                    for (hj in graph.vertices) {
                        table[Index(i, j, hi, hj)] = when {
                            intervalLength == 1 -> Entry(bi * distance[vi to hi]!! + dij * distance[hi to hj]!!)
                            else -> {
                                val (cost, hk) = graph.vertices.minByWithValue { hk -> table[Index(i, k, hi, hk)]!!.cost + table[Index(k, j, hk, hj)]!!.cost }!!
                                Entry(cost, hk)
                            }
                        }
                    }
                }
            }
        }

        val (cost, h0) = graph.vertices.minByWithValue { h0 -> table[Index(0, terminals.size, h0, h0)]?.cost!! }!!

        return GenericRndSolution(cost)
    }

    private data class Index<V>(val i: Int, val j: Int, val hi: V, val hj: V)
    private data class Entry<V>(val cost: Double, val hk: V? = null)
}


