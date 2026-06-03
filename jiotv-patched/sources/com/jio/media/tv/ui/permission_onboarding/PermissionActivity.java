package com.jio.media.tv.ui.permission_onboarding;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import androidx.compose.runtime.internal.StabilityInferred;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.ui.b;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.measurement.internal.ESeC.lFxRbOk;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.installations.FirebaseInstallations;
import com.jio.jioads.adinterfaces.q0;
import com.jio.jioplay.tv.JioTVApplication;
import com.jio.jioplay.tv.R;
import com.jio.jioplay.tv.activities.HomeActivity;
import com.jio.jioplay.tv.analytics.NewAnalyticsApi;
import com.jio.jioplay.tv.base.BaseActivity;
import com.jio.jioplay.tv.constants.AppConstants;
import com.jio.jioplay.tv.data.AppDataManager;
import com.jio.jioplay.tv.data.StaticMembers;
import com.jio.jioplay.tv.data.network.response.ExtendedProgramModel;
import com.jio.jioplay.tv.databinding.ActivitySplashBinding;
import com.jio.jioplay.tv.dialog.DeviceLimitExceedDialog;
import com.jio.jioplay.tv.preferences.JioPreferences;
import com.jio.jioplay.tv.receivers.InstallTrackersReceiver;
import com.jio.jioplay.tv.storage.SharedPreferenceUtils;
import com.jio.jioplay.tv.utils.CleverTapUtil;
import com.jio.jioplay.tv.utils.CommonUtils;
import com.jio.jioplay.tv.utils.FirebaseConfigUtil;
import com.jio.jioplay.tv.utils.JioRetryDialog;
import com.jio.jioplay.tv.utils.LogUtils;
import com.jio.jioplay.tv.utils.NetworkUtil;
import com.jio.jioplay.tv.utils.SecurityUtils;
import com.jio.jioplay.tv.utils.ThumbnailLayoutUtils;
import com.jio.jiowebviewsdk.configdatamodel.C;
import com.jio.media.tv.common.CommonExtensionsKt;
import com.jio.media.tv.ui.permission_onboarding.PermissionActivity;
import defpackage.az5;
import defpackage.hs7;
import defpackage.kp;
import defpackage.n0;
import defpackage.v2;
import defpackage.wr0;
import defpackage.y44;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.ArraysKt___ArraysKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt__StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes8.dex */
@StabilityInferred(parameters = 0)
@Metadata(d1 = {"\u0000¤\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u0011\n\u0002\b\b\n\u0002\u0010\u0015\n\u0002\b\u0013\n\u0002\b\u0003\n\u0002\b\u0003\n\u0002\b\u0003\n\u0002\b\t*\u0004dgjm\b\u0007\u0018\u00002\u00020\u00012\u00020\u00022\u00020\u00032\u00020\u0004B\t\b\u0007¢\u0006\u0004\b\u0005\u0010\u0006J\u0012\u0010,\u001a\u00020-2\b\u0010.\u001a\u0004\u0018\u00010/H\u0014J\b\u00100\u001a\u00020-H\u0002J\u0010\u00101\u001a\u00020-2\u0006\u00102\u001a\u000203H\u0002J\u0018\u00104\u001a\u00020-2\u0006\u00105\u001a\u00020\n2\u0006\u00102\u001a\u000203H\u0002J\u0010\u00106\u001a\u00020-2\u0006\u00107\u001a\u000208H\u0014J\b\u00109\u001a\u00020-H\u0002J\b\u0010:\u001a\u00020-H\u0002J\u0012\u0010;\u001a\u00020-2\b\u0010<\u001a\u0004\u0018\u00010=H\u0016J\u0006\u0010>\u001a\u00020\u001dJ(\u0010?\u001a\u00020-2\u0006\u0010@\u001a\u00020\n2\u0006\u0010A\u001a\u00020\n2\u0006\u0010B\u001a\u00020\b2\u0006\u0010C\u001a\u00020\u001dH\u0002J\b\u0010D\u001a\u00020-H\u0002J\u001b\u0010E\u001a\u00020-2\f\u0010F\u001a\b\u0012\u0004\u0012\u00020\n0GH\u0002¢\u0006\u0002\u0010HJ\b\u0010I\u001a\u00020\u001dH\u0002J\u0006\u0010J\u001a\u00020\u001dJ\b\u0010K\u001a\u00020-H\u0002J-\u0010L\u001a\u00020-2\u0006\u0010M\u001a\u00020\b2\u000e\u0010N\u001a\n\u0012\u0006\b\u0001\u0012\u00020\n0G2\u0006\u0010O\u001a\u00020PH\u0016¢\u0006\u0002\u0010QJ\b\u0010R\u001a\u00020-H\u0014J\"\u0010S\u001a\u00020-2\u0006\u0010M\u001a\u00020\b2\u0006\u0010T\u001a\u00020\b2\b\u0010U\u001a\u0004\u0018\u000108H\u0014J\b\u0010V\u001a\u00020-H\u0002J\b\u0010W\u001a\u00020-H\u0002J\u0006\u0010X\u001a\u00020-J\b\u0010Y\u001a\u00020-H\u0002J\b\u0010Z\u001a\u00020-H\u0002J\b\u0010[\u001a\u00020-H\u0014J\b\u0010\\\u001a\u00020-H\u0014J\b\u0010]\u001a\u00020-H\u0002J\b\u0010^\u001a\u00020-H\u0002J\u0010\u0010_\u001a\u00020-2\u0006\u0010M\u001a\u00020\bH\u0016J\u0006\u0010`\u001a\u00020-J\b\u0010a\u001a\u00020-H\u0002J\b\u0010b\u001a\u00020-H\u0016J\u000e\u0010o\u001a\u00020-2\u0006\u0010p\u001a\u00020\u001dJ\b\u0010q\u001a\u00020-H\u0002J\b\u0010r\u001a\u00020-H\u0002J\b\u0010s\u001a\u00020-H\u0002J\u0010\u0010t\u001a\u00020-2\u0006\u0010\u001b\u001a\u00020\nH\u0016R\u000e\u0010\u0007\u001a\u00020\bX\u0082D¢\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082D¢\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u00020\fX\u0086.¢\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u001c\u0010\u0011\u001a\u0004\u0018\u00010\u0012X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\u0014\"\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0017\u001a\u00020\u0018¢\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0010\u0010\u001b\u001a\u0004\u0018\u00010\nX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u001dX\u0082\u000e¢\u0006\u0002\n\u0000R\u001a\u0010\u001e\u001a\u00020\u001fX\u0086.¢\u0006\u000e\n\u0000\u001a\u0004\b \u0010!\"\u0004\b\"\u0010#R\u001a\u0010$\u001a\u00020%X\u0086.¢\u0006\u000e\n\u0000\u001a\u0004\b&\u0010'\"\u0004\b(\u0010)R\u0014\u0010*\u001a\b\u0012\u0004\u0012\u00020\b0+X\u0082\u0004¢\u0006\u0002\n\u0000R\u0010\u0010c\u001a\u00020dX\u0082\u0004¢\u0006\u0004\n\u0002\u0010eR\u0010\u0010f\u001a\u00020gX\u0082\u0004¢\u0006\u0004\n\u0002\u0010hR\u0010\u0010i\u001a\u00020jX\u0082\u0004¢\u0006\u0004\n\u0002\u0010kR\u0010\u0010l\u001a\u00020mX\u0082\u0004¢\u0006\u0004\n\u0002\u0010n¨\u0006u"}, d2 = {"Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;", "Lcom/jio/jioplay/tv/base/BaseActivity;", "Landroid/view/View$OnClickListener;", "Lcom/jio/jioplay/tv/utils/JioRetryDialog$OnRetryDialogListener;", "Lcom/jio/jioplay/tv/receivers/InstallTrackersReceiver$DeeplinkListener;", "<init>", "()V", "SMS_CONSENT_REQUEST", "", "TAG", "", "mBinding", "Lcom/jio/jioplay/tv/databinding/ActivitySplashBinding;", "getMBinding", "()Lcom/jio/jioplay/tv/databinding/ActivitySplashBinding;", "setMBinding", "(Lcom/jio/jioplay/tv/databinding/ActivitySplashBinding;)V", "permissionViewModel", "Lcom/jio/media/tv/ui/permission_onboarding/PermissionViewModel;", "getPermissionViewModel", "()Lcom/jio/media/tv/ui/permission_onboarding/PermissionViewModel;", "setPermissionViewModel", "(Lcom/jio/media/tv/ui/permission_onboarding/PermissionViewModel;)V", "handler", "Landroid/os/Handler;", "getHandler", "()Landroid/os/Handler;", C.JAVASCRIPT_DEEPLINK, "permissonEventSentAtStart", "", "splashScreen", "Landroidx/core/splashscreen/SplashScreen;", "getSplashScreen", "()Landroidx/core/splashscreen/SplashScreen;", "setSplashScreen", "(Landroidx/core/splashscreen/SplashScreen;)V", "firebaseConfigUtil", "Lcom/jio/jioplay/tv/utils/FirebaseConfigUtil;", "getFirebaseConfigUtil", "()Lcom/jio/jioplay/tv/utils/FirebaseConfigUtil;", "setFirebaseConfigUtil", "(Lcom/jio/jioplay/tv/utils/FirebaseConfigUtil;)V", "showDeviceLimitExceedDialogObserver", "Landroidx/lifecycle/Observer;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "checkInstallReferrer", "getInstallReferrerFromClient", "referrerClient", "Lcom/android/installreferrer/api/InstallReferrerClient;", "trackInstallReferrer", "referrerUrl", "onNewIntent", "intent", "Landroid/content/Intent;", "clevertapSetup", "setPreloginAndPostloginCallbacks", "onClick", "v", "Landroid/view/View;", "isSupportedDevice", "showFailedDialog", "btnText", "msg", "statusCode", "isCancelable", "checkValidClient", "seekPermission", "permissionList", "", "([Ljava/lang/String;)V", "isNotBluestack", "isRunningOnEmulator", "gotoSettingScreen", "onRequestPermissionsResult", "requestCode", "permissions", "grantResults", "", "(I[Ljava/lang/String;[I)V", "onResume", "onActivityResult", "resultCode", "data", "checkAndUpdatePermissionView", "setGoToSettingText", "proceedApplication", "navigateToHomeScreen", "goToHomeScreen", "onDestroy", "onStop", "clearAllListener", "appInitProcess", "onDialogButtonClicked", "finishPermissionActivity", "validateLoginStatus", "onUserLangPrefUpdated", "shouldNavigateToHomeScreenListener", "com/jio/media/tv/ui/permission_onboarding/PermissionActivity$shouldNavigateToHomeScreenListener$1", "Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity$shouldNavigateToHomeScreenListener$1;", "applyThemeListener", "com/jio/media/tv/ui/permission_onboarding/PermissionActivity$applyThemeListener$1", "Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity$applyThemeListener$1;", "logoutListener", "com/jio/media/tv/ui/permission_onboarding/PermissionActivity$logoutListener$1", "Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity$logoutListener$1;", "getLocationIfPermissionGrantedListener", "com/jio/media/tv/ui/permission_onboarding/PermissionActivity$getLocationIfPermissionGrantedListener$1", "Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity$getLocationIfPermissionGrantedListener$1;", "showHideSubtitle", "show", "finishAndClear", "handleAppShortcutsIntent", "checkForLastActivityRedirection", "handleDeepLink", "JioTvApp_prodGooglePlayStoreRelease"}, k = 1, mv = {2, 0, 0}, xi = 48)
@SourceDebugExtension({"SMAP\nPermissionActivity.kt\nKotlin\n*S Kotlin\n*F\n+ 1 PermissionActivity.kt\ncom/jio/media/tv/ui/permission_onboarding/PermissionActivity\n+ 2 ArraysJVM.kt\nkotlin/collections/ArraysKt__ArraysJVMKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 4 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,919:1\n37#2,2:920\n1863#3,2:922\n1#4:924\n*S KotlinDebug\n*F\n+ 1 PermissionActivity.kt\ncom/jio/media/tv/ui/permission_onboarding/PermissionActivity\n*L\n336#1:920,2\n560#1:922,2\n*E\n"})
public final class PermissionActivity extends BaseActivity implements View.OnClickListener, JioRetryDialog.OnRetryDialogListener, InstallTrackersReceiver.DeeplinkListener {
    public static final int $stable = 8;

    @Nullable
    private PermissionViewModel Z;

    @Nullable
    private String b0;
    private boolean c0;
    public FirebaseConfigUtil firebaseConfigUtil;
    public ActivitySplashBinding mBinding;
    public SplashScreen splashScreen;
    private final int X = 2;

    @NotNull
    private final String Y = "PermissionActivity";

    @NotNull
    private final Handler a0 = new Handler();

    @NotNull
    private final Observer<Integer> d0 = new y44(this, 2);

    @NotNull
    private final PermissionActivity$shouldNavigateToHomeScreenListener$1 e0 = new Observable.OnPropertyChangedCallback() { // from class: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$shouldNavigateToHomeScreenListener$1
        /* JADX WARN: Removed duplicated region for block: B:9:0x0017  */
        @Override // androidx.databinding.Observable.OnPropertyChangedCallback
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void onPropertyChanged(androidx.databinding.Observable r4, int r5) {
            /*
                r3 = this;
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r4 = r3.f8135a
                com.jio.media.tv.ui.permission_onboarding.PermissionViewModel r4 = r4.getZ()
                r5 = 0
                if (r4 == 0) goto L17
                androidx.databinding.ObservableBoolean r4 = r4.getK0()
                if (r4 == 0) goto L17
                boolean r4 = r4.get()
                r0 = 1
                if (r4 != r0) goto L17
                goto L18
            L17:
                r0 = r5
            L18:
                if (r0 == 0) goto L78
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r4 = r3.f8135a
                com.jio.media.tv.ui.permission_onboarding.PermissionViewModel r4 = r4.getZ()
                if (r4 == 0) goto L28
                java.lang.String r4 = r4.getTAG()
                if (r4 != 0) goto L2a
            L28:
                java.lang.String r4 = "Permission Activity"
            L2a:
                com.jio.jioplay.tv.JioTVApplication r0 = com.jio.jioplay.tv.JioTVApplication.getInstance()
                boolean r0 = r0.isStartupSequenceCompleted
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                java.lang.String r2 = "navigateToHomeScreen: "
                r1.<init>(r2)
                r1.append(r0)
                java.lang.String r0 = r1.toString()
                com.jio.jioplay.tv.utils.LogUtils.log(r4, r0)
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r4 = r3.f8135a
                android.content.Intent r4 = r4.getIntent()
                if (r4 == 0) goto L55
                android.net.Uri r4 = r4.getData()
                if (r4 == 0) goto L55
                java.lang.String r4 = r4.toString()
                if (r4 != 0) goto L57
            L55:
                java.lang.String r4 = ""
            L57:
                java.lang.String r0 = "Permission Activity intent link before navigate to home: "
                java.lang.String r4 = r0.concat(r4)
                java.lang.String r0 = "DeepLinkManager"
                com.jio.jioplay.tv.utils.LogUtils.log(r0, r4)
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r4 = r3.f8135a
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity.access$navigateToHomeScreen(r4)
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r4 = r3.f8135a
                com.jio.media.tv.ui.permission_onboarding.PermissionViewModel r4 = r4.getZ()
                if (r4 == 0) goto L78
                androidx.databinding.ObservableBoolean r4 = r4.getK0()
                if (r4 == 0) goto L78
                r4.set(r5)
            L78:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$shouldNavigateToHomeScreenListener$1.onPropertyChanged(androidx.databinding.Observable, int):void");
        }
    };

    @NotNull
    private final PermissionActivity$applyThemeListener$1 k0 = new Observable.OnPropertyChangedCallback() { // from class: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$applyThemeListener$1
        @Override // androidx.databinding.Observable.OnPropertyChangedCallback
        public void onPropertyChanged(Observable sender, int propertyId) {
            ObservableInt o0;
            ObservableInt o02;
            PermissionViewModel z = this.f8131a.getZ();
            if (((z == null || (o02 = z.getO0()) == null) ? 0 : o02.get()) > 0) {
                PermissionViewModel z2 = this.f8131a.getZ();
                Integer numValueOf = (z2 == null || (o0 = z2.getO0()) == null) ? null : Integer.valueOf(o0.get());
                if (numValueOf != null && numValueOf.intValue() == 1) {
                    this.f8131a.setTheme(R.style.AppTheme_Black);
                } else if (numValueOf != null && numValueOf.intValue() == 2) {
                    this.f8131a.setTheme(R.style.AppTheme);
                }
            }
        }
    };

    @NotNull
    private final PermissionActivity$logoutListener$1 l0 = new Observable.OnPropertyChangedCallback() { // from class: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$logoutListener$1
        /* JADX WARN: Removed duplicated region for block: B:9:0x0017  */
        @Override // androidx.databinding.Observable.OnPropertyChangedCallback
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void onPropertyChanged(androidx.databinding.Observable r2, int r3) {
            /*
                r1 = this;
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r2 = r1.f8134a
                com.jio.media.tv.ui.permission_onboarding.PermissionViewModel r2 = r2.getZ()
                r3 = 0
                if (r2 == 0) goto L17
                androidx.databinding.ObservableBoolean r2 = r2.getP0()
                if (r2 == 0) goto L17
                boolean r2 = r2.get()
                r0 = 1
                if (r2 != r0) goto L17
                goto L18
            L17:
                r0 = r3
            L18:
                if (r0 == 0) goto L30
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r2 = r1.f8134a
                r2.finishPermissionActivity()
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r2 = r1.f8134a
                com.jio.media.tv.ui.permission_onboarding.PermissionViewModel r2 = r2.getZ()
                if (r2 == 0) goto L30
                androidx.databinding.ObservableBoolean r2 = r2.getP0()
                if (r2 == 0) goto L30
                r2.set(r3)
            L30:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$logoutListener$1.onPropertyChanged(androidx.databinding.Observable, int):void");
        }
    };

    @NotNull
    private final PermissionActivity$getLocationIfPermissionGrantedListener$1 m0 = new Observable.OnPropertyChangedCallback() { // from class: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$getLocationIfPermissionGrantedListener$1
        /* JADX WARN: Removed duplicated region for block: B:9:0x0017  */
        @Override // androidx.databinding.Observable.OnPropertyChangedCallback
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void onPropertyChanged(androidx.databinding.Observable r2, int r3) {
            /*
                r1 = this;
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r2 = r1.f8133a
                com.jio.media.tv.ui.permission_onboarding.PermissionViewModel r2 = r2.getZ()
                r3 = 0
                if (r2 == 0) goto L17
                androidx.databinding.ObservableBoolean r2 = r2.getT0()
                if (r2 == 0) goto L17
                boolean r2 = r2.get()
                r0 = 1
                if (r2 != r0) goto L17
                goto L18
            L17:
                r0 = r3
            L18:
                if (r0 == 0) goto L30
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r2 = r1.f8133a
                r2.findLocation()
                com.jio.media.tv.ui.permission_onboarding.PermissionActivity r2 = r1.f8133a
                com.jio.media.tv.ui.permission_onboarding.PermissionViewModel r2 = r2.getZ()
                if (r2 == 0) goto L30
                androidx.databinding.ObservableBoolean r2 = r2.getT0()
                if (r2 == 0) goto L30
                r2.set(r3)
            L30:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$getLocationIfPermissionGrantedListener$1.onPropertyChanged(androidx.databinding.Observable, int):void");
        }
    };

    public static Unit o(PermissionActivity this$0, Boolean bool) {
        String string;
        MutableLiveData<Boolean> postLoginSuccess;
        Uri data;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (bool != null && bool.booleanValue()) {
            this$0.getMBinding().splashProgreesBar.setProgress(95);
            Intent intent = this$0.getIntent();
            if (intent == null || (data = intent.getData()) == null || (string = data.toString()) == null) {
                string = "";
            }
            LogUtils.log("DeepLinkManager", "Permission Activity intent link before navigate to home post login: ".concat(string));
            this$0.u();
            LogUtils.log(this$0.Y, "post login success");
            PermissionViewModel permissionViewModel = this$0.Z;
            if (permissionViewModel != null && (postLoginSuccess = permissionViewModel.getPostLoginSuccess()) != null) {
                postLoginSuccess.postValue(null);
            }
        }
        return Unit.INSTANCE;
    }

    public static Unit p(PermissionActivity this$0, Boolean bool) {
        MutableLiveData<Boolean> preLoginSuccess;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (bool != null && bool.booleanValue()) {
            LogUtils.log(this$0.Y, "pre login success");
            try {
                if (JioPreferences.getInstance(JioTVApplication.getInstance()).getLoginDetails() != null) {
                    LogUtils.log("LoginSDK", "loginDetail check true");
                    this$0.x();
                } else {
                    LogUtils.log("LoginSDK", "request for login check");
                    this$0.getMBinding().loginView.setVisibility(0);
                }
                PermissionViewModel permissionViewModel = this$0.Z;
                if (permissionViewModel != null && (preLoginSuccess = permissionViewModel.getPreLoginSuccess()) != null) {
                    preLoginSuccess.postValue(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Unit.INSTANCE;
    }

    public static Unit q(PermissionActivity this$0, Boolean bool) {
        MutableLiveData<Boolean> responseFailDialog;
        ObservableField<String> dialogBtnText;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (bool != null && bool.booleanValue()) {
            LogUtils.log(this$0.Y, this$0.getString(R.string.show_api_failure_dialog));
            PermissionViewModel permissionViewModel = this$0.Z;
            String str = (permissionViewModel == null || (dialogBtnText = permissionViewModel.getDialogBtnText()) == null) ? null : dialogBtnText.get();
            Intrinsics.checkNotNull(str);
            PermissionViewModel permissionViewModel2 = this$0.Z;
            String b0 = permissionViewModel2 != null ? permissionViewModel2.getB0() : null;
            Intrinsics.checkNotNull(b0);
            PermissionViewModel permissionViewModel3 = this$0.Z;
            Integer numValueOf = permissionViewModel3 != null ? Integer.valueOf(permissionViewModel3.getC0()) : null;
            Intrinsics.checkNotNull(numValueOf);
            this$0.w(numValueOf.intValue(), str, b0);
            PermissionViewModel permissionViewModel4 = this$0.Z;
            if (permissionViewModel4 != null && (responseFailDialog = permissionViewModel4.getResponseFailDialog()) != null) {
                responseFailDialog.setValue(null);
            }
        }
        return Unit.INSTANCE;
    }

    public static void r(PermissionActivity this$0, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        LogUtils.log(this$0.Y, "showDeviceLimitExceedDialogObserver");
        FragmentManager supportFragmentManager = this$0.getSupportFragmentManager();
        Intrinsics.checkNotNullExpressionValue(supportFragmentManager, "getSupportFragmentManager(...)");
        if (i == 0 || this$0.isFinishing() || supportFragmentManager.isDestroyed()) {
            return;
        }
        DeviceLimitExceedDialog.INSTANCE.getInstance(i == 1).show(supportFragmentManager, DeviceLimitExceedDialog.TAG);
        if (i == 1) {
            NewAnalyticsApi.sendDeviceRestrictionEvent("devicerestriction", "");
        }
        AppDataManager.get().setShowDeviceLimitExceedDialog(false, 0);
    }

    public static Unit s(PermissionActivity this$0, Boolean bool) {
        MutableLiveData<Boolean> configSuccess;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (bool != null && bool.booleanValue()) {
            this$0.getMBinding().splashProgreesBar.setProgress(70);
            PermissionViewModel permissionViewModel = this$0.Z;
            if (permissionViewModel != null && (configSuccess = permissionViewModel.getConfigSuccess()) != null) {
                configSuccess.postValue(null);
            }
        }
        return Unit.INSTANCE;
    }

    public final void finishPermissionActivity() {
        Intent intent = new Intent(this, (Class<?>) PermissionActivity.class);
        intent.addFlags(335642624);
        intent.putExtra("isLoggedOut", true);
        startActivity(intent);
        finishAndClear();
    }

    @NotNull
    public final FirebaseConfigUtil getFirebaseConfigUtil() {
        FirebaseConfigUtil firebaseConfigUtil = this.firebaseConfigUtil;
        if (firebaseConfigUtil != null) {
            return firebaseConfigUtil;
        }
        Intrinsics.throwUninitializedPropertyAccessException("firebaseConfigUtil");
        return null;
    }

    @NotNull
    /* JADX INFO: renamed from: getHandler, reason: from getter */
    public final Handler getA0() {
        return this.a0;
    }

    @NotNull
    public final ActivitySplashBinding getMBinding() {
        ActivitySplashBinding activitySplashBinding = this.mBinding;
        if (activitySplashBinding != null) {
            return activitySplashBinding;
        }
        Intrinsics.throwUninitializedPropertyAccessException("mBinding");
        return null;
    }

    @Nullable
    /* JADX INFO: renamed from: getPermissionViewModel, reason: from getter */
    public final PermissionViewModel getZ() {
        return this.Z;
    }

    @Override // android.app.Activity
    @NotNull
    public final SplashScreen getSplashScreen() {
        SplashScreen splashScreen = this.splashScreen;
        if (splashScreen != null) {
            return splashScreen;
        }
        Intrinsics.throwUninitializedPropertyAccessException("splashScreen");
        return null;
    }

    @Override // com.jio.jioplay.tv.receivers.InstallTrackersReceiver.DeeplinkListener
    public void handleDeepLink(@NotNull String deeplink) {
        Intrinsics.checkNotNullParameter(deeplink, "deeplink");
        this.b0 = deeplink;
        getIntent().putExtra(C.JAVASCRIPT_DEEPLINK, deeplink);
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x006c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final boolean isRunningOnEmulator() {
        /*
            r8 = this;
            java.lang.String r0 = android.os.Build.FINGERPRINT
            java.lang.String r1 = "FINGERPRINT"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            java.lang.String r2 = "generic"
            r3 = 0
            r4 = 2
            r5 = 0
            boolean r6 = defpackage.hs7.startsWith$default(r0, r2, r3, r4, r5)
            if (r6 != 0) goto L9d
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            java.lang.String r1 = "unknown"
            boolean r0 = defpackage.hs7.startsWith$default(r0, r1, r3, r4, r5)
            if (r0 != 0) goto L9d
            java.lang.String r0 = android.os.Build.MODEL
            java.lang.String r1 = "MODEL"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            java.lang.String r6 = "google_sdk"
            boolean r7 = kotlin.text.StringsKt__StringsKt.contains$default(r0, r6, r3, r4, r5)
            if (r7 != 0) goto L9d
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            java.lang.String r7 = "Emulator"
            boolean r7 = kotlin.text.StringsKt__StringsKt.contains$default(r0, r7, r3, r4, r5)
            if (r7 != 0) goto L9d
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            java.lang.String r1 = "Android SDK built for x86"
            boolean r0 = kotlin.text.StringsKt__StringsKt.contains$default(r0, r1, r3, r4, r5)
            if (r0 != 0) goto L9d
            java.lang.String r0 = android.os.Build.MANUFACTURER
            java.lang.String r1 = "MANUFACTURER"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            java.lang.String r1 = "Genymotion"
            boolean r0 = kotlin.text.StringsKt__StringsKt.contains$default(r0, r1, r3, r4, r5)
            if (r0 != 0) goto L9d
            java.lang.String r0 = android.os.Build.BRAND
            java.lang.String r1 = "BRAND"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            boolean r0 = defpackage.hs7.startsWith$default(r0, r2, r3, r4, r5)
            java.lang.String r1 = "DEVICE"
            if (r0 == 0) goto L6c
            java.lang.String r0 = android.os.Build.DEVICE
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            boolean r0 = defpackage.hs7.startsWith$default(r0, r2, r3, r4, r5)
            if (r0 != 0) goto L9d
        L6c:
            java.lang.String r0 = android.os.Build.PRODUCT
            boolean r2 = kotlin.jvm.internal.Intrinsics.areEqual(r6, r0)
            if (r2 != 0) goto L9d
            java.lang.String r2 = "PRODUCT"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r2)
            java.lang.String r2 = "vbox86p"
            boolean r0 = kotlin.text.StringsKt__StringsKt.contains$default(r0, r2, r3, r4, r5)
            if (r0 != 0) goto L9d
            java.lang.String r0 = android.os.Build.DEVICE
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            boolean r0 = kotlin.text.StringsKt__StringsKt.contains$default(r0, r2, r3, r4, r5)
            if (r0 != 0) goto L9d
            java.lang.String r0 = android.os.Build.HARDWARE
            java.lang.String r1 = "HARDWARE"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            java.lang.String r1 = "vbox86"
            boolean r0 = kotlin.text.StringsKt__StringsKt.contains$default(r0, r1, r3, r4, r5)
            if (r0 == 0) goto L9e
        L9d:
            r3 = 1
        L9e:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.jio.media.tv.ui.permission_onboarding.PermissionActivity.isRunningOnEmulator():boolean");
    }

    public final boolean isSupportedDevice() {
        if (!getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) {
            String MODEL = Build.MODEL;
            Intrinsics.checkNotNullExpressionValue(MODEL, "MODEL");
            if (!StringsKt__StringsKt.contains$default((CharSequence) MODEL, (CharSequence) "AFT", false, 2, (Object) null)) {
                return true;
            }
        }
        LogUtils.log(this.Y, "Unsupported device Fire TV device.");
        return false;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(@Nullable View v) {
        ArrayList<String> askPermissionList;
        try {
            PermissionViewModel permissionViewModel = this.Z;
            String[] strArr = (permissionViewModel == null || (askPermissionList = permissionViewModel.getAskPermissionList()) == null) ? null : (String[]) askPermissionList.toArray(new String[0]);
            Intrinsics.checkNotNull(strArr);
            v(strArr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    /* JADX WARN: Type inference failed for: r1v31, types: [zy5] */
    /* JADX WARN: Type inference failed for: r1v32, types: [zy5] */
    /* JADX WARN: Type inference failed for: r1v33, types: [zy5] */
    /* JADX WARN: Type inference failed for: r1v34, types: [zy5] */
    @Override // com.jio.jioplay.tv.base.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(@Nullable Bundle savedInstanceState) {
        String tag;
        String str;
        ObservableBoolean p0;
        ObservableBoolean t0;
        ObservableBoolean k0;
        MutableLiveData<Boolean> responseFailDialog;
        MutableLiveData<Boolean> postLoginSuccess;
        MutableLiveData<Boolean> configSuccess;
        MutableLiveData<Boolean> preLoginSuccess;
        ObservableInt o0;
        setSplashScreen(SplashScreen.INSTANCE.installSplashScreen(this));
        super.onCreate(savedInstanceState);
        getSplashScreen().setOnExitAnimationListener(new n0(18));
        String loginDetails = JioPreferences.getInstance(JioTVApplication.getInstance()).getLoginDetails();
        final int i = 0;
        final int i2 = 1;
        if (loginDetails != null) {
            try {
                if (hs7.equals(new JSONObject(loginDetails).optString("userType"), "Jio", true)) {
                    JioPreferences.getInstance(JioTVApplication.getInstance()).setGuestUser(false);
                }
            } catch (Exception unused) {
            }
        }
        if (!isTaskRoot() && getIntent().hasCategory("android.intent.category.LAUNCHER")) {
            String action = getIntent().getAction();
            if (action == null) {
                action = "";
            }
            if (action.equals("android.intent.action.MAIN")) {
                finish();
            }
        }
        Intent intent = getIntent();
        if (intent != null) {
            String stringExtra = intent.getStringExtra("feature");
            String stringExtra2 = intent.getStringExtra("shortcut_name");
            if (stringExtra2 != null) {
                NewAnalyticsApi.INSTANCE.sendAppShortcutsEvent(stringExtra2);
            }
            if (stringExtra != null) {
                switch (stringExtra.hashCode()) {
                    case -1068259517:
                        if (stringExtra.equals("movies")) {
                            getIntent().setData(Uri.parse("jioplay://tab/12"));
                            stringExtra2 = "Movies";
                        }
                        break;
                    case -906336856:
                        if (stringExtra.equals(FirebaseAnalytics.Event.SEARCH)) {
                            getIntent().setData(Uri.parse("jioplay://search"));
                            stringExtra2 = "Search";
                        }
                        break;
                    case 100636:
                        if (stringExtra.equals("epg")) {
                            getIntent().setData(Uri.parse("jioplay://tab/2"));
                            stringExtra2 = "Tv Guide";
                        }
                        break;
                    case 109413654:
                        if (stringExtra.equals("shows")) {
                            getIntent().setData(Uri.parse("jioplay://tab/3"));
                            stringExtra2 = "Shows";
                        }
                        break;
                }
                NewAnalyticsApi newAnalyticsApi = NewAnalyticsApi.INSTANCE;
                Intrinsics.checkNotNull(stringExtra2);
                newAnalyticsApi.sendAppShortcutsEvent(stringExtra2);
            }
        }
        Window window = getWindow();
        if (window != null) {
            window.addFlags(1024);
        }
        LogUtils.log(this.Y, "OnCreate");
        Intent intent2 = getIntent();
        if (intent2 != null && StringsKt__StringsKt.contains((CharSequence) String.valueOf(intent2.getData()), (CharSequence) "www.jiotv.com", true)) {
            intent2.setData(Uri.parse(hs7.replace(String.valueOf(intent2.getData()), "www.", "", true)));
        }
        LogUtils.log("DeepLinkManager", "Permission Activity link: ".concat(String.valueOf(getIntent().getData())));
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(new v2(this, i2));
        String sessionIdString = CommonUtils.getSessionIdString();
        Intrinsics.checkNotNullExpressionValue(sessionIdString, "getSessionIdString(...)");
        String strSubstring = sessionIdString.substring(0, 32);
        Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
        StaticMembers.sSessionId = strSubstring;
        StaticMembers.sSelectedLanguageId = SharedPreferenceUtils.getString(this, AppConstants.StorageConstant.APP_LANGUAGE, "6");
        CommonUtils.screenTrackingFirebase("PermissionActivity", "PermissionActivity");
        setFirebaseConfigUtil(new FirebaseConfigUtil(this));
        getFirebaseConfigUtil().activate();
        if (Build.VERSION.SDK_INT >= 26) {
            CleverTapAPI.createNotificationChannelGroup(getApplicationContext(), "CT", "CT1");
            CleverTapAPI.createNotificationChannel((Context) this, "2000", (CharSequence) "Redbull", "CT Jiotv", 5, true, "redbull.wav");
            CleverTapAPI.createNotificationChannel(getApplicationContext(), "CT1Ch", "CT1Ch", "CT Jiotv", 5, true);
            CleverTapAPI.createNotificationChannel(getApplicationContext(), "100", "CT1Ch", "CT Jiotv", 5, true);
        }
        setMBinding((ActivitySplashBinding) DataBindingUtil.setContentView(this, R.layout.activity_splash));
        if (this.Z != null) {
            this.Z = null;
        }
        this.Z = (PermissionViewModel) new ViewModelProvider(this).get(PermissionViewModel.class);
        if (JioTVApplication.getInstance().shouldPrintLogcatLogsInFile) {
            if (CommonUtils.hasExternalStoragePermission(this)) {
                CommonUtils.printLogcatLogsInFile(this);
            } else {
                String[] strArr = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
                PermissionViewModel permissionViewModel = this.Z;
                Integer numValueOf = permissionViewModel != null ? Integer.valueOf(permissionViewModel.getA0()) : null;
                Intrinsics.checkNotNull(numValueOf);
                ActivityCompat.requestPermissions(this, strArr, numValueOf.intValue());
            }
        }
        PermissionViewModel permissionViewModel2 = this.Z;
        if (permissionViewModel2 == null || (tag = permissionViewModel2.getTAG()) == null) {
            tag = "Permission Activity";
        }
        LogUtils.log(tag, "Application Start");
        getMBinding().splashProgreesBar.setProgress(0);
        getMBinding().permissionOnboardingLayout.setHandler(this);
        getMBinding().setPermissionViewModel(this.Z);
        PermissionViewModel permissionViewModel3 = this.Z;
        if (permissionViewModel3 != null && (o0 = permissionViewModel3.getO0()) != null) {
            o0.addOnPropertyChangedCallback(this.k0);
        }
        PermissionViewModel permissionViewModel4 = this.Z;
        if (permissionViewModel4 != null && (preLoginSuccess = permissionViewModel4.getPreLoginSuccess()) != null) {
            preLoginSuccess.observe(this, new az5(new Function1(this) { // from class: zy5
                public final /* synthetic */ PermissionActivity b;

                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj) {
                    int i3 = i;
                    PermissionActivity permissionActivity = this.b;
                    switch (i3) {
                        case 0:
                            return PermissionActivity.p(permissionActivity, (Boolean) obj);
                        case 1:
                            return PermissionActivity.s(permissionActivity, (Boolean) obj);
                        case 2:
                            return PermissionActivity.o(permissionActivity, (Boolean) obj);
                        default:
                            return PermissionActivity.q(permissionActivity, (Boolean) obj);
                    }
                }

                {
                    this.b = this;
                }
            }));
        }
        PermissionViewModel permissionViewModel5 = this.Z;
        if (permissionViewModel5 != null && (configSuccess = permissionViewModel5.getConfigSuccess()) != null) {
            configSuccess.observe(this, new az5(new Function1(this) { // from class: zy5
                public final /* synthetic */ PermissionActivity b;

                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj) {
                    int i3 = i2;
                    PermissionActivity permissionActivity = this.b;
                    switch (i3) {
                        case 0:
                            return PermissionActivity.p(permissionActivity, (Boolean) obj);
                        case 1:
                            return PermissionActivity.s(permissionActivity, (Boolean) obj);
                        case 2:
                            return PermissionActivity.o(permissionActivity, (Boolean) obj);
                        default:
                            return PermissionActivity.q(permissionActivity, (Boolean) obj);
                    }
                }

                {
                    this.b = this;
                }
            }));
        }
        PermissionViewModel permissionViewModel6 = this.Z;
        if (permissionViewModel6 != null && (postLoginSuccess = permissionViewModel6.getPostLoginSuccess()) != null) {
            final int i3 = 2;
            postLoginSuccess.observe(this, new az5(new Function1(this) { // from class: zy5
                public final /* synthetic */ PermissionActivity b;

                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj) {
                    int i32 = i3;
                    PermissionActivity permissionActivity = this.b;
                    switch (i32) {
                        case 0:
                            return PermissionActivity.p(permissionActivity, (Boolean) obj);
                        case 1:
                            return PermissionActivity.s(permissionActivity, (Boolean) obj);
                        case 2:
                            return PermissionActivity.o(permissionActivity, (Boolean) obj);
                        default:
                            return PermissionActivity.q(permissionActivity, (Boolean) obj);
                    }
                }

                {
                    this.b = this;
                }
            }));
        }
        PermissionViewModel permissionViewModel7 = this.Z;
        if (permissionViewModel7 != null && (responseFailDialog = permissionViewModel7.getResponseFailDialog()) != null) {
            final int i4 = 3;
            responseFailDialog.observe(this, new az5(new Function1(this) { // from class: zy5
                public final /* synthetic */ PermissionActivity b;

                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj) {
                    int i32 = i4;
                    PermissionActivity permissionActivity = this.b;
                    switch (i32) {
                        case 0:
                            return PermissionActivity.p(permissionActivity, (Boolean) obj);
                        case 1:
                            return PermissionActivity.s(permissionActivity, (Boolean) obj);
                        case 2:
                            return PermissionActivity.o(permissionActivity, (Boolean) obj);
                        default:
                            return PermissionActivity.q(permissionActivity, (Boolean) obj);
                    }
                }

                {
                    this.b = this;
                }
            }));
        }
        AppDataManager.get().getShowDeviceLimitExceedDialog().observe(this, this.d0);
        PermissionViewModel permissionViewModel8 = this.Z;
        if (permissionViewModel8 != null && (k0 = permissionViewModel8.getK0()) != null) {
            k0.addOnPropertyChangedCallback(this.e0);
        }
        PermissionViewModel permissionViewModel9 = this.Z;
        if (permissionViewModel9 != null && (t0 = permissionViewModel9.getT0()) != null) {
            t0.addOnPropertyChangedCallback(this.m0);
        }
        PermissionViewModel permissionViewModel10 = this.Z;
        if (permissionViewModel10 != null && (p0 = permissionViewModel10.getP0()) != null) {
            p0.addOnPropertyChangedCallback(this.l0);
        }
        JioPreferences.getInstance(JioTVApplication.getInstance());
        PermissionViewModel permissionViewModel11 = this.Z;
        if (permissionViewModel11 != null) {
            permissionViewModel11.updateTheme();
            permissionViewModel11.checkAppStartUpStatus();
        }
        ThumbnailLayoutUtils.getInstance().init(CommonUtils.getDeviceWidth(this), CommonUtils.getDeviceWidth(this));
        boolean zHasExtra = getIntent().hasExtra("isLoggedOut");
        setRequestedOrientation(1);
        PermissionViewModel permissionViewModel12 = this.Z;
        if (permissionViewModel12 != null) {
            permissionViewModel12.initAppStartup(zHasExtra);
        }
        CommonUtils.checkIsUpdateAvailable();
        boolean zIsSupportedDevice = isSupportedDevice();
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        char c = File.separatorChar;
        File file = new File(externalStorageDirectory + c + "windows" + c + "BstSharedFolder");
        file.exists();
        boolean zExists = file.exists();
        boolean zIsRunningOnEmulator = isRunningOnEmulator();
        boolean zHasSystemFeature = getPackageManager().hasSystemFeature("android.software.leanback");
        if (!zIsSupportedDevice || !SecurityUtils.isValidBuild() || !SecurityUtils.isValidVersionName() || com.google.firebase.crashlytics.internal.common.CommonUtils.isRooted() || zExists || zIsRunningOnEmulator || zHasSystemFeature) {
            if (com.google.firebase.crashlytics.internal.common.CommonUtils.isRooted()) {
                NewAnalyticsApi.INSTANCE.sendEvent("Rooted_devices");
                str = "Rooted device found.";
            } else {
                if (zExists) {
                    NewAnalyticsApi.INSTANCE.sendEvent("bluestack_device");
                } else if (zIsRunningOnEmulator) {
                    NewAnalyticsApi.INSTANCE.sendEvent("emulator_device");
                } else if (zHasSystemFeature) {
                    NewAnalyticsApi.INSTANCE.sendEvent("tv_device");
                } else {
                    NewAnalyticsApi.INSTANCE.sendEvent("Unsupported_device");
                }
                str = "Your device is not compatible with JioTV";
            }
            CommonUtils.showXposedFrameworkDetectionDialog(this, str, new b(this, 17));
        } else {
            proceedApplication();
        }
        if (SharedPreferenceUtils.isAppLaunchedFirstTime()) {
            final InstallReferrerClient installReferrerClientBuild = InstallReferrerClient.newBuilder(this).build();
            Intrinsics.checkNotNull(installReferrerClientBuild);
            installReferrerClientBuild.startConnection(new InstallReferrerStateListener() { // from class: com.jio.media.tv.ui.permission_onboarding.PermissionActivity$getInstallReferrerFromClient$1
                @Override // com.android.installreferrer.api.InstallReferrerStateListener
                public void onInstallReferrerServiceDisconnected() {
                }

                @Override // com.android.installreferrer.api.InstallReferrerStateListener
                public void onInstallReferrerSetupFinished(int responseCode) throws RemoteException {
                    if (responseCode != 0) {
                        return;
                    }
                    ReferrerDetails installReferrer = installReferrerClientBuild.getInstallReferrer();
                    Intrinsics.checkNotNullExpressionValue(installReferrer, "getInstallReferrer(...)");
                    String installReferrer2 = installReferrer.getInstallReferrer();
                    Intrinsics.checkNotNullExpressionValue(installReferrer2, "getInstallReferrer(...)");
                    installReferrer.getReferrerClickTimestampSeconds();
                    installReferrer.getInstallBeginTimestampSeconds();
                    installReferrer.getGooglePlayInstantParam();
                    PermissionActivity.access$trackInstallReferrer(this, installReferrer2, installReferrerClientBuild);
                }
            });
        }
        if (JioTVApplication.getInstance().isGuestUser()) {
            CleverTapUtil.INSTANCE.createGuestProfile(this);
        }
    }

    @Override // com.jio.jioplay.tv.utils.JioRetryDialog.OnRetryDialogListener
    public void onDialogButtonClicked(int requestCode) {
        if (requestCode == 400) {
            if (NetworkUtil.isConnectionAvailable()) {
                t();
                CommonUtils.restartApp(this);
                return;
            }
            return;
        }
        if (requestCode == 500) {
            finishAndClear();
            return;
        }
        if (requestCode == 700) {
            PermissionViewModel permissionViewModel = this.Z;
            if (permissionViewModel != null) {
                permissionViewModel.retryAndLogout(false, false);
                return;
            }
            return;
        }
        if (requestCode == 800) {
            PermissionViewModel permissionViewModel2 = this.Z;
            if (permissionViewModel2 != null) {
                permissionViewModel2.callConfigSuccess();
                return;
            }
            return;
        }
        if (requestCode == 900) {
            x();
            return;
        }
        if (requestCode != 1001) {
            return;
        }
        if (NetworkUtil.isConnectionAvailable()) {
            PermissionViewModel permissionViewModel3 = this.Z;
            if (permissionViewModel3 != null) {
                permissionViewModel3.loadData();
                return;
            }
            return;
        }
        if (AppDataManager.get().strings != null) {
            CommonUtils.showInternetError(this);
            String tryAgain = AppDataManager.get().strings.getTryAgain();
            Intrinsics.checkNotNullExpressionValue(tryAgain, "getTryAgain(...)");
            w(1001, tryAgain, AppDataManager.get().strings.getInternalServerError() + " 1006");
        }
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onNewIntent(@NotNull Intent intent) {
        Intrinsics.checkNotNullParameter(intent, "intent");
        super.onNewIntent(intent);
        getIntent().setData(intent.getData());
    }

    @Override // com.jio.jioplay.tv.base.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Intrinsics.checkNotNullParameter(permissions, "permissions");
        Intrinsics.checkNotNullParameter(grantResults, "grantResults");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SharedPreferenceUtils.setFirstTimePermission(false);
        NewAnalyticsApi newAnalyticsApi = NewAnalyticsApi.INSTANCE;
        newAnalyticsApi.sendPermissionScreenEvents(true, true, CommonUtils.hasLocationPermission(getApplicationContext()));
        if (ArraysKt___ArraysKt.contains(permissions, "android.permission.ACCESS_FINE_LOCATION")) {
            newAnalyticsApi.sendLocationPermissionEvent(CommonUtils.hasLocationPermission(this));
        }
        if (JioTVApplication.getInstance().shouldPrintLogcatLogsInFile && CommonUtils.hasExternalStoragePermission(this)) {
            CommonUtils.printLogcatLogsInFile(this);
        }
    }

    public final void proceedApplication() {
        String tag;
        String tag2;
        NewAnalyticsApi.INSTANCE.setStartTime(System.currentTimeMillis());
        String str = "Permission Activity";
        if (!NetworkUtil.isConnectionAvailable(this)) {
            getMBinding().splashProgreesBar.setVisibility(8);
            PermissionViewModel permissionViewModel = this.Z;
            if (permissionViewModel != null && (tag = permissionViewModel.getTAG()) != null) {
                str = tag;
            }
            LogUtils.log(str, lFxRbOk.cmhojBjm);
            CommonExtensionsKt.showFailureDialog(this, getString(R.string.try_again), getString(R.string.internet_error), 400, false);
            return;
        }
        getMBinding().splashProgreesBar.setVisibility(0);
        PermissionViewModel permissionViewModel2 = this.Z;
        if (permissionViewModel2 != null && (tag2 = permissionViewModel2.getTAG()) != null) {
            str = tag2;
        }
        LogUtils.log(str, "isConnectionAvailable true");
        this.a0.postDelayed(new kp(this, 7), 10000L);
        PermissionViewModel permissionViewModel3 = this.Z;
        if (permissionViewModel3 != null) {
            PermissionViewModel.preLoginCall$default(permissionViewModel3, 0, 1, null);
        }
    }

    public final void setFirebaseConfigUtil(@NotNull FirebaseConfigUtil firebaseConfigUtil) {
        Intrinsics.checkNotNullParameter(firebaseConfigUtil, "<set-?>");
        this.firebaseConfigUtil = firebaseConfigUtil;
    }

    public final void setMBinding(@NotNull ActivitySplashBinding activitySplashBinding) {
        Intrinsics.checkNotNullParameter(activitySplashBinding, "<set-?>");
        this.mBinding = activitySplashBinding;
    }

    public final void setPermissionViewModel(@Nullable PermissionViewModel permissionViewModel) {
        this.Z = permissionViewModel;
    }

    public final void setSplashScreen(@NotNull SplashScreen splashScreen) {
        Intrinsics.checkNotNullParameter(splashScreen, "<set-?>");
        this.splashScreen = splashScreen;
    }

    public final void t() {
        ObservableBoolean t0;
        ObservableBoolean p0;
        ObservableInt o0;
        ObservableBoolean k0;
        PermissionViewModel permissionViewModel = this.Z;
        if (permissionViewModel != null && (k0 = permissionViewModel.getK0()) != null) {
            k0.removeOnPropertyChangedCallback(this.e0);
        }
        PermissionViewModel permissionViewModel2 = this.Z;
        if (permissionViewModel2 != null && (o0 = permissionViewModel2.getO0()) != null) {
            o0.removeOnPropertyChangedCallback(this.k0);
        }
        PermissionViewModel permissionViewModel3 = this.Z;
        if (permissionViewModel3 != null && (p0 = permissionViewModel3.getP0()) != null) {
            p0.removeOnPropertyChangedCallback(this.l0);
        }
        PermissionViewModel permissionViewModel4 = this.Z;
        if (permissionViewModel4 != null && (t0 = permissionViewModel4.getT0()) != null) {
            t0.removeOnPropertyChangedCallback(this.m0);
        }
        AppDataManager.get().getShowDeviceLimitExceedDialog().removeObserver(this.d0);
        PermissionViewModel permissionViewModel5 = this.Z;
        if (permissionViewModel5 != null) {
            permissionViewModel5.clearAll();
        }
        this.Z = null;
    }

    public final void u() {
        String string;
        String tag;
        String string2;
        String string3;
        Uri data;
        Uri data2;
        PermissionViewModel permissionViewModel = this.Z;
        if (permissionViewModel != null) {
            permissionViewModel.initCrashlytics();
            permissionViewModel.setPreHomeScreenNavigation();
        }
        Intent intent = getIntent();
        String str = "";
        if (intent == null || (data2 = intent.getData()) == null || (string = data2.toString()) == null) {
            string = "";
        }
        LogUtils.log("DeepLinkManager", "Permission Activity set Home intent link: ".concat(string));
        Intent intent2 = new Intent(this, (Class<?>) HomeActivity.class);
        if (getIntent() != null) {
            Bundle bundle = new Bundle();
            bundle.putString(C.JAVASCRIPT_DEEPLINK, this.b0);
            bundle.putBoolean("isFromNotification", getIntent().getBooleanExtra("isFromNotification", false));
            Intent intent3 = getIntent();
            Bundle bundleExtra = intent3 != null ? intent3.getBundleExtra(AppConstants.IntentConstants.INTENT_REMINDER_BUNDLE) : null;
            Intent intent4 = new Intent(this, (Class<?>) HomeActivity.class);
            intent4.putExtras(bundle);
            Objects.toString(getIntent().getData());
            PermissionViewModel permissionViewModel2 = this.Z;
            int iNavigateToHomeScreen = permissionViewModel2 != null ? permissionViewModel2.navigateToHomeScreen(bundleExtra, getIntent()) : 4;
            if (iNavigateToHomeScreen != 1) {
                if (iNavigateToHomeScreen == 2) {
                    Intent intent5 = getIntent();
                    if (intent5 == null || (data = intent5.getData()) == null || (string3 = data.toString()) == null) {
                        string3 = "";
                    }
                    LogUtils.log("DeepLinkManager", "Permission Activity Home intent set link: ".concat(string3));
                    Uri data3 = getIntent().getData();
                    Intrinsics.checkNotNull(data3);
                    intent4.setData(data3);
                    JioTVApplication.getInstance().isAutoStart = false;
                } else if (iNavigateToHomeScreen == 3) {
                    intent4.setData(Uri.parse(AppDataManager.get().appConfig.getstartupDeeplinkUrl()));
                    JioTVApplication.getInstance().isAutoStart = true;
                    JioTVApplication.getInstance().isAutostartAlreadyDone = true;
                } else if (iNavigateToHomeScreen == 4) {
                    intent4.setData(null);
                }
            } else if (bundleExtra != null) {
                Parcelable parcelable = bundleExtra.getParcelable(AppConstants.IntentConstants.INTENT_PROGRAM_DATA);
                intent4.putExtra(AppConstants.IntentConstants.INTENT_PROGRAM_DATA, parcelable instanceof ExtendedProgramModel ? (ExtendedProgramModel) parcelable : null);
            }
            getIntent().removeExtra(AppConstants.IntentConstants.INTENT_REMINDER_BUNDLE);
            intent2 = intent4;
        } else {
            intent2.setData(null);
        }
        getMBinding().splashProgreesBar.setProgress(100);
        PermissionViewModel permissionViewModel3 = this.Z;
        if (permissionViewModel3 == null || (tag = permissionViewModel3.getTAG()) == null) {
            tag = "Permission Activity";
        }
        LogUtils.log(tag, "startActivity " + intent2);
        Uri data4 = intent2.getData();
        if (data4 != null && (string2 = data4.toString()) != null) {
            str = string2;
        }
        LogUtils.log("DeepLinkManager", "Permission Activity Home intent link: ".concat(str));
        getIntent().setData(null);
        startActivity(intent2);
        finishAndClear();
        overridePendingTransition(0, 0);
    }

    public final void w(int i, String str, String str2) {
        try {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(1);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.custom_jio_retry_dialog);
            View viewFindViewById = dialog.findViewById(R.id.txtDialogMessage);
            Intrinsics.checkNotNull(viewFindViewById, "null cannot be cast to non-null type android.widget.TextView");
            ((TextView) viewFindViewById).setText(str2);
            View viewFindViewById2 = dialog.findViewById(R.id.btnDialogPositive);
            Intrinsics.checkNotNull(viewFindViewById2, "null cannot be cast to non-null type android.widget.TextView");
            TextView textView = (TextView) viewFindViewById2;
            textView.setText(str);
            textView.setOnClickListener(new wr0(this, i, dialog, 1));
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (isFinishing()) {
                return;
            }
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void access$trackInstallReferrer(PermissionActivity permissionActivity, String str, InstallReferrerClient installReferrerClient) {
        permissionActivity.getClass();
        new Handler(permissionActivity.getMainLooper()).post(new q0(permissionActivity, str, 21));
        installReferrerClient.endConnection();
    }

    public final void finishAndClear() {
        t();
        finish();
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onDestroy() {
        super.onDestroy();
        t();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        LogUtils.log(this.Y, "onResume");
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onStop() {
        String loginDetails;
        super.onStop();
        JioPreferences jioPreferences = JioPreferences.getInstance(this);
        if (jioPreferences != null) {
            loginDetails = jioPreferences.getLoginDetails();
        } else {
            loginDetails = null;
        }
        if (loginDetails != null) {
            finishAndClear();
        }
    }

    @Override // com.jio.jioplay.tv.base.BaseActivity
    public void onUserLangPrefUpdated() {
        super.onUserLangPrefUpdated();
        LogUtils.log(this.Y, "onUserLangPrefUpdated: navigate to home screen");
        u();
    }

    public final void showHideSubtitle(boolean show) {
        int i;
        TextView textView = getMBinding().tvSubtitleMain;
        if (show) {
            i = 0;
        } else {
            i = 8;
        }
        textView.setVisibility(i);
    }

    public final void v(String[] strArr) {
        boolean zIsSupportedDevice = isSupportedDevice();
        boolean z = true;
        if (!SecurityUtils.isDebug && (!zIsSupportedDevice || !SecurityUtils.isValidBuild() || !SecurityUtils.isValidVersionName() || com.google.firebase.crashlytics.internal.common.CommonUtils.isRooted())) {
            if (com.google.firebase.crashlytics.internal.common.CommonUtils.isRooted()) {
                NewAnalyticsApi.INSTANCE.sendEvent("Rooted_devices");
            } else {
                NewAnalyticsApi.INSTANCE.sendEvent("Unsupported_device");
            }
            Toast.makeText(this, "Your device is not compatible with JioTV", 1).show();
            finishAndClear();
            return;
        }
        PermissionViewModel permissionViewModel = this.Z;
        if (permissionViewModel == null || !permissionViewModel.getL0()) {
            z = false;
        }
        Integer numValueOf = null;
        if (z) {
            Intent intent = new Intent();
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivityForResult(intent, 300);
            getMBinding().splashProgreesBar.setVisibility(0);
            return;
        }
        if (strArr.length == 0) {
            strArr[0] = "android.permission.READ_PHONE_STATE";
        }
        PermissionViewModel permissionViewModel2 = this.Z;
        if (permissionViewModel2 != null) {
            numValueOf = Integer.valueOf(permissionViewModel2.getA0());
        }
        Intrinsics.checkNotNull(numValueOf);
        ActivityCompat.requestPermissions(this, strArr, numValueOf.intValue());
    }

    public final void x() {
        String tag;
        getMBinding().splashProgreesBar.setProgress(20);
        PermissionViewModel permissionViewModel = this.Z;
        if (permissionViewModel == null || (tag = permissionViewModel.getTAG()) == null) {
            tag = "Permission Activity";
        }
        LogUtils.log(tag, " validateLoginStatus LoginController sendRequest");
        String loginDetails = JioPreferences.getInstance(JioTVApplication.getInstance()).getLoginDetails();
        if (loginDetails != null && loginDetails.length() > 10) {
            if (!JioTVApplication.getInstance().isGuestUser()) {
                AppDataManager.get().getUserProfile().readUserDetails(JioTVApplication.getInstance());
            }
            getMBinding().splashProgreesBar.setProgress(40);
            SharedPreferenceUtils.setFirstTimePermission(false);
            if (JioTVApplication.getInstance().shouldFallbackToPrevApis() && AppDataManager.get().getUserProfile().getUserType().equals("NONJIO")) {
                PermissionViewModel permissionViewModel2 = this.Z;
                if (permissionViewModel2 != null) {
                    permissionViewModel2.dialogWithOkFinish(getApplication().getString(R.string.error_msg_nonjio_something_went_wrong) + " code(2001)");
                }
                NewAnalyticsApi.INSTANCE.sendloginFunnelEvents("non_jio_not_allowed", "", "", "");
                return;
            }
            PermissionViewModel permissionViewModel3 = this.Z;
            if (permissionViewModel3 != null) {
                permissionViewModel3.loginUpdateComplete("OTP");
                return;
            }
            return;
        }
        LogUtils.log("LoginSDK", "request for login");
        getMBinding().loginView.setVisibility(0);
    }
}
