package nl.tue.co.rnd.test.instances

import nl.tue.co.rnd.graph.GenVPNInstance
import nl.tue.co.rnd.graph.RegularVPNInstance
import nl.tue.co.rnd.graph.WeightedEdge
import nl.tue.co.rnd.graph.WeightedGraph
import java.nio.file.Path

class NdReader {
    companion object {
        private val WHITESPACE = """\s+""".toRegex()
    }

    fun readFromFile(filePath: Path): RegularVPNInstance<Int> {
        val content = filePath.toFile().useLines { lines ->
            lines.groupBy(
                    keySelector = { it[0] },
                    valueTransform = { it.trim().split(WHITESPACE).drop(1) }
            )
        }

        val n = content['n']?.firstOrNull()?.firstOrNull()?.toIntOrNull() ?: error("n is undefined.")

        val terminals = content['t']?.firstOrNull()?.map { it.toInt() } ?: error("t is undefined.")

        val edges = content['e']?.map { WeightedEdge(it[0].toInt(), it[1].toInt(), it[2].toDouble()) } ?: error("e is undefined.")

        val graph = WeightedGraph((1..n).toSet(), edges.toSet())

        val terminalCapacity = content['c']?.zip(terminals)?.map { (c, t) -> t to c.last().toDouble() }?.toMap() ?: error("c is undefined.")

        return RegularVPNInstance(graph, terminalCapacity)
    }
}
