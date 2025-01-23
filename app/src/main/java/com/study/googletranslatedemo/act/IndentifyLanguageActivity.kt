package com.study.googletranslatedemo.act

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.study.googletranslatedemo.databinding.ActivityIdentifyLanguageBinding
import java.util.Locale

/**
 * author ZhangWei
 * date 2025-01-23
 */
class IndentifyLanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIdentifyLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentifyLanguageBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.btnIdentifyLanguage.setOnClickListener { startIdentifyLanguage() }
    }

    /**
     * 开始识别语种
     */
    private fun startIdentifyLanguage() {
        LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder()
                .setConfidenceThreshold(0.1f)
                .build()
        )
            .identifyLanguage(binding.etInputLanguage.text.toString())
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    Toast.makeText(this, "识别异常", Toast.LENGTH_SHORT).show()
                } else {
                    binding.etOutputLanguage.setText(Locale.forLanguageTag(languageCode).displayName)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "识别失败${e}", Toast.LENGTH_SHORT).show()
            }
    }
}