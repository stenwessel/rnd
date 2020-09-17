package nl.tue.co.rnd.graph

class WeightedEdge<V>(first: V, second: V, val weight: Double) : Edge<V>(first, second) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return Edge(this.first, this.second) == other
    }

    override fun hashCode(): Int {
        return Edge(this.first, this.second).hashCode()
    }
}
