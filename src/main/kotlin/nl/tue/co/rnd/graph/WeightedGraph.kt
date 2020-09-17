package nl.tue.co.rnd.graph

class WeightedGraph<V>(override val vertices: Set<V>, override val edges: Set<WeightedEdge<V>>) : Graph<V, WeightedEdge<V>> {

    fun neighbors(vertex: V) = vertices.filter { Edge(vertex, it) in edges }.toSet()

    fun weightOf(edge: Edge<V>) = edges.find { it == edge }?.weight
}
