package nl.tue.co.rnd.problem

import nl.tue.co.rnd.graph.WeightedGraph

class GenVpnInstance<V>(override val graph: WeightedGraph<V>,
                        val demandTree: WeightedGraph<V>,
                        override val terminals: Set<V>) : RndInstance<V>
