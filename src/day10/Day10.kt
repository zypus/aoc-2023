package day10

import AoCTask
import Vector2
import day10.Direction.*
import plus

// https://adventofcode.com/2023/day/10

enum class Direction(val vector: Vector2) {
    UP(Vector2(0, -1)),
    DOWN(Vector2(0, 1)),
    LEFT(Vector2(-1, 0)),
    RIGHT(Vector2(1, 0));

    fun connects(other: Direction): Boolean {
        return vector.x == -other.vector.x && vector.y == -other.vector.y
    }

    fun opposite(): Direction {
        return when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }
}

enum class PipeShape(val input: Direction, val output: Direction) {
    VERTICAL(UP, DOWN),
    HORIZONTAL(LEFT, RIGHT),
    NORTH_EAST(UP, RIGHT),
    NORTH_WEST(UP, LEFT),
    SOUTH_EAST(DOWN, RIGHT),
    SOUTH_WEST(DOWN, LEFT);

    fun connects(other: PipeShape, direction: Direction): Boolean {
        return if (direction == input || direction == output) {
            other.output.connects(direction) || other.input.connects(direction)
        } else {
            false
        }
    }

    companion object {
        fun fromDirections(input: Direction, output: Direction): PipeShape {
            val directions = listOf(input, output)
            return when {
                UP in directions && DOWN in directions -> VERTICAL
                LEFT in directions && RIGHT in directions -> HORIZONTAL
                UP in directions && RIGHT in directions -> NORTH_EAST
                UP in directions && LEFT in directions -> NORTH_WEST
                DOWN in directions && RIGHT in directions -> SOUTH_EAST
                DOWN in directions && LEFT in directions -> SOUTH_WEST
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun getOtherDirection(direction: Direction): Direction {
        return if (direction == input) output else input
    }
}

interface Shaped {
    val shape: PipeShape
}

sealed class Tile {

    abstract var distanceToStart: Long

    abstract val coord: Vector2

    abstract fun canConnectTo(other: Tile, direction: Direction): Boolean

    data class StartTile(override val coord: Vector2) : Tile(), Shaped {

        override lateinit var shape: PipeShape

        override fun canConnectTo(other: Tile, direction: Direction): Boolean {
            return other.canConnectTo(this, direction.opposite())
        }

        override var distanceToStart: Long = 0

        override fun toString(): String {
            return "S"
        }
    }

    data class Pipe(override val shape: PipeShape, override val coord: Vector2) : Tile(), Shaped {
        override var distanceToStart: Long = -1

        override fun toString(): String {
            return when (shape) {
                PipeShape.VERTICAL -> "│"
                PipeShape.HORIZONTAL -> "─"
                PipeShape.NORTH_EAST -> "└"
                PipeShape.NORTH_WEST -> "┘"
                PipeShape.SOUTH_EAST -> "┌"
                PipeShape.SOUTH_WEST -> "┐"
            }
        }

        override fun canConnectTo(other: Tile, direction: Direction): Boolean {
            return when (other) {
                is Empty -> false
                is Pipe -> shape.connects(other.shape, direction)
                is StartTile -> shape.connects(other.shape, direction)
            }
        }

        fun canConnectTo(direction: Direction): Boolean {
            return shape.input.connects(direction) || shape.output.connects(direction)
        }
    }

    data class Empty(override val coord: Vector2) : Tile() {
        override fun toString(): String {
            return "."
        }

        override fun canConnectTo(other: Tile, direction: Direction): Boolean {
            return false
        }

        override var distanceToStart: Long = -1

        var inside: Boolean = false
    }
}

data class Grid(val rows: List<List<Tile>>) {
    fun getOrNull(x: Int, y: Int): Tile? = rows.getOrNull(y)?.getOrNull(x)
    operator fun get(x: Int, y: Int): Tile = rows[y][x]

    val startTile: Tile.StartTile by lazy {
        rows.first { row ->
            row.any { it is Tile.StartTile }
        }.first { row ->
            row is Tile.StartTile
        } as Tile.StartTile
    }

    fun getNeighborOf(tile: Tile, direction: Direction): Tile? {
        return getOrNull(tile.coord.x + direction.vector.x, tile.coord.y + direction.vector.y)
    }

    fun computeDistances(): Long {
        var currentDistance = 0L
        var leftDirection = startTile.shape.input
        var rightDirection = startTile.shape.output
        var leftTile = getNeighborOf(tile = startTile, direction = leftDirection)!! as Tile.Pipe
        var rightTile = getNeighborOf(tile = startTile, direction = rightDirection)!! as Tile.Pipe
        while (leftTile.distanceToStart == -1L || rightTile.distanceToStart == -1L) {
            currentDistance++
            if (leftTile.distanceToStart == -1L) {
                leftTile.distanceToStart = currentDistance
            }
            if (rightTile.distanceToStart == -1L) {
                rightTile.distanceToStart = currentDistance
            }
            leftDirection = leftTile.shape.getOtherDirection(leftDirection.opposite())
            rightDirection = rightTile.shape.getOtherDirection(rightDirection.opposite())
            leftTile = getNeighborOf(tile = leftTile, direction = leftDirection)!! as Tile.Pipe
            rightTile = getNeighborOf(tile = rightTile, direction = rightDirection)!! as Tile.Pipe
        }
        return currentDistance
    }

    override fun toString(): String {
        return rows.joinToString("\n") { row ->
            row.joinToString("") { tile ->
                tile.toString()
            }
        }
    }

    fun toDistanceString(): String {
        return rows.joinToString("\n") { row ->
            row.joinToString("") { tile ->
                if (tile.distanceToStart >= 0) tile.distanceToStart.toString() else "."
            }
        }
    }

    fun toInsideOutsideString(): String {
        return rows.joinToString("\n") { row ->
            row.joinToString("") { tile ->
                when(tile) {
                    is Tile.Empty -> if (tile.inside) "I" else "O"
                    else -> tile.toString()
                }
            }
        }
    }

}

fun parseInput(input: List<String>): Grid {
    val rows = input.mapIndexed { y, row ->
        row.mapIndexed { x, char ->
            val coord = Vector2(x, y)
            val tile = when (char) {
                '.' -> Tile.Empty(coord)
                '|' -> Tile.Pipe(PipeShape.VERTICAL, coord)
                '-' -> Tile.Pipe(PipeShape.HORIZONTAL, coord)
                'L' -> Tile.Pipe(PipeShape.NORTH_EAST, coord)
                'J' -> Tile.Pipe(PipeShape.NORTH_WEST, coord)
                'F' -> Tile.Pipe(PipeShape.SOUTH_EAST, coord)
                '7' -> Tile.Pipe(PipeShape.SOUTH_WEST, coord)
                'S' -> Tile.StartTile(coord)
                else -> throw IllegalArgumentException()
            }
            tile
        }
    }
    val grid = Grid(rows)
    val startTile = grid.startTile
    val startTileDirections = Direction.entries.filter { direction ->
        val neighbour = startTile.coord + direction.vector
        val neighbourTile = grid.getOrNull(neighbour.x, neighbour.y)
        neighbourTile != null && neighbourTile is Tile.Pipe && neighbourTile.canConnectTo(direction)
    }
    check(startTileDirections.size == 2)
    startTile.shape = PipeShape.fromDirections(startTileDirections.first(), startTileDirections.last())
    return grid
}

fun extractLoopOnlyGrid(grid: Grid): Grid {
    val onlyLoopRows = grid.rows.map { row ->
        row.map {
            if (it.distanceToStart >= 0) it else Tile.Empty(it.coord)
        }
    }
    return Grid(onlyLoopRows)
}

fun upscale(grid: Grid): Grid {
    val scaledGridRows = grid.rows.flatMapIndexed { y, row ->
        val newY = 2 * y
        val firstRow = row.flatMapIndexed { x, tile ->
            val newX = 2 * x
            when (tile) {
                is Tile.Empty -> listOf(
                    Tile.Empty(Vector2(newX, newY)),
                    Tile.Empty(Vector2(newX + 1, newY)),
                )

                is Tile.Pipe, is Tile.StartTile -> {
                    val shaped = tile as Shaped
                    when (shaped.shape) {
                        PipeShape.HORIZONTAL, PipeShape.NORTH_EAST, PipeShape.SOUTH_EAST -> listOf(
                            Tile.Pipe(shaped.shape, Vector2(newX, newY)),
                            Tile.Pipe(PipeShape.HORIZONTAL, Vector2(newX + 1, newY)),
                        )

                        else -> listOf(
                            Tile.Pipe(shaped.shape, Vector2(newX, newY)),
                            Tile.Empty(Vector2(newX + 1, newY)),
                        )
                    }
                }
            }
        }
        val secondRow = firstRow.mapIndexed { x, tile ->
            when (tile) {
                is Tile.Pipe, is Tile.StartTile -> {
                    val shaped = tile as Shaped
                    when (shaped.shape) {
                        PipeShape.VERTICAL, PipeShape.SOUTH_EAST, PipeShape.SOUTH_WEST ->
                            Tile.Pipe(PipeShape.VERTICAL, Vector2(x, newY + 1))

                        else -> Tile.Empty(Vector2(x, newY + 1))
                    }
                }

                else -> Tile.Empty(Vector2(x, newY + 1))
            }
        }
        listOf(firstRow, secondRow)
    }
    return Grid(scaledGridRows)
}

fun computeInsideOutside(grid: Grid) {
    val visitedCoords = mutableSetOf<Vector2>()

    fun getNextSeed(): Tile.Empty? {
        return grid.rows.firstOrNull { row ->
            row.any { tile ->
                tile is Tile.Empty && tile.coord !in visitedCoords
            }
        }?.firstOrNull { row ->
            row is Tile.Empty && row.coord !in visitedCoords
        } as? Tile.Empty
    }

    var seed = getNextSeed()
    while (seed!= null) {
        val (visited, inside) = floodFillInsideOutside(grid, listOf(seed.coord))
        if (inside) {
            visited.forEach {
                (grid[it.x, it.y] as? Tile.Empty)?.inside = true
            }
        }
        visitedCoords.addAll(visited)
        seed = getNextSeed()
    }
}

fun downsample(grid: Grid): Grid {
    val sampledRows = grid.rows.filterIndexed { index, _ ->
        index % 2 == 0
    }.map {row ->
        row.filterIndexed { index, _ -> index % 2 == 0}
    }
    return Grid(sampledRows)
}

tailrec fun floodFillInsideOutside(grid: Grid, seeds: List<Vector2>, visited: MutableList<Vector2> = mutableListOf(), stillInside: Boolean = true): Pair<List<Vector2>, Boolean> {
    var inside = stillInside

    val newSeeds = seeds.flatMap { coord ->
        visited.add(coord)
        val tile = grid.getOrNull(coord.x, coord.y)

        if (tile!= null && tile is Tile.Empty) {
            Direction.entries.map { direction ->
                tile.coord + direction.vector
            }.filter {
                val isEdge = grid.getOrNull(it.x, it.y) == null
                if (inside && isEdge) {
                    inside = false
                }
                it !in visited && !isEdge
            }
        } else {
            emptyList()
        }
    }.distinct()
    return if (newSeeds.isEmpty()) {
        visited to inside
    } else {
        floodFillInsideOutside(grid, newSeeds, visited, inside)
    }
}

fun part1(input: List<String>): Long {
    val grid = parseInput(input)
    val maxDistance = grid.computeDistances()
    println(grid)
    println(grid.toDistanceString())
    return maxDistance
}

fun part2(input: List<String>): Int {
    val grid = parseInput(input)
    grid.computeDistances()
    val onlyLoopGrid = extractLoopOnlyGrid(grid)
    val scaledGrid = upscale(onlyLoopGrid)
    println(scaledGrid)
    computeInsideOutside(scaledGrid)
    println(scaledGrid.toInsideOutsideString())
    val downsampledGrid = downsample(scaledGrid)
    println(downsampledGrid.toInsideOutsideString())
    return downsampledGrid.rows.sumOf { row ->
        row.count { tile -> tile is Tile.Empty && tile.inside }
    }
}

fun main() = AoCTask("day10").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 4)
    check(part1(readTestInput(2)), 4)
    check(part1(readTestInput(3)), 8)
    check(part1(readTestInput(4)), 8)
    check(part2(readTestInput(5)), 4)
    check(part2(readTestInput(6)), 8)
    check(part2(readTestInput(7)), 10)

    println(part1(input))
    println(part2(input))
}
