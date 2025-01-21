package com.study.googletranslatedemo

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.study.googletranslatedemo.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var isSupport = false
    private var translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
    ).apply {
        downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener {
                isSupport = true
            }
            .addOnFailureListener {
                isSupport = false
            }
    }
    private lateinit var binding: ActivityMainBinding
    private val launcher =
        registerForActivityResult(GetContent()) { result ->
            result?.let {
                Glide.with(this)
                    .load(it)
                    .into(binding.ivWordImg)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(translator)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.apply {
            ivWordImg.setOnClickListener {
                launcher.launch("image/*")
            }
            btnTranslate.setOnClickListener {
                if (isSupport)
                    translator
                        .translate(etBefore.text.toString())
                        .addOnSuccessListener { translatedText ->
                            etAfter.setText(translatedText)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@MainActivity, "翻译失败${e}", Toast.LENGTH_SHORT)
                                .show()
                        }
                else
                    Toast.makeText(this@MainActivity, "暂不支持", Toast.LENGTH_SHORT).show()
            }
            btnIdentifyLanguage.setOnClickListener {
                LanguageIdentification.getClient(
                    LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(0.1f)
                        .build()
                )
                    .identifyLanguage(etInputLanguage.text.toString())
                    .addOnSuccessListener { languageCode ->
                        if (languageCode == "und") {
                            Toast.makeText(this@MainActivity, "识别异常", Toast.LENGTH_SHORT).show()
                        } else {
                            etOutputLanguage.setText(Locale.forLanguageTag(languageCode).displayName)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@MainActivity, "识别失败${e}", Toast.LENGTH_SHORT).show()
                    }
            }
            btnIdentifyWord.setOnClickListener {
                TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    .process(InputImage.fromBitmap(ivWordImg.drawable.toBitmap(), 0))
                    .addOnSuccessListener { visionText ->
                        etOutputWord.setText(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@MainActivity, "识别失败${e}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}