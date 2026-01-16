package com.study.googletranslatedemo.activity

import android.content.Intent
import com.study.googletranslatedemo.adapter.DemoFuncAdapter
import com.study.googletranslatedemo.base.BaseAct
import com.study.googletranslatedemo.data.DemoFunc
import com.study.googletranslatedemo.databinding.ActivityMainBinding


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
            DemoFunc.MaskFilter -> gotoAct(FilterActivity::class.java)
            DemoFunc.BaseAdjust -> gotoAct(AdjustActivity::class.java)
            DemoFunc.ShapeMask -> gotoAct(ShapeMaskActivity::class.java)
            DemoFunc.FilterMask -> gotoAct(FilterMaskActivity::class.java)
            DemoFunc.Crop -> gotoAct(CropActivity::class.java)
        }
    }

    private fun gotoAct(act: Class<*>) {
        startActivity(Intent(this, act))
    }
}