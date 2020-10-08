package nl.tue.co.rnd.graph

data class GenVPNInstance<V>(val graph: WeightedGraph<V>, val demandTree: WeightedGraph<V>, val terminals: Set<V>)
