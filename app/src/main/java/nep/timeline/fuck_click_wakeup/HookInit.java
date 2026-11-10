package nep.timeline.fuck_click_wakeup;

import android.view.MotionEvent;

import java.lang.reflect.Method;

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
                Class<?> clazz = XposedHelpers.findClassIfExists("com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController", classLoader);
                if (clazz == null) {
                    Class<?> controller = XposedHelpers.findClassIfExists("com.oplus.systemui.aod.scene.PanoramicAodGestureController", classLoader);
                    if (controller == null) {
                        XposedBridge.log(GlobalVars.TAG + " -> Your device is unsupported!");
                        return;
                    } else
                        for (Method method : controller.getDeclaredMethods()) {
                            if (!method.getName().equals("registerPanoramicAodGestureMonitor"))
                                continue;

                            XposedBridge.hookMethod(method, XC_MethodReplacement.DO_NOTHING);
                        }
                } else
                    XposedHelpers.findAndHookMethod("com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController", classLoader, "registerPanoramicAodWakeUpMonitor", XC_MethodReplacement.DO_NOTHING);
                
                XposedHelpers.findAndHookMethod("com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep$OnDoubleClickListener", classLoader, "onSingleTapConfirmed", MotionEvent.class, XC_MethodReplacement.returnConstant(false));

                XposedHelpers.findAndHookMethod("com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep", classLoader, "-$$Nest$mprocessPanoramicWakeup", "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep", XC_MethodReplacement.returnConstant(false));

                Class<?> helper = XposedHelpers.findClassIfExists("com.oplus.systemui.notification.interruption.wakeup.WakeupScreenHelper", classLoader);
                if (helper == null) {
                    XposedBridge.log(GlobalVars.TAG + " -> Your device is unsupported!");
                    return;
                }

                for (Method method : helper.getDeclaredMethods()) {
                    if (!method.getName().equals("powerOnScreen"))
                        continue;

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
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
                            param.setResult(null);
                            if (XposedHelpers.getBooleanField(aodData, "mAodIsInShow"))
                                return;
                            Object controller = XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.oplus.systemui.aod.display.OplusWakeUpController", classLoader), "instance");
                            if (controller == null)
                                return;
                            boolean isUpsideDown = XposedHelpers.getBooleanField(controller, "isUpsideDown");
                            if (!isUpsideDown)
                                XposedHelpers.callMethod(controller, "notifyWakeUpCallback", 0);
                        }
                    });
                }
            } catch (Throwable t) {
                XposedBridge.log(GlobalVars.TAG + " -> Your device is unsupported!");
                XposedBridge.log(t);
            }
        }
    }
}
