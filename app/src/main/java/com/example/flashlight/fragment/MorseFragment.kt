package com.example.flashlight.fragment

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashlight.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private val morseMap = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.",
    'D' to "-..", 'E' to ".", 'F' to "..-.",
    'G' to "--.", 'H' to "....", 'I' to "..",
    'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---",
    'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
    'S' to "...", 'T' to "-", 'U' to "..-",
    'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---",
    '3' to "...--", '4' to "....-", '5' to ".....",
    '6' to "-....", '7' to "--...", '8' to "---..",
    '9' to "----.", ' ' to "/"
)


/**
 * A simple [Fragment] subclass.
 * Use the [MorseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MorseFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var isFlashEnabled = false
    private var isSoundEnabled = false
    private var isLoopEnabled = false
    private var isPlaying = false
    private var isPaused = false
    private var currentIndex = 0
    private lateinit var playButton: ImageButton
    private lateinit var inputText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_morse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputText = view.findViewById<EditText>(R.id.eT)
        val morseOutput = view.findViewById<EditText>(R.id.etOutput)
        val soundBtn = view.findViewById<ImageButton>(R.id.soundBtn)
        val flashBtn = view.findViewById<ImageButton>(R.id.btnFlash)
        val loopBtn = view.findViewById<ImageButton>(R.id.loopBtn)
        playButton = view.findViewById<ImageButton>(R.id.btnPlay)

//        morseOutput.setHorizontallyScrolling(true)
        morseOutput.movementMethod = ScrollingMovementMethod()

        inputText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val morse = textToMorse(s.toString())
                morseOutput.setText(morse)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        soundBtn.setOnClickListener {
            isSoundEnabled = !isSoundEnabled
            val icon =
                if (isSoundEnabled) R.drawable.baseline_volume_up_32 else R.drawable.outline_volume_off_32
            soundBtn.setImageResource(icon)
        }

        flashBtn.setOnClickListener {
            isFlashEnabled = !isFlashEnabled
            val icon =
                if (isFlashEnabled) R.drawable.baseline_flash_on_32 else R.drawable.baseline_flash_off_32
            flashBtn.setImageResource(icon)
        }



        loopBtn.setOnClickListener {
            isLoopEnabled = !isLoopEnabled
            val icon = if (isLoopEnabled) R.drawable.sync_32 else R.drawable.sync_disable_32
            loopBtn.setImageResource(icon)
        }

        playButton.setOnClickListener {
            val morseCode = morseOutput.text.toString()

            if (!isPlaying) {
                // Bắt đầu mới
                currentIndex = 0
                isPaused = false
                isPlaying = true
                inputText.isEnabled = false
                updatePlayButtonIcon(true)
                playMorse(morseCode, morseOutput, requireContext())
            } else {
                // Nếu đang phát thì pause hoặc resume
                isPaused = !isPaused
                updatePlayButtonIcon(!isPaused)
                inputText.isEnabled = true
            }
        }

    }

    private fun textToMorse(text: String): String {
        return text.uppercase().mapNotNull { morseMap[it] }.joinToString(" ")
    }

    private fun playMorse(morse: String, morseOutput: EditText, context: Context) {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]

        isPlaying = true

        lifecycleScope.launch(Dispatchers.Default){
            do {
                while (currentIndex < morse.length) {
                    if (!isPlaying) break
                    if (isPaused) {
                        Thread.sleep(100)
                        continue
                    }

                    val char = morse[currentIndex]

                    withContext(Dispatchers.Main) {
                        val spannable = SpannableString(morse)
                        if (currentIndex > 0) {
                            spannable.setSpan(
                                ForegroundColorSpan(Color.BLUE),
                                0, currentIndex,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        spannable.setSpan(
                            ForegroundColorSpan(Color.RED),
                            currentIndex, currentIndex + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        morseOutput.setText(spannable)
                        morseOutput.setSelection(currentIndex)
                    }

                    when (char) {
                        '.' -> {
                            if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_DTMF_1, 150)
                            sleepWithFlashSync(150, cameraManager, cameraId)
                            Thread.sleep(150)
                        }

                        '-' -> {
                            if (isSoundEnabled) toneGen.startTone(ToneGenerator.TONE_DTMF_1, 400)
                            sleepWithFlashSync(400, cameraManager, cameraId)
                            Thread.sleep(200)
                        }

                        ' ' -> Thread.sleep(200)
                        '/' -> Thread.sleep(600)
                    }

                    Thread.sleep(100)
                    currentIndex++
                }

                // Reset text + trạng thái nếu xong
                if (!isLoopEnabled) {
                    isPlaying = false
                    isPaused = false
                    currentIndex = 0
                    withContext(Dispatchers.Main) {
                        morseOutput.setText(morse)
                        val spannable = SpannableString(morse)
                        spannable.setSpan(
                            ForegroundColorSpan(Color.GREEN),
                            0, morse.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        inputText.isEnabled = true
                    }
                    updatePlayButtonIcon(false)
                } else {
                    currentIndex = 0
                }

                Thread.sleep(500)

            } while (isPlaying && isLoopEnabled)
        }.start()
    }

    private fun updatePlayButtonIcon(isPlayingNow: Boolean) {
        val icon =
            if (isPlayingNow) R.drawable.baseline_pause_32 else R.drawable.baseline_play_arrow_32
        requireActivity().runOnUiThread {
            playButton.setImageResource(icon)
        }
    }

    private suspend fun sleepWithFlashSync(
        duration: Long,
        cameraManager: CameraManager,
        cameraId: String
    ) {
        if (isFlashEnabled) {
            withContext(Dispatchers.Main){
                cameraManager.setTorchMode(cameraId, true)
            }
            delay(duration)
            withContext(Dispatchers.Main) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } else {
            Thread.sleep(duration)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
        isPaused = false
        currentIndex = 0
        inputText.isEnabled = true
    }

//    private fun playMorseFlash(morseCode: String, context: Context) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val cameraId = cameraManager.cameraIdList[0]
//
//        Thread {
//            for (symbol in morseCode) {
//                when (symbol) {
//                    '.' -> {
//                        cameraManager.setTorchMode(cameraId, true)
//                        Thread.sleep(150)
//                        cameraManager.setTorchMode(cameraId, false)
//                        Thread.sleep(150)
//                    }
//                    '-' -> {
//                        cameraManager.setTorchMode(cameraId, true)
//                        Thread.sleep(400)
//                        cameraManager.setTorchMode(cameraId, false)
//                        Thread.sleep(200)
//                    }
//                    ' ' -> Thread.sleep(200)
//                    '/' -> Thread.sleep(600)
//                }
//            }
//        }.start()
//    }

}