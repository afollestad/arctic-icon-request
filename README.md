# Icon Request

This library allows icon pack dashboards to easily send icon requests
either traditionally via email, or through the [Arctic Manager](https://arcticmanager.com) system.

This library uses RxJava, so it is completely asynchronous, and flows are customizable.

<img src="https://raw.githubusercontent.com/afollestad/polar-icon-request/master/art/showcase2.png" width="600" />

---

# Gradle Dependency

[ ![Download](https://api.bintray.com/packages/drummer-aidan/maven/icon-request/images/download.svg) ](https://bintray.com/drummer-aidan/maven/icon-request/_latestVersion)
[![Build Status](https://travis-ci.org/afollestad/polar-icon-request.svg)](https://travis-ci.org/afollestad/polar-icon-request)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1eb8ed67c1f34eaf9bc176faeb6652bf)](https://www.codacy.com/app/drummeraidan_50/polar-icon-request?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=afollestad/polar-icon-request&amp;utm_campaign=Badge_Grade)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

Add this to your module's `build.gradle` file (make sure the version matches the jCenter badge above):

```gradle
dependencies {
	// ... other dependencies
	compile 'com.afollestad:icon-request:3.0.0'
}
```

---

# Table of Contents

1. [Getting Started](https://github.com/afollestad/polar-icon-request#getting-started)
    1. [Instantiate a Request](https://github.com/afollestad/polar-icon-request#instantiating-a-request)
    2. [Configure a Request](https://github.com/afollestad/polar-icon-request#configuring-a-request)
    3. [Load Unthemed Apps](https://github.com/afollestad/polar-icon-request#loading-unthemed-apps)
    4. [Selecting Apps](https://github.com/afollestad/polar-icon-request#selecting-apps)
    5. [Send a Request](https://github.com/afollestad/polar-icon-request#sending-a-request)
3. [Events](https://github.com/afollestad/polar-icon-request#events)
    1. [Loading](https://github.com/afollestad/polar-icon-request#loading)
    2. [Loaded](https://github.com/afollestad/polar-icon-request#loaded)
    3. [Selection Change](https://github.com/afollestad/polar-icon-request#selection-change)
    4. [Sending](https://github.com/afollestad/polar-icon-request#sending)
    5. [Sent](https://github.com/afollestad/polar-icon-request#sent)

---

# Getting Started

### Instantiating a Request

To create a new Request object, you use the static `make(Context, Bundle)` method. Generally, you'd 
want to call this on `onCreate(Bundle)` of an `Activity` or `Fragment`.

```java
PolarRequest request = PolarRequest.make(this, savedInstanceState)
```

### Configuring a Request

Configuration uses a builder class called `PolarConfig`:

```java
PolarConfig config = PolarConfig.create(this)
    .apiHost("https://your-server.com") // optional, specify for Request Manager usage
    .apiKey("1234") // optional, specify for Request Manager usage
    .emailRecipient("helloworld@hi.com") // required IF you don't specify an API key
    .appFilterName("appfilter.xml")
    .cacheFolder(getCacheDir().getAbsolutePath())
    .errorOnInvalidDrawables(true)
    .includeDeviceInfo(true)
    .emailSubject("New Icon Request")
    .emailHeader("These apps are unthemed!")
    .emailFooter("Thank you!")
    .build();
```

You can pass an instance of this class to `PolarRequest`:

```java
PolarRequest request = // ...
request.config(config);
```

### Loading Unthemed Apps

With a configured `PolarRequest` instance, you can load unthemed apps:

```java
request.load()
    .subscribe(loadResult -> {
      if(loadResult.success()) {
        List<AppModel> loadedApps = loadResult.apps();
        // Use apps
      } else {
        Exception error = loadResult.error();
        // Use error
      }
    });
```

### Selecting Apps

Once you've loaded apps, you can select/deselect apps that are sent in a request:

```java
PolarRequest request = // ...
AppModel app = // ...

request.toggleSelection(app);
request.select(app);
request.deselect(app);
request.selectAll();
request.deselectAll();
```

### Sending a Request

Once you've selected apps, you can send a request:

```java
request.send()
    .subscribe(sendResult -> {
      if(sendResult.success()) {
        int sentCount = sendResult.sentCount();
        boolean usedPolarRm = sendResult.usedPolarRm();
        // Use result?
      } else {
        Exception error = sendResult.error();
        // Use error
      }
    });
```

---

# Events

### Loading
 
This event is triggered when the library begins loading apps, and again when it's done. It's a toggle event.

```java
request.loading()
    .subscribe(isLoading -> {
      // Update progress indicator or something
    });
```
 
### Loaded

This event is triggered after `loading()` receives `false`, it contains the actual loaded apps. 
The same data is received directly from `request.load()`, also. 

```java
request.loaded()
    .subscribe(loadResult -> {
      if (loadResult.success()) {
        List<AppModel> apps = loadResult.apps();
        // Use apps
      } else {
        Exception error = loadResult.error();
        // Use error
      }
    });
```

This event is also triggered when you use `selectAll()` or `deselectAll()` since it is more efficient
 than sending a selection event for every single changed app.


### Selection Change

This event is triggered when a single app is selected or deselected.

```java
request.selectionChange()
    .subscribe(appModel -> {
      boolean selected = appModel.selected();
      // Use new state
    });
```

### Sending

This event is triggered when the library begins generating/sending a request, and again when it's done. 
It's a toggle event.


```java
request.sending()
    .subscribe(isSending -> {
      // Update progress indicator or something
    });
```

### Sent

This event is triggered after `sending()` receives `false`, it contains the actual send result. 
The same data is received directly from `request.send()` also.

```java
request.sent()
    .subscribe(sendResult -> {
      if(sendResult.success()) {
        int sentCount = sendResult.sentCount();
        boolean usedPolarRm = sendResult.usedPolarRm();
        // Use result?
      } else {
        Exception error = sendResult.error();
        // Use error
      }
    });
```