package com.pairip;

/* JADX INFO: loaded from: classes3.dex */
public final class StartupLauncher {
    private static boolean launchCalled = false;
    private static String startupProgramName = "fS2NPonHRPamHuOE";

    public static synchronized void launch() {
        if (launchCalled) {
            return;
        }
        launchCalled = true;
        VMRunner.invoke(startupProgramName, null);
    }

    private StartupLauncher() {
    }
}
