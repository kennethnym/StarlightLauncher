package kenneth.app.starlightlauncher.setup

import androidx.fragment.app.Fragment
import kenneth.app.starlightlauncher.setup.feature.FeatureFragment
import kenneth.app.starlightlauncher.setup.permission.PermissionFragment

/**
 * A list of fragment constructors that creates fragments for each setup step.
 * The list follows the order of the setup steps.
 */
internal val SETUP_PAGE_CONSTRUCTORS = listOf<() -> Fragment>(
    ::LandingFragment,
    ::FeatureFragment,
    ::PermissionFragment,
)

/**
 * Total number of setup steps.
 */
internal val SETUP_STEP_COUNT = SETUP_PAGE_CONSTRUCTORS.size
