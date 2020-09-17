package nl.tue.co.rnd.graph

class WeightedGraph<V>(override val vertices: Set<V>, override val edges: Set<WeightedEdge<V>>) : Graph<V, WeightedEdge<V>> {

    fun neighbors(vertex: V) = vertices.filter { v -> edges.any { e -> (e.first == v && e.second == vertex) || (e.first == vertex && e.second == v) } }.toSet()

    fun weightOf(edge: Edge<V>) = edges.find { e -> (e.first == edge.first && e.second == edge.second) || (e.first == edge.second && e.second == edge.first) }?.weight
}
