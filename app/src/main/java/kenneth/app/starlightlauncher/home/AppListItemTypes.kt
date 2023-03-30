package kenneth.app.starlightlauncher.home

import android.content.pm.LauncherActivityInfo
import java.util.*

// maps language tags to the corresponding function that categorizes items in the app list.
// e.g. in English, the app list is categorized based on the first letter of app names:
// special characters, a-z, and letters outside of the english language.
private val appListItemTypeCategorizer = mapOf(
    "en-US" to ::defaultAppListItemType,
    "en-GB" to ::defaultAppListItemType,
)

/**
 * Returns the item type of the given app in the app list according to the given locale.
 * Different languages have different ways to categorize items in a list.
 *
 * For example, in English, the app list is categorized based on the first letter of the name of an app:
 *  - Special characters, e.g. 1-9, #, %, ^
 *  - A-Z
 *  - Characters outside of the English language.
 *
 *  In Chinese, the app list can be categorized based on the number of strokes of the first character.
 */
fun itemTypeOfApp(app: LauncherActivityInfo, locale: Locale): Int {
    val categorizer = appListItemTypeCategorizer[locale.toLanguageTag()] ?: ::defaultAppListItemType
    return categorizer(app.label)
}

internal fun defaultAppListItemType(appLabel: CharSequence): Int {
    val numberOrPunctuation = 0
    val otherCharacters = 27
    val lowerCaseAUniCode = 97

    val firstLetter = appLabel[0].lowercaseChar()
    return when {
        Regex("[\\p{P}\\p{N}]").matchesAt(appLabel, 0) ->
            numberOrPunctuation

        Regex("[a-z]", RegexOption.IGNORE_CASE).matchesAt(appLabel, 0) ->
            // converts a-z to 1-26
            firstLetter.code - lowerCaseAUniCode + 1

        else -> otherCharacters
    }
}
