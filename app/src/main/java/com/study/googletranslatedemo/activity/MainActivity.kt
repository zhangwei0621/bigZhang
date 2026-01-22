package com.study.googletranslatedemo.activity

import android.content.Intent
import com.study.googletranslatedemo.adapter.DemoFuncAdapter
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.data.DemoFunc
import com.study.googletranslatedemo.databinding.ActivityMainBinding
import com.study.googletranslatedemo.demo.editor.adjust.AdjustActivity
import com.study.googletranslatedemo.demo.editor.blur.BlurImpl
import com.study.googletranslatedemo.demo.editor.crop.CropActivity
import com.study.googletranslatedemo.demo.editor.draw.DrawActivity
import com.study.googletranslatedemo.demo.editor.draw.MosaicDrawActivity
import com.study.googletranslatedemo.demo.editor.effect.splash.FilterPaintActivity
import com.study.googletranslatedemo.demo.editor.filter.FilterActivity
import com.study.googletranslatedemo.demo.editor.frame.FrameActivity
import com.study.googletranslatedemo.demo.editor.sqbg.SqbgActivity
import com.study.googletranslatedemo.demo.editor.sticker.StickerActivity
import com.study.googletranslatedemo.utils.filter.FilterRule

class MainActivity : BaseAct<ActivityMainBinding>() {
    override fun getViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        binding.rvFunc.adapter = DemoFuncAdapter(
            list = DemoFunc.entries,
            onClick = { gotoFunc(it) }
        )
    }

    private fun gotoFunc(func: DemoFunc) {
        when (func) {
            DemoFunc.DevelopFunction -> gotoAct(DevelopActivity::class.java)
            DemoFunc.EditorCrop -> gotoAct(CropActivity::class.java)
            DemoFunc.EditorFilter -> gotoAct(FilterActivity::class.java)
            DemoFunc.EditorAdjust -> gotoAct(AdjustActivity::class.java)
            DemoFunc.EditorEffectSplash -> FilterPaintActivity.start(this, FilterRule.BLACK)
            DemoFunc.EditorFrame -> gotoAct(FrameActivity::class.java)
            DemoFunc.EditorDraw -> gotoAct(DrawActivity::class.java)
            DemoFunc.EditorDrawMosaic -> gotoAct(MosaicDrawActivity::class.java)
            DemoFunc.EditorBlur -> BlurImpl.start(this)
            DemoFunc.EditorSticker -> gotoAct(StickerActivity::class.java)
            DemoFunc.EditorSqbg -> gotoAct(SqbgActivity::class.java)
        }
    }

    private fun gotoAct(act: Class<*>) {
        startActivity(Intent(this, act))
    }
}