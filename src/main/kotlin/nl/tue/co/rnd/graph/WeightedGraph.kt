package nl.tue.co.rnd.graph

class WeightedGraph<V>(override val vertices: Set<V>, override val edges: Set<WeightedEdge<V>>) : Graph<V, WeightedEdge<V>>
