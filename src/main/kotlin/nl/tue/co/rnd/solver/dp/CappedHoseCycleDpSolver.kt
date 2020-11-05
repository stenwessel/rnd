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
                val vk = terminals[k]
                val bi = instance.terminalCapacity(vi)
                val bj = instance.terminalCapacity(vj)
                val bk = instance.terminalCapacity(vk)
                val dij = instance.connectionCapacity(vi, vj)

                for (hi in graph.vertices) {
                    for (hj in graph.vertices) {
                        table[Index(i, j, hi, hj)] = when {
                            intervalLength == 1 -> Entry(bi * distance[vi to hi]!! + dij * distance[hi to hj]!! + bj * distance[hj to vj]!!)
                            else -> {
                                val (cost, hk) = graph.vertices.minByWithValue { hk -> table[Index(i, k, hi, hk)]!!.cost + table[Index(k, j, hk, hj)]!!.cost - bk * distance[vk to hk]!! }!!
                                Entry(cost, hk)
                            }
                        }
                    }
                }
            }
        }

        val v0 = terminals[0]
        val b0 = instance.terminalCapacity(terminals[0])
        val (cost, h0) = graph.vertices.minByWithValue { h0 -> table[Index(0, terminals.size, h0, h0)]?.cost!! - b0 * distance[v0 to h0]!! }!!

        return GenericRndSolution(cost)
    }

    private data class Index<V>(val i: Int, val j: Int, val hi: V, val hj: V)
    private data class Entry<V>(val cost: Double, val hk: V? = null)
}


