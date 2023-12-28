package day21

import AoCTask
import FOURWAY_DIRECTIONS
import Grid
import GridBoundaryCondition
import Vector2
import blue
import blueBg
import cyan
import cyanBg
import green
import greenBg
import minus
import plus
import red
import redBg
import yellow
import yellowBg

// https://adventofcode.com/2023/day/21

sealed class Tile {

    data class GardenTile(
        val pos: Vector2
    ) : Tile() {
        var step: Long = -1L
    }

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
                    Tile.GardenTile(pos).also {
                        it.step = 0L
                    }
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


val stepColors = listOf(String::red, String::redBg, String::green, String::greenBg, String::blue, String::blueBg, String::yellow, String::yellowBg, String::cyan, String::cyanBg)



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

data class GardenGrid(val grid: Grid<Tile>, val sector: Vector2) {
    var done = false
    var activeTiles = setOf<Tile.GardenTile>()
    var signature = -1L
    var maxStep = 0L

    var minUp = 0L
    var minDown = 0L
    var minLeft = 0L
    var minRight = 0L

    fun update(): Set<Triple<Long, Vector2, Triple<Vector2, Vector2?, Long>>> {
//        FOURWAY_DIRECTIONS.map { Triple(signature, sector, (sector + it) to Vector2.ZERO) }.toSet()
//        if (done) {
//            val mins = listOf(minLeft, minRight, minUp, minDown)
//            return FOURWAY_DIRECTIONS.mapIndexed { i, dir ->
//                Triple(signature, sector, Triple(sector + dir, null, mins[i]))
//            }.toSet()
//        }
        if (done) return emptySet()
        val newActiveTiles = mutableSetOf<Tile.GardenTile>()
        val externalTiles = mutableSetOf<Triple<Long, Vector2, Triple<Vector2, Vector2, Long>>>()
        activeTiles.forEach { tile ->
            val neighborDirections = validDirections[tile.pos]!!
            neighborDirections.forEach { dir ->
                val newPos = tile.pos + dir
                if (!grid.isInBounds(newPos)) {
                    val nextSector = sector + dir
                    externalTiles.add(Triple(signature, sector, Triple(nextSector, newPos, tile.step + 1)))
                } else if (signature == -1L) {
                    val neighbor = grid[newPos] as Tile.GardenTile
                    if (neighbor.step == -1L) {
                        neighbor.step = tile.step + 1
                        maxStep = maxOf(maxStep, neighbor.step)
                        newActiveTiles.add(neighbor)
                    }
                }
                if (tile.pos.x == 0) {
                    minLeft = minOf(minLeft, tile.step)
                }
                if (tile.pos.x == grid.width - 1) {
                    minRight = minOf(minRight, tile.step)
                }
                if (tile.pos.y == 0) {
                    minUp = minOf(minUp, tile.step)
                }
                if (tile.pos.y == grid.height - 1) {
                    minDown = minOf(minDown, tile.step)
                }
            }
        }
        activeTiles = newActiveTiles
        if (activeTiles.isEmpty()) {
            done = true
            grid.forEachIndexed { index, tile ->
                if (tile is Tile.GardenTile) {
                    signature = toString().hashCode().toLong() + getOddSteps(Long.MAX_VALUE) + getEvenSteps(Long.MAX_VALUE)
                }
            }
        }
        return if (done && useSignature) {
            externalTiles.map { Triple(signature, sector, it.third) }.groupBy { it.second }.mapValues {
                it.value.minBy { it.third.third }
            }.values.toSet()
        } else {
            externalTiles
        }
    }

    fun getOddSteps(maxSteps: Long): Long {
        return grid.filterIsInstance<Tile.GardenTile>().count { it.step in 0..maxSteps && it.step % 2 == 1L }.toLong()
    }

    fun getEvenSteps(maxSteps: Long): Long {
        return grid.filterIsInstance<Tile.GardenTile>().count { it.step in 0..maxSteps && it.step % 2 == 0L }.toLong()
    }

    val offset: Long get() {
        val minSteps = grid.minOf { tile ->
            if (tile is Tile.GardenTile) tile.step else Long.MAX_VALUE
        }
        return if (minSteps % 2 == 0L) minSteps else minSteps-1
    }

    fun shiftSteps(offset: Long) {
        grid.forEachIndexed { index, tile ->
            if (tile is Tile.GardenTile) {
                if (tile.step >= 0) {
                    tile.step += offset
                }
            }
        }
        maxStep += offset
        minLeft += offset
        minRight += offset
        minUp += offset
        minDown += offset
    }

    override fun toString(): String {
        val builder = StringBuilder()
        with(builder) {
//            appendLine("$sector ${getEvenSteps(maxSteps)} ${getOddSteps(maxSteps)}")
            grid.forEachRow { row ->
                row.forEach { tile ->
                    when (tile) {
                        is Tile.GardenTile -> {
                            if (tile.step > -1L) {
                                val step = (tile.step) % 10L
                                val color = stepColors[step.toInt()]
                                append(color(step.toString()))
                            } else {
                                append(".")
                            }
                        }

                        is Tile.RockTile -> append("#")
                    }
                }
                appendLine()
            }
        }
        return builder.toString()
    }

