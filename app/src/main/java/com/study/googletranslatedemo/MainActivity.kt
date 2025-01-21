package com.study.googletranslatedemo

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.study.googletranslatedemo.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var isSupportTranslator = false
    private var isInitTranslatorPosition = true
    private var translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
    ).apply {
        downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener {
                isSupportTranslator = true
            }
            .addOnFailureListener {
                isSupportTranslator = false
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
            ivChange.setOnClickListener { swapLanguages() }
            ivWordImg.setOnClickListener { launcher.launch("image/*") }
            //开始翻译
            btnTranslate.setOnClickListener { startTranslate() }
            //开始语种识别
            btnIdentifyLanguage.setOnClickListener { startIdentifyLanguage() }
            //开始文字识别
            btnIdentifyWord.setOnClickListener { startIdentifyWord() }
        }
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
                    Toast.makeText(this@MainActivity, "翻译失败${e}", Toast.LENGTH_SHORT)
                        .show()
                }
        else
            Toast.makeText(this@MainActivity, "暂不支持", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@MainActivity, "识别异常", Toast.LENGTH_SHORT).show()
                } else {
                    binding.etOutputLanguage.setText(Locale.forLanguageTag(languageCode).displayName)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, "识别失败${e}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * 开始识别图片文本
     */
    private fun startIdentifyWord() {
        val bitmap = binding.ivWordImg.drawable.toBitmap()
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            .process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { visionText ->
                // 绘制高亮区域
                drawTextHighlight(bitmap, visionText)
                //展示识别文本
                binding.etOutputWord.setText(visionText.text)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, "识别失败${e}", Toast.LENGTH_SHORT).show()
            }
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
        // 动画结束后切换文字内容
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                val temp = binding.tvBefore.text
                binding.tvBefore.text = binding.tvAfter.text
                binding.tvAfter.text = temp

                val temp2 = binding.etBefore.text
                binding.etBefore.text = binding.etAfter.text
                binding.etAfter.text = temp2
            }
        })

        isInitTranslatorPosition = !isInitTranslatorPosition
        isSupportTranslator = false
        translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(if (isInitTranslatorPosition) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE)
                .setTargetLanguage(if (isInitTranslatorPosition) TranslateLanguage.CHINESE else TranslateLanguage.ENGLISH)
                .build()
        ).apply {
            downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
                .addOnSuccessListener {
                    isSupportTranslator = true
                }
                .addOnFailureListener {
                    isSupportTranslator = false
                }
        }
    }

    /**
     * 高亮图片识别文本块
     */
    private fun drawTextHighlight(bitmap: Bitmap, text: Text) {
        // 创建一个新的 Bitmap 作为输出
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // 设置绘制样式
        val paint = Paint().apply {
            color = Color.argb(120, 255, 255, 0) // 半透明黄色
            style = Paint.Style.FILL
        }

        // 设置绘制边框样式
        val borderPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        // 遍历识别结果
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    // 绘制高亮矩形
                    val rect = element.boundingBox
                    if (rect != null) {
                        canvas.drawRect(rect, paint)    // 填充颜色
                        canvas.drawRect(rect, borderPaint) // 红色边框
                    }
                }
            }
        }

        binding.ivWordImg.setImageBitmap(mutableBitmap)
    }
}