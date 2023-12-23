package day20

import AoCTask
import primeFactors

// https://adventofcode.com/2023/day/20

enum class PulseType {
    LOW, HIGH
}

data class Pulse(val from: String, val to: String, val type: PulseType) {
    override fun toString(): String {
        return "$from -${type.name.lowercase()}-> $to"
    }
}

sealed class Module {

    abstract val name: String

    abstract val targets: List<String>

    abstract fun handlePulse(pulse: Pulse): List<Pulse>

    abstract val stateRepresentation: String

    data object Button : Module() {
        override val name: String = "button"
        override val targets: List<String> = listOf("broadcaster")

        override fun handlePulse(pulse: Pulse): List<Pulse> {
            return listOf()
        }

        fun trigger(): Pulse {
            return Pulse(name, "broadcaster", PulseType.LOW)
        }

        override val stateRepresentation: String = "B"
    }

    data class Broadcaster(override val targets: List<String>): Module() {
        override val name: String = "broadcaster"

        override fun handlePulse(pulse: Pulse): List<Pulse> {
            return targets.map { target ->
                Pulse(name, target, pulse.type)
            }
        }

        override val stateRepresentation: String = "C"
    }

    data class FlipFlop(override val name: String, override val targets: List<String>): Module() {
        var state = false

        override val stateRepresentation: String get() = if (state) "1" else "0"

        override fun handlePulse(pulse: Pulse): List<Pulse> {
            return when(pulse.type) {
                PulseType.HIGH -> emptyList()
                PulseType.LOW -> {
                    state = !state
                    targets.map {
                        Pulse(name, it, if (state) PulseType.HIGH else PulseType.LOW)
                    }
                }
            }
        }
    }

    data class Conjunction(override val name: String, override val targets: List<String>): Module() {

        fun initMemory(inputs: List<String>) {
            inputs.forEach { input ->
                memory[input] = PulseType.LOW
            }
        }

        val memory: MutableMap<String, PulseType> = mutableMapOf()

        override val stateRepresentation: String
            get() = "{${memory.values.joinToString(",") { it.name.uppercase().first().toString() }  }"

        override fun handlePulse(pulse: Pulse): List<Pulse> {
            memory[pulse.from] = pulse.type
            val outPulseType = if (memory.values.all { it == PulseType.HIGH }) {
                PulseType.LOW
            } else {
                PulseType.HIGH
            }
            return targets.map {
                Pulse(name, it, outPulseType)
            }
        }

        override fun toString(): String {
            return "Conjunction(name=$name, inputs=${memory.keys}, targets=$targets)"
        }
    }
}

fun parseInput(input: List<String>): Map<String, Module> {
    val modules = input.map { line ->
        val (source, targetList) = line.split(" -> ")
        val targets = targetList.split(",").map { it.trim() }
        val module = when {
            source == "broadcaster" -> Module.Broadcaster(targets)
            source.startsWith("%") -> Module.FlipFlop(source.drop(1), targets)
            source.startsWith("&") -> Module.Conjunction(source.drop(1), targets)
            else -> throw IllegalArgumentException("Unknown module: $source")
        }
        module
    }
    val conjunctions = modules.filterIsInstance<Module.Conjunction>()
    conjunctions.forEach { con ->
        val inputs = modules.filter { con.name in it.targets }.map { it.name }
        con.initMemory(inputs)
    }
    return modules.associateBy { it.name }
}

fun createMermaidDiagram(modules: Map<String, Module>): String {
    val builder = StringBuilder()
    with(builder) {
        appendLine("stateDiagram-v2")
        appendLine("classDef conjunction fill:#0a0")
        appendLine("\t[*] --> broadcaster")
        modules.values.forEach { module ->
            module.targets.forEach { target ->
                val source = if (module is Module.Conjunction) {
                    "${module.name}:::conjunction"
                } else {
                    module.name
                }
                if (target == "rx") {
                    appendLine("\t$source --> [*]")
                } else {
                    appendLine("\t$source--> $target")
                }
            }
        }
    }
    return builder.toString()
}

fun getAncestorsOf(module: Module, modules: Map<String, Module>): List<Module> {
    val ancestors = mutableListOf<Module>()
    val open = mutableListOf(module)
    while (open.isNotEmpty()) {
        val current = open.removeAt(0)
        if (current != module) {
            ancestors.add(current)
        }
        val inputs = modules.values.filter {
            it.targets.contains(current.name)
        }
        val newAncestors = inputs.filter {
            it !in ancestors
        }
        open.addAll(newAncestors)
    }
    return ancestors
}

fun simulate(modules: Map<String, Module>) {
    val pendingPulses = mutableListOf<Pulse>()
    pendingPulses.add(Module.Button.trigger())
    while (pendingPulses.isNotEmpty()) {
        val pulse = pendingPulses.removeAt(0)
        val targetModule = modules[pulse.to]
        val pulses = targetModule?.handlePulse(pulse) ?: emptyList()
        pendingPulses.addAll(pulses)
    }
}

