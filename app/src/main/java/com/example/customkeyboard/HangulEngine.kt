package com.example.customkeyboard

class HangulEngine {
    var choseong: Int = -1
    var jungseong: Int = -1
    var jongseong: Int = -1

    companion object {
        val CHOSEONG_JAMO = listOf(
            0x3131, 0x3132, 0x3134, 0x3137, 0x3138, 0x3139, 0x3141, 0x3142, 0x3143,
            0x3144, 0x3145, 0x3146, 0x3147, 0x3148, 0x3149, 0x314A, 0x314B, 0x314C, 0x314D
        ) // 19 choseongs

        val JUNGSEONG_JAMO = (0x314E..0x3162).toList() // 21 jungseongs

        val JONGSEONG_JAMO = listOf(
            0, 0x3131, 0x3132, 0x3133, 0x3134, 0x3135, 0x3136, 0x3137, 0x3139, 0x313A,
            0x313B, 0x313C, 0x313D, 0x313E, 0x313F, 0x3140, 0x3141, 0x3142, 0x3143, 0x3144,
            0x3145, 0x3146, 0x3147, 0x3149, 0x314A, 0x314B, 0x314C, 0x314D
        ) // 27 jongseongs (index 0 is none)

        fun combineJungseong(first: Int, second: Int): Int? {
            return when {
                first == 0x3156 && second == 0x314E -> 0x3157 // ㅗ + ㅏ = ㅘ
                first == 0x3156 && second == 0x314F -> 0x3158 // ㅗ + ㅐ = ㅙ
                first == 0x3156 && second == 0x3162 -> 0x3159 // ㅗ + ㅣ = ㅚ
                first == 0x315B && second == 0x3152 -> 0x315C // ㅜ + ㅓ = ㅝ
                first == 0x315B && second == 0x3153 -> 0x315D // ㅜ + ㅔ = ㅞ
                first == 0x315B && second == 0x3162 -> 0x315E // ㅜ + ㅣ = ㅟ
                first == 0x3160 && second == 0x3162 -> 0x3161 // ㅡ + ㅣ = ㅢ
                else -> null
            }
        }

        fun combineJongseong(first: Int, second: Int): Int? {
            return when {
                first == 0x3131 && second == 0x3144 -> 0x3133 // ㄱ + ㅅ = ㄳ
                first == 0x3134 && second == 0x3147 -> 0x3135 // ㄴ + ㅈ = ㄵ
                first == 0x3134 && second == 0x314D -> 0x3136 // ㄴ + ㅎ = ㄶ
                first == 0x3139 && second == 0x3131 -> 0x313A // ㄹ + ㄱ = ㄺ
                first == 0x3139 && second == 0x3141 -> 0x313B // ㄹ + ㅁ = ㄻ
                first == 0x3139 && second == 0x3142 -> 0x313C // ㄹ + ㅂ = ㄼ
                first == 0x3139 && second == 0x3144 -> 0x313D // ㄹ + ㅅ = ㄽ
                first == 0x3139 && second == 0x314B -> 0x313E // ㄹ + ㅌ = ㄾ
                first == 0x3139 && second == 0x314C -> 0x313F // ㄹ + ㅍ = ㄿ
                first == 0x3139 && second == 0x314D -> 0x3140 // ㄹ + ㅎ = ㅀ
                first == 0x3142 && second == 0x3144 -> 0x3143 // ㅂ + ㅅ = ㅄ
                else -> null
            }
        }

        fun splitJongseong(combined: Int): Pair<Int, Int>? {
            return when (combined) {
                0x3133 -> Pair(0x3131, 0x3144) // ㄳ -> ㄱ, ㅅ
                0x3135 -> Pair(0x3134, 0x3147) // ㄵ -> ㄴ, ㅈ
                0x3136 -> Pair(0x3134, 0x314D) // ㄶ -> ㄴ, ㅎ
                0x313A -> Pair(0x3139, 0x3131) // ㄺ -> ㄹ, ㄱ
                0x313B -> Pair(0x3139, 0x3141) // ㄻ -> ㄹ, ㅁ
                0x313C -> Pair(0x3139, 0x3142) // ㄼ -> ㄹ, ㅂ
                0x313D -> Pair(0x3139, 0x3144) // ㄽ -> ㄹ, ㅅ
                0x313E -> Pair(0x3139, 0x314B) // ㄾ -> ㄹ, ㅌ
                0x313F -> Pair(0x3139, 0x314C) // ㄿ -> ㄹ, ㅍ
                0x3140 -> Pair(0x3139, 0x314D) // ㅀ -> ㄹ, ㅎ
                0x3143 -> Pair(0x3142, 0x3144) // ㅄ -> ㅂ, ㅅ
                else -> null
            }
        }

        fun splitJungseong(combined: Int): Pair<Int, Int>? {
            return when (combined) {
                0x3157 -> Pair(0x3156, 0x314E) // ㅘ -> ㅗ, ㅏ
                0x3158 -> Pair(0x3156, 0x314F) // ㅙ -> ㅗ, ㅐ
                0x3159 -> Pair(0x3156, 0x3162) // ㅚ -> ㅗ, ㅣ
                0x315C -> Pair(0x315B, 0x3152) // ㅝ -> ㅜ, ㅓ
                0x315D -> Pair(0x315B, 0x3153) // ㅞ -> ㅜ, ㅔ
                0x315E -> Pair(0x315B, 0x3162) // ㅟ -> ㅜ, ㅣ
                0x3161 -> Pair(0x3160, 0x3162) // ㅢ -> ㅡ, ㅣ
                else -> null
            }
        }
    }

