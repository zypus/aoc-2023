package day15

import AoCTask

// https://adventofcode.com/2023/day/15

data class Label(val label: String) {
    override fun hashCode(): Int {
        return computeHash(label)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Label

        return label == other.label
    }
}

data class Lens(val label: Label, val focalLength: Int)

sealed class Instruction {
    data class AddLens(val lens: Lens) : Instruction()
    data class RemoveLens(val label: Label) : Instruction()
}

fun computeHash(text: String): Int = text.fold(0) { acc, c ->
    ((acc + c.code) * 17) % 256
}

fun part1(input: List<String>): Int {
    return input.first().split(",").sumOf { computeHash(it) }
}

fun part2(input: List<String>): Int {
    val instructions = input.first().split(",").map {
        if ("=" in it) {
            val (label, focalLength) = it.split("=")
            Instruction.AddLens(Lens(Label(label), focalLength.toInt()))
        } else {
            val label = it.trimEnd('-')
            Instruction.RemoveLens(Label(label))
        }
    }
    val boxes: MutableMap<Int, MutableList<Lens>> = mutableMapOf()
    instructions.forEach { instruction ->
        when (instruction) {
            is Instruction.AddLens -> {
                val box = boxes.getOrPut(instruction.lens.label.hashCode()) { mutableListOf() }
                val index = box.indexOfFirst { it.label == instruction.lens.label }
                if (index != -1) {
                    box[index] = instruction.lens
                } else {
                    box.add(instruction.lens)
                }
            }
            is Instruction.RemoveLens -> boxes[instruction.label.hashCode()]?.removeIf { it.label == instruction.label }
        }
    }
    val totalFocusingPower = boxes.entries.sumOf { (key, lenses) ->
        val boxNumber = key + 1
        lenses.mapIndexed { index, lens ->
            boxNumber * (index+1) * lens.focalLength
        }.sum()
    }
    return totalFocusingPower
}

fun main() = AoCTask("day15").run {

    check(computeHash("HASH"), 52)
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 1320)
    check(part2(testInput), 145)

    println(part1(input))
    println(part2(input))
}
