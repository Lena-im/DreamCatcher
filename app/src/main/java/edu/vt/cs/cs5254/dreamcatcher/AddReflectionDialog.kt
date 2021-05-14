package edu.vt.cs.cs5254.dreamcatcher

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import edu.vt.cs.cs5254.dreamcatcher.util.KeyboardUtil.Companion.hideSoftKeyboard

class AddReflectionDialog: DialogFragment() {

    interface Callbacks {
        fun onReflectionProvided(reflectionText: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewGroup = requireActivity().findViewById(android.R.id.content) as ViewGroup
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_add_reflection, viewGroup, false)
        val textView = view.findViewById(R.id.reflection_text) as EditText
        val okListener = DialogInterface.OnClickListener { _, _ ->
            targetFragment?.let { fragment ->
                (fragment as Callbacks).onReflectionProvided(textView.text.toString())
            }
            hideSoftKeyboard(requireContext(), view)
        }
        val cancelListener = DialogInterface.OnClickListener { _, _ ->
            hideSoftKeyboard(requireContext(), view)
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Add Reflection")
            .setPositiveButton(android.R.string.ok, okListener)
            .setNegativeButton(android.R.string.cancel, cancelListener)
            .create()
    }
}