data class ConjunctionRecorder(val conjunction: Module.Conjunction, val ancestors: List<Module>) {

    val states: MutableList<String> = mutableListOf()
    val highPulses: MutableList<Int> = mutableListOf()

    var cursor = -1
    var cycleStart = -1
    var cycleLength = -1

    fun recordState() {
        if (cycleStart == -1) {
            val state = ancestors.joinToString(separator = "") { it.stateRepresentation }
            if (state!in states) {
                states.add(state)
                cursor++
                if (!conjunction.memory.all { it.value == PulseType.HIGH }) {
                    highPulses.add(cursor)
                }
            } else {
                cycleStart = states.indexOf(state)
                cycleLength = states.size - cycleStart
                cursor = cycleStart
            }
        } else {
            cursor++
            if (cursor >= states.size) {
                cursor = cycleStart
            }
        }
    }

    fun cycleString(): String {
        return "$cursor ($cycleStart..${states.size}) [$cycleLength] highs(${highPulses.joinToString(",")})"
    }
}

fun part1(input: List<String>): Long {
    val modules = parseInput(input)
    val pendingPulses = mutableListOf<Pulse>()
    var lowPulses = 0L
    var highPulses = 0L
    repeat(1000) {
        pendingPulses.add(Module.Button.trigger())
        while (pendingPulses.isNotEmpty()) {
            val pulse = pendingPulses.removeAt(0)
            when(pulse.type) {
                PulseType.LOW -> lowPulses++
                PulseType.HIGH -> highPulses++
            }
            println(pulse)
            val targetModule = modules[pulse.to]
            val pulses = targetModule?.handlePulse(pulse) ?: emptyList()
            pendingPulses.addAll(pulses)
        }
    }
    return lowPulses * highPulses
}

fun part2(input: List<String>): Long {
    val modules = parseInput(input)
    val conjunctions = modules.values.filterIsInstance<Module.Conjunction>()
    val recorders = conjunctions.map { con ->
        ConjunctionRecorder(con, getAncestorsOf(con, modules))
    }
    recorders.forEach { it.recordState() }
    val importantRecords = recorders.filter { it.conjunction.name in listOf("vd", "ns", "bh", "dl") }
    var buttonPresses = 0L

    while (importantRecords.any { it.cycleStart == -1 }) {
        buttonPresses++
        simulate(modules)
        recorders.forEach { it.recordState() }
    }
    importantRecords.forEach { println(it.cycleString()) }
    importantRecords.forEach { println(primeFactors(it.cycleLength.toLong())) }

    var combinedCycleLength = 0L
    var currentPrimeFactors = emptySet<Long>()

    var totalStepsTaken = 0L

    importantRecords.forEach {recorder ->
        val target = recorder.highPulses.first()
        val stepsNeededForAlignment = if (recorder.cursor < target) {
            target - recorder.cursor
        } else {
            recorder.states.size - recorder.cursor + target - recorder.cycleStart
        }
        val stepsNeeded = if (combinedCycleLength > 0) {
            var cycleShift = combinedCycleLength % recorder.cycleLength
            if (cycleShift > recorder.cycleLength / 2) {
                cycleShift -= recorder.cycleLength
            }
            var cyclesNeeded = 0
            println("$cycleShift $stepsNeededForAlignment")
            while (cyclesNeeded * combinedCycleLength % recorder.cycleLength != stepsNeededForAlignment.toLong()) {
                cyclesNeeded++
            }
            println("Needs $cyclesNeeded cycles")
            val stepsToTake = cyclesNeeded * combinedCycleLength
            println("$stepsToTake $stepsNeededForAlignment ${stepsToTake % recorder.cycleLength}")
            val newPrimesFactors = primeFactors(recorder.cycleLength.toLong())
            currentPrimeFactors = currentPrimeFactors + newPrimesFactors
            combinedCycleLength = currentPrimeFactors.reduce { acc, factor -> acc * factor }
            stepsToTake
        } else {
            currentPrimeFactors = primeFactors(recorder.cycleLength.toLong()).toSet()
            combinedCycleLength = recorder.cycleLength.toLong()
            stepsNeededForAlignment.toLong()
        }
        totalStepsTaken += stepsNeeded
        importantRecords.forEach { rec ->
            rec.cursor = ((rec.cursor - rec.cycleStart + stepsNeeded) % rec.cycleLength + rec.cycleStart).toInt()
        }
        println("After alignment ($combinedCycleLength):")
    }
    check(importantRecords.all { it.cursor == it.highPulses.first() })
    return buttonPresses + totalStepsTaken
}

fun main() = AoCTask("day20").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 32000000)
    check(part1(readTestInput(2)), 11687500)
    //check(part2(testInput), 1)

    println(part1(input))
    println(part2(input))
}
