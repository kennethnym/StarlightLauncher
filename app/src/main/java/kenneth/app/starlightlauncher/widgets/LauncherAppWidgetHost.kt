package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.starlightlauncher.LauncherState
import kenneth.app.starlightlauncher.R
import javax.inject.Inject

@Module
@InstallIn(ActivityComponent::class)
internal abstract class LauncherAppWidgetHostProvider {
    @ActivityScoped
    @Binds
    abstract fun bindAppWidgetHost(impl: LauncherAppWidgetHost): AppWidgetHost
}

internal class LauncherAppWidgetHost @Inject constructor(
    @ApplicationContext context: Context,
    private val launcherState: LauncherState,
) :
    AppWidgetHost(context, R.id.app_widget_host_id) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = LauncherAppWidgetHostView(context, launcherState)
}