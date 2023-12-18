package day17

import AoCTask
import FOURWAY_DIRECTIONS
import Grid
import Vector2
import aStar
import green
import minus
import plus
import unaryMinus

// https://adventofcode.com/2023/day/17

private fun printPathOverGrid(grid: Grid<Int>, shortestPath: List<Vector2>) {
    grid.forEachRowIndexed { y, row ->
        row.forEachIndexed { x, cost ->
            val pos = Vector2(x, y)
            val s = if (pos in shortestPath) {
                val i = shortestPath.indexOf(pos)
                if (i > 0) {
                    val prev = shortestPath[i - 1]
                    when (pos - prev) {
                        else -> cost.toString().green()
                    }
                } else {
                    cost.toString()
                }
            } else {
                cost.toString()
            }
            print(s)
        }
        println()
    }
}

private fun parseGrid(input: List<String>) =
    Grid(input.map { line -> line.map { c -> c.digitToInt() } })

data class Tile(val position: Vector2, val direction: Vector2, val consecutiveCount: Int)

fun part1(input: List<String>): Int {
    val grid = parseGrid(input)
    val start = Tile(Vector2(0, 0), Vector2.ZERO, 0)
    val goal = Vector2(grid.width - 1, grid.height - 1)
    val shortestPath = aStar(
        start,
        isGoal = { it.position == goal },
        cost = { _, to -> grid[to.position] },
        neighbourhood = { current ->
            val neighbours = FOURWAY_DIRECTIONS.map {
                val count = if (it == current.direction) current.consecutiveCount + 1 else 1
                Tile(current.position + it, it, count)
            }
            neighbours.filter {
                grid.getOrNull(it.position) != null
            }.filter {
                it.consecutiveCount <= 3
            }.filter {
                it.direction != -current.direction
            }
        },
        heuristic = { (it.position - goal).manhattanLength }
    )
    val cost = shortestPath?.drop(1)?.sumOf {
        grid[it.position]
    } ?: Int.MAX_VALUE
    if (shortestPath != null) {
        printPathOverGrid(grid, shortestPath.map { it.position })
    }
    return cost
}

fun part2(input: List<String>): Int {
    val grid = parseGrid(input)
    val start = Tile(Vector2(0, 0), Vector2.ZERO, 0)
    val goal = Vector2(grid.width - 1, grid.height - 1)
    val shortestPath = aStar(
        start,
        isGoal = { it.position == goal && it.consecutiveCount >= 4 },
        cost = { _, to -> grid[to.position] },
        neighbourhood = { current ->
            val neighbours = FOURWAY_DIRECTIONS.map {
                val count = if (it == current.direction) current.consecutiveCount + 1 else 1
                Tile(current.position + it, it, count)
            }
            neighbours.filter {
                grid.getOrNull(it.position) != null
            }.filter {
                it.consecutiveCount <= 10
            }.filter {
                it.direction != -current.direction
            }.filter {
                it.direction == current.direction || current.consecutiveCount >= 4 || current.direction == Vector2.ZERO
            }
        },
        heuristic = { (it.position - goal).manhattanLength }
    )
    val cost = shortestPath?.drop(1)?.sumOf {
        grid[it.position]
    } ?: Int.MAX_VALUE
    if (shortestPath != null) {
        printPathOverGrid(grid, shortestPath.map { it.position })
    }
    return cost
}

fun main() = AoCTask("day17").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 102)
    check(part2(testInput), 94)
    check(part2(readTestInput(2)), 71)

    println(part1(input))
    println(part2(input))
}
