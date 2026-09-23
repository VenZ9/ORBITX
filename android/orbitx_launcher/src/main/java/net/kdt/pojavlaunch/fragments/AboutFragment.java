package net.kdt.pojavlaunch.fragments;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.BuildConfig;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.UiMotion;
import net.kdt.pojavlaunch.utils.animation.MotionSpeed;

/**
 * About page — the OrbitX product page.
 *
 * <p>Shows the OrbitX mark, the wordmark, the live build facts and the GPL v3
 * notice. The old team/credits/community blocks (people cards, lineage tiles,
 * Discord / GitHub / YouTube links) were removed with the surrounding UI.
 */
public class AboutFragment extends Fragment {

    public static final String TAG = "AboutFragment";

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public AboutFragment() {
        super(R.layout.fragment_about);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Version chip
        TextView versionChip = view.findViewById(R.id.about_version_chip);
        if (versionChip != null) {
            versionChip.setText("OrbitX v" + BuildConfig.VERSION_NAME);
        }

        // Build facts — exactly which build is installed.
        TextView buildVersion = view.findViewById(R.id.about_build_version);
        if (buildVersion != null) buildVersion.setText(BuildConfig.VERSION_NAME);

        TextView buildChannel = view.findViewById(R.id.about_build_channel);
        if (buildChannel != null) {
            buildChannel.setText(BuildConfig.DEBUG ? "DEBUG" : "RELEASE");
        }

        TextView buildAbi = view.findViewById(R.id.about_build_abi);
        if (buildAbi != null) {
            String abi = "—";
            try {
                String[] abis = Build.SUPPORTED_ABIS;
                if (abis != null && abis.length > 0) abi = abis[0];
            } catch (Throwable ignored) { }
            buildAbi.setText(abi);
        }

        // Back
        View back = view.findViewById(R.id.about_back_button);
        if (back != null) {
            UiMotion.pressFeedback(back);
            back.setOnClickListener(v -> navigateBack());
        }
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { navigateBack(); }
                });

        // ── Entrance choreography (best & fast; no-op when animations Off) ──
        if (MotionSpeed.isEnabled()) {
            view.post(() -> {
                if (!isAdded() || isRemoving()) return;
                View heroCard = view.findViewById(R.id.about_hero_card);
                if (heroCard != null) {
                    heroCard.setAlpha(0f);
                    heroCard.setScaleX(0.9f);
                    heroCard.setScaleY(0.9f);
                    heroCard.animate().alpha(1f).scaleX(1f).scaleY(1f)
                            .setDuration((long)(400 * MotionSpeed.factor()))
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                            .start();
                }
                // The OrbitX mark pops in with a jelly overshoot.
                View logo = view.findViewById(R.id.about_cs_logo);
                if (logo != null) UiMotion.popIn(logo);

                // Staggered cascade: hero → features → legal
                cascade(view.findViewById(R.id.about_credits_heading), 100);
                cascade(view.findViewById(R.id.about_build_heading), 170);
                cascade(view.findViewById(R.id.about_legal_heading), 250);
                cascade(view.findViewById(R.id.about_legal_card), 320);

                // Legal notice — TYPEWRITER reveal, starts right after the card lands.
                mHandler.postDelayed(() -> typewriter(
                        view.findViewById(R.id.about_legal_text),
                        getString(R.string.cs_about_legal_text)), 700);
            });
        } else {
            // Animations off → show the legal text instantly.
            TextView legal = view.findViewById(R.id.about_legal_text);
            if (legal != null) legal.setText(getString(R.string.cs_about_legal_text));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeCallbacksAndMessages(null);
    }

    /** Reveals the legal notice word-by-word like it is being typed. */
    private void typewriter(@Nullable TextView tv, @NonNull String fullText) {
        if (tv == null || !isAdded()) return;
        final String[] words = fullText.split(" ");
        tv.setText("");
        tv.setVisibility(View.VISIBLE);
        final long stepMs = Math.max(8L, MotionSpeed.scale(18L)); // per word
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            final int idx = i;
            mHandler.postDelayed(() -> {
                if (!isAdded()) return;
                if (idx > 0) sb.append(" ");
                sb.append(words[idx]);
                tv.setText(sb.toString());
            }, stepMs * (idx + 1));
        }
    }

    private void cascade(@Nullable View v, long delayMs) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(18f * getResources().getDisplayMetrics().density);
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(MotionSpeed.scale(delayMs))
                .setDuration(MotionSpeed.scale(300L))
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .start();
    }

    private void navigateBack() {
        Fragment parent = getParentFragment();
        if (parent instanceof MainMenuFragment) {
            ((MainMenuFragment) parent).refreshHomeState();
        } else if (parent != null) {
            parent.getChildFragmentManager().popBackStackImmediate();
        } else {
            Tools.removeCurrentFragment(requireActivity());
        }
    }
}
