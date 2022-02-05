package kenneth.app.starlightlauncher.searching.views
//
//import android.content.Context
//import android.content.pm.ResolveInfo
//import android.util.AttributeSet
//import android.util.TypedValue
//import android.view.LayoutInflater
//import androidx.core.view.isVisible
//import kenneth.app.starlightlauncher.R
//import kenneth.app.starlightlauncher.databinding.SuggestedResultCardBinding
////import kenneth.app.starlightlauncher.searching.SearchResult
//import kenneth.app.starlightlauncher.SmartSearcher
//import kenneth.app.starlightlauncher.SuggestedResultType
//import kenneth.app.starlightlauncher.views.BlurView
//import kenneth.app.starlightlauncher.views.SectionCard
//
///**
// * Displayed at the top of the search page to show the user suggested result from the launcher.
// */
//class SuggestedResultCard(context: Context, attrs: AttributeSet) :
//    SectionCard<Any>(context, attrs) {
//    private val binding = SuggestedResultCardBinding.inflate(LayoutInflater.from(context), this)
//
//    init {
//        title = context.getString(R.string.suggested_section_title)
//        blurAmount = TypedValue().run {
//            context.theme.resolveAttribute(R.attr.blurAmount, this, true)
//            data
//        }
//    }
//
////    /**
////     * Displays the given suggested result in this card.
////     */
////    override fun display(result: SearchResult.Suggested) {
////        super.display(result)
////        binding.suggestedContent.removeAllViews()
////
////        when (result) {
////            is SearchResult.Suggested.Wifi -> {
////                binding.suggestedContent.addView(WifiControl(context))
////            }
////            is SearchResult.Suggested.Bluetooth -> {
////                binding.suggestedContent.addView(BluetoothControl(context))
////            }
////            is SearchResult.Suggested.Url -> {
////                binding.suggestedContent.addView(
////                    OpenUrlControl(context).apply {
////                        url = result.query
////                    }
////                )
////            }
////            is SearchResult.Suggested.Math -> {
////                binding.suggestedContent.addView(
////                    MathResultView(context).also {
////                        it.showResult(result)
////                    }
////                )
////            }
////            is SearchResult.Suggested.App -> {
////                binding.suggestedContent.addView(
////                    SuggestedAppView(context).apply {
////                        setSuggestedApp(result.suggestedApp)
////                    }
////                )
////            }
////            is SearchResult.Suggested.None -> hide()
////        }
////    }
//}