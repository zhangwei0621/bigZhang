package com.study.googletranslatedemo.act

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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.study.googletranslatedemo.databinding.ActivityIdentifyWordBinding

/**
 * author ZhangWei
 * date 2025-01-23
 */
class IndentifyWordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIdentifyWordBinding
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
        binding = ActivityIdentifyWordBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.btnIdentifyWord.setOnClickListener { startIdentifyWord() }
        binding.ivWordImg.setOnClickListener { launcher.launch("image/*") }
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
                Toast.makeText(this, "识别失败${e}", Toast.LENGTH_SHORT).show()
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