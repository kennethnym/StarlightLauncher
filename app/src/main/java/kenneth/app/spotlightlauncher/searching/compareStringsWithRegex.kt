package kenneth.app.spotlightlauncher.searching

/**
 * Compares two strings with the given regex and ranks them. They are ranked based on the
 * following criterion, from highest to lowest:
 *   - whichever string has the longer matching substring
 *   - whichever string has a matching substring at a lower index in the string
 */
fun compareStringsWithRegex(string1: String, string2: String, regex: Regex): Int {
    val result1 = regex.findAll(string1).toList()
    val result2 = regex.findAll(string2).toList()

    // first, find the longest match in all matches
    // if the query has a longer match of the name of the first app than the second app
    // the first app should come first

    val result1LongestMatch = result1.foldIndexed(0) { i, len, result ->
        when {
            i == 0 -> len + 1
            result.range.first - result1[i - 1].range.first > 1 -> 1
            else -> len + 1
        }
    }

    val result2LongestMatch = result2.foldIndexed(0) { i, len, result ->
        when {
            i == 0 -> len + 1
            result.range.first - result2[i - 1].range.first > 1 -> 1
            else -> len + 1
        }
    }

    if (result1LongestMatch != result2LongestMatch) {
        return result2LongestMatch - result1LongestMatch
    }

    // if the longest matches have the same length
    // then find which match comes first
    // for example, if the query is "g", string1 is "google", and string2 is "settings"
    // app1 should come first because the "g" in google comes first

    val result1FirstMatchIndex = result1[0].range.first
    val result2FirstMatchIndex = result2[0].range.first

    return result1FirstMatchIndex - result2FirstMatchIndex
}