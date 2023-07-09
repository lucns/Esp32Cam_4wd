package com.esp32_4wd;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class BaseActivity extends Activity {

    public static class DialogClickCallback {

        public void onDismiss() {
        }

        public boolean onPositive(String text) {
            return true;
        }

        public void onPositive() {
        }

        public void onNegative(String text) {
        }

        public void onNegative() {
        }

        public void onNeutral() {
        }

        public void onOptionSelected(int index) {
        }
    }

    public static class OptionData {

        public String title;
        public boolean enabled;
        private int color;

        public OptionData(String title) {
            this.title = title;
            enabled = true;
        }

        public OptionData(String title, boolean enabled) {
            this.title = title;
            this.enabled = enabled;
        }

        public OptionData(String title, boolean enabled, int color) {
            this.title = title;
            this.enabled = enabled;
            this.color = color;
        }
    }

    private boolean createdComplete, isPaused;
    private Dialog dialog;
    protected TextView textTitle, textDescription;
    protected Button positiveButton, negativeButton, neutralButton;

    public BaseActivity() {
    }

    public void showDialogConfirmWithRedDescription(String title, String description, DialogClickCallback callback) {
        showDialogConfirm(title, description, callback);
        textDescription.setTextColor(getColor(R.color.red));
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public Dialog generateDialog(int layoutId, boolean isCancelable) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(isCancelable);
        dialog.setContentView(layoutId);
        return dialog;
    }

    public Dialog generateDialog(int layoutId, int theme, boolean isCancelable) {
        hideDialog();
        dialog = new Dialog(this, theme);
        dialog.setCancelable(isCancelable);
        dialog.setContentView(layoutId);
        return dialog;
    }

    public void setDialogTitleColor(int color) {
        textTitle.setTextColor(color);
    }

    public void setDialogButtonsText(String positiveText, String negativeText) {
        setDialogButtonsText(positiveText, negativeText, null);
    }

    public void setDialogButtonsText(String positiveText, String negativeText, String neutralText) {
        if (positiveText != null) positiveButton.setText(positiveText);
        if (negativeText != null) negativeButton.setText(negativeText);
        if (neutralText != null) neutralButton.setText(neutralText);
    }

    public void setDialogTitle(String text) {
        if (dialog != null && dialog.isShowing() && textTitle != null) textTitle.setText(text);
    }

    public void setDialogDescription(String text) {
        if (dialog != null && dialog.isShowing() && textDescription != null)
            textDescription.setText(text);
    }

    public void setDialogTexts(String title, String description) {
        if (dialog != null && dialog.isShowing()) {
            if (textDescription != null) textDescription.setText(description);
            if (textTitle != null) textTitle.setText(title);
        }
    }

    public void disableNegativeButton() {
        if (negativeButton != null) {
            negativeButton.setTextColor(getColor(R.color.gray));
            negativeButton.setEnabled(false);
        }
    }

    public void showDialogOptions(int titles, DialogClickCallback callback) {
        showDialogOptions(getResources().getStringArray(titles), callback);
    }

    public void showDialogOptions(String[] titles, DialogClickCallback callback) {
        boolean[] b = new boolean[titles.length];
        int[] c = new int[titles.length];
        for (int i = 0; i < titles.length; i++) {
            b[i] = true;
        }
        showDialogOptions(titles, b, c, callback);
    }

    public void showDialogOptions(String[] titles, boolean[] enabled, DialogClickCallback callback) {
        showDialogOptions(titles, enabled, new int[titles.length], callback);
    }

    public void showDialogOptions(String[] titles, boolean[] enabled,
                                  int[] colors, DialogClickCallback callback) {
        OptionData[] o = new OptionData[titles.length];
        for (int i = 0; i < titles.length; i++) {
            o[i] = new OptionData(titles[i], enabled[i], colors[i]);
        }
        showDialogOptions(o, callback);
    }

    private void showDialogOptions(OptionData[] optionData, DialogClickCallback callback) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_list_options);

        LayoutInflater inflater = LayoutInflater.from(this);
        ListView listView = dialog.findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<OptionData>(this, R.layout.list_item_options, optionData) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View root = inflater.inflate(R.layout.list_item_options, null, false);
                TextView textView = root.findViewById(R.id.textView);
                textView.setText(optionData[position].title);
                if (optionData[position].color != 0) {
                    textView.setTextColor(optionData[position].color);
                }
                if (!optionData[position].enabled) {
                    root.setOnClickListener(null);
                    root.setEnabled(false);
                    textView.setAlpha(0.5f);
                }
                return root;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                callback.onOptionSelected(position);
            }
        });
        dialog.show();
    }

    public void showDialogInfo(String title, DialogClickCallback dialogClickCallback) {
        showDialogInfo(title, null, dialogClickCallback);
    }

    public void showDialogInfo(String title, String description) {
        showDialogInfo(title, description, null);
    }

    public void showDialogInfo(String title, String description, DialogClickCallback dialogClickCallback) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_info);
        textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setText(title);
        textDescription = dialog.findViewById(R.id.textDescription);
        if (description != null) textDescription.setText(description);
        dialog.findViewById(R.id.buttonPositive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (dialogClickCallback != null) dialogClickCallback.onPositive();
            }
        });
        dialog.show();
    }

    public void showDialogConfirm(String title, DialogClickCallback dialogClickCallback) {
        showDialogConfirm(title, null, false, dialogClickCallback);
    }

    public void showDialogConfirm(String title, String description, DialogClickCallback dialogClickCallback) {
        showDialogConfirm(title, description, false, dialogClickCallback);
    }

    public void showDialogConfirm(String title, String description, boolean showNeutralButton, DialogClickCallback dialogClickCallback) {
        hideDialog();
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                switch (v.getId()) {
                    case R.id.buttonNegative:
                        dialogClickCallback.onNegative();
                        break;
                    case R.id.buttonPositive:
                        dialogClickCallback.onPositive();
                        break;
                    default:
                        dialogClickCallback.onNeutral();
                        break;
                }
            }
        };
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_confirm);
        textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setText(title);
        textDescription = dialog.findViewById(R.id.textDescription);
        if (description != null) textDescription.setText(description);
        positiveButton = dialog.findViewById(R.id.buttonPositive);
        negativeButton = dialog.findViewById(R.id.buttonNegative);
        neutralButton = dialog.findViewById(R.id.buttonNeutral);
        neutralButton.setOnClickListener(onClickListener);
        positiveButton.setOnClickListener(onClickListener);
        negativeButton.setOnClickListener(onClickListener);
        dialog.show();
    }

    public void showDialogAlert(String title, String description) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_info);
        textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setTextColor(getColor(R.color.main_dark));
        textTitle.setText(title);
        textDescription = dialog.findViewById(R.id.textDescription);
        if (description != null) textDescription.setText(description);
        Button button = dialog.findViewById(R.id.buttonPositive);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void showDialogError(String textTitle) {
        showDialogError(textTitle, null, null);
    }

    public void showDialogError(String text, String description, DialogClickCallback dialogClickCallback) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_info);
        textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setTextColor(getColor(R.color.red));
        textTitle.setText(text);
        textDescription = dialog.findViewById(R.id.textDescription);
        if (description != null) textDescription.setText(description);
        Button button = dialog.findViewById(R.id.buttonPositive);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (dialogClickCallback != null) dialogClickCallback.onPositive();
            }
        });

        dialog.show();
    }

    public void showDialogWait(String message) {
        showDialogWait(message, null, null);
        negativeButton.setEnabled(false);
    }

    public void showDialogWait(String message, DialogClickCallback dialogClickCallback) {
        showDialogWait(message, null, dialogClickCallback);
    }

    public void showDialogWait(String message, boolean enableNegativeButton, DialogClickCallback dialogClickCallback) {
        showDialogWait(message, null, dialogClickCallback);
        negativeButton.setEnabled(enableNegativeButton);
    }

    public void showDialogWait(String message, String description, DialogClickCallback dialogClickCallback) {
        hideDialog();
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_wait);
        negativeButton = dialog.findViewById(R.id.buttonNegative);
        negativeButton.setEnabled(dialogClickCallback != null);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (dialogClickCallback != null) dialogClickCallback.onNegative();
            }
        });
        negativeButton.setEnabled(dialogClickCallback != null);
        textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setText(message);
        textDescription = dialog.findViewById(R.id.textDescription);
        if (description != null) textDescription.setText(description);
        dialog.show();
    }

    public void hideDialog() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public abstract boolean onCreated();

    public void onResumed() {
    }

    public void onPaused() {
    }

    public void onDestroyed() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createdComplete = onCreated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        if (createdComplete) onResumed();
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        if (createdComplete) onPaused();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        if (createdComplete) onDestroyed();
    }

    public void startActivity(Intent intent) {
        if (isFinishing() || isDestroyed()) return;
        super.startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
