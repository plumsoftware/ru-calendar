package com.plumsoftware.rucalendar.data;

import androidx.annotation.ColorRes;

import com.plumsoftware.rucalendar.R;

/**
 * Тип события (ТЗ п. 2.1). Порядок объявления — порядок событий одного дня (п. 6.3):
 * официальный, памятная дата, профессиональный, неофициальный; собственные события — последними.
 */
public enum EventType {
    OFFICIAL("official", "Официальный праздник", "Официальные",
            R.color.type_official, R.color.type_official_plate, R.color.red_container,
            R.color.type_official_solid, R.color.type_official_deep),
    MEMORIAL("memorial", "Памятная дата", "Памятные даты",
            R.color.type_memorial, R.color.type_memorial_plate, R.color.green_container,
            R.color.type_memorial_solid, R.color.type_memorial_deep),
    PROFESSIONAL("professional", "Профессиональный праздник", "Профессиональные",
            R.color.type_professional, R.color.type_professional_plate, R.color.blue_container,
            R.color.type_professional_solid, R.color.type_professional_deep),
    UNOFFICIAL("unofficial", "Неофициальный праздник", "Неофициальные",
            R.color.type_unofficial, R.color.type_unofficial_plate, R.color.orange_container,
            R.color.type_unofficial_solid, R.color.type_unofficial_deep),
    USER("user", "Моё событие", "Мои события",
            R.color.type_user_default, 0, R.color.purple_container,
            R.color.type_user_default, R.color.type_user_default);

    /** Порядок чипов в ряду фильтров (п. 4.1). */
    public static final EventType[] FILTER_ORDER = {OFFICIAL, PROFESSIONAL, UNOFFICIAL, MEMORIAL, USER};

    public final String key;
    public final String label;
    public final String filterLabel;
    @ColorRes
    public final int colorRes;
    /** Фон плашки; для USER считается из цвета события. */
    @ColorRes
    public final int plateColorRes;
    /** Цвет, который ждёт старый EventActivity, чтобы определить тип. */
    @ColorRes
    public final int legacyColorRes;
    /** Заливка шапки экрана праздника и полосы листка в карточке дня. */
    @ColorRes
    public final int solidColorRes;
    /** Тёмный оттенок для кнопок и чипов на заливке. */
    @ColorRes
    public final int deepColorRes;

    EventType(String key, String label, String filterLabel, int colorRes, int plateColorRes, int legacyColorRes,
              int solidColorRes, int deepColorRes) {
        this.key = key;
        this.label = label;
        this.filterLabel = filterLabel;
        this.colorRes = colorRes;
        this.plateColorRes = plateColorRes;
        this.legacyColorRes = legacyColorRes;
        this.solidColorRes = solidColorRes;
        this.deepColorRes = deepColorRes;
    }

    public static EventType fromKey(String key) {
        for (EventType type : values()) {
            if (type.key.equals(key)) return type;
        }
        throw new IllegalArgumentException("Unknown event type: " + key);
    }
}
