package day23

import AoCTask
import Grid
import Vector2
import aStar
import blue
import blueBg
import cyan
import cyanBg
import fourWayNeighbourhood
import green
import greenBg
import magenta
import magentaBg
import minus
import plus
import printlnEach
import red
import redBg
import yellow
import yellowBg

// https://adventofcode.com/2023/day/23


sealed class Tile {

    abstract val pos: Vector2

    data class Forest(override val pos: Vector2) : Tile()

    data class Path(override val pos: Vector2) : Tile()
    data class Slope(override val pos: Vector2, val dir: Vector2) : Tile()
}

fun parseInput(input: List<String>): Grid<Tile> {
    val tiles = input.mapIndexed { y, row ->
        row.mapIndexed { x, c ->
            val pos = Vector2(x, y)
            when (c) {
                '#' -> Tile.Forest(pos)
                '.' -> Tile.Path(pos)
                '>' -> Tile.Slope(pos, Vector2(1, 0))
                '<' -> Tile.Slope(pos, Vector2(-1, 0))
                '^' -> Tile.Slope(pos, Vector2(0, -1))
                'v' -> Tile.Slope(pos, Vector2(0, 1))
                else -> throw IllegalArgumentException("Unknown tile character: $c")
            }
        }
    }
    return Grid(tiles)
}

fun showPath(grid: Grid<Tile>, path: List<Vector2>) {
    grid.forEachRowIndexed { y, row ->
        row.forEachIndexed { x, tile ->
            if (tile.pos in path) {
                val index = path.indexOf(tile.pos)
                val count = path.count { it == tile.pos }
                if (count == 1) {
                    print("O".green())
                } else {
                    print("$count".red())
                }
            } else {
                when (tile) {
                    is Tile.Forest -> print("#")
                    is Tile.Path -> print(".")
                    is Tile.Slope -> print(".")
                }
            }
        }
        println()
    }
}

fun showMultiplePaths(grid: Grid<Tile>, paths: List<List<Vector2>>) {
    val colors = listOf(
        String::magenta,
        String::blue,
        String::green,
        String::yellow,
        String::red,
        String::cyan,
        String::magentaBg,
        String::blueBg,
        String::greenBg,
        String::yellowBg,
        String::redBg,
        String::cyanBg
    )
    grid.forEachRowIndexed { y, row ->
        row.forEachIndexed { x, tile ->
            val pathIndex = paths.indexOfFirst { tile.pos in it }
            if (pathIndex > -1) {
                val count = paths.count { tile.pos in it }
                if (count == 1) {
                    val color = colors[pathIndex % colors.size]
                    print(color("O"))
                } else {
                    print("$count".red())
                }
            } else {
                when (tile) {
                    is Tile.Forest -> print("#")
                    is Tile.Path -> print(".")
                    is Tile.Slope -> print(".")
                }
            }
        }
        println()
    }
}

fun findAllIntersections(grid: Grid<Tile>): List<Vector2> {
    val intersections = mutableSetOf<Vector2>()
    grid.forEachRowIndexed { y, row ->
        row.forEachIndexed { x, tile ->
            if (tile !is Tile.Forest) {
                val count = grid.getNeighbors(tile.pos) { it !is Tile.Forest }.size
                if (count >= 3) {
                    intersections.add(tile.pos)
                }
            }
        }
    }
    return intersections.toList()
}

fun part1(input: List<String>): Int {
    val grid = parseInput(input)
    val start = grid.rows.first().first { it is Tile.Path }
    val end = grid.rows.last().first { it is Tile.Path }
    val finishedPaths = mutableListOf<List<Vector2>>()
    var unfinishedPaths = listOf(listOf(start.pos))
    while (unfinishedPaths.isNotEmpty()) {
        val extendedPaths = unfinishedPaths.flatMap { path ->
            val end = path.last()
            val current = grid[end]
            val options = if (current is Tile.Slope) {
                listOf(current.pos + current.dir)
            } else {
                grid.getNeighbors(end) {
                    it !is Tile.Forest
                }.map { it.pos }
            }
            val validOptions = options.filter { it !in path }
            validOptions.map {
                path + it
            }
        }
        val (finished, unfinished) = extendedPaths.partition { it.last() == end.pos }
        finishedPaths.addAll(finished)
        unfinishedPaths = unfinished
    }
    return finishedPaths.maxOf { it.size - 1 }
}

