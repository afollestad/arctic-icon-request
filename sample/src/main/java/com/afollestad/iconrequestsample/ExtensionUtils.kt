@file:JvmName("RxViews")

package com.afollestad.iconrequestsample

import android.support.v4.view.ViewCompat
import android.view.View
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plusAssign(subscription: Disposable) {
  add(subscription)
}

val View.isAttachedToWindowCompat: Boolean get() = ViewCompat.isAttachedToWindow(this)

fun View.unsubscribeOnDetach(subscriptionFactory: () -> Disposable) {
  val attachedSubs = ensureAttachedSubscriptions()
  if (isAttachedToWindowCompat) {
    // Run lambda synchronously, and after that, check again whether the view is still attached.
    val sub = subscriptionFactory()
    if (isAttachedToWindowCompat) {
      // Since the view is still attached, register the subscription.
      attachedSubs.subs += sub
    } else {
      // Since the view got detached, the subscription must be unsubscribed immediately,
      // otherwise it may leak!
      sub.dispose()
    }
  } else {
    // Defer lambda execution until the view is attached to window.
    attachedSubs += subscriptionFactory
  }
}

private fun View.ensureAttachedSubscriptions(): AttachedSubscriptions {
  var attachedSubs = getTag(R.id.tag_attached_subscriptions) as AttachedSubscriptions?
  if (attachedSubs == null) {
    attachedSubs = AttachedSubscriptions()
    setTag(R.id.tag_attached_subscriptions, attachedSubs)
    addOnAttachStateChangeListener(attachedSubs)
  }
  return attachedSubs
}

private class AttachedSubscriptions : View.OnAttachStateChangeListener {
  val subs = CompositeDisposable()
  private val subscriptionFactories by lazy { mutableListOf<() -> Disposable>() }

  operator fun plusAssign(subscriptionFactory: () -> Disposable) {
    subscriptionFactories += subscriptionFactory
  }

  override fun onViewAttachedToWindow(v: View) {
    subscriptionFactories.apply {
      forEach { factory -> subs += factory() }
      clear()
    }
  }

  override fun onViewDetachedFromWindow(v: View) = subs.clear()
}

fun Disposable.unsubscribeOnDetach(view: View) {
  kotlin.check(
      view.isAttachedToWindowCompat
  ) { "Expected view to already be attached to a window." }
  view.unsubscribeOnDetach { this }
}