package day16

import AoCTask
import Vector2
import plus
import java.util.*

// https://adventofcode.com/2023/day/16

typealias BeamDirections = EnumSet<BeamDirection>

enum class BeamDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    infix fun and(other: BeamDirection) = BeamDirections.of(this, other)
}

val NO_DIRECTION: BeamDirections = BeamDirections.noneOf(BeamDirection::class.java)

infix fun BeamDirections.allOf(other: BeamDirections) = containsAll(other)
infix fun BeamDirections.and(other: BeamDirection) = BeamDirections.of(other, *this.toTypedArray())

enum class MirrorOrientation {
    LEFT,
    RIGHT
}

enum class SplitterOrientation {
    VERTICAL,
    HORIZONTAL
}

sealed class Tile {

    var outgoingBeams = NO_DIRECTION

    val energised: Boolean get() = outgoingBeams.isNotEmpty()

    abstract fun clone(): Tile

    abstract fun processBeam(beam: Beam): List<Beam>

    class Empty() : Tile() {
        override fun processBeam(beam: Beam): List<Beam> {
            return if (beam.direction.toBeamDirection() !in outgoingBeams) {
                outgoingBeams = outgoingBeams.and(beam.direction.toBeamDirection())
                val newPosition = beam.position + beam.direction
                listOf(beam.copy(position = newPosition))
            } else {
                emptyList()
            }
        }
        override fun clone(): Tile {
            return Empty()
        }
    }

    data class Mirror(val orientation: MirrorOrientation) : Tile() {
        override fun processBeam(beam: Beam): List<Beam> {
            val dir = beam.direction.toBeamDirection()
            val newDir = when (orientation) {
                MirrorOrientation.LEFT -> {
                    when (dir) {
                        BeamDirection.UP -> Vector2.LEFT
                        BeamDirection.DOWN -> Vector2.RIGHT
                        BeamDirection.LEFT -> Vector2.UP
                        BeamDirection.RIGHT -> Vector2.DOWN
                    }
                }

                MirrorOrientation.RIGHT -> when (dir) {
                    BeamDirection.UP -> Vector2.RIGHT
                    BeamDirection.DOWN -> Vector2.LEFT
                    BeamDirection.LEFT -> Vector2.DOWN
                    BeamDirection.RIGHT -> Vector2.UP
                }
            }
            return if (newDir.toBeamDirection() !in outgoingBeams) {
                outgoingBeams = outgoingBeams.and(newDir.toBeamDirection())
                val newPosition = beam.position + newDir
                listOf(Beam(newPosition, newDir))
            } else {
                emptyList()
            }
        }
        override fun clone(): Tile {
            return Mirror(orientation)
        }
    }

    data class Splitter(val orientation: SplitterOrientation) : Tile() {
        override fun processBeam(beam: Beam): List<Beam> {
            val dir = beam.direction.toBeamDirection()
            val newDirs = when (orientation) {
                SplitterOrientation.VERTICAL -> when (dir) {
                    BeamDirection.DOWN, BeamDirection.UP -> listOf(beam.direction)
                    BeamDirection.LEFT, BeamDirection.RIGHT -> listOf(Vector2.UP, Vector2.DOWN)
                }
                SplitterOrientation.HORIZONTAL -> when (dir) {
                    BeamDirection.LEFT, BeamDirection.RIGHT -> listOf(beam.direction)
                    BeamDirection.UP, BeamDirection.DOWN -> listOf(Vector2.LEFT, Vector2.RIGHT)
                }
            }
            return newDirs.mapNotNull { newDir ->
                if (newDir.toBeamDirection() !in outgoingBeams) {
                    outgoingBeams = outgoingBeams.and(newDir.toBeamDirection())
                    val newPosition = beam.position + newDir
                    Beam(newPosition, newDir)
                } else {
                    null
                }
            }
        }
        override fun clone(): Tile {
            return Splitter(orientation)
        }
    }
}

data class Beam(val position: Vector2, val direction: Vector2)

