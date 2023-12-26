package day25

import AoCTask
import kotlin.math.min
import kotlin.streams.asStream

// https://adventofcode.com/2023/day/25

data class Vertex(val ids: Set<String>) {
    constructor(singleId: String): this(setOf(singleId))
}

data class Edge(val vertices: Set<Vertex>, val weight: Int = 1) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge

        return vertices == other.vertices
    }

    override fun hashCode(): Int {
        return vertices.hashCode()
    }

    operator fun contains(vertex: Vertex): Boolean {
        return vertex in vertices
    }

    override fun toString(): String {
        return "${vertices.first().ids}-${vertices.last().ids}($weight)"
    }
}

data class Graph(val vertices: HashSet<Vertex>, val edges: HashSet<Edge>) {

    fun toMermaidString(highlightEdges: List<Edge> = emptyList()): String {
        val builder = StringBuilder()
        with(builder) {
            appendLine("stateDiagram-v2")
            appendLine("classDef cut fill:#f00")
            edges.forEach { edge ->
                val from = edge.vertices.first()
                val to = edge.vertices.last()
                if (edge in highlightEdges) {
                    appendLine("${from.ids.first()}:::cut --> ${to.ids.first()}:::cut")
                } else {
                    appendLine("${from.ids.first()} --> ${to.ids.first()}")
                }
            }

        }
        return builder.toString()
    }
}

fun parseInput(input: List<String>): Graph  {
    val edges = input.flatMap { line ->
        val (vertex, targets) = line.split(": ")
        targets.split(" ").map { target ->
            Edge(setOf(Vertex(vertex), Vertex(target)))
        }
    }
    val vertices = edges.fold(emptySet<Vertex>()) {
        acc, edge -> acc + edge.vertices
    }
    return Graph(vertices.toHashSet(), edges.toHashSet())
}

data class Cut(val s: Vertex, val t: Vertex, val weight: Int)

fun maximumAdjacencySearch(graph: Graph): Cut {
    val start = graph.vertices.first()
    val foundSet = mutableListOf(start)
    val cutWeights = mutableListOf<Int>()
    val candidates = graph.vertices.toMutableSet()
    candidates.remove(start)
    while (candidates.isNotEmpty()) {
        val (next, weight) = candidates.parallelStream().map { candidate ->
            val weight = foundSet.sumOf { found ->
                val targetEdge = Edge(setOf(found, candidate))
                val foundEdge = graph.edges.find { edge -> edge == targetEdge }
                foundEdge?.weight ?: 0
            }
            candidate to weight
        }.toList().maxBy { it.second }
        candidates.remove(next)
        foundSet.add(next)
        cutWeights.add(weight)
    }

    println(foundSet.map { it.ids })

    return Cut(
        foundSet.dropLast(1).last(),
        foundSet.last(),
        cutWeights.last()
    )
}

data class MinCut(val first: Graph, val second: Graph, val cutEdges: List<Edge>)

fun minCut(graph: Graph): MinCut {
    var g = graph
    val currentPartition = mutableSetOf<Vertex>()
    var currentBestPartition = setOf<Vertex>()
    var currentBestCut: Cut? = null
    while (g.vertices.size > 1) {
        val cut = maximumAdjacencySearch(g)
        if (currentBestCut == null || cut.weight < currentBestCut.weight) {
            currentBestCut = cut
            currentBestPartition = currentPartition + cut.t
        }
        currentPartition.add(cut.t)
        println(currentPartition.map { it.ids })
        g = mergeVerticesFromCut(g, cut)
        println(g.edges)
    }

    return constructMinCutResult(graph, currentBestPartition.flatMap { it.ids }.toSet())
}

fun constructMinCutResult(graph: Graph, partition: Set<String>): MinCut {
    val (firstVertices, secondVertices) = graph.vertices.partition {
        it.ids.intersect(partition).isNotEmpty()
    }
    val firstEdges = mutableListOf<Edge>()
    val secondEdges = mutableListOf<Edge>()
    val cutEdges = mutableListOf<Edge>()
    graph.edges.forEach { edge ->
        when {
            edge.vertices.all { it in firstVertices } -> firstEdges.add(edge)
            edge.vertices.all { it in secondVertices } -> secondEdges.add(edge)
            else -> cutEdges.add(edge)
        }
    }
    return MinCut(
        Graph(firstVertices.toHashSet(), firstEdges.toHashSet()),
        Graph(secondVertices.toHashSet(), secondEdges.toHashSet()),
        cutEdges
    )
}

