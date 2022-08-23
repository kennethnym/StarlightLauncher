package kenneth.app.starlightlauncher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kenneth.app.starlightlauncher.databinding.FragmentMainScreenBinding

internal class MainScreenFragment(
    private val bindingRegister: BindingRegister,
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let { context ->
        FragmentMainScreenBinding.inflate(inflater).run {
            bindingRegister.mainScreenBinding = this
            root
        }
    }
}
