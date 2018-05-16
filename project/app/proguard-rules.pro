# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\alexander\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Skip obfuscation
-dontobfuscate
-optimizations !field/removal/writeonly,!field/marking/private,!class/merging/*,!code/allocation/variable

-keep public class org.owntracks.android.** {
  public protected private *;
}


# PAHO (https://github.com/eclipse/paho.mqtt.android/issues/79)
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes EnclosingMethod
-keep class org.eclipse.paho.client.mqttv3.* { *; }
-keep class org.eclipse.paho.client.mqttv3.*$* { *; }

# GREENDAO
-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
public static java.lang.String TABLENAME;
}
-keep class **$Properties
-dontwarn org.greenrobot.greendao.database.**
-dontwarn rx.**


# EVENTBUS
-keepattributes *Annotation*
-keepclassmembers,includedescriptorclasses class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# JACKSON
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
 -dontwarn com.fasterxml.jackson.databind.**
 -keep class org.codehaus.** { *; }

-keep public class org.owntracks.android.messages.** {
 *;
 }

-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}

-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}

# OKHTTP
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
