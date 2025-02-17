# CaptureSposed
<img align="left" src="images/ic_launcher-playstore.png" width="115" />

With the release of Android 14, Google added an API to enable app developers to detect screenshots. This API has since been adopted by popular apps such as Snapchat.

CaptureSposed is an Xposed module that effectively disables this API as well as the screen recording detection API added in Android 15.

**⚠️ WARNING:** CaptureSposed is intended for rooted devices running Android 14 or newer and requires Xposed. The required Xposed variant to use is LSPosed. Other Xposed variants will not work. This module cannot be guaranteed to work on all devices. In the worst case, it can cause a bootloop. Use at your own risk. Additionally, this module does not protect against screenshot detection from apps that use the pre-Android 14 approach of using file system listeners to detect screenshots ([ref 1](https://abangfadli.medium.com/shotwatch-android-screenshot-detector-library-6a75d7242109), [ref 2](https://viveksb007.wordpress.com/2017/11/10/how-snapchat-detects-when-screenshot-is-taken-hypothesis/)).

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.keshav.capturesposed"><img src="images/download-buttons/google-play-badge.png" height="80" /></a>
  <a href="https://f-droid.org/packages/com.keshav.capturesposed/"><img src="images/download-buttons/get-it-on.png" height="80" /></a>
  <br />
  <a href="https://apt.izzysoft.de/packages/com.keshav.capturesposed"><img src="images/download-buttons/IzzyOnDroidButton.png" height="80" /></a>
  <a href="https://github.com/99keshav99/CaptureSposed/releases"><img src="images/download-buttons/badge_github.png" height="80" /></a>
</p>

### To use CaptureSposed:
1. Install LSposed. This requires your device to be rooted.
2. Install CaptureSposed.
3. Grant root access to CaptureSposed.
4. Activate the CaptureSposed module in the LSposed user interface.
5. Reboot your device and sign in.
6. Open the CaptureSposed app and attempt to take a screenshot and make a screen recording of it. If the screenshot and screen recording is not detected, then the module is working as intended.