data class Edge(val path: List<Vector2>) {
    val start = path.first()
    val end = path.last()

    fun ensureStartsWith(pos: Vector2) = if (start == pos) path.drop(1) else path.reversed().drop(1)
}

fun part2(input: List<String>): Int {
    val grid = parseInput(input)
    val start = grid.rows.first().first { it is Tile.Path }
    val goal = grid.rows.last().first { it is Tile.Path }

    val intersections = findAllIntersections(grid)

    val points = intersections + start.pos + goal.pos

    val allPairs = points.flatMapIndexed() { i, point ->
        points.drop(i + 1).map { other ->
            point to other
        }
    }

    val allPaths = allPairs.parallelStream().flatMap { (a, b) ->
        val starts = grid.getNeighbors(a) { it !is Tile.Forest }
        val goals = grid.getNeighbors(b) { it !is Tile.Forest }

        val paths = starts.flatMap { start ->
            goals.map { goal -> start.pos to goal.pos }
        }.mapNotNull { (start, goal) ->
            aStar(
                start,
                { it == goal },
                { _, _ -> 1 },
                { pos ->
                    grid.getNeighbors(pos) {
                        it !is Tile.Forest
                    }.map { it.pos }.filter {
                        it != a && it != b
                    }
                },
                { pos -> (pos - goal).manhattanLength }
            )
        }.map {
            listOf(a, *it.toTypedArray(), b)
        }

        paths.map { Edge(it) }.stream()
    }

    val edges = allPaths.filter { edge ->
        val otherPoints = points.filter { it != edge.start && it != edge.end }
        otherPoints.none { it in edge.path }
    }.toList()

    edges.printlnEach()
    showMultiplePaths(grid, edges.map { edge -> edge.path })

    val longestPath = findLongestPath(start.pos, goal.pos, edges)

    showPath(grid, longestPath)

    return longestPath.size - 1
}

fun findLongestPath(start: Vector2, goal: Vector2, edges: List<Edge>, currentPath: List<Edge> = emptyList()): List<Vector2> {
    if (start == goal) {
        return currentPath.flatMap { it.path.dropLast(1) } + goal
    }
    val possibleEdges = edges.filter { edge ->
        edge.path.first() == start || edge.path.last() == start
    }
    if (possibleEdges.isEmpty()) {
        return emptyList()
    }
    return possibleEdges.map {edge ->
        val (newStart, newPath) = if (edge.path.first() == start) {
            edge.path.last() to (currentPath + edge)
        } else {
            edge.path.first() to (currentPath + Edge(edge.path.reversed()))
        }
        findLongestPath(newStart, goal, edges-possibleEdges.toSet(), newPath)
    }.maxBy { path ->
        path.size
    }
}

