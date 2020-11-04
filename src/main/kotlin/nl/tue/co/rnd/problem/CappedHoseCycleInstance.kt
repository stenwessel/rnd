package nl.tue.co.rnd.problem

import nl.tue.co.rnd.graph.WeightedGraph
import nl.tue.co.rnd.util.CircularList

class CappedHoseCycleInstance<V>(graph: WeightedGraph<V>,
                                 val orderedTerminals: CircularList<V>,
                                 terminalCapacity: Map<V, Double>,
                                 connectionCapacity: List<Double>)
    : CappedHoseInstance<V>(graph, terminalCapacity, orderedTerminals.asSequence().withIndex().associate { (i, v) -> UPair(v, orderedTerminals[i+1]) to connectionCapacity[i] }) {

    override val terminals = orderedTerminals.toSet()
}
