package com.antest1.kcanotify;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.getAirForceResultString;
import static com.antest1.kcanotify.KcaApiData.getCurrentNodeAlphabet;
import static com.antest1.kcanotify.KcaApiData.getEngagementString;
import static com.antest1.kcanotify.KcaApiData.getFormationString;
import static com.antest1.kcanotify.KcaApiData.getItemString;
import static com.antest1.kcanotify.KcaApiData.getNodeFullInfo;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaConstants.*;

public class KcaBattleViewService extends Service {
    LayoutInflater mInflater;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver refreshreceiver, hdmgreceiver;
    public static boolean active;
    public static JsonObject api_data;
    public static String currentNodeInfo = "";
    public static int[] startNowHps;
    public static int[] startNowHpsCombined;

    boolean fc_flag = false;
    boolean ec_flag = false;

    JsonArray api_formation;
    JsonObject api_kouku;

    private View mView;
    private WindowManager mManager;
    private View mViewBackup;

    private int[] shipViewList = {0,
            R.id.fm_1, R.id.fm_2, R.id.fm_3, R.id.fm_4, R.id.fm_5, R.id.fm_6,
            R.id.em_1, R.id.em_2, R.id.em_3, R.id.em_4, R.id.em_5, R.id.em_6
    };

    private int[] shipNameViewList = {0,
            R.id.fm_1_name, R.id.fm_2_name, R.id.fm_3_name, R.id.fm_4_name, R.id.fm_5_name, R.id.fm_6_name,
            R.id.em_1_name, R.id.em_2_name, R.id.em_3_name, R.id.em_4_name, R.id.em_5_name, R.id.em_6_name
    };

    private int[] shipLevelViewList = {0,
            R.id.fm_1_lv, R.id.fm_2_lv, R.id.fm_3_lv, R.id.fm_4_lv, R.id.fm_5_lv, R.id.fm_6_lv,
            R.id.em_1_lv, R.id.em_2_lv, R.id.em_3_lv, R.id.em_4_lv, R.id.em_5_lv, R.id.em_6_lv
    };

    private int[] shipHpTxtViewList = {0,
            R.id.fm_1_hp_txt, R.id.fm_2_hp_txt, R.id.fm_3_hp_txt, R.id.fm_4_hp_txt, R.id.fm_5_hp_txt, R.id.fm_6_hp_txt,
            R.id.em_1_hp_txt, R.id.em_2_hp_txt, R.id.em_3_hp_txt, R.id.em_4_hp_txt, R.id.em_5_hp_txt, R.id.em_6_hp_txt
    };

    private int[] shipHpBarViewList = {0,
            R.id.fm_1_hp_bar, R.id.fm_2_hp_bar, R.id.fm_3_hp_bar, R.id.fm_4_hp_bar, R.id.fm_5_hp_bar, R.id.fm_6_hp_bar,
            R.id.em_1_hp_bar, R.id.em_2_hp_bar, R.id.em_3_hp_bar, R.id.em_4_hp_bar, R.id.em_5_hp_bar, R.id.em_6_hp_bar
    };

    private int[] shipYomiViewList = {0, 0, 0, 0, 0, 0, 0,
            R.id.em_1_yomi, R.id.em_2_yomi, R.id.em_3_yomi, R.id.em_4_yomi, R.id.em_5_yomi, R.id.em_6_yomi
    };

    private int[] shipCombinedViewList = {0,
            R.id.fs_1, R.id.fs_2, R.id.fs_3, R.id.fs_4, R.id.fs_5, R.id.fs_6,
            R.id.es_1, R.id.es_2, R.id.es_3, R.id.es_4, R.id.es_5, R.id.es_6
    };

    private int[] shipNameCombinedViewList = {0,
            R.id.fs_1_name, R.id.fs_2_name, R.id.fs_3_name, R.id.fs_4_name, R.id.fs_5_name, R.id.fs_6_name,
            R.id.es_1_name, R.id.es_2_name, R.id.es_3_name, R.id.es_4_name, R.id.es_5_name, R.id.es_6_name
    };