fun growBothSides(
    start: Vector2,
    goal: Vector2,
    grid: Grid<Tile>,
    edges: List<Edge>,
    pathFromStart: List<Vector2>,
    pathFromGoal: List<Vector2>
): List<Pair<List<Vector2>, List<Vector2>>> {

//    println("start: $start")
//    println("goal: $goal")
//    println("pathFromStart: $pathFromStart")
//    println("pathFromGoal: $pathFromGoal")

    val usedTiles = pathFromStart.toSet() + pathFromGoal.toSet() - start - goal

    if (start == goal) {
        return listOf(pathFromStart to pathFromGoal.reversed().drop(1))
    } else {
        val shortestPath = aStar(
            start,
            { it == goal },
            { _, _ -> 1 },
            { pos ->
                grid.getNeighbors(pos) {
                    it !is Tile.Forest
                }.map { it.pos }.filter {
                    it !in usedTiles
                }
            },
            { pos -> (pos - goal).manhattanLength }
        )

        if (shortestPath == null) {
            return emptyList()
        } else {
            val startOptions =
                edges.filter { it.start == start || it.end == start }.sortedByDescending { it.path.size }.filter {
                    it.path.none { p -> p in usedTiles }
                }.map {
                    it.ensureStartsWith(start)
                }.map { start to it }
            val goalOptions =
                edges.filter { it.start == goal || it.end == goal }.sortedByDescending { it.path.size }.filter {
                    it.path.none { p -> p in usedTiles }
                }.map {
                    it.ensureStartsWith(goal)
                }.map { goal to it }

            val combinedOptions = (startOptions + goalOptions).sortedByDescending { it.second.size }

            var currentStartOption: List<Vector2>? = null
            var currentGoalOption: List<Vector2>? = null

            return combinedOptions.flatMap { option ->
                if (option.first == start) {
                    currentStartOption = option.second
                } else {
                    currentGoalOption = option.second
                }
                val startOption = currentStartOption
                val goalOption = currentGoalOption
                if (startOption != null && goalOption != null) {
                    if (startOption.none { it in (goalOption + goal) } || startOption.last() == goalOption.last()) {
                        growBothSides(
                            start = startOption.last(),
                            goal = goalOption.last(),
                            grid = grid,
                            edges = edges,
                            pathFromStart = pathFromStart + startOption,
                            pathFromGoal = pathFromGoal + goalOption
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
        }
    }
}

fun bruteForcePart2(input: List<String>): Int {
    val grid = parseInput(input)
    val start = grid.rows.first().first { it is Tile.Path }
    val goal = grid.rows.last().first { it is Tile.Path }
    val finishedPaths = mutableListOf<List<Vector2>>()
    var unfinishedPaths = listOf(listOf(start.pos))
    var counter = 0
    val optionsByPos = grid.filter {
        it !is Tile.Forest
    }.associateWith {
        grid.getNeighbors(it.pos) {
            it !is Tile.Forest
        }.map {
            it.pos
        }
    }.mapKeys { it.key.pos }
    while (unfinishedPaths.isNotEmpty()) {
        println(counter)
        val extendedPaths = unfinishedPaths.parallelStream().flatMap { path ->
            var end = path.last()
            val shortedPath = aStar(
                end,
                { pos -> pos == goal.pos },
                { from, to -> 1 },
                { pos ->
                    optionsByPos[pos]!!.filter {
                        it !in path
                    }
                },
                { pos -> (pos - goal.pos).manhattanLength }
            )
            if (shortedPath != null) {
                var options = optionsByPos[end]!!.filter { it !in path }
                if (options.isNotEmpty()) {
                    val currentPath = path.toMutableList()
                    while (options.size == 1 && end != goal.pos) {
                        currentPath.add(options.first())
                        end = currentPath.last()
                        options = optionsByPos[end]!!.filter { it !in currentPath }
                    }
                    if (options.isEmpty()) {
                        listOf(currentPath).stream()
                    } else {
                        options.stream().map {
                            currentPath + it
                        }
                    }
                } else {
                    emptyList<List<Vector2>>().stream()
                }
            } else {
                emptyList<List<Vector2>>().stream()
            }
        }
        val (finished, unfinished) = extendedPaths.toList().partition { it.last() == goal.pos }
        finishedPaths.addAll(finished)
        unfinishedPaths = unfinished
        counter++
        if (counter % 10 == 0) {
            val path1 = finishedPaths.maxByOrNull { it.size }
            println(path1?.size)
            if (path1 != null) {
                showPath(grid, path1)
            }
            val path2 = unfinishedPaths.maxByOrNull { it.size }
            println(path2?.size)
            if (path2 != null) {
                showPath(grid, path2)
            }
        }
    }
    return finishedPaths.maxOf { it.size - 1 }
}

fun main() = AoCTask("day23").run {

    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 94)
    check(part2(testInput), 154)

    println(part1(input))
    println(part2(input))
}
