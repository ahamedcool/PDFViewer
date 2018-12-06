package com.appknot.gtr_test

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

class MainActivity : AppCompatActivity() , OnPageChangeListener, OnPageErrorListener, OnLoadCompleteListener {
    private lateinit var uri: Uri
    private var dataList: ArrayList<ThumbnailAdapterData> = ArrayList()
    private var mAdapter: ThumbnailAdapter? = null
    private var totalPage = 0
    private var mGridLayoutManager: GridLayoutManager? = null
    private var pdfFileName: String? = null
    private var pdfiumCore: PdfiumCore? = null
    private var pdfDocument: PdfDocument? = null
    private var threadList: ArrayList<Runnable> = ArrayList()
    private var thumbWidth = 500
    private var thumbheight = 500
    private var rvDecoration: ThumbnailItemDecoration? = null
    private var isToolbarShow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar_main)
        uri = Uri.EMPTY
        uri?.let { supportActionBar?.setTitle(getFileName(uri)) }
        checkPermissionBeforePicker()

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_cycle_info, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_btn_1 -> {
                checkPermissionBeforePicker()
            }
            R.id.menu_btn_2 -> {
                showThumbNailView()
            }
            R.id.menu_btn_3 -> {
                displayFromUri(uri)
            }
            R.id.menu_btn_4 -> {
                displayFromUri(uri, false, false)
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            uri = intent!!.data
            uri?.let { supportActionBar?.setTitle(getFileName(uri)) }
            // PDF 로드 전
            clearPdfMemory()
            displayFromUri(uri)
            // PDF 로드 후
            readyImageFromPdf(uri)
            hideThumbNailView()
        }
    }
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        applyOrientationChanged(newConfig)
    }
    override fun onPageChanged(page: Int, pageCount: Int) {}
    override fun onPageError(page: Int, t: Throwable?) {}
    override fun loadComplete(nbPages: Int) {}

    private fun checkPermissionBeforePicker() {
        val permissionCheck = ContextCompat.checkSelfPermission(this,
                READ_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(READ_EXTERNAL_STORAGE),
                    PERMISSION_CODE
            )
            return
        }
        launchPicker()
    }
    private fun launchPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        try {
            startActivityForResult(intent, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            //alert user that file manager not working
        }
    }
    private fun displayFromUri(uri: Uri, isHorizontalMode: Boolean = true, isFlingMode: Boolean = true) {
        pdfFileName = getFileName(uri)
        pdfView.fromUri(uri)
                .defaultPage(0)
                .onPageChange(this) // 리스너 등록 (onPageChanged)
                .onLoad(this) // 리스너 등록 (loadComplete)
                .onPageError(this) // 리스트 등록 (onPageError)
                .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                .swipeHorizontal(isHorizontalMode)
                .autoSpacing(true)
                .enableAntialiasing(true)
                .pageFling(isFlingMode) // 플링
                .pageFitPolicy(FitPolicy.BOTH)
                .onPageError(this)
                .pageSnap(true) // 한 페이지에 하나만 나오도록 (여백생성)
                .load()
        pdfView.setOnClickListener {
                if (isToolbarShow) {
                    isToolbarShow = false
                    hideAppbarWithAnim()
                } else {
                    isToolbarShow = true
                    showAppbarWithAnim()
                }
        }
    }
    private fun getFileName(uri: Uri): String {
        var result: String = ""
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }
    private fun showThumbNailView() {
        if (rv_main.visibility == View.GONE) {
            rv_main.visibility = View.VISIBLE
            pdfView.visibility = View.GONE
        } else {
            rv_main.visibility = View.GONE
            pdfView.visibility = View.VISIBLE
        }
    }
    private fun hideThumbNailView() {
        rv_main.visibility = View.GONE
        pdfView.visibility = View.VISIBLE
    }
    private fun showAppbarWithAnim() {
        var TOOLBAR_ELEVATION = 10f
        toolbar_main.animate()
                .translationY(0f)
                .setInterpolator(LinearInterpolator())
                .setDuration(180)
    }
    private fun hideAppbarWithAnim() {
        toolbar_main.animate()
                .translationY((-toolbar_main.getHeight()).toFloat())
                .setInterpolator(LinearInterpolator())
                .setDuration(180)
    }
    private fun Int.dpToPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density).toInt()
    private fun Int.pxToDp(displayMetrics: DisplayMetrics): Int = (this / displayMetrics.density).toInt()


    /**
     * 이전 페이지에서 사용되는 스레드 종료 & pdfiumCore 종료
     */
    private fun clearPdfMemory() {
        for (i in threadList.indices) {
            if (threadList[i] != null) (threadList[i] as ThumbThread).stop()
        }
        threadList = ArrayList()
        pdfiumCore?.closeDocument(pdfDocument) // important!
    }
    /**
     * PDF 썸네일 로드 준비
     * @param pdfUri
     */
    private fun readyImageFromPdf(pdfUri: Uri) {
        try {
            pdfiumCore = PdfiumCore(this)
            val fd = contentResolver.openFileDescriptor(pdfUri, "r")
            pdfDocument = pdfiumCore?.newDocument(fd)
            totalPage = pdfiumCore?.let { pdfiumCore?.getPageCount(pdfDocument) }?:kotlin.run { 0 }
            pdfiumCore?.openPage(pdfDocument, 0, totalPage - 1) // 전체 페이지 오픈
            applyOrientationChanged(resources.configuration)

            /* 썸네일 크기 얻기 */
            val widthTmp = pdfiumCore?.let {
                pdfiumCore?.getPageWidthPoint(pdfDocument, 0) } ?:kotlin.run { 0 }
            val heightTmp = pdfiumCore?.let {
                pdfiumCore?.getPageHeightPoint(pdfDocument, 0) } ?:kotlin.run { 0 }
            thumbheight = thumbWidth * heightTmp / widthTmp
        } catch (e: Exception) {
        }
    }

    /**
     * Orientation Config 변경 시 호출
     */
    private fun applyOrientationChanged(config: Configuration?) {
        dataList = ArrayList()
        for (i in 0 until totalPage) {
            dataList.add(ThumbnailAdapterData(null, false))
        }
        var columnCount = PORTRAIT_COLUMN_COUNT
        rvDecoration?.let { rv_main.removeItemDecoration(rvDecoration) }
        if (config?.orientation == Configuration.ORIENTATION_PORTRAIT) {
            columnCount = PORTRAIT_COLUMN_COUNT
            rvDecoration = ThumbnailItemDecoration(PORTRAIT_COLUMN_COUNT, RV_DECO_SPACE, false)
            mGridLayoutManager = GridLayoutManager(this, PORTRAIT_COLUMN_COUNT)
        } else {
            columnCount = LANDSCAPE_COLUMN_COUNT
            rvDecoration = ThumbnailItemDecoration(LANDSCAPE_COLUMN_COUNT, RV_DECO_SPACE, false)
            mGridLayoutManager = GridLayoutManager(this, LANDSCAPE_COLUMN_COUNT)
        }

        var metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        thumbWidth = (metrics.widthPixels - 10.dpToPx(metrics)) / columnCount
        rvDecoration = ThumbnailItemDecoration(columnCount, RV_DECO_SPACE, false)
        mAdapter = ThumbnailAdapter(dataList)
        rv_main.addItemDecoration(rvDecoration)
        rv_main.layoutManager = mGridLayoutManager
        rv_main.adapter = mAdapter
        mAdapter!!.notifyDataSetChanged()
    }

    companion object {
        const val REQUEST_CODE = 42
        const val PERMISSION_CODE = 42042

        const val PORTRAIT_COLUMN_COUNT = 3                                                         // 가로모드 일때의 썸네일 컬럼 수
        const val LANDSCAPE_COLUMN_COUNT = 5                                                        // 세로모드 일때의 썸네일 컬럼 수
        const val RV_DECO_SPACE = 50 // 50px                                                        // RecyclerView ItemDecoration 간격
    }
    /**
     * 썸네일 처리 스레드
     */
    internal inner class ThumbThread(private val pdfiumCore: PdfiumCore, private val pdfDocument: PdfDocument, private val curPage: Int) : Runnable {
        val thread: Thread
        private var isRun = true // 실행 여부 (다른 Doc이동 시 비활성화)

        init {
            this.thread = Thread(this)
        }
        fun start() {
            thread.start()
        }
        fun stop() {
            this.isRun = false
            thread.interrupt()
        }
        override fun run() {
            if (dataList.get(curPage).isThumb == false) {
                if (isRun) {
                    val bmp = Bitmap.createBitmap(thumbWidth, thumbheight, Bitmap.Config.RGB_565)
                    pdfiumCore.renderPageBitmap(pdfDocument, bmp, curPage, 0, 0,
                            thumbWidth, thumbheight)
                    dataList.set(curPage, ThumbnailAdapterData(bmp, true))
                    runOnUiThread { mAdapter?.setData(dataList, curPage) }
                }
            }
        }
    }

    /**
     * 썸네일 어뎁터
     */
    inner class ThumbnailAdapter(mDataSet: ArrayList<ThumbnailAdapterData>) : RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {
        private var mDataset: ArrayList<ThumbnailAdapterData>
        private var bitmapDefault: Bitmap? = null

        init {
            this.mDataset = mDataSet
            bitmapDefault = (resources.getDrawable(R.drawable.thumb_default,
                    null) as BitmapDrawable).bitmap
        }
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var iv: ImageView
            init {
                iv = itemView.findViewById(R.id.iv_thumbnail)
                iv.setOnClickListener {
                    hideThumbNailView()
                    pdfView.jumpTo(getAdapterPosition())
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_thumbnail, parent, false)
            val vh = ViewHolder(v)
            val llLayoutParam = LinearLayout.LayoutParams(thumbWidth, thumbheight)
            v.findViewById<LinearLayout>(R.id.ll_wrapper).layoutParams = llLayoutParam
            return vh
        }
        override fun getItemCount(): Int {
            return mDataset.size
        }
        fun setData(mDataset: ArrayList<ThumbnailAdapterData>, position: Int) {
            this.mDataset = mDataset
            notifyItemChanged(position)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (mDataset[position].isThumb) {
                holder.iv.setImageBitmap(mDataset[position].bitmap)
            } else {
                holder.iv.setImageBitmap(bitmapDefault)
            }
            if (mDataset.get(position).isThumb == false && pdfiumCore != null) {
                val thumbThread = ThumbThread(pdfiumCore!!, pdfDocument!!, position)
                threadList.add(thumbThread)
                thumbThread.start()
            }
        }
    }
}
