package nl.tue.co.rnd.graph

class WeightedGraph<V, E : WeightedEdge<V>>(override val vertices: Set<V>, override val edges: Set<E>) : Graph<V, E>
