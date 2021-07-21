package kenneth.app.spotlightlauncher.searching

import kotlin.math.max

/**
 * Compares two strings with the given regex and ranks them. They are ranked based on the
 * following criterion, from highest to lowest:
 *   - whichever string has the longer matching substring
 *   - whichever string has a matching substring at a lower index in the string
 *   - whichever string is shorter
 *
 * @param string1 The first string to compare
 * @param string2 The second string to compare
 * @return A negative number if [string1] comes first, a positve number if [string2] comes first,
 * or 0 if both strings should stay in place.
 */
fun compareStringsWithRegex(string1: String, string2: String, regex: Regex): Int {
    val result1 = regex.findAll(string1).toList()
    val result2 = regex.findAll(string2).toList()

    // first, find the longest match in all matches
    // if the query has a longer match of the name of the first app than the second app
    // the first app should come first

    val string1LongestRegexMatch = longestConsecutiveRegexMatchInString(result1, string1)
    val string2LongestRegexMatch = longestConsecutiveRegexMatchInString(result2, string2)

    if (string1LongestRegexMatch != string2LongestRegexMatch) {
        return string2LongestRegexMatch - string1LongestRegexMatch
    }

    // if the longest matches have the same length
    // then find which match comes first
    // for example, if the query is "g", string1 is "google", and string2 is "settings"
    // app1 should come first because the "g" in google comes first

    val result1FirstMatchIndex = result1[0].range.first
    val result2FirstMatchIndex = result2[0].range.first

    if (result1FirstMatchIndex != result2FirstMatchIndex) {
        return result1FirstMatchIndex - result2FirstMatchIndex
    }

    return string1.length - string2.length
}

/**
 * Finds the length of the longest consecutive regex match in the given string.
 *
 * @param matches The list of regex matches in [string]
 * @param string The string that contains the regex matches
 * @return The number of characters of the longest consecutive regex match.
 */
private fun longestConsecutiveRegexMatchInString(matches: List<MatchResult>, string: String): Int =
    matches
        .foldIndexed(Pair(0, 0)) { i, pair, match ->
            val (longestMatchLength, currentLongestMatchLength) = pair
            val currentMatchLength = match.range.last - match.range.first + 1
            when {
                i == 0 -> pair.copy(second = currentLongestMatchLength + currentMatchLength)
                isNeighboringRegexMatches(matches[i - 1], match) -> {
                    val newLen = currentLongestMatchLength + currentMatchLength
                    if (i == matches.size - 1)
                        pair.copy(
                            first = max(longestMatchLength, newLen),
                            second = 0
                        )
                    else
                        pair.copy(second = newLen)
                }
                else -> pair.copy(
                    first = max(longestMatchLength, currentLongestMatchLength),
                    second = 0
                )
            }
        }
        .first

/**
 * Determines if the two given regex matches are right next to each other,
 * i.e. there are no characters in between the two regex matches.
 *
 * @param match1 The first regex match
 * @param match2 The second regex match. Must come after [match1]
 * @return Whether [match1] and [match2] are right next to each other.
 */
private fun isNeighboringRegexMatches(match1: MatchResult, match2: MatchResult): Boolean =
    match2.range.first - match1.range.last == 1