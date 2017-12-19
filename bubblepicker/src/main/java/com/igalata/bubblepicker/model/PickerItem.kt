package com.igalata.bubblepicker.model

import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt

/**
 * Created by irinagalata on 1/19/17.
 */
data class PickerItem @JvmOverloads constructor(var title: String? = null,
                                                var titleLong: String? = null,
                                                var icon: Drawable? = null,
                                                @ColorInt var color: Int? = null,
                                                @ColorInt var selectedColor: Int? = null,
                                                @ColorInt var textColor: Int? = null,
                                                @ColorInt var selectedTextColor: Int? = null,
                                                var isSelected: Boolean = false)