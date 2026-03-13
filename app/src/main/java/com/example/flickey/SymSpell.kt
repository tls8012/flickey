package com.example.flickey

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class SymSpell {

    // deleted string -> List of original words (already joined by comma, sorted by frequency in dict)
    private val dictionary = mutableMapOf<String, List<String>>()

    fun load(context: Context, filename: String = "symspell_dict.txt") {
        try {
            context.assets.open(filename).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(":")
                        if (parts.size == 2) {
                            val deleted = parts[0]
                            val originals = parts[1].split(",")
                            dictionary[deleted] = originals
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    fun lookup(word: String, limit: Int = 3): List<String> {
        val lowerWord = word.lowercase()
        val deletes = getDeletes(lowerWord, 2)
        
        val suggestions = mutableMapOf<String, Int>()

        for (del in deletes) {
            val matches = dictionary[del]
            if (matches != null) {
                // Determine approximate frequency by index (0 is highest frequency)
                matches.forEachIndexed { index, match ->
                    if (!suggestions.containsKey(match)) {
                        suggestions[match] = index
                    }
                }
            }
        }
        
        // Return words sorted by frequency (lower index is better)
        return suggestions.entries
            .sortedBy { it.value }
            .map { it.key }
            .take(limit)
    }
}
