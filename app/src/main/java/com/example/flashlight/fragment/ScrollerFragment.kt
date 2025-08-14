package com.example.flashlight.fragment

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.flashlight.R
import yuku.ambilwarna.AmbilWarnaDialog

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [ScrollerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScrollerFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var scrollAnimator: ValueAnimator? = null
    private var scrollSpeed: Long = 2000L
    private var originalText: String = ""
    private lateinit var seekBlinkSpeed: SeekBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scroller, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputText = view.findViewById<EditText>(R.id.inputText)
        val previewText = view.findViewById<TextView>(R.id.previewText)
        val colorText = view.findViewById<View>(R.id.colorText)
        val colorBackground = view.findViewById<View>(R.id.colorBackground)

        val horizontalScroll = view.findViewById<HorizontalScrollView>(R.id.horizontalScroll)

        val btnLeft = view.findViewById<ImageButton>(R.id.btnLeft)
        val btnRight = view.findViewById<ImageButton>(R.id.btnRight)
        val btnPause = view.findViewById<ImageButton>(R.id.btnPause)

        val seekSpeed = view.findViewById<SeekBar>(R.id.seekSpeed)
        val tvSpeedValue = view.findViewById<TextView>(R.id.tvSpeedValue)

        val checkTextBlink = view.findViewById<CheckBox>(R.id.checkTextBlink)
        val colorTextBox = view.findViewById<View>(R.id.colorTextBox)

        var textBlinkAnimator: ValueAnimator? = null

        val colorBox = view.findViewById<View>(R.id.colorBox)
        val checkBackgroundBlink = view.findViewById<CheckBox>(R.id.checkBackgroundBlink)
        var bgBlinkAnimator: ValueAnimator? = null

        seekBlinkSpeed = view.findViewById<SeekBar>(R.id.seekBlinkSpeed)
        val tvSpeed = view.findViewById<TextView>(R.id.tvSpeed)
        seekBlinkSpeed.visibility = View.GONE
        tvSpeed.visibility = View.GONE
        fun updateBlinkSpeedVisibility() {
            seekBlinkSpeed.visibility =
                if (checkTextBlink.isChecked || checkBackgroundBlink.isChecked) View.VISIBLE
                else View.GONE
            tvSpeed.visibility =
                if (checkTextBlink.isChecked || checkBackgroundBlink.isChecked) View.VISIBLE
                else View.GONE
        }

        val seekSize = view.findViewById<SeekBar>(R.id.seekSize)
        val tvSizeValue = view.findViewById<TextView>(R.id.tvSizeValue)
        val defaultTextSize = 72f
        seekSize.max = 140
        seekSize.progress = 70

        previewText.isSelected = true

        btnLeft.setOnClickListener {
            startScrolling(
                horizontalScroll,
                previewText,
                true
            ) // Cuộn sang trái (từ phải sang trái)
        }

        btnRight.setOnClickListener {
            startScrolling(
                horizontalScroll,
                previewText,
                false
            ) // Cuộn sang phải (từ trái sang phải)
        }

        btnPause.setOnClickListener {
            scrollAnimator?.cancel()
            // Khôi phục văn bản gốc khi pause để tránh duplicate vẫn còn
            if (originalText.isNotEmpty()) {
                previewText.text = originalText
            }
        }

        colorText.setOnClickListener {
            val currentColor = previewText.currentTextColor
            AmbilWarnaDialog(
                requireContext(),
                currentColor,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        previewText.setTextColor(color)
                        colorText.backgroundTintList = ColorStateList.valueOf(color)
                    }

                    override fun onCancel(dialog: AmbilWarnaDialog?) {
                        // Không làm gì cả
                    }
                }).show()
        }

        colorBackground.setOnClickListener {
            val bgColor = (previewText.background as? ColorDrawable)?.color ?: Color.BLACK
            AmbilWarnaDialog(requireContext(), bgColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    previewText.setBackgroundColor(color)
                    colorBackground.backgroundTintList = ColorStateList.valueOf(color)
                }
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
            }).show()
        }
        colorTextBox.setOnClickListener {
            val currentColor = colorTextBox.backgroundTintList?.defaultColor ?: Color.YELLOW
            AmbilWarnaDialog(requireContext(), currentColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    colorTextBox.backgroundTintList = ColorStateList.valueOf(color)
                }
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
            }).show()
        }

        colorBox.setOnClickListener {
            val currentColor = colorBox.backgroundTintList?.defaultColor ?: Color.CYAN
            AmbilWarnaDialog(requireContext(), currentColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    colorBox.backgroundTintList = ColorStateList.valueOf(color)
                }
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
            }).show()
        }

        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cập nhật TextView mỗi khi người dùng nhập
                previewText.text = s.toString()
                originalText = s.toString()

                previewText.post {
                    horizontalScroll.smoothScrollTo(0, 0) // Reset vị trí về đầu để preview rõ ràng
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = if (progress <= 0) 1 else progress
                val minDuration = 1000L  // nhanh nhất
                val maxDuration = 10000L // chậm nhất
                val ratio = 1f - (speed / 10f) // đổi tốc độ thành phần trăm

                scrollSpeed = (minDuration + (maxDuration - minDuration) * ratio).toLong()
                tvSpeedValue.text = "$speed"

                scrollAnimator?.duration = scrollSpeed
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percentage = progress + 20 // Chuyển range từ 0-140 thành 20%-160%
                val newSize = defaultTextSize * (percentage / 100f)

                previewText.textSize = newSize
                tvSizeValue.text = "$percentage%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        checkTextBlink.setOnCheckedChangeListener { _, isChecked ->
            textBlinkAnimator?.cancel() // dừng animator cũ nếu có

            if (isChecked) {
                // Lấy màu chữ gốc và màu nhấp nháy
                val originalColor = previewText.currentTextColor
                val blinkColor = (colorTextBox.backgroundTintList?.defaultColor ?: Color.YELLOW)

                textBlinkAnimator = createBlinkAnimator(originalColor, blinkColor){
                    previewText.setTextColor(it)
                }
                textBlinkAnimator.start()
            } else {
                // Tắt nhấp nháy -> khôi phục màu gốc
                previewText.setTextColor(
                    colorText.backgroundTintList?.defaultColor ?: Color.YELLOW
                )
            }
            updateBlinkSpeedVisibility()
        }
        checkBackgroundBlink.setOnCheckedChangeListener { _, isChecked ->
            bgBlinkAnimator?.cancel()
            if (isChecked) {
                val originalColor = (previewText.background as? ColorDrawable)?.color ?: Color.BLACK
                val blinkColor = colorBox.backgroundTintList?.defaultColor ?: Color.CYAN
                bgBlinkAnimator = createBlinkAnimator(originalColor, blinkColor) {
                    previewText.setBackgroundColor(it)
                }
                bgBlinkAnimator?.start()
            } else {
                previewText.setBackgroundColor(
                    colorBackground.backgroundTintList?.defaultColor ?: Color.BLACK
                )
            }
            updateBlinkSpeedVisibility()
        }

        seekBlinkSpeed.max = 100
        seekBlinkSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (checkTextBlink.isChecked) {
                    checkTextBlink.isChecked = false
                    checkTextBlink.isChecked = true
                }
                if (checkBackgroundBlink.isChecked) {
                    checkBackgroundBlink.isChecked = false
                    checkBackgroundBlink.isChecked = true
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startScrolling(
        scrollView: HorizontalScrollView,
        textView: TextView,
        isLeftDirection: Boolean
    ) {
        scrollAnimator?.cancel()

        originalText = textView.text.toString()
        if (originalText.isEmpty()) return

        // Duplicate nhiều lần để cuộn mượt hơn
        val duplicatedText = "$originalText     $originalText     $originalText     $originalText"
        textView.text = duplicatedText

        textView.post {
            val textWidth = textView.width / 4 // vì bạn duplicate 3 lần
            val viewWidth = scrollView.width
            val distance = textWidth

            if (distance <= viewWidth) return@post

            val pixelsPerSecond = 100f // Điều chỉnh để có tốc độ mượt
            val durationMs = (distance / pixelsPerSecond * 1000).toLong()

            scrollAnimator = if (isLeftDirection) {
                ValueAnimator.ofInt(0, distance).apply {
                    addUpdateListener { animator ->
                        val value = animator.animatedValue as Int
                        scrollView.scrollTo(value, 0)
                    }
                }
            } else {
                scrollView.scrollTo(distance, 0)
                ValueAnimator.ofInt(distance, 0).apply {
                    addUpdateListener { animator ->
                        val value = animator.animatedValue as Int
                        scrollView.scrollTo(value, 0)
                    }
                }
            }

            scrollAnimator?.apply {
                duration = durationMs
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                start()
            }
        }
    }
    fun createBlinkAnimator(startColor: Int, blinkColor: Int, onUpdate: (Int) -> Unit): ValueAnimator {
        val minDuration = 100L   // Nhanh nhất
        val maxDuration = 2000L  // Chậm nhất
        val progress = seekBlinkSpeed.progress.coerceIn(0, 100)

        val durationMs = maxDuration - ((maxDuration - minDuration) * (progress / 100f)).toLong()

        return ValueAnimator.ofArgb(startColor, blinkColor).apply {
            duration = durationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                onUpdate(animator.animatedValue as Int)
            }
        }
    }
}