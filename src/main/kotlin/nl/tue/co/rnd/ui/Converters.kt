package nl.tue.co.rnd.ui

import nl.tue.co.rnd.graph.WeightedGraph
import org.graphstream.graph.Element
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.graph.implementations.SingleGraph

fun <V> WeightedGraph<V>.toGraphstream(id: String = "graph", terminals: Set<V> = emptySet()) = SingleGraph(id).also {
    val vertexMap = this.vertices.associateWith { v ->
        it.addNode(v.toString()).apply {
            this["v"] = v
            if (v in terminals) {
                this["ui.class"] = "terminal"
            }
        }
    }
    for (e in this.edges) it.addEdge("{${e.first},${e.second}}", vertexMap[e.first], vertexMap[e.second]).apply {
        this["weight"] = e.weight
    }
}

operator fun Element.set(attribute: String, value: Any?) = this.setAttribute(attribute, value)
operator fun Element.set(attribute: String, values: Array<Any?>) = this.setAttribute(attribute, *values)

inline operator fun <reified T> Element.get(key: String): T? = this.getAttribute(key, T::class.java)
