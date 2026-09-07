package org.lyf.chromeautofillbridge;

import android.app.Activity;
import android.view.View;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.graphics.Rect;
import android.util.SparseArray;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Map;
import java.util.WeakHashMap;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.autofill.AutofillManager;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;

/** Experimental restoration of the framework compatibility bridge inside Chrome. */
public final class ModuleMain extends XposedModule {
    private static final String CHROME = "com.android.chrome";
    private static final String TAG = "ChromeAutofillBridge";
    private boolean mainProcess;
    private volatile boolean bridgeActive;
    private boolean installed;
    private Method enableCompat;
    private Method isCompat;
    private final ThreadLocal<String> documentOrigin = new ThreadLocal<>();
    private final ThreadLocal<Integer> documentDepth = ThreadLocal.withInitial(() -> 0);
    private final Map<View,String> capturedOrigins = new WeakHashMap<>();

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        mainProcess = CHROME.equals(param.getProcessName());
    }

    @Override public void onPackageLoaded(PackageLoadedParam param) {
        if (!mainProcess || installed || !CHROME.equals(param.getPackageName())) return;
        try {
            enableCompat = AutofillManager.class.getDeclaredMethod("enableCompatibilityMode");
            isCompat = AutofillManager.class.getDeclaredMethod("isCompatibilityModeEnabledLocked");
            enableCompat.setAccessible(true);
            isCompat.setAccessible(true);
            Method contextCompat = Context.class.getDeclaredMethod("isAutofillCompatibilityEnabled");
            hook(contextCompat).intercept(chain -> bridgeActive ? true : chain.proceed());
            hook(Activity.class.getDeclaredMethod("onResume")).intercept(chain -> {
                Object result = chain.proceed();
                Activity activity = (Activity) chain.getThisObject();
                // Defer until the complete subclass onResume has finished.
                activity.getWindow().getDecorView().post(() -> activate(activity));
                return result;
            });
            installOriginHooks();
            installed = true;
            log(Log.INFO, TAG, "API " + getApiVersion() + ": hooks installed; waiting for Chrome activity");
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Required framework hook unavailable", error);
        }
    }

    /** R8 renames members; match stable Chromium interface/return types instead. */
    private Object webContents(View view) throws Exception {
        for(Class<?> c=view.getClass();c!=null && !c.getName().startsWith("android.");c=c.getSuperclass()) {
            for(Field f:c.getDeclaredFields()) {
                if(f.getType().getName().equals("org.chromium.content_public.browser.WebContents")
                        && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    return f.get(view);
                }
            }
        }
        return null;
    }

    private String origin(View view) {
        try {
            Object wc=webContents(view);
            if(wc==null)return null;
            String agreed=null;
            int getters=0;
            for(Method m:wc.getClass().getMethods()) {
                if(m.getParameterCount()!=0 || Modifier.isStatic(m.getModifiers())
                        || !m.getReturnType().getName().equals("org.chromium.url.GURL"))continue;
                // Chromium exposes exactly getVisibleUrl and getLastCommittedUrl here.
                // Require both to agree, including the port, before assigning an origin.
                Object gurl=m.invoke(wc);
                if(gurl==null)return null;
                String spec=null;
                int strings=0;
                for(Field f:gurl.getClass().getDeclaredFields()) {
                    if(f.getType()==String.class && !Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true); spec=(String)f.get(gurl); strings++;
                    }
                }
                if(strings!=1 || spec==null)return null;
                URI u=new URI(spec);
                if(!"https".equalsIgnoreCase(u.getScheme()) || u.getHost()==null || u.getRawUserInfo()!=null)return null;
                String current="https://"+u.getHost().toLowerCase(java.util.Locale.ROOT)
                        +(u.getPort()==-1 || u.getPort()==443 ? "" : ":"+u.getPort());
                if(agreed!=null && !agreed.equals(current))return null;
                agreed=current; getters++;
            }
            return getters==2 ? agreed : null;
        } catch(Throwable ignored) { return null; }
    }

    private void installOriginHooks() throws Exception {
        Method populate=View.class.getDeclaredMethod("populateVirtualStructure",ViewStructure.class,
                AccessibilityNodeProvider.class,AccessibilityNodeInfo.class,AccessibilityNodeInfo.class,boolean.class);
        hook(populate).intercept(chain -> {
            if(!bridgeActive || !Boolean.TRUE.equals(chain.getArg(4)))return chain.proceed();
            View view=(View)chain.getThisObject();
            ViewStructure structure=(ViewStructure)chain.getArg(0);
            AccessibilityNodeInfo info=(AccessibilityNodeInfo)chain.getArg(2);
            String role=info.getExtras().getString("AccessibilityNodeInfo.chromeRole", "");
            int depth=documentDepth.get();
            boolean document="rootWebArea".equals(role) || "webArea".equals(role);
            boolean frame=role.toLowerCase(java.util.Locale.ROOT).contains("iframe");
            if(frame || (document && depth>0)) {
                structure.setChildCount(0);
                return null;
            }
            if(document) {
                String current=origin(view);
                if(current==null) { structure.setChildCount(0); return null; }
                structure.setWebDomain(current);
                capturedOrigins.put(view,current);
                documentDepth.set(depth+1);
                documentOrigin.set(current);
                try { return chain.proceed(); }
                finally { documentDepth.set(depth); documentOrigin.remove(); }
            }
            if(depth>0 && documentOrigin.get()!=null)structure.setWebDomain(documentOrigin.get());
            if(info.isEditable() && depth==0) { structure.setChildCount(0); return null; }
            return chain.proceed();
        });
        // Keep native Chrome UI, such as the omnibox, out of third-party package-name matching.
        hook(AutofillManager.class.getDeclaredMethod("notifyViewEntered",View.class)).intercept(chain -> {
            if(bridgeActive)return null;
            return chain.proceed();
        });
        hook(AutofillManager.class.getDeclaredMethod("notifyViewEntered",View.class,int.class,Rect.class)).intercept(chain -> {
            if(bridgeActive && origin((View)chain.getArg(0))==null)return null;
            return chain.proceed();
        });
        hook(View.class.getDeclaredMethod("autofill",SparseArray.class)).intercept(chain -> {
            if(bridgeActive) {
                View view=(View)chain.getThisObject();
                String captured=capturedOrigins.get(view);
                if(captured==null || !captured.equals(origin(view)))return null;
            }
            return chain.proceed();
        });
    }

    private void activate(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        try {
            String service = Settings.Secure.getString(activity.getContentResolver(), "autofill_service");
            if (!isThirdPartyAutofillService(service)) {
                log(Log.INFO, TAG, "Select a third-party Android autofill service");
                return;
            }
            Uri state = Uri.parse("content://" + CHROME
                    + ".AutofillThirdPartyModeContentProvider/autofill_third_party_mode");
            try (Cursor cursor = activity.getContentResolver().query(state,
                    new String[]{"autofill_third_party_state"}, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) {
                    log(Log.WARN, TAG, "Chrome mode unavailable; activation skipped");
                    return;
                }
                int index = cursor.getColumnIndex("autofill_third_party_state");
                if (index < 0 || cursor.getInt(index) != 0) {
                    log(Log.INFO, TAG, "Select Autofill with Google in Chrome and restart Chrome");
                    return;
                }
            }
            Method getOptions = Context.class.getDeclaredMethod("getAutofillOptions");
            Object options = getOptions.invoke(activity);
            if (options != null) {
                var compatField = options.getClass().getField("compatModeEnabled");
                compatField.setAccessible(true);
                compatField.setBoolean(options, true);
                log(Log.INFO, TAG, "Process-local AutofillOptions compatibility flag set");
            }
            AutofillManager manager = activity.getSystemService(AutofillManager.class);
            if (manager == null || !manager.isEnabled()) return;
            // Every resumed activity gets its own bridge registered as the current policy.
            // The framework itself uses this re-registration pattern across activities.
            enableCompat.invoke(manager);
            bridgeActive = Boolean.TRUE.equals(isCompat.invoke(manager));
            log(Log.INFO, TAG, bridgeActive
                    ? "Compatibility bridge active with document-origin binding"
                    : "Compatibility bridge inactive");
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Bridge activation failed", error);
        }
    }

    private static boolean isThirdPartyAutofillService(String service) {
        if (service == null || service.isBlank()) return false;
        int slash = service.indexOf('/');
        String packageName = slash > 0 ? service.substring(0, slash) : service;
        if (packageName.isBlank()) return false;
        // Google Password Manager stays enabled in Chrome; this bridge is for the
        // separately selected Android provider (Bitwarden, 1Password, and others).
        return !packageName.equals("com.google.android.gms")
                && !packageName.equals("com.google.android.googlequicksearchbox")
                && !packageName.equals("android");
    }
}
