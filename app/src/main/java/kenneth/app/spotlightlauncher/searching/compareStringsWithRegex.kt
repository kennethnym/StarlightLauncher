package kenneth.app.spotlightlauncher.searching

import android.util.Log

/**
 * Compares two strings with the given regex and ranks them.
 *
 * @param string1 The first string to compare
 * @param string2 The second string to compare
 * @param regex The [Regex] that will be matched with [string1] and [string2].
 * @return A negative number if [string1] comes first, a positive number if [string2] comes first,
 * or 0 if both strings should stay in place.
 */
fun compareStringsWithRegex(string1: String, string2: String, regex: Regex): Int {
    val result1 = regex.findAll(string1).toList()
    val result2 = regex.findAll(string2).toList()
    val searchTerm = regex.toString().run {
        substring(1 until length - 1)
    }.lowercase()

    /**
     * Maps chars in a string to their corresponding index in the string.
     */
    val searchTermIndexMap = searchTerm.foldIndexed(mutableMapOf<Char, IntRange>()) { i, m, char ->
        when {
            m.contains(char) -> m.apply {
                this[char] = this[char]!!.first..i
            }
            else -> m.apply { this[char] = i..i }
        }
    }

    val result1IndexRange = regexMatchIndexRange(result1, searchTermIndexMap)
    val result2IndexRange = regexMatchIndexRange(result2, searchTermIndexMap)


    result1IndexRange.zip(result2IndexRange)
        .forEach { (matchRange1, matchRange2) ->
            // matchRange1 and matchRange2 defines the range of the index of the regex match
            // in the search keyword
            //
            // for example, if the character 'a' is matched in the string 'authenticator'
            // and the search keyword is 'abc', then matchRanges will be:
            // [[0, 0] <- the first 'a' in 'authenticator', 'a' is the first character in 'abc'
            //  [2, 2] <- the first 'c' in 'authenticator', 'c' is the third character in 'abc']
            //
            // longer ranges will be ranked higher
            // if both ranges have the same size, then smaller indices will be ranked higher
            // (i.e. the matched character appears earlier in the search term.)

            val matchRange1Size = matchRange1.second - matchRange1.first + 1
            val matchRange2Size = matchRange2.second - matchRange2.first + 1

            when {
                matchRange1Size != matchRange2Size ->
                    return@compareStringsWithRegex matchRange2Size - matchRange1Size

                matchRange1.first != matchRange2.first ->
                    return@compareStringsWithRegex matchRange1.first - matchRange2.first
            }
        }

    return result2IndexRange.size - result1IndexRange.size
}

private fun regexMatchIndexRange(matches: List<MatchResult>, indexMap: Map<Char, IntRange>) =
    matches
        .foldIndexed(mutableListOf<Pair<Int, Int>>()) { i, ranges, match ->
            val char = match.value[0].lowercaseChar()
            val charIndexInSearchTerm = indexMap[char]
            ranges.apply {
                when {
                    charIndexInSearchTerm == null -> {
                    }

                    isEmpty() || match.range.first - matches[i - 1].range.first > 1 -> add(
                        charIndexInSearchTerm.first to charIndexInSearchTerm.first
                    )

                    else -> {
                        val (_, lastMatch) = last()
                        when {
                            lastMatch in charIndexInSearchTerm ->
                                this[lastIndex] = last().run { copy(second = second + 1) }

                            charIndexInSearchTerm.first - lastMatch == 1 -> {
                                this[lastIndex] = last().run {
                                    copy(second = charIndexInSearchTerm.first)
                                }
                            }

                            else -> add(
                                charIndexInSearchTerm.first to charIndexInSearchTerm.first
                            )
                        }
                    }
                }
            }
        }