fun mergeVerticesFromCut(graph: Graph, cut: Cut): Graph {
    val (affectedEdges, notAffectedEdges) = graph.edges.partition { edge -> cut.s in edge || cut.t in edge}
    val targets = affectedEdges.mapNotNull {edge ->
        if (cut.s in edge && cut.t in edge) {
            null
        } else if (cut.s in edge) {
            (edge.vertices - cut.s).first() to edge.weight
        } else {
            (edge.vertices - cut.t).first() to edge.weight
        }
    }.groupBy {
        it.first
    }.mapValues {
        it.value.sumOf { it.second }
    }
    val newVertices = graph.vertices - cut.t
    val newEdges = notAffectedEdges + targets.map { Edge(setOf(cut.s, it.key), it.value) }
    return Graph(newVertices.toHashSet(), newEdges.toHashSet())
}

fun mergeVerticesOfEdge(graph: Graph, edge: Edge): Graph {
    val edgeEndpoints = edge.vertices.flatMap { it.ids }.toSet()
    val verticesToMerge = graph.vertices.filter { it.ids.intersect(edgeEndpoints).isNotEmpty() }.toSet()
    val mergedIds = verticesToMerge.fold(emptySet<String>()) { merged, vertex ->
        merged + vertex.ids
    }
    val mergedVertex = Vertex(mergedIds)
    val newEdges = (graph.edges - edge).map { e ->
        val first = e.vertices.first()
        val second = e.vertices.last()
        if (first.ids.intersect(mergedIds).isNotEmpty()) {
            Edge(setOf(mergedVertex, second), e.weight)
        } else if (second.ids.intersect(mergedIds).isNotEmpty()) {
            Edge(setOf(first, mergedVertex), e.weight)
        } else {
            e
        }
    }
    val newVertices = graph.vertices - verticesToMerge + mergedVertex
    return Graph(newVertices.toHashSet(), newEdges.toHashSet())
}

fun kargersAlgorithm(graph: Graph): MinCut {
    var g = graph
    while (g.vertices.size > 2) {
        val edge = g.edges.random()
        g = mergeVerticesOfEdge(g, edge)
    }
    return constructMinCutResult(graph, g.vertices.first().ids)
}

fun partX(input: List<String>): Int {
    val graph = parseInput(input)
    val vertexEdgeCounts = graph.vertices.associateWith {
        graph.edges.filter { edge -> edge.vertices.contains(it) }.size
    }
    val groups = vertexEdgeCounts.entries.groupBy { it.value }
    groups.entries.forEach {
        println("${it.key}: ${it.value.size}")
    }
    return 54
}

fun part1(input: List<String>): Int {
    val graph = parseInput(input)
    val minCut = sequence {
        var iter = 0
        while (true) {
            yield(iter++)
        }
    }.map { iter ->
        val minCut = (1..10).toList().parallelStream().map {
            kargersAlgorithm(graph)
        }.toList().minBy { it.cutEdges.size }
        println("Iter $iter: ${minCut.cutEdges.size}")
        println("${minCut.first.vertices.size} * ${minCut.second.vertices.size}")
        println(minCut.cutEdges)
        minCut
    }.dropWhile {
        it.cutEdges.size > 3
    }.first()
    println("${minCut.first.vertices.size} * ${minCut.second.vertices.size}")
    println(minCut.cutEdges)
    return minCut.first.vertices.size * minCut.second.vertices.size
}

fun part2(input: List<String>): Int {
    return input.size
}

fun main() = AoCTask("day25").run {
    // test if implementation meets criteria from the description, like:
//    part1(readTestInput(2), 100)
    check(part1(testInput), 54)
    //check(part2(testInput), 1)

    println(part1(input))
    println(part2(input))
}

fun main2() = AoCTask("day25").run {
    val graph = parseInput(input)
    val minCutEdges = listOf(
        Edge(setOf(Vertex("zns"), Vertex("jff"))),
        Edge(setOf(Vertex("kzx"), Vertex("qmr"))),
        Edge(setOf(Vertex("nvb"), Vertex("fts"))),
    )
    println(graph.toMermaidString())
}
