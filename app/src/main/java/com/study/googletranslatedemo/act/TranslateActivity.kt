package com.study.googletranslatedemo.act

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.study.googletranslatedemo.dialog.ILanguageDialog
import com.study.googletranslatedemo.dialog.LanguageListDialog
import com.study.googletranslatedemo.databinding.ActivityTranslateBinding
import java.util.Locale

/**
 * author ZhangWei
 * date 2025-01-23
 */
class TranslateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTranslateBinding
    private lateinit var translator: Translator
    private var isSupportTranslator = false
    private var isNormal = true
    private var currentSourceLanguage: String = TranslateLanguage.ENGLISH
    private var currentTargetLanguage: String = TranslateLanguage.CHINESE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranslateBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
        initTranslator()
        lifecycle.addObserver(translator)
    }

    private fun initTranslator() {
        isSupportTranslator = false
        val sourceLanguage = if (isNormal) currentSourceLanguage else currentTargetLanguage
        val targetLanguage = if (isNormal) currentTargetLanguage else currentSourceLanguage
        translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
        ).apply {
            downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
                .addOnSuccessListener {
                    binding.etBefore.setText("")
                    binding.etAfter.setText("")
                    binding.tvBefore.text = Locale.forLanguageTag(sourceLanguage).displayName
                    binding.tvAfter.text = Locale.forLanguageTag(targetLanguage).displayName
                    isSupportTranslator = true
                    Log.i("TAG", "Success : ")
                }
                .addOnFailureListener { e ->
                    Log.i("TAG", "Failure :" + e.message)
                    isSupportTranslator = false
                }
        }
    }

    private fun initView() {
        //开始翻译
        binding.btnTranslate.setOnClickListener { startTranslate() }
        binding.ivChange.setOnClickListener { swapLanguages() }
        binding.tvBefore.setOnClickListener {
            downloadLanguageList(true)
        }
        binding.tvAfter.setOnClickListener {
            downloadLanguageList(false)
        }
    }

    /**
     * 下载语言列表
     */
    private fun downloadLanguageList(isBefore: Boolean) {
        LanguageListDialog.createDialog(
            if (isBefore) {
                if (isNormal) currentSourceLanguage else currentTargetLanguage
            } else {
                if (isNormal) currentTargetLanguage else currentSourceLanguage
            }, object : ILanguageDialog {
                override fun changeLanguage(string: String) {
                    if (isBefore) {
                        if (isNormal) {
                            currentSourceLanguage = string
                        } else {
                            currentTargetLanguage = string
                        }
                    } else {
                        if (isNormal) {
                            currentTargetLanguage = string
                        } else {
                            currentSourceLanguage = string
                        }
                    }
                    initTranslator()
                }
            }).show(supportFragmentManager, null)
    }

    /**
     * 开始翻译
     */
    private fun startTranslate() {
        if (isSupportTranslator)
            translator.translate(binding.etBefore.text.toString())
                .addOnSuccessListener { translatedText ->
                    binding.etAfter.setText(translatedText)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "翻译失败${e}", Toast.LENGTH_SHORT)
                        .show()
                }
        else
            Toast.makeText(this, "暂不支持", Toast.LENGTH_SHORT).show()
    }

    /**
     * 交换语言翻译语言位置
     */
    private fun swapLanguages() {
        // 动画：滑动
        val leftOut = ObjectAnimator.ofFloat(
            binding.tvBefore, "translationX", 0f, -500f
        )
        val leftIn = ObjectAnimator.ofFloat(
            binding.tvBefore, "translationX", 500f, 0f
        )
        val rightOut = ObjectAnimator.ofFloat(
            binding.tvAfter, "translationX", 0f, 500f
        )
        val rightIn = ObjectAnimator.ofFloat(
            binding.tvAfter, "translationX", -500f, 0f
        )
        // 动画：透明度渐变
        val leftAlphaOut = ObjectAnimator.ofFloat(
            binding.tvBefore, "alpha", 1f, 0f
        )
        val leftAlphaIn = ObjectAnimator.ofFloat(
            binding.tvBefore, "alpha", 0f, 1f
        )
        val rightAlphaOut = ObjectAnimator.ofFloat(
            binding.tvAfter, "alpha", 1f, 0f
        )
        val rightAlphaIn = ObjectAnimator.ofFloat(
            binding.tvAfter, "alpha", 0f, 1f
        )

        // 动画集合
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(leftOut, rightOut, leftAlphaOut, rightAlphaOut)
        animatorSet.playTogether(leftIn, rightIn, leftAlphaIn, rightAlphaIn)
        animatorSet.duration = 500
        animatorSet.start()
        isNormal = !isNormal
        initTranslator()
    }
}