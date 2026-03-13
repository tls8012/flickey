package com.example.flickey

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class Trie {
    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var isWord = false
    }

    private val root = TrieNode()
    // Words in trie_dict.txt are already ordered by frequency.
    // Store original forms ordered by their frequency to return sorted suggestions.
    private val orderedWords = mutableListOf<String>()

    fun load(context: Context, filename: String = "trie_dict.txt") {
        try {
            context.assets.open(filename).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { word ->
                        if (word.isNotBlank()) {
                            insert(word)
                            orderedWords.add(word)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun insert(word: String) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isWord = true
    }

    fun getPrefixMatches(prefix: String, limit: Int = 3): List<String> {
        val lowerPrefix = prefix.lowercase()
        var node = root
        for (char in lowerPrefix) {
            node = node.children[char] ?: return emptyList()
        }

        // Fast path: find matching words from orderedWords list directly to preserve frequency order
        // This relies on orderedWords being loaded in frequency order.
        return orderedWords.filter { it.startsWith(lowerPrefix) }.take(limit)
    }
}
