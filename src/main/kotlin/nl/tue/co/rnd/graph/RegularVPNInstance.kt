package nl.tue.co.rnd.graph

data class RegularVPNInstance<V>(val graph: WeightedGraph<V>, val terminalCapacity: Map<V, Double>)
