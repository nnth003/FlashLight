package com.example.flashlight.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.TextView
import com.example.flashlight.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [ScrollerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScrollerFragment : Fragment() {
    // TODO: Rename and change types of parameters


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
        val horizontalScroll = view.findViewById<HorizontalScrollView>(R.id.horizontalScroll)

        previewText.isSelected = true
        previewText.setHorizontallyScrolling(true)
        previewText.movementMethod = ScrollingMovementMethod()

        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cập nhật TextView mỗi khi người dùng nhập
                previewText.text = s.toString()

                previewText.post {
                    horizontalScroll.smoothScrollTo(previewText.width, 0)
                }
            }

            override fun afterTextChanged(s: Editable?) {  }
        })
    }
}