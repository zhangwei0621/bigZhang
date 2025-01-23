package com.study.googletranslatedemo.act

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.study.googletranslatedemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.apply {
            btnTranslate.setOnClickListener {
                startActivity(Intent(this@MainActivity, TranslateActivity::class.java))
            }
            btnIdentifyLanguage.setOnClickListener {
                startActivity(Intent(this@MainActivity, IndentifyLanguageActivity::class.java))
            }
            btnIdentifyWord.setOnClickListener {
                startActivity(Intent(this@MainActivity, IndentifyWordActivity::class.java))
            }
        }
    }

}