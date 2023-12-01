#!/bin/bash

//usr/bin/env echo '
/**** BOOTSTRAP kscript ****\'>/dev/null
command -v kscript >/dev/null 2>&1 || curl -L "https://git.io/fpF1K" | bash 1>&2
exec kscript $0 "$@"
\*** IMPORTANT: Any code including imports and annotations must come after this line ***/

import java.io.File
import kotlin.system.exitProcess
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

fun getPageContent(url: URL): String {
    return url.openStream().use {
        it.bufferedReader().readText()
    }
}

fun downloadInputFile(url: URL, file: File, session: String) {
    val connection = url.openConnection()
    connection.setRequestProperty("Cookie", "session=$session")
    val content = connection.getInputStream().use {
        it.bufferedReader().readText()
    }
    file.writeText(content.trim())
}

if (args.isEmpty()) {
    println("Please, provide the day number you want to create:")
}
var day: String = args.getOrElse(0) { readlnOrNull() ?: "" }
while (day.toIntOrNull() !in 1..25) {
    println("Please provide a valid number (1..25):")
    day = readlnOrNull() ?: ""
}

val sessionFile = File(".session")

val paddedDay = day.padStart(2, '0')

val dir = File("src/day$paddedDay")

if (dir.exists()) {
    println("Day $day is already set up.")
} else {
    println("- Copying template files")
    val templateDir = File("src/day00")
    templateDir.copyRecursively(dir)
    val url = "https://adventofcode.com/2023/day/${day}"
    val targetFile = File(templateDir.absolutePath.replace("00", paddedDay))

    dir.listFiles().forEach { file ->
        val newContent =
            file.readText().replace("00", paddedDay).replace("<link>", url)
        val newName = file.name.replace("00", paddedDay)
        val newFile = file.resolveSibling(newName)
        newFile.writeText(newContent)
        file.delete()
    }
    if (sessionFile.exists()) {
        println("- Downloading input for day $day")
        val session = sessionFile.readText()
        try {
            downloadInputFile(URL("$url/input"),  targetFile.resolve("Day$paddedDay.txt"), session)
        } catch (e: Exception) {
            println("Failed autoloading input file: $e")
        }
    } else {
        println("No .session file with session info found, skipping autodownload for input file")
    }

    try {
        println("- Grabbing test input for day $day")
        val html = getPageContent(URL(url))
        val parts = html.split("(<pre><code>)|(</code></pre>)".toRegex())
        val match = parts.getOrNull(1)
        if (match != null) {
            val testInput = match.replace("<.*?>".toRegex(), "").trimEnd('\n')
            targetFile
                .resolve("Day${paddedDay}_test.txt")
                .writeText(testInput)
        } else {
            println("Failed grabbing test input: Couldn't find <pre></pre> tags")
            println("Please grab it manually")
        }
    } catch (e: Exception) {
        println("Failed grabbing test input: $e")
        println("Please grab it manually")
    }

    println("DONE preparing day $day for you.")
    println("You find the days instructions here: $url")
    println("Happy Kotlin!")
}