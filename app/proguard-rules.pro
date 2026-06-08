# Keep JNI bridge — native methods must not be obfuscated
-keep class com.zerodrop.app.ScoreBridge {
    native <methods>;
    public <methods>;
    protected <methods>;
}

# Keep data classes used at JNI boundary
-keep class com.zerodrop.app.GameSnapshot {
    *;
}
-keep class com.zerodrop.app.FsmState {
    *;
}
-keep class com.zerodrop.app.GameUiState {
    *;
}

# Keep ViewModel (reflection-based factory)
-keep class com.zerodrop.app.GameViewModel {
    public <init>(...);
}
-keep class com.zerodrop.app.GameViewModel$Factory {
    *;
}

# Kotlin metadata needed for serialization/reflection
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# General keep rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