    private int[] shipLevelCombinedViewList = {0,
            R.id.fs_1_lv, R.id.fs_2_lv, R.id.fs_3_lv, R.id.fs_4_lv, R.id.fs_5_lv, R.id.fs_6_lv,
            R.id.es_1_lv, R.id.es_2_lv, R.id.es_3_lv, R.id.es_4_lv, R.id.es_5_lv, R.id.es_6_lv
    };

    private int[] shipHpTxtCombinedViewList = {0,
            R.id.fs_1_hp_txt, R.id.fs_2_hp_txt, R.id.fs_3_hp_txt, R.id.fs_4_hp_txt, R.id.fs_5_hp_txt, R.id.fs_6_hp_txt,
            R.id.es_1_hp_txt, R.id.es_2_hp_txt, R.id.es_3_hp_txt, R.id.es_4_hp_txt, R.id.es_5_hp_txt, R.id.es_6_hp_txt
    };

    private int[] shipHpBarCombinedViewList = {0,
            R.id.fs_1_hp_bar, R.id.fs_2_hp_bar, R.id.fs_3_hp_bar, R.id.fs_4_hp_bar, R.id.fs_5_hp_bar, R.id.fs_6_hp_bar,
            R.id.es_1_hp_bar, R.id.es_2_hp_bar, R.id.es_3_hp_bar, R.id.es_4_hp_bar, R.id.es_5_hp_bar, R.id.es_6_hp_bar
    };

    private int[] shipYomiCombinedViewList = {0, 0, 0, 0, 0, 0, 0,
            R.id.es_1_yomi, R.id.es_2_yomi, R.id.es_3_yomi, R.id.es_4_yomi, R.id.es_5_yomi, R.id.es_6_yomi
    };

    WindowManager.LayoutParams mParams;
    ScrollView battleview;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean getStatus() {
        return active;
    }

    private static String makeHpString(int currenthp, int maxhp) {
        return String.format("HP %d/%d", currenthp, maxhp);
    }

    private static String makeLvString(int level) {
        return String.format("Lv %d", level);
    }

    public static int getFriendIdx(int i) {
        return i;
    }

    public static int getEnemyIdx(int i) {
        return i + 6;
    }

    public static int getFriendCbIdx(int i) {
        return i - 6;
    }

    public static int getEnemyCbIdx(int i) {
        return i;
    }

    public static int getStatus(int value) {
        if (value > 75) return STATE_NORMAL;
        else if (value > 50) return STATE_LIGHTDMG;
        else if (value > 25) return STATE_MODERATEDMG;
        else return STATE_HEAVYDMG;
    }