    companion object {
        lateinit var validDirections: Map<Vector2, List<Vector2>>
        var useSignature = true
        var maxSteps: Long = Long.MAX_VALUE
    }
}

class SparseGrid {
    val cells = mutableMapOf<Vector2, GardenGrid>()

    operator fun get(pos: Vector2): GardenGrid? = cells[pos]

    operator fun set(pos: Vector2, value: GardenGrid) {
        cells[pos] = value
    }

    operator fun get(pos: Vector2, cell: Vector2): Tile? = get(pos)?.grid?.get(cell)

    override fun toString(): String {
        val builder = StringBuilder()
        val minX = cells.minOf { it.key.x }
        val maxX = cells.maxOf { it.key.x }
        val minY = cells.minOf { it.key.y }
        val maxY = cells.maxOf { it.key.y }
        val (width, height) = cells.entries.first().let {
            it.value.grid.width to it.value.grid.height
        }
        val empty = Array(width) { Array(height) { ' ' }.joinToString("") }.joinToString("\n")
        with(builder) {
            for (y in minY..maxY) {
                val keys = (minX..maxX).map { x -> Vector2(x, y) }
                val sectors = keys.map {
                    val grid = cells[it]
                    val sector = grid?.toString() ?: empty
                    sector.lines()
                }
                (1..height).forEach {
                    sectors.forEach {sector ->
                        append(sector[it-1])
                    }
                    appendLine()
                }
            }
        }
        return builder.toString()
    }
}

fun part2(input: List<String>, steps: Int): Long {
    val (_, originalGrid) = parseInput(input, true)
    GardenGrid.validDirections = originalGrid.filterIsInstance<Tile.GardenTile>().associateWith {
        FOURWAY_DIRECTIONS.filter { dir ->
            val pos = it.pos + dir
            originalGrid[pos] is Tile.GardenTile
        }
    }.mapKeys { it.key.pos }
    val grid = GardenGrid(originalGrid, Vector2.ZERO)
    grid.activeTiles = originalGrid.filterIsInstance<Tile.GardenTile>().filter { it.step == 0L}.toSet()
    while (!grid.done) {
        grid.update()
    }
    println(grid)

    val evenSteps = grid.getOddSteps(Long.MAX_VALUE)
    val oddSteps = grid.getEvenSteps(Long.MAX_VALUE)

    val evenCorners = evenSteps - grid.getEvenSteps(65)
    val oddCorners = oddSteps - grid.getOddSteps(65)

    val n = (steps.toLong()-originalGrid.width/2) / originalGrid.width

    println(n)

    val fullTiles = 1 + 4 * (steps.toLong() * (steps + 1) / 2)
    val emptyTile = steps.toLong() * steps

    val upperLimit = fullTiles - emptyTile
    println(fullTiles)
    println(emptyTile)
    println(upperLimit)

    val answer = (n+1)*(n+1) * oddSteps + (n*n) * evenSteps - (n+1) * oddCorners + n * evenCorners

    // 617729401414635

    return answer
}

fun part3(input: List<String>, steps: Int): Long {
    val (_, originalGrid) = parseInput(input, true)
    GardenGrid.validDirections = originalGrid.filterIsInstance<Tile.GardenTile>().associateWith {
        FOURWAY_DIRECTIONS.filter { dir ->
            val pos = it.pos + dir
            originalGrid[pos] is Tile.GardenTile
        }
    }.mapKeys { it.key.pos }

    fun newGrid(sector: Vector2): GardenGrid {
        val grid = Grid(originalGrid.mapRows {
            it.map { t ->
                when (t) {
                    is Tile.GardenTile -> t.copy()
                    is Tile.RockTile -> t
                }
            }
        }, boundaryCondition = GridBoundaryCondition.WRAP)
        return GardenGrid(grid, sector)
    }

    val sparseGrid = SparseGrid()
    val sourceSector = GardenGrid(originalGrid, Vector2.ZERO).also {
        val startTile = it.grid.find { tile -> tile is Tile.GardenTile && tile.step == 0L } as? Tile.GardenTile
        if (startTile != null) {
            it.activeTiles = setOf(startTile)
        }
    }
    sparseGrid[Vector2.ZERO] = sourceSector

    var evenCounter = 0L
    var oddCounter = 0L

    val signatures = mutableSetOf<Long>()
    val signatureDirections = mutableMapOf<Long, List<GardenGrid?>>()
    val signatureMap = mutableMapOf<Vector2, Long>()

    val useSignatures = false
    GardenGrid.useSignature = useSignatures

    var maxStep = 0L

    val doneSectorRecords = mutableSetOf<Vector2>()

    while (maxStep < steps) {
        println("0: ${sparseGrid.cells.entries.size}")

        val boundaryUpdates = sparseGrid.cells.entries.flatMap { (sector, grid) ->
            val result = grid.update()
            maxStep = maxOf(maxStep, grid.maxStep)
            result
        }.toList()

        println("1")

        val doneSectors = sparseGrid.cells.entries.parallelStream().map {(sector, grid) ->
            if (grid.done) {
                val neighbors = FOURWAY_DIRECTIONS.map { dir -> sector + dir }.map { sparseGrid[it] }
                if (neighbors.all { it == null || it.done }) {
                    sector to grid
                } else {
                    null
                }
            } else {
                null
            }
        }.toList().filterNotNull().distinctBy { it.first }

        println("2")

        boundaryUpdates.forEach { (signature, sector, data) ->
            val (nextSector, nextPos, targetStep) = data
            if (nextSector !in doneSectorRecords) {
                if (useSignatures && signature != -1L && signature in signatures) {
                    val dir = sector - nextSector
                    val dirIndex = FOURWAY_DIRECTIONS.indexOf(dir)
                    val signatureGrid = signatureDirections[signature]?.get(dirIndex)
                    if (signatureGrid != null) {
                        val currentGrid = sparseGrid[nextSector]
                        val activeTiles = mutableSetOf<Tile.GardenTile>()
                        val gridClone = signatureGrid.copy(sector = nextSector, grid = Grid(signatureGrid.grid.mapRows {
                            it.map { tile ->
                                when (tile) {
                                    is Tile.GardenTile -> tile.copy().also {
                                        if (tile.pos.x == 0 || tile.pos.y == 0 || tile.pos.x == signatureGrid.grid.width-1 || tile.pos.y == signatureGrid.grid.height - 1) {
                                            activeTiles.add(it)
                                        }
                                    }
                                    is Tile.RockTile -> tile
                                }
                            }
                        }, boundaryCondition = GridBoundaryCondition.WRAP))
                        gridClone.activeTiles = activeTiles
                        gridClone.done = false
                        if (currentGrid != null) {
                            gridClone.shiftSteps(currentGrid.offset)
                        } else {
                            gridClone.shiftSteps(targetStep)
                        }
                        sparseGrid[nextSector] = gridClone
                    }
                } else if (nextPos != null) {
                    val grid = sparseGrid[nextSector] ?: newGrid(nextSector)
                    val cell = grid.grid[nextPos] as Tile.GardenTile
                    if (cell.step == -1L || cell.step > targetStep) {
                        cell.step = targetStep
                        grid.activeTiles = grid.activeTiles.plus(cell)
                    }
                    sparseGrid[nextSector] = grid
                }

            }
        }

        println("3")

        doneSectors.forEach { (sector, grid) ->
            if (grid.signature !in signatures) {
                signatures.add(grid.signature)
                val neighbors = FOURWAY_DIRECTIONS.map {
                        dir -> sector + dir
                }.map {
                    sparseGrid[it]
                }.map {
                    it?.shiftSteps(-it.offset)
                    it
                }
                signatureDirections[grid.signature] = neighbors
            }
            signatureMap[sector] = grid.signature
        }

        println("4")

        doneSectors.forEach { (sector, grid) ->
            evenCounter += grid.getEvenSteps(steps.toLong())
            oddCounter += grid.getOddSteps(steps.toLong())
            sparseGrid.cells.remove(sector)
            doneSectorRecords.add(sector)
        }

        println(maxStep)

    }

    println("Unique signatures: ${signatures.size}")
    val minX = signatureMap.minOfOrNull { it.key.x } ?: 0
    val maxX = signatureMap.maxOfOrNull { it.key.x } ?: 0
    val minY = signatureMap.minOfOrNull { it.key.y } ?: 0
    val maxY = signatureMap.maxOfOrNull { it.key.y } ?: 0
    val signatureList = signatures.toList()
    (minY..maxY).forEach { y ->
        (minX..maxX).forEach { x ->
            val key = Vector2(x, y)
            if (signatureMap[key]!= null) {
                val index = signatureList.indexOf(signatureMap[key]!!)
                print(index.toString().padStart(2, ' '))
            } else {
                print("  ")
            }
        }
        println()
    }
//    signatures.forEach { signature ->
//        val directions = signatureDirections[signature]
//        println("Signature: $signature")
//        directions?.forEach {
//            if (it!= null) {
////                println(it.offset)
////                println(it.maxStep)
//                println(it.toString())
//            } else {
//                println("null")
//            }
//        }
//    }

//    println(sparseGrid)

    val positions = sparseGrid.cells.entries.sumOf { (sector, grid) ->
        if (steps % 2 == 0) grid.getEvenSteps(steps.toLong()) else grid.getOddSteps(steps.toLong())
    } + if (steps % 2 == 0) evenCounter else oddCounter

    return positions
}

fun main() = AoCTask("day21").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput, 6), 16)
//    check(part2(testInput, 6), 16)
//    check(part2(testInput, 10), 50)
//    check(part2(testInput, 100), 6536)
//    check(part2(testInput, 500), 167004)
//    check(part2(testInput, 1000), 668697)
//    check(part2(testInput, 5000), 16733044)

    println(part1(input, 64))
    println(part2(input, 26501365))
//    println(part3(input, 1000))
}
