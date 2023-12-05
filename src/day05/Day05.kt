package day05

import AoCTask
import split

// https://adventofcode.com/2023/day/5

data class Range(val from: Long, val to: Long, val size: Long) {

    val offset = to - from
    val domain: LongRange = from..< from + size

    operator fun contains(value: Long): Boolean {
        return value in domain
    }

    fun map(value: Long): Long {
        return value + offset
    }

    fun partition(value: MultiRange): Pair<MultiRange, MultiRange> {
        val mappableSubRanges = mutableListOf<LongRange>()
        val unmappableSubRanges = mutableListOf<LongRange>()
        value.subRanges.forEach { subRange ->
            if (subRange.first in domain && subRange.last in domain) {
                mappableSubRanges.add(subRange)
            } else if (subRange.first in domain && subRange.last !in domain) {
                mappableSubRanges.add(subRange.first.. domain.last)
                unmappableSubRanges.add(domain.last+1.. subRange.last)
            } else if (subRange.first !in domain && subRange.last in domain) {
                mappableSubRanges.add(domain.first.. subRange.last)
                unmappableSubRanges.add(subRange.first..< domain.first)
            } else {
                unmappableSubRanges.add(subRange)
            }
        }
        return MultiRange(mappableSubRanges) to MultiRange(unmappableSubRanges)
    }

    fun map(value: MultiRange): MultiRange {
        val mappedRanges = value.subRanges.map {
            map(it.first).. map(it.last)
        }
        return MultiRange(mappedRanges)
    }
}

data class RangeMap(val name: String, val ranges: List<Range>) {
    fun map(value: Long): Long {
        return ranges.firstOrNull { value in it }?.let { return it.map(value) } ?: value
    }

    fun map(value: MultiRange): MultiRange {
        var unmappedValue = value
        val mappedValue = ranges.flatMap { range ->
            val (mappable, unmappable) = range.partition(unmappedValue)
            unmappedValue = unmappable
            range.map(mappable).subRanges
        }
        return MultiRange(mappedValue + unmappedValue.subRanges)
    }
}

data class MultiRange(val subRanges: List<LongRange>)

fun parseInput(input: List<String>): Pair<List<Long>, List<RangeMap>> {
    val sections = input.split {
        it.isBlank()
    }
    val seeds  = sections.first().first().split(": ").last().split(" ").map { it.toLong() }
    val rangeMaps = parseRangeMaps(sections)
    return seeds to rangeMaps
}

fun parseRangeMaps(sections: List<List<String>>): List<RangeMap> {
    val rangeMaps = sections.drop(1).map { section ->
        val name = section.first().removeSuffix(":")
        val ranges = section.drop(1).map { range ->
            val (destination, source, size) = range.trim().split(" ").map { it.toLong() }
            Range(source, destination, size)
        }
        RangeMap(name, ranges)
    }
    return rangeMaps
}

fun parseInput2(input: List<String>): Pair<List<MultiRange>, List<RangeMap>> {
    val sections = input.split {
        it.isBlank()
    }
    val seedRanges = sections.first().first().split(": ").last().split(" ").map { it.toLong() }.windowed(2, 2).map {
        val seedRange = it.first()..< it.first() + it.last()
        MultiRange(listOf(seedRange))
    }
    val rangeMaps = parseRangeMaps(sections)
    return seedRanges to rangeMaps
}

fun part1(input: List<String>): Long {
    val (seeds, rangeMaps) = parseInput(input)
    val locations = rangeMaps.fold(seeds) { current, rangeMap ->
        current.map { rangeMap.map(it) }
    }
    return locations.min()
}

fun part2(input: List<String>): Long {
    val (seedRanges, rangeMaps) = parseInput2(input)
    val locationRanges = rangeMaps.fold(seedRanges) { current, rangeMap ->
        current.map { rangeMap.map(it) }
    }
    val minLocation = locationRanges.minOf { range ->
        range.subRanges.minOf { it.first }
    }
    return minLocation
}

fun main() = AoCTask("day05").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 35)
    check(part2(testInput), 46)

    println(part1(input))
    println(part2(input))
}
