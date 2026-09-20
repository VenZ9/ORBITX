package com.movtery.zalithlauncher.ui.fragment.about

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.movtery.zalithlauncher.InfoCenter
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentAboutInfoPageBinding
import com.movtery.zalithlauncher.ui.subassembly.about.AboutItemBean
import com.movtery.zalithlauncher.ui.subassembly.about.AboutItemBean.AboutItemButtonBean
import com.movtery.zalithlauncher.ui.subassembly.about.AboutRecyclerAdapter
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.path.UrlManager

/**
 * The "About OrbitX Launcher" info page.
 *
 * OrbitX is a downstream fork, so the one entry kept here credits PojavLauncher, whose
 * runtime contract this launcher drives.  Everything that pointed at the upstream
 * project's own community - the Discord invite, the QQ group dialog, the Zalith
 * repository button, the per-person contributor rows with their Bilibili and Afdian
 * pages, and the sponsor page fetched from the upstream info repository - has been
 * removed, so nothing on this screen sends a user to another launcher's channels.
 *
 * The full third-party attribution and the verbatim licence texts live in the
 * repository's LICENSE and LICENSE-THIRD-PARTY.md, which is where GPL-3.0 wants them
 * and which is not part of the shipped UI.
 */
class AboutInfoPageFragment : Fragment(R.layout.fragment_about_info_page) {
    private lateinit var binding: FragmentAboutInfoPageBinding
    private val mAboutData: MutableList<AboutItemBean> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutInfoPageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadAboutData(requireContext().resources)

        val context = requireActivity()

        binding.apply {
            dec1.text = InfoCenter.replaceName(context, R.string.about_dec1)
            dec2.text = InfoCenter.replaceName(context, R.string.about_dec2)
            dec3.text = InfoCenter.replaceName(context, R.string.about_dec3)

            // Both buttons now resolve to OrbitX's own project page / the GPL text.
            githubButton.setOnClickListener { ZHTools.openLink(requireActivity(), UrlManager.URL_HOME) }
            licenseButton.setOnClickListener { ZHTools.openLink(requireActivity(), UrlManager.URL_LICENSE) }

            val aboutAdapter = AboutRecyclerAdapter(this@AboutInfoPageFragment.mAboutData)
            aboutRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = aboutAdapter
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadAboutData(resources: Resources) {
        mAboutData.clear()

        // PojavLauncher is credited because OrbitX runs on its runtime contract (the
        // renderer environment variables, the native exec bridge and the JRE layout).
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.ic_pojav_full, requireContext().theme),
                "PojavLauncherTeam",
                getString(R.string.about_PojavLauncher_desc),
                AboutItemButtonBean(
                    requireActivity(),
                    "GitHub",
                    "https://github.com/PojavLauncherTeam/PojavLauncher"
                )
            )
        )
    }
}
