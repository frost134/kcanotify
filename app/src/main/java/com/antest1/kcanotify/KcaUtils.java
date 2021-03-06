package com.antest1.kcanotify;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import com.commonsware.cwac.provider.StreamProvider;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.httpclient.ChunkedInputStream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static android.R.attr.data;
import static android.R.attr.min;
import static android.R.attr.orientation;
import static android.R.attr.value;
import static com.antest1.kcanotify.KcaAlarmService.ALARM_CHANNEL_ID;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_DISABLE_CUSTOMTOAST;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_UPDATE_SERVER;

public class KcaUtils {
    public static String getStringFromException(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString().replaceAll("\n", " / ").replaceAll("\t", "");
    }

    public static String format(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    public static JsonElement parseJson(String v) {
        return new JsonParser().parse(v);
    }

    public static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        if (list.size() > 0) {
            int i;
            for (i = 0; i < list.size() - 1; i++) {
                resultStr = resultStr.concat(list.get(i));
                resultStr = resultStr.concat(delim);
            }
            resultStr = resultStr.concat(list.get(i));
        }
        return resultStr;
    }

    public static String getStringPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        try {
            return String.valueOf(pref.getInt(key, 0));
        } catch (Exception e) {
            // Nothing to do
        }
        return pref.getString(key, "");
    }

    public static Boolean getBooleanPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        return pref.getBoolean(key, false);
    }

    // 값 저장하기
    public static void setPreferences(Context ctx, String key, Object value) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putString(key, String.valueOf(value));
        } else {
            editor.putString(key, value.toString());
        }
        editor.commit();
    }

    public static String getUpdateServer(Context ctx) {
        return getStringPreferences(ctx, PREF_UPDATE_SERVER);
    }

    public static byte[] gzipcompress(String value) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutStream = new GZIPOutputStream(
                new BufferedOutputStream(byteArrayOutputStream));
        gzipOutStream.write(value.getBytes());
        gzipOutStream.finish();
        gzipOutStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gzipdecompress(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ByteStreams.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private static int bytetoint(byte[] arr) {
        int csize = 0;
        for (int i = 0; i < arr.length; i++) {
            csize = csize << 4;
            if (arr[i] >= 0x30 && arr[i] <= 0x39) {
                csize += arr[i] - 0x30; // (0x30 = '0')
            } else if (arr[i] >= 0x61 && arr[i] <= 0x66) {
                csize += arr[i] - 0x61 + 0x0a; // (0x61 = 'a')
            } else if (arr[i] >= 0x41 && arr[i] <= 0x46) {
                csize += arr[i] - 0x41 + 0x0a; // (0x41 = 'A')
            }
        }
        return csize;
    }

    public static byte[] unchunkdata(byte[] contentBytes) throws IOException {
        byte[] unchunkedData = null;
        byte[] buffer = new byte[1024];
        ByteArrayInputStream bis = new ByteArrayInputStream(contentBytes);
        ChunkedInputStream cis = new ChunkedInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int read = -1;
        while ((read = cis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        unchunkedData = bos.toByteArray();
        bos.close();

        return unchunkedData;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (final byte b : a)
            sb.append(KcaUtils.format("%02x ", b & 0xff));
        return sb.toString();
    }

    public static boolean[] makeExcludeFlag(int[] list) {
        boolean[] flag = {false, false, false, false, false, false};
        for (int i = 0; i < list.length; i++) {
            flag[list[i]] = true;
        }
        return flag;
    }

    public static boolean isPackageExist(Context context, String name) {
        boolean isExist = false;

        PackageManager pkgMgr = context.getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith(name)) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    public static Intent getKcIntent(Context context) {
        Intent kcIntent;
        if (isPackageExist(context, KC_PACKAGE_NAME)) {
            kcIntent = context.getPackageManager().getLaunchIntentForPackage(KC_PACKAGE_NAME);
            kcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return kcIntent;
        } else {
            return null;
        }
    }

    public static Context getContextWithLocale(Context ac, Context bc) {
        Locale locale;
        String[] pref_locale = getStringPreferences(ac, PREF_KCA_LANGUAGE).split("-");
        if (pref_locale[0].equals("default")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(pref_locale[0], pref_locale[1]);
        }
        Configuration configuration = new Configuration(ac.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            return bc.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            DisplayMetrics metrics = new DisplayMetrics();
            bc.getResources().updateConfiguration(configuration, bc.getResources().getDisplayMetrics());
            return bc;
        }
    }



    public static String getStringWithLocale(Context ac, Context bc, int id) {
        return getContextWithLocale(ac, bc).getString(id);
    }

    public static JsonObject getJsonObjectCopy(JsonObject data) {
        return new JsonParser().parse(data.toString()).getAsJsonObject();
    }

    public static int setDefaultGameData(Context context, KcaDBHelper helper) {
        boolean valid_data = false;
        String current_version = getStringPreferences(context, PREF_KCA_DATA_VERSION);
        String default_version = context.getString(R.string.default_gamedata_version);

        if (helper.getJsonObjectValue(DB_KEY_STARTDATA) != null && KcaUtils.compareVersion(current_version, default_version)) {
            if (KcaApiData.isGameDataLoaded()) return 1;
            JsonObject start_data = helper.getJsonObjectValue(DB_KEY_STARTDATA);
            if (start_data.has("api_data") && start_data.get("api_data").isJsonObject()) {
                KcaApiData.getKcGameData(start_data.getAsJsonObject("api_data"));
                valid_data = true;
            }
        }

        if (!valid_data) {
            try {
                AssetManager assetManager = context.getAssets();
                AssetManager.AssetInputStream ais =
                        (AssetManager.AssetInputStream) assetManager.open("api_start2");
                byte[] bytes = KcaUtils.gzipdecompress(ByteStreams.toByteArray(ais));
                helper.putValue(DB_KEY_STARTDATA, new String(bytes));
                JsonElement data = new JsonParser().parse(new String(bytes));
                JsonObject api_data = new Gson().fromJson(data, JsonObject.class).getAsJsonObject("api_data");
                KcaApiData.getKcGameData(api_data);
                setPreferences(context, PREF_KCA_VERSION, default_version);
                setPreferences(context, PREF_KCA_DATA_VERSION, default_version);
            } catch (Exception e) {
                return 0;
            }
            return 1;
        } else {
            return 1;
        }
    }

    public static Uri getContentUri(@NonNull Context context, @NonNull Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (uri.toString().startsWith("file")) {
                File file = new File(uri.getPath());
                Uri content_uri = StreamProvider.getUriForFile("com.antest1.kcanotify.provider", file);
                return content_uri;
            } else {
                return uri;
            }
        }
        return uri;
    }

    public static void playNotificationSound(MediaPlayer mediaPlayer, Context context, Uri uri) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            if (!uri.equals(Uri.EMPTY)) {
                mediaPlayer.setDataSource(context, uri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attr = new AudioAttributes.Builder()
                            .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                            .build();
                    mediaPlayer.setAudioAttributes(attr);
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                }
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    public static int getNotificationId(int type, int n) {
        return n + 1000 * type;
    }

    public static int getId(String resourceName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resourceName);
            return idField.getInt(idField);
        } catch (Exception e) {
            throw new RuntimeException("No resource ID found for: "
                    + resourceName + " / " + c, e);
        }
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    // True: latest, False: need to update
    public static boolean compareVersion(String version_current, String version_default) {
        if (version_current != null && version_current.length() == 0) return false;
        if (version_current.equals(version_default)) return true;
        String[] current_split = version_current.replace("r", ".0.").split("\\.");
        String[] default_split = version_default.replace("r", ".0.").split("\\.");
        int min_length = Math.min(current_split.length, default_split.length);
        for (int i = 0; i < min_length; i++) {
            if (Integer.parseInt(current_split[i]) > Integer.parseInt(default_split[i]))
                return true;
            else if (Integer.parseInt(current_split[i]) < Integer.parseInt(default_split[i]))
                return false;
        }
        return current_split.length > default_split.length;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static int getWindowLayoutType() {
        int windowLayoutType = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        return windowLayoutType;
    }

    public static NotificationCompat.Builder createBuilder(Context context, String channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new NotificationCompat.Builder(context, channel);
        } else {
            return new NotificationCompat.Builder(context);
        }
    }

    public static String getTimeStr(int left_time, boolean is_min) {
        int sec, min, hour;
        sec = left_time;
        min = sec / 60;
        hour = min / 60;
        sec = sec % 60;
        min = min % 60;
        if (is_min) return KcaUtils.format("%02d:%02d", hour * 60 + min, sec);
        else return KcaUtils.format("%02d:%02d:%02d", hour, min, sec);
    }

    public static String getTimeStr(int left_time) {
        return getTimeStr(left_time, false);
    }

    public static Calendar getJapanCalendarInstance() {
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    public static SimpleDateFormat getJapanSimpleDataFormat(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return dateFormat;
    }

    public static long getCurrentDateTimestamp (long current_time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        String timetext = dateFormat.format(new Date(current_time));
        long timestamp = 0;
        try {
            timestamp = dateFormat.parse(timetext).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    public static int getLastDay(int year, int month) {
        int[] day31 = {1, 3, 5, 7, 8, 10, 12};
        if (month == 2) {
            if (year % 100 != 0 && year % 4 == 0) return 29;
            else return 28;
        } else {
            return Arrays.binarySearch(day31, month) >= 0 ? 31 : 30;
        }
    }

    public static void doVibrate(Vibrator v, int time) {
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(time);
        }
    }

    public static void showCustomToast(Context a, Context b, KcaCustomToast toast, String body, int duration, int color) {
        if (getBooleanPreferences(a, PREF_DISABLE_CUSTOMTOAST)) {
            JsonObject data = new JsonObject();
            data.addProperty("text", body);
            data.addProperty("duration", duration);
            data.addProperty("color", color);
            Intent toastIntent = new Intent(b, KcaCustomToastService.class);
            toastIntent.setAction(KcaCustomToastService.TOAST_SHOW_ACTION);
            toastIntent.putExtra("data", data.toString());
            a.startService(toastIntent);
            //Toast.makeText(ctx, body, duration).show();
        } else {
            toast.showToast(body, duration, color);
        }
    }

    public static String getOrientationPrefix(int value) {
        if (value == Configuration.ORIENTATION_PORTRAIT) {
            return "ori_v_";
        } else {
            return "ori_h_";
        }
    }

    public static KcaDownloader getDewnloader(Context context){
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.MINUTES)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(KcaUtils.getUpdateServer(context))
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        return retrofit.create(KcaDownloader.class);
    }

    public static boolean checkOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    public static int getGravity(int status) {
        int value;
        switch (status) {
            case 1:
                value = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                break;
            case 0:
                value = Gravity.CENTER;
                break;
            case -1:
                value = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                break;
            default:
                value = Gravity.CENTER;
                break;
        }
        return value;
    }

    // Image Downscale Functions from Android Reference
    // https://developer.android.com/topic/performance/graphics/load-bitmap.html

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}
