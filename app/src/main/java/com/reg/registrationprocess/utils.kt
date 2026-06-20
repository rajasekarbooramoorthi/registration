package com.reg.registrationprocess

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach

fun ViewGroup.deepForEach(function: View.() -> Unit) {
    this.forEach { child ->
        child.function()
        if (child is ViewGroup) {
            child.deepForEach(function)
        }
    }
}

fun ViewGroup.findToolbar(): Toolbar? {
    var foundToolbar: Toolbar? = null
    deepForEach {
        if (this is Toolbar && foundToolbar == null) {
            foundToolbar = this
        }
    }
    return foundToolbar
}

fun Activity.setToolbarInsetsActivity() {
    //val decorView = window.decorView as ViewGroup
    // val toolbar = decorView.findToolbar()
    // toolbar?.setInsets()
    findViewById<View?>(android.R.id.content)?.apply {
        applyBottomInsets()
    }
}

fun Activity.setToolbarInsetsFullscreen() {
    val decorView = window.decorView as ViewGroup
    val toolbar = decorView.findToolbar()
    toolbar?.setInsets()
    findViewById<View?>(android.R.id.content)?.apply {
        applyBottomTopInsets()
    }
}


fun View.setInsets() {
    this.let { view ->
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )

            insets
        }
    }
}

fun View.applyBottomInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            view.paddingLeft,
            systemBars.top,
            view.paddingRight,
            systemBars.bottom
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyBottomTopInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            view.paddingLeft,
            systemBars.top,
            view.paddingRight,
            systemBars.bottom
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyNoPaddingInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            view.paddingBottom
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun Activity.setToolbarInsetsFragment() {
    val decorView = window.decorView as ViewGroup
    val toolbar = decorView.findToolbar()
    toolbar?.setInsets()
}
