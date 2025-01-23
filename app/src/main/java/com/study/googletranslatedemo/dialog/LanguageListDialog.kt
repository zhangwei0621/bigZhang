package com.study.googletranslatedemo.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModel
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.study.googletranslatedemo.adapter.LanguageAdapter
import com.study.googletranslatedemo.databinding.DialogLanguageBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * author ZhangWei
 * date 2025-01-23
 */
class LanguageListDialog : DialogFragment() {
    companion object {
        var lan = "Sign"
        fun createDialog(currentLan: String, dialog: ILanguageDialog) = LanguageListDialog().apply {
            iLanguageDialog = dialog
            arguments = Bundle().apply {
                putString(lan, currentLan)
            }
        }
    }

    private lateinit var binding: DialogLanguageBinding
    private var currentLanText = ""
    private var iLanguageDialog: ILanguageDialog? = null
    private val deferred = CompletableDeferred<Unit>()
    private var curDownloadModel: MutableList<RemoteModel> = mutableListOf()
    private val languageAdapter = LanguageAdapter { languageData ->
        if (languageData.isDownload) {
            //已下载
            iLanguageDialog?.changeLanguage(languageData.name)
            dismissAllowingStateLoss()
        } else {
            //未下载
            RemoteModelManager.getInstance().download(
                languageData.remoteModel, DownloadConditions.Builder()
                    .requireWifi()
                    .build()
            ).addOnSuccessListener {
                Log.i("TAG", "onViewCreated: 下载成功")
                iLanguageDialog?.changeLanguage(languageData.name)
                dismissAllowingStateLoss()
            }.addOnFailureListener {
                Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
                Log.i("TAG", "onViewCreated: 下载失败")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentLanText = arguments?.getString(lan) ?: ""
        //获取已下载语言模型
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                curDownloadModel = models.toMutableList()
                deferred.complete(Unit)
            }.addOnFailureListener {
                deferred.complete(Unit)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLanguageBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            deferred.await()
            languageAdapter.updateData(TranslateLanguage.getAllLanguages().toList().map { lan ->
                val translateRemoteModel = TranslateRemoteModel.Builder(lan).build()
                LanguageData(
                    lan,
                    translateRemoteModel,
                    curDownloadModel.contains(translateRemoteModel)
                )
            })
        }
        binding.apply {
            currentLan.text = Locale.forLanguageTag(currentLanText).displayName
            binding.rvLan.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = languageAdapter
            }
        }
    }
}

interface ILanguageDialog {
    fun changeLanguage(string: String)
}

data class LanguageData(
    val name: String,
    val remoteModel: RemoteModel,
    val isDownload: Boolean
)