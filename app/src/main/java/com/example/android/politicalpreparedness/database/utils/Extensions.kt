package com.example.android.politicalpreparedness.database.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.android.politicalpreparedness.R

///**
// * Extension function to setup the RecyclerView
// */
//fun <T> RecyclerView.setup(
//    adapter: ElectionListAdapter<T>
//) {
//    this.apply {
//        layoutManager = LinearLayoutManager(this.context)
//        this.adapter = adapter
//    }
//}

fun Fragment.setTitle(title: String) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }
}

fun Fragment.setDisplayHomeAsUpEnabled(bool: Boolean) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(
            bool
        )
    }
}

// animate changing the view visibility
fun View.fadeIn() {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeIn.alpha = 1f
        }
    })
}

// animate changing the view visibility
fun View.fadeOut() {
    this.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeOut.alpha = 1f
            this@fadeOut.visibility = View.GONE
        }
    })
}

fun Fragment.showLoadingAnimation() {
    view?.findViewById<ProgressBar>(R.id.progressBar)?.visibility = View.VISIBLE
}

fun Fragment.hideLoadingAnimation() {
    view?.findViewById<ProgressBar>(R.id.progressBar)?.visibility = View.GONE
}