fun Vector2.toBeamDirection(): BeamDirection {
    return when {
        this == Vector2.UP -> BeamDirection.UP
        this == Vector2.DOWN -> BeamDirection.DOWN
        this == Vector2.LEFT -> BeamDirection.LEFT
        this == Vector2.RIGHT -> BeamDirection.RIGHT
        else -> throw IllegalArgumentException("Cannot convert $this to a beam direction")
    }
}

data class TileGrid(val tiles: List<List<Tile>>) {
    operator fun get(position: Vector2) = tiles[position.y][position.x]
    operator fun get(x: Int, y: Int) = tiles[y][x]
    fun getOrNull(x: Int, y: Int): Tile? = tiles.getOrNull(y)?.getOrNull(x)
    fun getOrNull(position: Vector2): Tile? = getOrNull(position.x, position.y)

    val width: Int get() = tiles.firstOrNull()?.size?: 0
    val height: Int get() = tiles.size

    fun <T> map(block: (Tile) -> T): List<T> {
        return tiles.flatMap { row ->
            row.map(block)
        }
    }

    fun clone(): TileGrid {
        return TileGrid(tiles.map { row ->
            row.map { tile ->
                tile.clone()
            }
        })
    }

    fun countEnergisedTiles(): Int {
        return map { tile ->
            if (tile.energised) 1 else 0
        }.sum()
    }
}

private fun parseGrid(input: List<String>): TileGrid {
    val tiles = input.map {
        it.map { c ->
            when (c) {
                '.' -> Tile.Empty()
                '/' -> Tile.Mirror(MirrorOrientation.RIGHT)
                '\\' -> Tile.Mirror(MirrorOrientation.LEFT)
                '|' -> Tile.Splitter(SplitterOrientation.VERTICAL)
                '-' -> Tile.Splitter(SplitterOrientation.HORIZONTAL)
                else -> throw IllegalArgumentException()
            }
        }
    }
    return TileGrid(tiles)
}

private fun energiseTiles(grid: TileGrid, initialBeam: Beam): TileGrid {
    val energisedGrid = grid.clone()
    var beams = listOf<Beam>(initialBeam)
    while (beams.isNotEmpty()) {
        val newBeams: List<Beam> = beams.flatMap { beam ->
            val tile = energisedGrid[beam.position]
            tile.processBeam(beam)
        }
        beams = newBeams.filter {
            grid.getOrNull(it.position) != null
        }
    }
    return energisedGrid
}

fun part1(input: List<String>): Int {
    val grid = parseGrid(input)
    val energisedGrid = energiseTiles(grid, Beam(Vector2(0, 0), Vector2.RIGHT))
    return energisedGrid.countEnergisedTiles()
}

fun part2(input: List<String>): Int {
    val grid = parseGrid(input)
    val topBottomEdge = (0..<grid.width).flatMap { x ->
        listOf(0, grid.height - 1).flatMap { y ->
            val position = Vector2(x, y)
            val defaultBeams = if (y == 0) {
                listOf(Beam(position, Vector2.DOWN))
            } else {
                listOf(Beam(position, Vector2.UP))
            }
            when (x) {
                0 -> defaultBeams + Beam(position, Vector2.LEFT)
                grid.width - 1 -> defaultBeams + Beam(position, Vector2.RIGHT)
                else -> defaultBeams
            }
        }
    }
    val leftRightEdge = (1..<grid.height).flatMap { y ->
        listOf(0, grid.width - 1).map { x ->
            val position = Vector2(x, y)
            if (x == 0) {
                Beam(position, Vector2.RIGHT)
            } else {
                Beam(position, Vector2.LEFT)
            }
        }
    }
    return (topBottomEdge + leftRightEdge).asSequence().map { beam ->
        energiseTiles(grid, beam).countEnergisedTiles()
    }.maxOrNull()?: 0
}

fun main() = AoCTask("day16").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 46)
    check(part2(testInput), 51)

    println(part1(input))
    println(part2(input))
}
