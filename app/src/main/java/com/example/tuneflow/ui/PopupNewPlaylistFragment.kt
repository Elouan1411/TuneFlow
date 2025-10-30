package com.example.tuneflow.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.example.tuneflow.R

class PopupNewPlaylistFragment(
    private val onCreate: (String) -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setStyle(STYLE_NO_TITLE, R.color.transparent)
        return inflater.inflate(R.layout.popup_new_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editText = view.findViewById<EditText>(R.id.editPlaylistName)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnCreate = view.findViewById<LinearLayout>(R.id.btnCreate)
        val btnClose = view.findViewById<ImageView>(R.id.btnClosePopup)

        btnCancel.setOnClickListener { dismiss() }
        btnClose.setOnClickListener { dismiss() }

        btnCreate.setOnClickListener {
            val name = editText.text.toString().trim()
            if (name.isNotEmpty()) {
                onCreate(name)
                dismiss()
            } else {
                editText.error = "Veuillez entrer un nom"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}
