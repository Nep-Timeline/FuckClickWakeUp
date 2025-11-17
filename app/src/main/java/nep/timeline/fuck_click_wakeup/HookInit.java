package nep.timeline.fuck_click_wakeup;

import android.view.MotionEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam packageParam) {
        if ("com.android.systemui".equals(packageParam.packageName)) {
            ClassLoader classLoader = packageParam.classLoader;

            try {
                XposedHelpers.findAndHookMethod("com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController", classLoader, "registerPanoramicAodWakeUpMonitor", XC_MethodReplacement.DO_NOTHING);
                
                XposedHelpers.findAndHookMethod("com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep$OnDoubleClickListener", classLoader, "onSingleTapConfirmed", MotionEvent.class, XC_MethodReplacement.returnConstant(false));

                XposedHelpers.findAndHookMethod("com.oplus.systemui.notification.interruption.wakeup.WakeupScreenHelper", classLoader, "powerOnScreen", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object aodData = XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.oplus.systemui.aod.aodclock.constant.AodData", classLoader), "sAodData");
                        if (aodData == null)
                            return;
                        boolean isAodEnable = (boolean) XposedHelpers.callMethod(aodData, "isAodEnable");
                        if (!isAodEnable)
                            return;
                        boolean isPanoramicAod = (boolean) XposedHelpers.callMethod(aodData, "isPanoramicAod");
                        if (!isPanoramicAod)
                            return;
                        Object smoothTransitionController = XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.oplus.systemui.aod.display.SmoothTransitionController", classLoader), "sSmoothTransitionController");
                        if (smoothTransitionController == null)
                            return;
                        if (XposedHelpers.getBooleanField(smoothTransitionController, "userEnablePanoramicAllDay"))
                            return;
                        param.setResult(null);
                        // if (XposedHelpers.getBooleanField(aodData, "mAodIsInShow"))
                        //     return;
                        Object controller = XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.oplus.systemui.aod.display.OplusWakeUpController", classLoader), "instance");
                        if (controller == null)
                            return;
                        boolean isUpsideDown = XposedHelpers.getBooleanField(controller, "isUpsideDown");
                        if (!isUpsideDown)
                            XposedHelpers.callMethod(controller, "notifyWakeUpCallback", 0);
                    }
                });
            } catch (Throwable ignored) {
                XposedBridge.log(GlobalVars.TAG + " -> Your device is unsupported!");
            }
        }
    }
}
