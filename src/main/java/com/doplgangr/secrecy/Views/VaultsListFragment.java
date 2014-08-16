/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with context work for additional information
 * regarding copyright ownership.  The ASF licenses context file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use context file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy.Views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;

import com.doplgangr.secrecy.FileSystem.Vault;
import com.doplgangr.secrecy.FileSystem.storage;
import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.Settings.Prefs_;
import com.doplgangr.secrecy.Util;
import com.doplgangr.secrecy.Views.DummyViews.SwipeDismissTouchListener;
import com.flurry.android.FlurryAgent;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DrawableRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@EFragment(R.layout.activity_list_vault)
@OptionsMenu(R.menu.list_vault)
public class VaultsListFragment extends Fragment {
    @ViewById(R.id.list)
    LinearLayout mLinearView;
    @ViewById(R.id.nothing)
    View nothing;
    @DrawableRes(R.drawable.file_selector)
    Drawable selector;
    @Pref
    Prefs_ Pref;
    ActionBarActivity context;
    VaultsAdapter adapter;
    OnVaultSelectedListener mOnVaultSelected;
    OnFragmentFinishListener mFinishListener;
    private boolean isPaused = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnVaultSelected = (OnVaultSelectedListener) activity;
            mFinishListener = (OnFragmentFinishListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Listener");
        }
    }

    @AfterViews
    void oncreate() {
        context = (ActionBarActivity) getActivity();
        if (mLinearView != null)
            mLinearView.removeAllViews();
        //if (context.getSupportActionBar() != null)
        //  context.getSupportActionBar().setSubtitle(storage.getRoot().getAbsolutePath());
        java.io.File root = storage.getRoot();
        java.io.File[] files = root.listFiles();
        adapter = new VaultsAdapter(context, null);
        for (int i = 0; i < files.length; i++)
            if (files[i].isDirectory() && !files[i].equals(storage.getTempFolder())) {
                adapter.add(files[i].getName());
                final View mView = adapter.getView(i, mLinearView); //inject vaults into list
                mLinearView.addView(mView, i);
                setClickListener(mView, i);
            }
        if (adapter.getCount() == 0) {
            nothing.setVisibility(View.VISIBLE);
            mLinearView.setVisibility(View.GONE);
        } else {
            nothing.setVisibility(View.GONE);
            mLinearView.setVisibility(View.VISIBLE);
        }
        showTutorial();
    }

    public void setClickListener(final View mView, final int i) {
        final SwipeDismissTouchListener mSwipeListener = new SwipeDismissTouchListener(
                mView,
                null,
                new SwipeDismissTouchListener.OnDismissCallback() {
                    @Override
                    public void onDismiss(View view, Object token) {
                        switchView(mView, R.id.vault_delete_layout);
                        mView.findViewById(R.id.delete_ok)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View ignored) {
                                        switchView(mView, R.id.vault_decrypt_layout);
                                        mView.findViewById(R.id.open_ok)
                                                .setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View ignored) {
                                                        String password = ((EditText) mView.findViewById(R.id.open_password))
                                                                .getText().toString();
                                                        delete(i, password);
                                                        switchView(mView, R.id.vault_decrypt_layout);
                                                    }
                                                });
                                        mView.findViewById(R.id.open_cancel)
                                                .setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View ignored) {
                                                        switchView(mView, R.id.vault_name_layout);
                                                    }
                                                });
                                    }
                                });
                        mView.findViewById(R.id.delete_cancel)
                                .setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        switchView(mView, R.id.vault_name_layout);
                                    }
                                });
                    }
                }
        );

        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwipeListener.onPanic();
                open(adapter.getItem(i), mView, i);
            }
        });
        mView.setOnTouchListener(mSwipeListener);
    }

    @OptionsItem(R.id.action_add_vault)
    void add() {
        final View dialogView = View.inflate(context, R.layout.new_credentials, null);
        final EditText password = new EditText(context);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.new_vault))
                .setMessage(getString(R.string.prompt_credentials))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = ((EditText) dialogView.findViewById(R.id.newName)).getText().toString();
                        String password = ((EditText) dialogView.findViewById(R.id.password)).getText().toString();
                        String Confirmpassword = ((EditText) dialogView.findViewById(R.id.confirmPassword)).getText().toString();
                        File directory = new File(storage.getRoot().getAbsolutePath() + "/" + name);
                        if (!password.equals(Confirmpassword) || "".equals(password))
                            passwordWrong();
                        else if (directory.mkdirs()) {
                            try {
                                File file = new File(context.getFilesDir(), ".nomedia");
                                file.delete();
                                file.createNewFile();
                                FileOutputStream outputStream = new FileOutputStream(file);
                                outputStream.write(name.getBytes());
                                outputStream.close();
                                Uri nomediaURI = Uri.fromFile(file);
                                Vault newVault = new Vault(name, password, true);
                                newVault.addFile(context, nomediaURI);
                                file.delete();
                                oncreate();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else
                            failedtocreate();

                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    void passwordWrong() {
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.error_wrong_password_confirmation))
                .setMessage(getString(R.string.error_wrong_password_confirmation_message))
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    void failedtocreate() {
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.error_cannot_create_vault))
                .setMessage(getString(R.string.error_cannot_create_vault_message))
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    void open(final String vault, final View mView, final int i) {
        // vault name
        // View of lisitem
        // position of listitem in list
        FlurryAgent.logEvent("Vault_open");
        switchView(mView, R.id.vault_decrypt_layout);
        mView.findViewById(R.id.open_ok)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String value = ((EditText) mView.findViewById(R.id.open_password))
                                .getText().toString();
                        mOnVaultSelected.onVaultSelected(vault, value);
                    }
                });
        mView.findViewById(R.id.open_cancel)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switchView(mView, R.id.vault_name_layout);
                    }
                });
    }

    void delete(final int position, final String password) {
        String value = password;
        Boolean pwState = new Vault(adapter.getItem(position), value).delete();
        if (!pwState)
            Util.alert(context,
                    getString(R.string.error_delete_password_incorrect),
                    getString(R.string.error_delete_password_incorrect_message),
                    Util.emptyClickListener,
                    null
            );
        oncreate();
    }

    @UiThread
    void switchView(final View parentView, int showView) {
        ViewAnimator viewAnimator = (ViewAnimator) parentView.findViewById(R.id.viewAnimator);
        viewAnimator.setInAnimation(context, R.anim.slide_down);
        int viewIndex = 0;
        switch (showView) {
            case R.id.vault_name_layout:
                viewIndex = 0;
                break;
            case R.id.vault_decrypt_layout:
                viewIndex = 1;
                break;
            case R.id.vault_delete_layout:
                viewIndex = 2;
                break;
        }
        viewAnimator.setDisplayedChild(viewIndex);
        View passwordView = parentView.findViewById(R.id.open_password);
        if (passwordView != null)
            passwordView.requestFocus();
    }

    void finish() {
        mFinishListener.onFinish(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        oncreate();
    }

    void showTutorial() {

        if ((adapter.getCount() > 0) && (Pref.showVaultSwipeDeleteTutorial().get())) {
            final View mView =
                    context.getLayoutInflater().inflate(R.layout.vault_item_tutorial, mLinearView, false);
            mLinearView.addView(mView, 0);
            mView.setOnTouchListener(new SwipeDismissTouchListener(
                    mView,
                    null,
                    new SwipeDismissTouchListener.OnDismissCallback() {
                        @Override
                        public void onDismiss(View view, Object token) {
                            mLinearView.removeView(mView);
                            Pref.edit()
                                    .showVaultSwipeDeleteTutorial()
                                    .put(false)
                                    .apply();
                        }
                    }));

        }
    }

    public interface OnVaultSelectedListener {
        public void onVaultSelected(String vault, String password);
    }

    public interface OnFragmentFinishListener {
        public void onFinish(Fragment fragment);

        public void onNew(Bundle bundle, Fragment fragment);
    }

    public interface onPanic {
        void onPanic();
    }

}