    public Drawable getProgressDrawable(Context context, int value) {
        if (value > 75) {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_normal);
        } else if (value > 50) {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_lightdmg);
        } else if (value > 25) {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_moderatedmg);
        } else {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_heavydmg);
        }
    }

    public void setBattleview() {
        if (api_data != null) {
            boolean is_practice = api_data.has("api_practice_flag");

            if (api_data.has("api_maparea_id")) { // start, next
                Log.e("KCA", "START/NEXT");
                //Log.e("KCA", api_data.toString());
                int api_maparea_id = api_data.get("api_maparea_id").getAsInt();
                int api_mapinfo_no = api_data.get("api_mapinfo_no").getAsInt();
                int api_no = api_data.get("api_no").getAsInt();
                String currentNode = getCurrentNodeAlphabet(api_maparea_id, api_mapinfo_no, api_no);
                int api_event_id = api_data.get("api_event_id").getAsInt();
                int api_event_type = api_data.get("api_event_kind").getAsInt();
                int api_color_no = api_data.get("api_color_no").getAsInt();
                currentNodeInfo = getNodeFullInfo(getApplicationContext(), currentNode, api_event_id, api_event_type, true);
                currentNodeInfo = currentNodeInfo.replaceAll("[()]", "");

                // View Settings
                fc_flag = KcaBattle.isCombined;
                ec_flag = (api_event_type == API_NODE_EVENT_KIND_ECBATTLE);
                setViewLayout(fc_flag, false);

                ((TextView) battleview.findViewById(R.id.battle_node)).setText(currentNodeInfo);
                ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");

                if (api_event_id == API_NODE_EVENT_ID_OBTAIN) {
                    JsonArray api_itemget = api_data.getAsJsonArray("api_itemget");
                    List<String> itemTextList = new ArrayList<String>();
                    for (int i = 0; i < api_itemget.size(); i++) {
                        JsonObject itemdata = api_itemget.get(i).getAsJsonObject();
                        String itemname = getItemString(getApplicationContext(), itemdata.get("api_id").getAsInt());
                        int itemgetcount = itemdata.get("api_getcount").getAsInt();
                        itemTextList.add(String.format("%s +%d", itemname, itemgetcount));
                    }
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(KcaUtils.joinStr(itemTextList, " / "));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                } else if (api_event_id == API_NODE_EVENT_ID_AIR) {
                    JsonObject itemdata = api_data.getAsJsonObject("api_itemget");
                    String itemname = getItemString(getApplicationContext(), itemdata.get("api_id").getAsInt());
                    int itemgetcount = itemdata.get("api_getcount").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(String.format("%s +%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItemSpecial));
                } else if (api_event_id == API_NODE_EVENT_ID_LOSS) {
                    JsonObject api_happening = api_data.getAsJsonObject("api_happening");
                    String itemname = getItemString(getApplicationContext(), api_happening.get("api_mst_id").getAsInt());
                    int itemgetcount = api_happening.get("api_count").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(String.format("%s -%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorVortex));
                } else if (api_event_id == API_NODE_EVENT_ID_SENDAN) {
                    ((LinearLayout) mView.findViewById(R.id.battleviewpanel))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    JsonObject api_itemget_eo_comment = api_data.getAsJsonObject("api_itemget_eo_comment");
                    String itemname = getItemString(getApplicationContext(), api_itemget_eo_comment.get("api_id").getAsInt());
                    int itemgetcount = api_itemget_eo_comment.get("api_getcount").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(String.format("%s +%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
                }

                switch (api_color_no) {
                    case 2:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                        break;
                    case 6:
                    case 9:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItemSpecial));
                        break;
                    case 3:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorVortex));
                        break;
                    case 4:
                        if (api_event_id == API_NODE_EVENT_ID_NOEVENT) {
                            if (api_event_type == API_NODE_EVENT_KIND_SELECTABLE) { // selectable
                                battleview.findViewById(R.id.battle_node)
                                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSelectable));
                            } else {
                                battleview.findViewById(R.id.battle_node)
                                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
                            }

                        } else {
                            battleview.findViewById(R.id.battle_node)
                                    .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBattle));
                        }
                        break;
                    case 5:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBossBattle));
                        break;
                    case 7:
                    case 10:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAirBattle));
                        break;
                    case 8:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
                        break;
                    default:
                        break;
                }
            }

            if (is_practice) {
                ((TextView) battleview.findViewById(R.id.battle_node)).setText(getString(R.string.node_info_practice));
                ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");
                battleview.findViewById(R.id.battle_node)
                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
            }

            if (api_data.has("api_deck_port")) { // common sortie, practice
                setViewLayout(fc_flag, ec_flag);
                JsonObject deckportdata = api_data.getAsJsonObject("api_deck_port");
                if (deckportdata != null) {
                    JsonArray deckData = deckportdata.getAsJsonArray("api_deck_data");
                    JsonArray portData = deckportdata.getAsJsonArray("api_ship_data");
                    for (int i = 7; i < 13; i++) {
                        battleview.findViewById(shipViewList[i]).setVisibility(View.INVISIBLE);
                        battleview.findViewById(shipCombinedViewList[i]).setVisibility(View.INVISIBLE);
                    }
                    for (int i = 0; i < deckData.size(); i++) {
                        if (i == 0) {
                            JsonObject mainDeckData = deckData.get(i).getAsJsonObject();
                            ((TextView) battleview.findViewById(R.id.friend_fleet_name)).
                                    setText(mainDeckData.get("api_name").getAsString());
                            JsonArray mainDeck = mainDeckData.getAsJsonArray("api_ship");

                            JsonObject shipData = new JsonObject();
                            //Log.e("KCA", String.valueOf(portData.size()));
                            for (int j = 0; j < portData.size(); j++) {
                                JsonObject data = portData.get(j).getAsJsonObject();
                                shipData.add(String.valueOf(data.get("api_id").getAsInt()), data);
                            }

                            for (int j = 0; j < mainDeck.size(); j++) {
                                if (mainDeck.get(j).getAsInt() == -1) {
                                    //Log.e("KCA", String.format("%d: invisible", j + 1));
                                    battleview.findViewById(shipViewList[j + 1]).setVisibility(View.INVISIBLE);
                                } else {
                                    //Log.e("KCA", String.format("%d: visible", j + 1));
                                    JsonObject data = shipData.getAsJsonObject(String.valueOf(mainDeck.get(j)));
                                    int maxhp = data.get("api_maxhp").getAsInt();
                                    int nowhp = data.get("api_nowhp").getAsInt();
                                    int level = data.get("api_lv").getAsInt();
                                    JsonObject kcShipData = KcaApiData.getKcShipDataById(data.get("api_ship_id").getAsInt(), "name");
                                    String kcname = getShipTranslation(kcShipData.get("name").getAsString());
                                    ((TextView) battleview.findViewById(shipNameViewList[j + 1])).setText(kcname);
                                    ((TextView) battleview.findViewById(shipNameViewList[j + 1]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

                                    if (fc_flag) {
                                        ((TextView) battleview.findViewById(shipNameViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                        battleview.findViewById(shipNameViewList[j + 1]).setPadding(0, 2, 0, 0);
                                    } else {
                                        ((TextView) battleview.findViewById(shipNameViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                    }

                                    ((TextView) battleview.findViewById(shipLevelViewList[j + 1])).setText(makeLvString(level));
                                    ((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).setText(makeHpString(nowhp, maxhp));
                                    if (fc_flag || ec_flag) {
                                        ((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                                    } else {
                                        ((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                    }

                                    int hpPercent = nowhp * VIEW_HP_MAX / maxhp;
                                    ((ProgressBar) battleview.findViewById(shipHpBarViewList[j + 1])).setProgress(hpPercent);
                                    ((ProgressBar) battleview.findViewById(shipHpBarViewList[j + 1])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));

                                    battleview.findViewById(shipViewList[j + 1]).setVisibility(View.VISIBLE);
                                }
                            }
                        } else if (i == 1) { // TODO: CHECK NEEDED
                            JsonObject combinedDeckData = deckData.get(i).getAsJsonObject();
                            ((TextView) battleview.findViewById(R.id.friend_combined_fleet_name)).
                                    setText(combinedDeckData.get("api_name").getAsString());
                            JsonArray combinedDeck = combinedDeckData.getAsJsonArray("api_ship");
                            JsonObject shipData = new JsonObject();
                            //Log.e("KCA", String.valueOf(portData.size()));
                            for (int j = 0; j < portData.size(); j++) {
                                JsonObject data = portData.get(j).getAsJsonObject();
                                shipData.add(String.valueOf(data.get("api_id").getAsInt()), data);
                            }

                            for (int j = 0; j < combinedDeck.size(); j++) {
                                if (combinedDeck.get(j).getAsInt() == -1) {
                                    //Log.e("KCA", String.format("%d: invisible", j + 1));
                                    battleview.findViewById(shipCombinedViewList[j + 1]).setVisibility(View.INVISIBLE);
                                } else {
                                    //Log.e("KCA", String.format("%d: visible", j + 1));
                                    JsonObject data = shipData.getAsJsonObject(String.valueOf(combinedDeck.get(j)));
                                    int maxhp = data.get("api_maxhp").getAsInt();
                                    int nowhp = data.get("api_nowhp").getAsInt();
                                    int level = data.get("api_lv").getAsInt();
                                    JsonObject kcShipData = KcaApiData.getKcShipDataById(data.get("api_ship_id").getAsInt(), "name");
                                    String kcname = getShipTranslation(kcShipData.get("name").getAsString());
                                    ((TextView) battleview.findViewById(shipNameCombinedViewList[j + 1])).setText(kcname);
                                    ((TextView) battleview.findViewById(shipNameCombinedViewList[j + 1]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                                    if (fc_flag) {
                                        ((TextView) battleview.findViewById(shipNameCombinedViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                                        battleview.findViewById(shipNameCombinedViewList[j + 1]).setPadding(0, 2, 0, 0);
                                    } else {
                                        ((TextView) battleview.findViewById(shipNameCombinedViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                    }
                                    ((TextView) battleview.findViewById(shipLevelCombinedViewList[j + 1])).setText(makeLvString(level));
                                    ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[j + 1])).setText(makeHpString(nowhp, maxhp));
                                    ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

                                    int hpPercent = nowhp * VIEW_HP_MAX / maxhp;
                                    ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[j + 1])).setProgress(hpPercent);
                                    ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[j + 1])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));

                                    battleview.findViewById(shipCombinedViewList[j + 1]).setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                }
            }

            if (api_data.has("api_ship_ke")) { // Battle (Common)
                Log.e("KCA", "BATTLE");
                JsonArray api_ship_ke = api_data.getAsJsonArray("api_ship_ke");
                JsonArray api_ship_lv = api_data.getAsJsonArray("api_ship_lv");
                Log.e("KCA", api_data.toString());
                int[] nowhps = new int[13];
                int[] afterhps = new int[13];
                int[] nowhps_combined = new int[13];
                int[] afterhps_combined = new int[13];
                Arrays.fill(nowhps, -1);
                Arrays.fill(afterhps, -1);
                Arrays.fill(nowhps_combined, -1);
                Arrays.fill(nowhps_combined, -1);

                boolean start_flag = api_data.has("api_formation");
                if (start_flag) { // day/sp_night Battle Process
                    api_formation = api_data.getAsJsonArray("api_formation");
                    // air power show
                    if (api_data.has("api_kouku") && !api_data.get("api_kouku").isJsonNull()) {
                        api_kouku = api_data.getAsJsonObject("api_kouku");
                    } else {
                        api_kouku = null;
                    }
                }

                // fleet formation and engagement show
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).
                        setText(getFormationString(getApplicationContext(), api_formation.get(0).getAsInt()));
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).
                        setText(getFormationString(getApplicationContext(), api_formation.get(1).getAsInt()));
                ((TextView) battleview.findViewById(R.id.battle_engagement)).
                        setText(getEngagementString(getApplicationContext(), api_formation.get(2).getAsInt()));

                // airforce result
                if (api_kouku != null && !api_kouku.get("api_stage1").isJsonNull()) {
                    JsonObject api_stage1 = api_kouku.getAsJsonObject("api_stage1");
                    int api_disp_seiku = api_stage1.get("api_disp_seiku").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_airpower))
                            .setText(getAirForceResultString(getApplicationContext(), api_disp_seiku));
                }

                if (KcaBattle.currentEnemyDeckName.length() > 0) {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(KcaBattle.currentEnemyDeckName);
                } else {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(getString(R.string.enemy_fleet_name));
                }

                for (int i = 1; i < api_ship_ke.size(); i++) {
                    if (api_ship_ke.get(i).getAsInt() == -1) {
                        battleview.findViewById(shipViewList[getEnemyIdx(i)]).setVisibility(View.INVISIBLE);
                    } else {
                        int level = api_ship_lv.get(i).getAsInt();
                        JsonObject kcShipData = KcaApiData.getKcShipDataById(api_ship_ke.get(i).getAsInt(), "name,yomi");
                        String kcname = getShipTranslation(kcShipData.get("name").getAsString());
                        String kcyomi = getShipTranslation(kcShipData.get("yomi").getAsString());

                        ((TextView) battleview.findViewById(shipNameViewList[getEnemyIdx(i)])).setText(kcname);
                        if (ec_flag) {
                            if (kcname.length() > 7) {
                                ((TextView) battleview.findViewById(shipNameViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                                battleview.findViewById(shipNameViewList[getEnemyIdx(i)]).setPadding(0, 4, 0, 0);
                            } else {
                                ((TextView) battleview.findViewById(shipNameViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            }
                        }
                        if (!is_practice) {
                            if (fc_flag || ec_flag) {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
                            } else {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                            }
                            if (kcyomi.equals(getString(R.string.yomi_elite))) {
                                if (fc_flag && ec_flag) {
                                    ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(getString(R.string.yomi_elite_short));
                                } else {
                                    ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(kcyomi);
                                }
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)]))
                                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorElite));
                            } else if (kcyomi.equals(getString(R.string.yomi_flagship))) {
                                if (fc_flag && ec_flag) {
                                    ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(getString(R.string.yomi_flagship_short));
                                } else {
                                    ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(kcyomi);
                                }
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)]))
                                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFlagship));
                            }
                        }
                        ((TextView) battleview.findViewById(shipLevelViewList[getEnemyIdx(i)])).setText(makeLvString(level));
                        battleview.findViewById(shipViewList[getEnemyIdx(i)]).setVisibility(View.VISIBLE);
                    }
                }

                JsonArray api_maxhps = api_data.getAsJsonArray("api_maxhps");
                JsonArray api_nowhps = api_data.getAsJsonArray("api_nowhps");
                JsonArray api_afterhps = api_data.getAsJsonArray("api_afterhps");
                for (int i = 1; i < api_maxhps.size(); i++) {
                    int maxhp = api_maxhps.get(i).getAsInt();
                    int afterhp = api_afterhps.get(i).getAsInt();
                    if (maxhp == -1) continue;
                    else {
                        int hpPercent = afterhp * VIEW_HP_MAX / maxhp;
                        ((TextView) battleview.findViewById(shipHpTxtViewList[i])).setText(makeHpString(afterhp, maxhp));
                        ((ProgressBar) battleview.findViewById(shipHpBarViewList[i])).setProgress(hpPercent);
                        ((ProgressBar) battleview.findViewById(shipHpBarViewList[i])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                        if (fc_flag || ec_flag) {
                            ((TextView) battleview.findViewById(shipHpTxtViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                        } else {
                            ((TextView) battleview.findViewById(shipHpTxtViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        }

                    }
                    nowhps[i] = api_nowhps.get(i).getAsInt();
                    afterhps[i] = api_afterhps.get(i).getAsInt();
                }

                // For enemy combined fleet
                if (api_data.has("api_ship_ke_combined")) {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(getString(R.string.enemy_main_fleet_name));
                    ((TextView) battleview.findViewById(R.id.enemy_combined_fleet_name)).
                            setText(getString(R.string.enemy_combined_fleet_name));

                    JsonArray api_ship_ke_combined = api_data.getAsJsonArray("api_ship_ke_combined");
                    for (int i = 1; i < api_ship_ke_combined.size(); i++) {
                        if (api_ship_ke_combined.get(i).getAsInt() == -1) {
                            battleview.findViewById(shipCombinedViewList[getEnemyIdx(i)]).setVisibility(View.INVISIBLE);
                        } else {
                            int level = api_ship_lv.get(i).getAsInt();
                            JsonObject kcShipData = KcaApiData.getKcShipDataById(api_ship_ke_combined.get(i).getAsInt(), "name,yomi");
                            String kcname = getShipTranslation(kcShipData.get("name").getAsString());
                            String kcyomi = getShipTranslation(kcShipData.get("yomi").getAsString());

                            ((TextView) battleview.findViewById(shipNameCombinedViewList[getEnemyIdx(i)])).setText(kcname);
                            if (kcname.length() > 7) {
                                ((TextView) battleview.findViewById(shipNameCombinedViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                                battleview.findViewById(shipNameCombinedViewList[getEnemyIdx(i)]).setPadding(0, 4, 0, 0);
                            } else {
                                ((TextView) battleview.findViewById(shipNameCombinedViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            }
                            if (!is_practice) {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
                                if (kcyomi.equals(getString(R.string.yomi_elite))) {
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)])).setText(kcyomi);
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorElite));
                                } else if (kcyomi.equals(getString(R.string.yomi_flagship))) {
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)])).setText(kcyomi);
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFlagship));
                                }
                            }
                            ((TextView) battleview.findViewById(shipLevelCombinedViewList[getEnemyIdx(i)])).setText(makeLvString(level));
                            battleview.findViewById(shipCombinedViewList[getEnemyIdx(i)]).setVisibility(View.VISIBLE);
                        }
                    }

                    JsonArray api_maxhps_combined = api_data.getAsJsonArray("api_maxhps_combined");
                    JsonArray api_nowhps_combined = api_data.getAsJsonArray("api_nowhps_combined");
                    JsonArray api_afterhps_combined = api_data.getAsJsonArray("api_afterhps_combined");
                    for (int i = 7; i < api_maxhps_combined.size(); i++) {
                        int maxhp_combined = api_maxhps_combined.get(i).getAsInt();
                        int afterhp_combined = api_afterhps_combined.get(i).getAsInt();
                        if (maxhp_combined == -1) continue;
                        else {
                            int hpPercent = afterhp_combined * VIEW_HP_MAX / maxhp_combined;
                            ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).setText(makeHpString(afterhp_combined, maxhp_combined));
                            ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[i])).setProgress(hpPercent);
                            ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[i])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                            ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                        }
                        nowhps_combined[i] = api_nowhps_combined.get(i).getAsInt();
                        afterhps_combined[i] = api_afterhps_combined.get(i).getAsInt();
                    }
                }

                // Rank Data
                if (start_flag) {
                    startNowHps = nowhps;
                    startNowHpsCombined = nowhps_combined;
                }

                Log.e("KCA", Arrays.toString(startNowHps));
                Log.e("KCA", Arrays.toString(startNowHpsCombined));

                String api_url = api_data.get("api_url").getAsString();
                JsonObject rankData;
                if (api_url.equals(API_REQ_SORTIE_LDAIRBATTLE) || api_url.equals(API_REQ_COMBINED_LDAIRBATTLE)) {
                    rankData = KcaBattle.calculateLdaRank(afterhps, startNowHps, afterhps_combined, startNowHpsCombined);
                } else {
                    rankData = KcaBattle.calculateRank(afterhps, startNowHps, afterhps_combined, startNowHpsCombined);
                }

                if (rankData.has("fnowhpsum")) {
                    int friendNowSum = rankData.get("fnowhpsum").getAsInt();
                    int friendAfterSum = rankData.get("fafterhpsum").getAsInt();
                    int friendDamageRate = rankData.get("fdmgrate").getAsInt();
                    String dmgshow = String.format("%d/%d (%d%%)", friendAfterSum, friendNowSum, friendDamageRate);
                    ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText(dmgshow);
                } else {
                    ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText("");
                }

                if (rankData.has("enowhpsum")) {
                    int enemyNowSum = rankData.get("enowhpsum").getAsInt();
                    int enemyAfterSum = rankData.get("eafterhpsum").getAsInt();
                    int enemyDamageRate = rankData.get("edmgrate").getAsInt();
                    String dmgshow = String.format("%d/%d (%d%%)", enemyAfterSum, enemyNowSum, enemyDamageRate);
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText(dmgshow);
                } else {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText("");
                }

                int rank = rankData.get("rank").getAsInt();
                switch (rank) {
                    case JUDGE_SS:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_ss));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankS));
                        break;
                    case JUDGE_S:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_s));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankS));
                        break;
                    case JUDGE_A:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_a));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankA));
                        break;
                    case JUDGE_B:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_b));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankB));
                        break;
                    case JUDGE_C:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_c));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankC));
                        break;
                    case JUDGE_D:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_d));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankD));
                        break;
                    case JUDGE_E:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_e));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankE));
                        break;
                    default:
                        break;
                }
                //battleresult.setText(api_data.getAsJsonArray("api_ship_ke").toString());
            } else if(api_data.has("api_mvp")) {
                int mvp_idx = api_data.get("api_mvp").getAsInt();
                if(mvp_idx != -1) {
                    ((TextView) battleview.findViewById(shipNameViewList[mvp_idx]))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMVP));
                }
                if (api_data.has("api_mvp_combined")) {
                    int mvp_idx_combined = api_data.get("api_mvp_combined").getAsInt();
                    if(mvp_idx_combined != -1) {
                        ((TextView) battleview.findViewById(shipNameCombinedViewList[mvp_idx_combined]))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMVP));
                    }
                }
            } else {
                Log.e("KCA", api_data.entrySet().toString());
            }
        } else {
            Log.e("KCA", "api_data is null");
        }
    }

    public void setViewLayout(boolean fc_flag, boolean ec_flag) {
        LinearLayout friend_main_fleet = (LinearLayout) battleview.findViewById(R.id.friend_main_fleet);
        LinearLayout friend_combined_fleet = (LinearLayout) battleview.findViewById(R.id.friend_combined_fleet);
        LinearLayout enemy_main_fleet = (LinearLayout) battleview.findViewById(R.id.enemy_main_fleet);
        LinearLayout enemy_combined_fleet = (LinearLayout) battleview.findViewById(R.id.enemy_combined_fleet);
        Log.e("KCA", String.valueOf(fc_flag) + "-" + String.valueOf(ec_flag));

        friend_combined_fleet.setVisibility(fc_flag ? View.VISIBLE : View.GONE);
        enemy_combined_fleet.setVisibility(ec_flag ? View.VISIBLE : View.GONE);

        if (fc_flag && ec_flag) {
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
            friend_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
            enemy_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
        } else if (fc_flag) {
            enemy_combined_fleet.setVisibility(View.GONE);
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.35f));
            friend_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
        } else if (ec_flag) {
            friend_combined_fleet.setVisibility(View.GONE);
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.35f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
            enemy_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
        } else {
            friend_combined_fleet.setVisibility(View.GONE);
            enemy_combined_fleet.setVisibility(View.GONE);
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.5f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.5f));
        }
    }

    public void setView() {
        //if(mViewBackup != null) mView = mViewBackup;
        api_data = KcaViewButtonService.getCurrentApiData();
        battleview = (ScrollView) mView.findViewById(R.id.battleview);
        battleview.setOnTouchListener(mViewTouchListener);
        setBattleview();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        active = true;
        broadcaster = LocalBroadcastManager.getInstance(this);
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.view_sortie_battle, null);
        //setView();
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.CENTER;

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);

        hdmgreceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e("KCA", "=> Received Intent");
                //mViewBackup = mView;
                //mManager.removeView(mView);
                ((LinearLayout) mView.findViewById(R.id.battleviewpanel))
                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHeavyDmgStatePanel));
                //mManager.addView(mView, mParams);
                if (KcaViewButtonService.getClickCount() == 0) {
                    mView.setVisibility(View.GONE);
                }
                mView.invalidate();
                mManager.updateViewLayout(mView, mParams);
            }
        };
        refreshreceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e("KCA", "=> Received Intent");
                //mViewBackup = mView;
                //mManager.removeView(mView);
                try {
                    setView();
                    if (KcaViewButtonService.getClickCount() == 0) {
                        mView.setVisibility(View.GONE);
                    }
                    //mManager.addView(mView, mParams);
                    mView.invalidate();
                    mManager.updateViewLayout(mView, mParams);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.battleview_error), Toast.LENGTH_LONG).show();
                }

            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver((hdmgreceiver), new IntentFilter(KCA_MSG_BATTLE_VIEW_HDMG));
        LocalBroadcastManager.getInstance(this).registerReceiver((refreshreceiver), new IntentFilter(KCA_MSG_BATTLE_VIEW_REFRESH));
    }

    @Override
    public void onDestroy() {
        active = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hdmgreceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mView.setVisibility(View.VISIBLE);
        return super.onStartCommand(intent, flags, startId);
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime = -1;
        private long clickDuration;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        mView.setVisibility(View.GONE);
                    }
                    break;
            }
            return true;
        }
    };

}