package kenneth.app.starlightlauncher.api.util

import org.apache.commons.text.similarity.FuzzyScore
import java.util.*

private val localeToFuzzyScore = mutableMapOf<Locale, FuzzyScore>()

/**
 * Returns a fuzzy score of how similar [keyword] is to [string].
 *
 * @param string The target string the keyword should match against.
 * @param keyword The search keyword.
 * @param locale The locale of the device.
 * @return A score of how similar the keyword is to to string,
 *         the higher the score, the more similar the keyword is.
 */
fun fuzzyScore(string: String, keyword: String, locale: Locale): Int =
    localeToFuzzyScore
        .getOrPut(locale) { FuzzyScore(locale) }
        .run { fuzzyScore(string, keyword) }

/**
 * Returns a fuzzy score of how similar [keyword] is to [string].
 *
 * @param string The target string the keyword should match against.
 * @param keyword The search keyword.
 * @param locale The locale of the device.
 * @return A score of how similar the keyword is to to string,
 *         the higher the score, the more similar the keyword is.
 */
fun fuzzyScore(string: CharSequence, keyword: String, locale: Locale): Int =
    localeToFuzzyScore
        .getOrPut(locale) { FuzzyScore(locale) }
        .run { fuzzyScore(string, keyword) }
