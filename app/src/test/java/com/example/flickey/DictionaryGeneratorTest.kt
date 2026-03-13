package com.example.flickey

import org.junit.Test
import java.io.File

class DictionaryGeneratorTest {

    private fun getDeletes(word: String, maxDist: Int): Set<String> {
        val deletes = mutableSetOf<String>()
        val queue = mutableListOf(word)
        deletes.add(word)

        var currentDist = 0
        while (currentDist < maxDist) {
            val nextQueue = mutableListOf<String>()
            for (w in queue) {
                for (i in w.indices) {
                    val del = w.substring(0, i) + w.substring(i + 1)
                    if (deletes.add(del)) {
                        nextQueue.add(del)
                    }
                }
            }
            queue.clear()
            queue.addAll(nextQueue)
            currentDist++
        }
        return deletes
    }

    @Test
    fun generateDictionaries() {
        // Run from the app/ directory, so "src/main/assets/..." works.
        // If not, try navigating from project root.
        var baseDir = File("src/main/assets")
        if (!baseDir.exists()) {
            baseDir = File("app/src/main/assets")
        }
        
        val inFile = File(baseDir, "english_words_50k_wordfreq.txt")
        val symOutFile = File(baseDir, "symspell_dict.txt")
        val trieOutFile = File(baseDir, "trie_dict.txt")

        if (!inFile.exists()) {
            throw IllegalStateException("Input file not found at ${inFile.absolutePath}")
        }

        println("Generating dictionaries from ${inFile.absolutePath}...")

        val words = mutableListOf<String>()
        inFile.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(" ")
                if (parts.isNotEmpty()) {
                    val word = parts[0].trim().toLowerCase()
                    var valid = word.isNotBlank()
                    for (i in 0 until word.length) {
                        if (!word[i].isLetter()) {
                            valid = false
                            break
                        }
                    }
                    if (valid) {
                        words.add(word)
                    }
                }
            }
        }

        println("Total words loaded: ${words.size}")
        trieOutFile.bufferedWriter().use { writer ->
            words.forEach { 
                writer.write(it)
                writer.newLine()
            }
        }
        println("Trie data generated at ${trieOutFile.absolutePath}")

        val topWords = words.take(20000)
        val symSpellMap = mutableMapOf<String, MutableSet<String>>()

        println("Generating SymSpell data for top ${topWords.size} words...")

        topWords.forEach { word ->
            val deletes = getDeletes(word, 2)
            deletes.forEach { del ->
                symSpellMap.getOrPut(del) { mutableSetOf() }.add(word)
            }
        }

        symOutFile.bufferedWriter().use { writer ->
            symSpellMap.forEach { (del, origWords) ->
                val sortedOrig = origWords.toList().sortedBy { topWords.indexOf(it) }
                writer.write("$del:${sortedOrig.joinToString(",")}")
                writer.newLine()
            }
        }
        println("SymSpell data generated at ${symOutFile.absolutePath} with ${symSpellMap.size} entries.")
    }
}
