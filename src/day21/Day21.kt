package day21

import AoCTask
import FOURWAY_DIRECTIONS
import Grid
import GridBoundaryCondition
import Vector2
import green
import plus

// https://adventofcode.com/2023/day/21

enum class StepState {
    UNVISITED,
    JUST_VISITED,
    EVEN,
    ODD
}

sealed class Tile {

    data class GardenTile(
        val pos: Vector2,
        var state: StepState = StepState.UNVISITED
    ) : Tile()

    data object RockTile : Tile()
}

fun parseInput(input: List<String>, looping: Boolean = false): Pair<Vector2, Grid<Tile>> {
    var start = Vector2(0, 0)
    val tiles = input.mapIndexed { y, line ->
        line.mapIndexed { x, c ->
            val pos = Vector2(x, y)
            when (c) {
                '.' -> Tile.GardenTile(pos)
                '#' -> Tile.RockTile
                'S' -> {
                    start = pos
                    Tile.GardenTile(pos, state = StepState.JUST_VISITED)
                }

                else -> throw IllegalArgumentException("Unknown tile: $c")
            }
        }
    }
    return start to Grid(
        tiles,
        boundaryCondition = if (looping) GridBoundaryCondition.WRAP else GridBoundaryCondition.EDGE
    )
}

//
fun printGrid(grid: Grid<Tile>, steps: Int) {
    grid.forEachRow { row ->
        row.forEach { tile ->
            when (tile) {
                is Tile.GardenTile -> {
                    val stepped = if (steps % 2 == 0) tile.state == StepState.EVEN else tile.state == StepState.ODD
                    if (stepped) {
                        print("0".green())
                    } else {
                        print(".")
                    }
                }

                is Tile.RockTile -> print("#")
            }
        }
        println()
    }
}
//
//fun printGridIncreases(grid: Grid<Tile>) {
//    grid.forEachRow { row ->
//        row.forEach { tile ->
//            when (tile) {
//                is Tile.GardenTile -> {
//                    val odd = tile.oddDelta.toString().padStart(2, ' ')
//                    val even = tile.evenDelta.toString().padEnd(2, ' ')
//                    print(" $odd|$even ")
//                }
//                is Tile.RockTile -> print(" (###) ")
//            }
//        }
//        println()
//    }
//}

fun part1(input: List<String>, steps: Int): Int {
    val (start, grid) = parseInput(input)
    var currentLocations = setOf(start)
    repeat(steps) {
        val newLocations = currentLocations.flatMap {
            grid.getNeighbors(it) {
                it is Tile.GardenTile
            } as List<Tile.GardenTile>
        }.map { it.pos }
        currentLocations = newLocations.toSet()
    }
    return currentLocations.size
}

fun part2(input: List<String>, steps: Int): Long {
    var (_, originalGrid) = parseInput(input, true)
    val validDirections = originalGrid.filterIsInstance<Tile.GardenTile>().associateWith {
        FOURWAY_DIRECTIONS.filter { dir ->
            val pos = it.pos + dir
            originalGrid[pos] is Tile.GardenTile
        }
    }.mapKeys { it.key.pos }
    val doneGrids = mutableSetOf<Vector2>()
    val grids = mutableMapOf(Vector2.ZERO to originalGrid)
    var evenSteps = 0L
    var oddSteps = 0L
    repeat(steps) { step ->
        grids.entries.flatMap { (sector, grid) ->
            grid.filterIsInstance<Tile.GardenTile>().filter {
                it.state == StepState.JUST_VISITED
            }.map {
                sector to it
            }
        }.forEach { (sector, tile) ->
            val grid = grids[sector]!!
            tile.state = if (step % 2 == 0) StepState.ODD else StepState.EVEN
            validDirections[tile.pos]?.forEach { dir ->
                val newPos = tile.pos + dir
                val neighbor = if (!grid.isInBounds(newPos)) {
                    val nextSector = sector + dir
                    if (nextSector !in doneGrids) {
                        val nextGrid = grids.getOrPut(nextSector) {
                            Grid(grid.mapRows {
                                it.map { t ->
                                    when (t) {
                                        is Tile.GardenTile -> t.copy(state = StepState.UNVISITED)
                                        is Tile.RockTile -> t
                                    }
                                }
                            }, boundaryCondition = GridBoundaryCondition.WRAP)
                        }
                        nextGrid[newPos]
                    } else {
                        null
                    }
                } else {
                    grid[newPos]
                }
                if (neighbor is Tile.GardenTile && neighbor.state == StepState.UNVISITED) {
                    neighbor.state = StepState.JUST_VISITED
                }
            }
        }

//        printGrid(originalGrid, step)

        val dones = grids.entries.filter { (sector, grid) ->
            grid.none { it is Tile.GardenTile && it.state == StepState.JUST_VISITED }
        }

        dones.forEach { (sector, grid) ->
            evenSteps += grid.count { it is Tile.GardenTile && it.state == StepState.EVEN }
            oddSteps += grid.count { it is Tile.GardenTile && it.state == StepState.ODD }
            doneGrids.add(sector)
            grids.remove(sector)
        }

        if (step % 1000 == 0) {
            println(step)
        }
    }

    grids.forEach { (sector, grid) ->
        grid.filterIsInstance<Tile.GardenTile>().forEach {
            if (it.state == StepState.JUST_VISITED) {
                it.state = if (steps % 2 == 0) StepState.ODD else StepState.EVEN
            }
            when (it.state) {
                StepState.EVEN -> evenSteps += 1
                StepState.ODD -> oddSteps += 1
                else -> {}
            }
        }
    }

    return if (steps % 2 == 0) oddSteps else evenSteps
}

fun main() = AoCTask("day21").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput, 6), 16)
    check(part2(testInput, 6), 16)
    check(part2(testInput, 10), 50)
    check(part2(testInput, 100), 6536)
    check(part2(testInput, 500), 167004)
    check(part2(testInput, 1000), 668697)
    check(part2(testInput, 5000), 16733044)

    println(part1(input, 64))
    println(part2(input, 26501365))
}
