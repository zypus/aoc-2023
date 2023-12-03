package day03

import AoCTask

// https://adventofcode.com/2023/day/3

data class Part(
    val id: Int,
    val number: Int,
    val row: Int,
    val range: IntRange
)

data class Symbol(
    val symbol: String,
    val row: Int,
    val column: Int
)

data class InputData(val parts: List<Part>, val symbols: List<Symbol>)

val partRegex = Regex("""(\d+)""")
val symbolRegex = Regex("""([^\d.])""")

fun parseInput(input: List<String>): InputData {
    var id = 0
    val parts = mutableListOf<Part>()
    val symbols = mutableListOf<Symbol>()
    input.forEachIndexed { index, line ->
        parts += partRegex.findAll(line).map { matchResult ->
            Part(id++, matchResult.value.toInt(), index, matchResult.range)
        }.toList()
        symbols += symbolRegex.findAll(line).map { matchResult ->
            Symbol(matchResult.value, index, matchResult.range.first)
        }.toList()
    }
    return InputData(parts, symbols)
}

fun partsNextToSymbol(input: List<String>, symbol: Symbol, partsByRow: Map<Int, List<Part>>): List<Part> {
    val start = maxOf(symbol.row - 1, 0)
    val end = minOf(symbol.row + 1, input.size - 1)
    val parts = (start..end).flatMap { partsByRow.getOrDefault(it, emptyList()) }
    val symbolRange = maxOf(symbol.column - 1, 0)..minOf(symbol.column + 1, input.first().length-1)
    return parts.filter { part -> symbolRange.intersect(part.range).isNotEmpty() }
}

fun part1(input: List<String>): Int {
    val inputData = parseInput(input)
    val partsByRow = inputData.parts.groupBy { it.row }
    val partsNextToSymbols = inputData.symbols.flatMap { sym ->
        partsNextToSymbol(input, sym, partsByRow)
    }.distinctBy { it.id }
    return partsNextToSymbols.sumOf {
        it.number
    }
}

fun part2(input: List<String>): Int {
    val inputData = parseInput(input)
    val partsByRow = inputData.parts.groupBy { it.row }
    val gearRatios = inputData.symbols.filter {
        it.symbol == "*"
    }.map {
        partsNextToSymbol(input, it, partsByRow)
    }.filter {
        it.size == 2
    }.map {
        it[0].number * it[1].number
    }
    return gearRatios.sum()
}

fun main() = AoCTask("day03").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 4361)
    check(part2(testInput), 467835)

    println(part1(input))
    println(part2(input))
}
