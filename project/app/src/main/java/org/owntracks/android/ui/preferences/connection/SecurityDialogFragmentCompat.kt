package org.owntracks.android.ui.preferences.connection

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.rengwuxian.materialedittext.MaterialEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.owntracks.android.R
import timber.log.Timber


class SecurityDialogFragmentCompat constructor(
        key: String,
        private val model: Model,
        private val positiveCallback: (Model) -> Unit
) :
        PreferenceDialogFragmentCompatWithKey(key) {
    data class Model(
            internal val tlsEnabled: Boolean,
            internal val tlsCaCert: String,
            internal val tlsClientCert: String,
            internal val tlsClientCertPassword: String
    )

    private var tlsField: SwitchCompat? = null
    private var tlsCaCertField: EditText? = null
    private var tlsClientCertField: EditText? = null
    private var tlsClientCrtPasswordField: EditText? = null
    override val validatedFields: List<MaterialEditText?> = emptyList()

    private fun uriToFilename(uri: Uri): String {
        if (uri.scheme.equals("content")) {
            requireContext().applicationContext.contentResolver.query(uri, null, null, null, null)
                    .use { cursor ->
                        if (cursor != null && cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index < 0) {
                                throw IndexOutOfBoundsException("DISPLAY_NAME column not present in data store")
                            }
                            return cursor.getString(index)
                        }
                    }
        }
        return "" // STOPSHIP: 23/07/2021
//        if (result == null) {
//            result = uri.path
//            val cut = result!!.lastIndexOf('/')
//            if (cut != -1) {
//                result = result!!.substring(cut + 1)
//            }
//        }
//        return result
    }

    private val something = registerForActivityResult(ActivityResultContracts.GetContent()) {
        lifecycleScope.launch(Dispatchers.IO) {
            Timber.v("CopyTask with URI: %s", it)
            val filename: String = uriToFilename(it)
            Timber.v("filename for save is: %s", filename)
            requireContext().applicationContext.contentResolver.openInputStream(it)
                    .use { inputStream ->
                        requireContext().applicationContext.openFileOutput(
                                filename,
                                Context.MODE_PRIVATE
                        )
                                .use { outputStream ->
                                    val buffer = ByteArray(256)
                                    var bytesRead: Int
                                    while (inputStream!!.read(buffer).also { bytesRead = it } != -1) {
                                        outputStream.write(buffer, 0, bytesRead)
                                    }
                                }
                    }
            Timber.v("copied file to private storage: %s", filename)
        }
    }

    private fun certificateFieldPopupMenu(
            textField: EditText,
            activityResultLauncher: ActivityResultLauncher<String>
    ) = PopupMenu(requireContext(), textField).apply {
        menuInflater.inflate(R.menu.picker, menu)
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clear -> {
                    textField.setText("")
                    true
                }
                R.id.select -> {
                    activityResultLauncher.launch("*/*")
                    true
                }
                else -> true
            }
        }
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        tlsCaCertField =
                view?.findViewById<EditText>(R.id.tlsCaCrt)?.apply {
                    setText(model.tlsCaCert)
                    val popup = certificateFieldPopupMenu(this, something)
                    setOnClickListener {
                        popup.show()
                    }
                }
        tlsClientCertField =
                view?.findViewById<EditText>(R.id.tlsClientCrt)?.apply { setText(model.tlsClientCert) }
        tlsClientCrtPasswordField =
                view?.findViewById<EditText>(R.id.tlsClientCrtPassword)
                        ?.apply { setText(model.tlsClientCertPassword) }

        tlsField =
                view?.findViewById<SwitchCompat>(R.id.tls)?.apply {
                    isChecked = model.tlsEnabled
                    setCertificateFieldVisibilities(isChecked)
                    setOnClickListener { switch ->
                        setCertificateFieldVisibilities((switch as SwitchCompat).isChecked)
                    }
                }
    }

    private fun setCertificateFieldVisibilities(checked: Boolean) =
            listOf(
                    tlsCaCertField,
                    tlsClientCertField,
                    tlsClientCrtPasswordField
            ).forEach {
                it?.visibility =
                        if (checked) View.VISIBLE else View.GONE
            }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            ifLet(
                    tlsField,
                    tlsCaCertField,
                    tlsClientCertField,
                    tlsClientCrtPasswordField
            ) { (tlsField, tlsCaCertField, tlsClientCertField, tlsClientCrtPasswordField) ->
                positiveCallback(
                        Model(
                                (tlsField as SwitchCompat).isChecked,
                                tlsCaCertField.text.toString(),
                                tlsClientCertField.text.toString(),
                                tlsClientCrtPasswordField.text.toString()
                        )
                )
            }
        }
    }
}