package nl.tue.co.rnd.graph.random

import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import kotlin.math.floor
import kotlin.math.ln
import kotlin.random.Random
import kotlin.random.nextInt

class RandomDemandTreeGenerator {
    fun generateWithRandomTerminals(random: Random, possibleTerminals: Set<Int>): WeightedGraph<Int> {
        require(possibleTerminals.all { it >= 0 })

        val terminals = repeatUntil({ it.size >= 4 }) { // Not 2 because we have covered all other cases already
            possibleTerminals.filter { random.nextBoolean() }
        }

        val internalNodesNum = random.nextInt(3 until terminals.size) // [3, terminals.size - 1]
        val todoInternalNodes = ArrayDeque((-1 downTo -internalNodesNum).toList())

        val processedInternalNodes = mutableListOf(todoInternalNodes.removeFirst())
        val tree = mutableSetOf<WeightedEdge<Int>>()

        while (todoInternalNodes.isNotEmpty()) {
            val newVertex = todoInternalNodes.removeFirst()
            val oldVertex = processedInternalNodes.random(random)

            tree.add(WeightedEdge(newVertex, oldVertex, random.sampleGeometric(0.4))) // Very very random

            processedInternalNodes.add(newVertex)
        }

        // Glue the terminals
        for (terminal in terminals) {
            val buddy = processedInternalNodes.random(random)
            tree.add(WeightedEdge(terminal, buddy, random.sampleGeometric(0.4)))
        }

        val adjacencyList = mutableMapOf<Int, MutableList<Pair<Int, WeightedEdge<Int>>>>()
        for (edge in tree) {
            adjacencyList.getOrPut(edge.first) { mutableListOf() }.add(edge.second to edge)
            adjacencyList.getOrPut(edge.second) { mutableListOf() }.add(edge.first to edge)
        }

        // Remove inappropriate leaves
        for (v in adjacencyList.keys) {
            var other = v
            var adjacent = adjacencyList[other]
            while (other !in terminals && adjacent!!.size == 1) {
                val oldOther = other
                other = adjacent[0].first
                val edge = adjacent[0].second

                // Remove from from adj list here
                adjacent.clear()
                processedInternalNodes.remove(oldOther)

                // Remove from adj list there, assuming two leaves cannot directly be connected
                adjacent = adjacencyList[other]!!
                adjacent.remove(oldOther to edge)

                tree.remove(edge)
            }
        }

        return WeightedGraph((terminals + processedInternalNodes).toSet(), tree)
    }
}

fun <R> repeatUntil(predicate: (R) -> Boolean, construct: () -> R): R {
    var result: R

    do {
        result = construct()
    }
    while (!predicate(result))

    return result
}

fun Random.sampleGeometric(successProbability: Double) = floor(ln(nextDouble(Double.MIN_VALUE, 1.0)) / ln(1 - successProbability)) + 1.0
