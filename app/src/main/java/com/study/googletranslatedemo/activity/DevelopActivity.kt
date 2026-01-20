package com.study.googletranslatedemo.activity

import android.content.Intent
import android.util.Log
import com.study.googletranslatedemo.adapter.DevelopFuncAdapter
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.data.DevelopFunc
import com.study.googletranslatedemo.databinding.ActivityDevelopBinding
import com.study.googletranslatedemo.develop.decode.DecodeHelper
import com.study.googletranslatedemo.develop.filtermask.FilterMaskActivity
import com.study.googletranslatedemo.develop.paint.PaintActivity
import com.study.googletranslatedemo.develop.shapemask.ShapeMaskActivity

/**
 * 开发技术测试，这里不直接做demo功能，只做一些核心逻辑的测试
 */
class DevelopActivity : BaseAct<ActivityDevelopBinding>() {
    override fun getViewBinding() = ActivityDevelopBinding.inflate(layoutInflater)

    override fun initView() {
        binding.rvFunc.adapter = DevelopFuncAdapter(
            list = DevelopFunc.entries,
            onClick = { gotoFunc(it) }
        )
    }

    private fun gotoFunc(func: DevelopFunc) {
        when (func) {
            DevelopFunc.ShapeMask -> gotoAct(ShapeMaskActivity::class.java)
            DevelopFunc.FilterMask -> gotoAct(FilterMaskActivity::class.java)
            DevelopFunc.Decode -> Log.d("", "${DecodeHelper().test()}")
            DevelopFunc.Paint -> gotoAct(PaintActivity::class.java)
        }
    }

    private fun gotoAct(act: Class<*>) {
        startActivity(Intent(this, act))
    }
}