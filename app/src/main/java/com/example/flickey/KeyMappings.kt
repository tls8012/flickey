package com.example.flickey

enum class Layer {
    KR, EN, SP
}

data class KnobConfig(
    val centerLabel: String,
    val directions: List<String?> // 8 directions, starting from 1 (South-West) or standard? 
    // User said: 8 is up, 4 is left, 6 is right, 2 is down. NumPad style.
    // 7 8 9
    // 4   6
    // 1 2 3
    // My FlickKnob uses angle. 
    // 0=Right(6), 1=DR(3), 2=Down(2), 3=DL(1), 4=Left(4), 5=UL(7), 6=Up(8), 7=UR(9)
    // Let's standardize on the internal 0-7 index and map the user's NumPad to it.
    // 0: Right (6)
    // 1: Down-Right (3)
    // 2: Down (2)
    // 3: Down-Left (1)
    // 4: Left (4)
    // 5: Up-Left (7)
    // 6: Up (8)
    // 7: Up-Right (9)
)

object KeyMappings {
    // Helper to create list of 8 items in CCW order starting from Right (0 deg)
    // 0: Right
    // 1: Up-Right
    // 2: Up
    // 3: Up-Left
    // 4: Left
    // 5: Down-Left
    // 6: Down
    // 7: Down-Right
    private fun list8(
        r: String?, ur: String?, u: String?, ul: String?,
        l: String?, dl: String?, d: String?, dr: String?
    ): List<String?> = listOf(r, ur, u, ul, l, dl, d, dr)

    // Punctuation
    // 7=!, 8=Shift, 9=?, 4=Backspace, 6=Space, 1=,, 2=Enter, 3=.
    // Map to CCW:
    // 0(R) = Space (6)
    // 1(UR) = ? (9)
    // 2(U) = Shift (8)
    // 3(UL) = ! (7)
    // 4(L) = Backspace (4)
    // 5(DL) = , (1)
    // 6(D) = Enter (2)
    // 7(DR) = . (3)
    val PUNCTUATION = KnobConfig(
        centerLabel = ".,?!",
        directions = list8(
            "SPACE", "?", "SHIFT", "!", "BACKSPACE", ",", "ENTER", "."
        )
    )

    // Korean Consonants Page 1
    // User didn't specify mapping, so I'll distribute them logically or clockwise.
    // Let's just fill them.
    val KR_C_1 = KnobConfig(
        centerLabel = "자음1",
        directions = list8("ㄹ", "ㄱ", "ㄴ", "ㄷ", "ㅇ", "ㅅ", "ㅂ", "ㅁ") 
    )
    // Korean Consonants Page 2
    val KR_C_2 = KnobConfig(
        centerLabel = "자음2",
        directions = list8("ㅋ", "ㄲ", "ㅈ", "ㅊ", "ㅎ", "ㅍ", "ㅌ", "ㄸ")
    )

    // Korean Vowels Page 1
    val KR_V_1 = KnobConfig(
        centerLabel = "모음1",
        directions = list8("ㅓ", "ㅏ", "ㅑ", "ㅕ", "ㅣ", "ㅡ", "ㅜ", "ㅗ")
    )
    // Korean Vowels Page 2
    val KR_V_2 = KnobConfig(
        centerLabel = "모음2",
        directions = list8("ㅠ", "ㅘ", "ㅙ", "ㅚ", "ㅝ", "ㅞ", "ㅟ", "ㅢ") // Just filling
    )

    // English Consonants Page 1
    val EN_C_1 = KnobConfig(
        centerLabel = "ABC",
        directions = list8("c", "b", "a", "h", "g", "f", "e", "d")
    )
    // English Consonants Page 2
    val EN_C_2 = KnobConfig(
        centerLabel = "IJK",
        directions = list8("k", "j", "i", "q", "p", "n", "m", "l")
    )

    // English Vowels
    val EN_V_1 = KnobConfig(
        centerLabel = "Vow1",
        directions = list8("o", "i", "e", "a", "z", "y", "x", "u")
    )
    val EN_V_2 = KnobConfig(
        centerLabel = "Vow2",
        directions = list8("r", "p", "o", "n", "v", "t", "s", "w")
    )
    
    // Special/Numbers
    val SP_C_1 = KnobConfig(
        centerLabel = "123",
        directions = list8("6", "9", "8", "7", "4", "1", "2", "3") // Numpad style mapping
    )
    val SP_C_2 = KnobConfig(
        centerLabel = "#$%",
        directions = list8("^", "*", "&", "%", "$", "!", "@", "#")
    )
    val SP_V_1 = KnobConfig(
        centerLabel = "+-=",
        directions = list8(">", "]", "}", ")", "(", "{", "[", "<")
    )
    val SP_V_2 = KnobConfig(
        centerLabel = "(){}",
        directions = list8("=", "+", "-", "_", "|", "\\", "/", "*")
    )
}

class KeyboardState {
    var currentLayer: Layer = Layer.KR
    var consonantPage: Int = 1
    var vowelPage: Int = 1
    
    // Listeners
    var onStateChanged: (() -> Unit)? = null

    fun toggleLayer() {
        currentLayer = when (currentLayer) {
            Layer.KR -> Layer.EN
            Layer.EN -> Layer.KR // Toggle between KR/EN for the main button
            else -> Layer.KR
        }
        resetPages()
        onStateChanged?.invoke()
    }

    fun setSpecialLayer() {
        if (currentLayer != Layer.SP) {
            currentLayer = Layer.SP
        } else {
            currentLayer = Layer.KR // Toggle back
        }
        resetPages()
        onStateChanged?.invoke()
    }

    fun toggleConsonantPage() {
        consonantPage = if (consonantPage == 1) 2 else 1
        onStateChanged?.invoke()
    }

    fun toggleVowelPage() {
        vowelPage = if (vowelPage == 1) 2 else 1
        onStateChanged?.invoke()
    }

    private fun resetPages() {
        consonantPage = 1
        vowelPage = 1
    }

    fun getConsonantConfig(): KnobConfig {
        return when (currentLayer) {
            Layer.KR -> if (consonantPage == 1) KeyMappings.KR_C_1 else KeyMappings.KR_C_2
            Layer.EN -> if (consonantPage == 1) KeyMappings.EN_C_1 else KeyMappings.EN_C_2
            Layer.SP -> if (consonantPage == 1) KeyMappings.SP_C_1 else KeyMappings.SP_C_2
        }
    }

    fun getVowelConfig(): KnobConfig {
        return when (currentLayer) {
            Layer.KR -> if (vowelPage == 1) KeyMappings.KR_V_1 else KeyMappings.KR_V_2
            Layer.EN -> if (vowelPage == 1) KeyMappings.EN_V_1 else KeyMappings.EN_V_2
            Layer.SP -> if (vowelPage == 1) KeyMappings.SP_V_1 else KeyMappings.SP_V_2
        }
    }
}
