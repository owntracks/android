# Skip obfuscation
-dontobfuscate
-optimizations !field/removal/writeonly,!field/marking/private,!class/merging/*,!code/allocation/variable

# Keep our package
-keep public class org.owntracks.android.** {
  public protected private *;
}

# Keep kotlin.Metadata annotations to maintain metadata on kept items.
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

## EventBus
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Accessed via reflection, avoid renaming or removal
-keep class org.greenrobot.eventbus.android.AndroidComponentsImpl

## Jackson
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
 -dontwarn com.fasterxml.jackson.databind.**
 -keep class org.codehaus.** { *; }

-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}

-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}

## Paho
-keep class org.eclipse.paho.clent.mqttv3.** { *; }
-keep class org.eclipse.paho.client.mqttv3.logging.JSR47Logger { *; }

## Conscrypt
-keep class org.conscrypt.** { *; }

# Backward compatibility code.
-dontnote libcore.io.Libcore
-dontnote org.apache.harmony.xnet.provider.jsse.OpenSSLRSAPrivateKey
-dontnote org.apache.harmony.security.utils.AlgNameMapper
-dontnote sun.security.x509.AlgorithmId

-dontwarn android.util.StatsEvent
-dontwarn dalvik.system.BlockGuard
-dontwarn dalvik.system.BlockGuard$Policy
-dontwarn dalvik.system.CloseGuard
-dontwarn com.android.org.conscrypt.AbstractConscryptSocket
-dontwarn com.android.org.conscrypt.ConscryptFileDescriptorSocket
-dontwarn com.android.org.conscrypt.OpenSSLSocketImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.OpenSSLSocketImpl

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE