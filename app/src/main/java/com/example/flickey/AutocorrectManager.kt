package com.example.flickey

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutocorrectManager(private val context: Context) {

    private val trie = Trie()
    private val symSpell = SymSpell()
    var isLoaded = false
        private set

    init {
        CoroutineScope(Dispatchers.IO).launch {
            trie.load(context)
            symSpell.load(context)
            withContext(Dispatchers.Main) {
                isLoaded = true
            }
        }
    }

    fun getSuggestions(word: String): List<String> {
        if (!isLoaded || word.isBlank()) {
            return emptyList()
        }

        val suggestions = mutableListOf<String>()

        // 1. Try prefix matches first (autocomplete)
        val prefixMatches = trie.getPrefixMatches(word, 3)
        suggestions.addAll(prefixMatches)

        // 2. If we need more suggestions, try spell check
        if (suggestions.size < 3) {
            val spellMatches = symSpell.lookup(word, 3 - suggestions.size)
            for (match in spellMatches) {
                if (!suggestions.contains(match)) {
                    suggestions.add(match)
                }
            }
        }

        return suggestions
    }
}
