# Icon Request

This library allows icon pack dashboards to easily send icon requests
either traditionally via email, or through the [Arctic Manager](https://arcticmanager.com) system.

<img src="https://raw.githubusercontent.com/afollestad/arctic-icon-request/master/art/showcase2.png" width="450" />

---

# Gradle Dependency

[ ![Download](https://api.bintray.com/packages/drummer-aidan/maven/icon-request/images/download.svg) ](https://bintray.com/drummer-aidan/maven/icon-request/_latestVersion)
[![Build Status](https://travis-ci.org/afollestad/arctic-icon-request.svg)](https://travis-ci.org/afollestad/arctic-icon-request)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1eb8ed67c1f34eaf9bc176faeb6652bf)](https://www.codacy.com/app/drummeraidan_50/arctic-icon-request?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=afollestad/arctic-icon-request&amp;utm_campaign=Badge_Grade)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

Add this to your module's `build.gradle` file (make sure the version matches the jCenter badge above):

```gradle
dependencies {
	// ... other dependencies
	compile 'com.afollestad:icon-request:4.0.0'
}
```

---

# Table of Contents

1. [Creating a Request](https://github.com/afollestad/arctic-icon-request#creating-a-request)
2. [Options](https://github.com/afollestad/arctic-icon-request#options)
    1. [Saved Instance State](https://github.com/afollestad/arctic-icon-request#saved-instance-state)
    2. [UriTransformer](https://github.com/afollestad/arctic-icon-request#uritransformer)
    3. [ArcticConfig](https://github.com/afollestad/arctic-icon-request#arcticconfig)
3. [Load Unthemed Apps](https://github.com/afollestad/arctic-icon-request#loading-unthemed-apps)
4. [Selecting Apps](https://github.com/afollestad/arctic-icon-request#selecting-apps)
5. [Sending a Request](https://github.com/afollestad/arctic-icon-request#sending-a-request)
6. [Cleanup](https://github.com/afollestad/arctic-icon-request#cleanup)

---

# Creating a Request

To create a new Request object, you use the simple constructor. The only *required*
parameter is a `Context`.

```kotlin
val request = ArcticRequest(this)
```

---

# Options

There are more optional parameters that you can pass:

```kotlin
val request = ArcticRequest(
  context = this,
  savedInstanceState = savedInstanceState,
  config = config,
  uriTransformer = uriTransformer,
  onLoading = { },
  onLoadError = {
     // `it` is a Throwable
  },
  onLoaded = {
    // `it` is a List<AppModel>
  },
  onSelectionChange = {
    // `it` is a AppModel
  },
  onSending = { },
  onSendError = {
    // `it` is a Throwable
  },
  onSent = {
    // `it` is an Int (how many apps were sent)
  }
)
```

### Saved Instance State

Above, `savedInstanceState` is a `Bundle` received from `onCreate(...)` in your Activity
or Fragment which is used to restore your appfilter and app list state when recreating
your UI (e.g. after screen rotation). But you also need to make use of `saveInstanceState`
in order for that to work.

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
  request.saveInstance(outState)
  super.onSaveInstanceState(outState)
}
```

### UriTransformer

The `uriTransformer` allows you to modify the Uri pointing to a generated ZIP file before it
gets passed through an Intent to an email client. On newer versions of Android, apps can
only share files through `FileProvider` Uris. See the sample project for an example of how
you can transform a local file Uri into a `FileProvider` Uri.

### ArcticConfig

The `config` takes an `ArcticConfig` instance which has various optional parameters:

```kotlin
val config = ArcticConfig(
    cacheFolder = File(externalCacheDir, "com.afollestad.arctic"),
    appFilterName = "appfilter.xml",
    emailRecipient = "fake-email@helloworld.com",
    emailSubject = "Icon Request",
    emailHeader = "These apps aren't themed on my device!",
    emailFooter = "Hello world",
    includeDeviceInfo = true,
    errorOnInvalidDrawables = true,
    apiHost = "arcticmanager.com",
    apiKey = "ojtopiu23rp9u34p0iu43-9i4"
)
```

You can pass an `apiHost` and `apiKey` to integrate with [Arctic Request Manager](https://arcticmanager.com).

---

# Loading Unthemed Apps

With a configured `ArcticRequest` instance, you can load unthemed apps:

```kotlin
request.performLoad()
```

Your `onLoaded` callback will receive a List of unthemed apps. If an error occurs,
the `onLoadError` callback is invoked.

---

# Selecting Apps

Once you've loaded apps, you can select/deselect apps that are sent in a request:

```kotlin
ArcticRequest request = // ...
AppModel app = // ...

request.toggleSelection(app);
request.select(app);
request.deselect(app);
request.selectAll();
request.deselectAll();
```

---

# Sending a Request

Once you've selected apps, you can send a request:

```kotlin
request.performSend()
```

Your `onSent` callback will be invoked if all is well; your `onSendError` callback
is invoked if an error occurs.

---

# Cleanup

When appropriate, you should call `dispose()` on your request object. This will let
go of any pending actions that could result in memory leaks, or accessing your UI views
after your app is in the background.

```kotlin
override fun onPause() {
  request.dispose()
  super.onPause()
}
```