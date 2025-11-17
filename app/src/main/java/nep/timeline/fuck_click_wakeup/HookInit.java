package nep.timeline.fuck_click_wakeup;

import android.util.Log;
import android.content.Context;
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
                        param.setResult(null);
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Class<?> companion = XposedHelpers.findClass("com.oplus.systemui.aod.display.OplusWakeUpController$Companion", classLoader);
                        Object controller = XposedHelpers.callStaticMethod(companion, "getInstance", context);
                        boolean isUpsideDown = XposedHelpers.getBooleanField(companion, "isUpsideDown");
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
