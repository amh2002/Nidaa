# beta8 RC1 release rules.
#
# The app is offline-first: no reflection-based serialization, org.json for the
# updater JSON, Preferences DataStore for settings. Those need no keep rules
# beyond what their libraries already ship.

# WorkManager instantiates workers by class name, so the class and its
# (Context, WorkerParameters) constructor must survive R8.
-keep class com.nidaa.prayer.update.UpdateWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# PackageInstaller delivers the result to this manifest receiver through a
# PendingIntent; keep the class name stable.
-keep class com.nidaa.prayer.update.InstallResultReceiver {
    <init>();
}

# Verbose/debug logging is stripped from the release bytecode. Error-level
# logging stays on purpose (the widget fallback crash logger).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