    fun isEmpty(): Boolean = choseong == -1 && jungseong == -1 && jongseong == -1

    fun clear() {
        choseong = -1
        jungseong = -1
        jongseong = -1
    }

    fun getComposingString(): String {
        if (isEmpty()) return ""

        if (choseong != -1 && jungseong == -1) {
            return choseong.toChar().toString()
        }

        if (choseong == -1 && jungseong != -1) {
            return jungseong.toChar().toString()
        }

        val choIdx = CHOSEONG_JAMO.indexOf(choseong)
        val jungIdx = JUNGSEONG_JAMO.indexOf(jungseong)
        val jongIdx = if (jongseong != -1) JONGSEONG_JAMO.indexOf(jongseong) else 0

        if (choIdx != -1 && jungIdx != -1) {
            val unicodeVal = 0xAC00 + (choIdx * 21 * 28) + (jungIdx * 28) + jongIdx
            return unicodeVal.toChar().toString()
        }

        val sb = StringBuilder()
        if (choseong != -1) sb.append(choseong.toChar())
        if (jungseong != -1) sb.append(jungseong.toChar())
        if (jongseong != -1) sb.append(jongseong.toChar())
        return sb.toString()
    }

    /**
     * Inputs a Korean compatibility Jamo character.
     * Returns a completed character to commit if any, and updates internal composing state.
     */
    fun inputJamo(jamo: Char): String? {
        val code = jamo.code
        val isConsonant = CHOSEONG_JAMO.contains(code)
        val isVowel = JUNGSEONG_JAMO.contains(code)

        if (isConsonant) {
            when {
                // 1. Empty state
                isEmpty() -> {
                    choseong = code
                    return null
                }
                // 2. Choseong only
                choseong != -1 && jungseong == -1 -> {
                    val completed = getComposingString()
                    clear()
                    choseong = code
                    return completed
                }
                // 3. Jungseong only
                choseong == -1 && jungseong != -1 -> {
                    val completed = getComposingString()
                    clear()
                    choseong = code
                    return completed
                }
                // 4. Choseong + Jungseong
                choseong != -1 && jungseong != -1 && jongseong == -1 -> {
                    if (JONGSEONG_JAMO.contains(code)) {
                        jongseong = code
                        return null
                    } else {
                        val completed = getComposingString()
                        clear()
                        choseong = code
                        return completed
                    }
                }
                // 5. Choseong + Jungseong + Jongseong
                choseong != -1 && jungseong != -1 && jongseong != -1 -> {
                    val combined = combineJongseong(jongseong, code)
                    if (combined != null) {
                        jongseong = combined
                        return null
                    } else {
                        val completed = getComposingString()
                        clear()
                        choseong = code
                        return completed
                    }
                }
            }
        } else if (isVowel) {
            when {
                // 1. Empty state
                isEmpty() -> {
                    jungseong = code
                    return null
                }
                // 2. Choseong only
                choseong != -1 && jungseong == -1 -> {
                    jungseong = code
                    return null
                }
                // 3. Jungseong only
                choseong == -1 && jungseong != -1 -> {
                    val combined = combineJungseong(jungseong, code)
                    if (combined != null) {
                        jungseong = combined
                        return null
                    } else {
                        val completed = getComposingString()
                        clear()
                        jungseong = code
                        return completed
                    }
                }
                // 4. Choseong + Jungseong
                choseong != -1 && jungseong != -1 && jongseong == -1 -> {
                    val combined = combineJungseong(jungseong, code)
                    if (combined != null) {
                        jungseong = combined
                        return null
                    } else {
                        val completed = getComposingString()
                        clear()
                        jungseong = code
                        return completed
                    }
                }
                // 5. Choseong + Jungseong + Jongseong (Liaison Shift Rule)
                choseong != -1 && jungseong != -1 && jongseong != -1 -> {
                    val split = splitJongseong(jongseong)
                    if (split != null) {
                        // Compound Jongseong: e.g. ㄺ -> ㄹ stays as Jongseong, ㄱ moves to next Choseong
                        val firstJong = split.first
                        val secondJong = split.second
                        
                        // Temporarily set jongseong to the first part to build the completed syllable
                        jongseong = firstJong
                        val completed = getComposingString()
                        
                        // Start the new syllable with the second part as choseong and new vowel
                        clear()
                        choseong = secondJong
                        jungseong = code
                        return completed
                    } else {
                        // Simple Jongseong: e.g. ㄱ -> moves to next Choseong
                        val movingCho = jongseong
                        
                        // Temporarily remove jongseong to build completed syllable
                        jongseong = -1
                        val completed = getComposingString()
                        
                        // Start the new syllable
                        clear()
                        choseong = movingCho
                        jungseong = code
                        return completed
                    }
                }
            }
        }
        return null
    }

