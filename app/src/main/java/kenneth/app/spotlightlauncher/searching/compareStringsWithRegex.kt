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

    val result1IndexRange = regexMatchIndexRange(result1)
    val result2IndexRange = regexMatchIndexRange(result2)

    result1IndexRange.zip(result2IndexRange)
        .forEach { (matchRange1, matchRange2) ->
            val matchRange1Size = matchRange1.second - matchRange1.first + 1
            val matchRange2Size = matchRange2.second - matchRange2.first + 1

            when {
                matchRange1Size != matchRange2Size ->
                    return@compareStringsWithRegex matchRange2Size - matchRange1Size

                matchRange1.first != matchRange2.first ->
                    return@compareStringsWithRegex matchRange1.first - matchRange2.first
            }
        }

    return when {
        result2IndexRange.size != result1IndexRange.size ->
            result2IndexRange.size - result1IndexRange.size

        else -> string1.length - string2.length
    }
}

private fun regexMatchIndexRange(matches: List<MatchResult>) =
    matches
        .foldIndexed(mutableListOf<Pair<Int, Int>>()) { i, ranges, match ->
            ranges.apply {
                when {
                    ranges.isEmpty() -> add(match.range.first to match.range.last)

                    match.range.first - last().second == 1 ->
                        this[lastIndex] = last().copy(second = match.range.last)

                    else -> add(match.range.first to match.range.last)
                }
            }
        }
