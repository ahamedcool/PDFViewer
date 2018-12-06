package com.appknot.gtr_test

import android.graphics.Bitmap

/**
 * ThumbNail Adapter 데이터
 * @param bitmap 썸네일 이미지 비트맵
 * @param isThumb 썸네일 스레드 시작 여부
 */
data class ThumbnailAdapterData(var bitmap: Bitmap?, var isThumb: Boolean = false) {
    init {
        this.isThumb = isThumb
    }
}