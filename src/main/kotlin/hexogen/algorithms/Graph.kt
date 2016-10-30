package hexogen.algorithms

import hexogen.collections.DisjointSet

interface Edge<N> {
    val A: N
    val B: N
}

class Hybrid<N, E : Edge<N>>(edges: Sequence<E>, threshold: Double, rng: () -> Double) {
    private val rng = rng
    private val threshold = threshold
    private val map = mapEdges(edges)
    private val kruskal = Kruskal(edges)
    private val tracer = Tracer({ node: N -> getEdges(node) })

    private fun mapEdges(edges: Sequence<E>): Map<N, List<E>> {
        val result = mutableMapOf<N, MutableList<E>>()
        fun link(node: N, edge: E) = result.getOrPut(node, { mutableListOf() }).add(edge)
        for (edge in edges) {
            link(edge.A, edge)
            link(edge.B, edge)
        }
        return result
    }

    private fun getEdges(node: N): Sequence<E> =
            map[node]?.asSequence() ?: emptySequence<E>()

    private fun nextTracer(): E? =
            (if (rng() < threshold) null else tracer.next())?.apply {
                kruskal.merge(A, B)
            }

    private fun nextKruskal(): E? =
            kruskal.next()?.apply {
                tracer.visit(A)
                tracer.visit(B)
                tracer.reset(if (rng() < 0.5) A else B)
            }

    fun next(): E? = nextTracer() ?: nextKruskal()
}

class Tracer<N, E : Edge<N>>(edges: (N) -> Sequence<E>) {
    private val edges = edges
    private val visited = mutableSetOf<N>()
    private var head: N? = null

    private fun opposite(node: N, edge: E): N? =
            when (node) {
                edge.A -> edge.B
                edge.B -> edge.A
                else -> null
            }

    fun visit(node: N, reset: Boolean = false): Boolean {
        val added = visited.add(node)
        if (reset && added) head = node
        return added
    }

    fun next(): E? {
        val current = head ?: return null
        return edges(current).firstOrNull { visit(opposite(current, it)!!, true) }
    }

    fun reset(node: N) {
        head = node
    }
}

class Kruskal<N, E : Edge<N>>(edges: Sequence<E>) {
    val iterator = edges.iterator()
    val sets = DisjointSet<N>()

    fun merge(a: N, b: N) = sets.merge(a, b)

    fun next(): E? {
        if (!iterator.hasNext())
            return null

        val edge = iterator.next()
        if (sets.test(edge.A, edge.B))
            return next()

        sets.merge(edge.A, edge.B)

        return edge
    }
}
