package nl.tue.co.rnd.graph

/**
 * Graph over vertices [V] with edges [E].
 */
interface Graph<V, E : Edge<V>> {

    /**
     * The set of vertices of this graph.
     */
    val vertices: Set<V>

    /**
     * The set of edges of this graph.
     */
    val edges: Set<E>
}
