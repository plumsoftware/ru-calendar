package com.plumsoftware.rucalendar.ui.feed;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.widget.TextViewCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.ads.BannerAds;
import com.plumsoftware.rucalendar.config.AdsConfig;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.plumsoftware.rucalendar.data.CalendarRepository;
import com.plumsoftware.rucalendar.data.DayEvent;
import com.plumsoftware.rucalendar.data.FavoritesRepository;
import com.plumsoftware.rucalendar.ui.Motion;

import java.time.LocalDate;
import java.util.List;

/** Лента: ближайшие события на 12 месяцев вперёд и поиск (ТЗ п. 4.4). */
public class FeedFragment extends Fragment {
    private static final String STATE_FAVORITES = "favorites_only";

    public interface Host {
        void openDay(DayEvent event);
    }

    private CalendarRepository repository;
    private FeedAdapter adapter;
    private RecyclerView list;
    private TextView empty;
    private EditText search;
    private TextView favoritesChip;
    private FavoritesRepository favorites;
    /** Показывать только избранные праздники. */
    private boolean favoritesOnly;
    @Nullable
    private BannerAdView banner;
    /** Высота баннера внизу ленты; 0 — баннера нет. */
    private int bannerHeight;
    private String lastQuery;
    private LocalDate lastToday;
    private boolean focusOnStart;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = new CalendarRepository(context);
        favorites = new FavoritesRepository(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        list = view.findViewById(R.id.list);
        empty = view.findViewById(R.id.empty);
        search = view.findViewById(R.id.search);
        favoritesChip = view.findViewById(R.id.favorites_chip);
        if (savedInstanceState != null) favoritesOnly = savedInstanceState.getBoolean(STATE_FAVORITES, false);
        renderFavoritesChip();
        favoritesChip.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            renderFavoritesChip();
            lastQuery = null;
            reload(true);
        });

        adapter = new FeedAdapter(repository.production(), event -> host().openDay(event));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        list.setItemAnimator(null);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Результаты показываются по мере ввода
                reload(true);
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        applyInsets(view);
        setupBanner(view);
        reload(false);
        if (focusOnStart) {
            focusOnStart = false;
            focusSearch();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (banner != null) {
            banner.destroy();
            banner = null;
        }
        bannerHeight = 0;
    }

    /** Баннер на главной странице — внизу ленты; при выключенном флаге даже не загружается. */
    private void setupBanner(View root) {
        if (!AdsConfig.SHOW_MAIN_SCREEN_BANNER) return;
        FrameLayout container = root.findViewById(R.id.ad_container);
        container.post(() -> {
            if (!isAdded() || getView() == null) return;
            banner = BannerAds.load(requireActivity(), container, AdsConfig.BANNER_MAIN_SCREEN_AD, height -> {
                bannerHeight = height;
                ViewCompat.requestApplyInsets(root);
            });
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_FAVORITES, favoritesOnly);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            hideKeyboard();
        } else {
            // Фильтры с «Месяца» и свои события могли измениться
            forceReload();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) forceReload();
    }

    /** Открыть ленту с фокусом в поиске (кнопка поиска на «Месяце»). */
    public void focusSearch() {
        if (search == null) {
            focusOnStart = true;
            return;
        }
        search.requestFocus();
        search.post(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void forceReload() {
        lastQuery = null;
        reload(false);
    }

    private void reload(boolean animate) {
        if (search == null) return;
        String query = search.getText().toString().trim();
        LocalDate today = LocalDate.now();
        if (query.equals(lastQuery) && today.equals(lastToday)) return;
        lastQuery = query;
        lastToday = today;

        LocalDate until = today.plusMonths(12);
        // Поиск включает и прошедшие даты текущего года
        LocalDate from = query.isEmpty() ? today : today.withDayOfYear(1);
        final List<DayEvent> shown = favoritesOnly
                ? repository.filter(repository.favorites(favorites, from, until), query)
                : repository.search(query, from, until);
        final boolean noFavorites = favoritesOnly && query.isEmpty();

        Runnable apply = () -> {
            adapter.submit(shown, today);
            empty.setText(noFavorites ? R.string.feed_favorites_empty : R.string.feed_empty);
            empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        };
        if (!animate) {
            apply.run();
            return;
        }
        // Список результатов меняется через затухание, 150 мс (п. 9.2)
        list.animate().cancel();
        list.animate().alpha(0f).setDuration(75).setInterpolator(Motion.STANDARD).withEndAction(() -> {
            apply.run();
            list.scrollToPosition(0);
            list.animate().alpha(1f).setDuration(75).setInterpolator(Motion.STANDARD).start();
        }).start();
    }

    /** Включённый чип — тёмная капсула с залитой звездой, выключенный — белая с контурной. */
    private void renderFavoritesChip() {
        Context context = requireContext();
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable capsule = new GradientDrawable();
        capsule.setCornerRadius(18 * density);
        int textColor;
        if (favoritesOnly) {
            capsule.setColor(ContextCompat.getColor(context, R.color.ds_button_primary_bg));
            textColor = ContextCompat.getColor(context, R.color.ds_button_primary_text);
        } else {
            capsule.setColor(ContextCompat.getColor(context, R.color.ds_surface));
            capsule.setStroke(Math.round(density), ContextCompat.getColor(context, R.color.ds_outline));
            textColor = ContextCompat.getColor(context, R.color.ds_text_primary);
        }
        int inset = Math.round(4 * density);
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(18 * density);
        mask.setColor(0xFFFFFFFF);
        favoritesChip.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1F000000),
                new InsetDrawable(capsule, 0, inset, 0, inset), new InsetDrawable(mask, 0, inset, 0, inset)));
        favoritesChip.setTextColor(textColor);
        favoritesChip.setCompoundDrawablesRelativeWithIntrinsicBounds(
                favoritesOnly ? R.drawable.ic_star_filled_16 : R.drawable.ic_favorite_16, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(favoritesChip, ColorStateList.valueOf(textColor));
        favoritesChip.setSelected(favoritesOnly);
        ViewCompat.setStateDescription(favoritesChip,
                getString(favoritesOnly ? R.string.filter_on : R.string.filter_off));
    }

    private void applyInsets(View root) {
        View header = root.findViewById(R.id.feed_header);
        final float density = getResources().getDisplayMetrics().density;
        final int navBar = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            header.setPadding(header.getPaddingLeft(), bars.top + Math.round(12 * density),
                    header.getPaddingRight(), header.getPaddingBottom());
            // Список не прячется ни под нижней панелью, ни под клавиатурой, ни под баннером
            int bottom = Math.max(bars.bottom + navBar, ime.bottom);
            int extra = bannerHeight > 0 ? bannerHeight + Math.round(12 * density) : Math.round(16 * density);
            list.setPadding(list.getPaddingLeft(), list.getPaddingTop(), list.getPaddingRight(), bottom + extra);
            // Баннер — над нижней панелью
            View ad = root.findViewById(R.id.ad_container);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) ad.getLayoutParams();
            if (lp.bottomMargin != bars.bottom + navBar) {
                lp.bottomMargin = bars.bottom + navBar;
                ad.setLayoutParams(lp);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void hideKeyboard() {
        if (search == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        search.clearFocus();
    }

    private Host host() {
        return (Host) requireActivity();
    }
}