    /**
     * Handles backspace.
     * Returns a Pair of:
     * - String?: completed text to insert before (always null for backspace deletion)
     * - Boolean: true if composing state has changed (which means we should setComposingText)
     */
    fun backspace(): Pair<String?, Boolean> {
        if (isEmpty()) {
            return Pair(null, false)
        }

        when {
            // 1. Choseong only
            choseong != -1 && jungseong == -1 -> {
                clear()
                return Pair(null, true)
            }
            // 2. Jungseong only
            choseong == -1 && jungseong != -1 -> {
                val split = splitJungseong(jungseong)
                if (split != null) {
                    jungseong = split.first
                } else {
                    clear()
                }
                return Pair(null, true)
            }
            // 3. Choseong + Jungseong
            choseong != -1 && jungseong != -1 && jongseong == -1 -> {
                val split = splitJungseong(jungseong)
                if (split != null) {
                    jungseong = split.first
                } else {
                    jungseong = -1
                }
                return Pair(null, true)
            }
            // 4. Choseong + Jungseong + Jongseong
            choseong != -1 && jungseong != -1 && jongseong != -1 -> {
                val split = splitJongseong(jongseong)
                if (split != null) {
                    jongseong = split.first
                } else {
                    jongseong = -1
                }
                return Pair(null, true)
            }
        }
        return Pair(null, false)
    }
}
