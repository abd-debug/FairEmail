package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2023 by Marcel Bokhorst (M66B)
*/

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static android.system.OsConstants.ENOSPC;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.OperationCanceledException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.security.KeyChain;
import android.system.ErrnoException;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.ImageSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.MenuCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.google.android.material.snackbar.Snackbar;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableFile;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.RecipientInfoGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyAgreeRecipientInfoGenerator;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.RuntimeOperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

public class FragmentCompose extends FragmentBase {
    private enum State {NONE, LOADING, LOADED}

    private ViewGroup view;
    private View vwAnchorMenu;
    private ScrollView scroll;
    private Spinner spIdentity;
    private EditText etExtra;
    private TextView tvDomain;
    private MultiAutoCompleteTextView etTo;
    private ImageButton ibToAdd;
    private MultiAutoCompleteTextView etCc;
    private ImageButton ibCcAdd;
    private MultiAutoCompleteTextView etBcc;
    private ImageButton ibBccAdd;
    private EditText etSubject;
    private ImageButton ibCcBcc;
    private ImageButton ibRemoveAttachments;
    private RecyclerView rvAttachment;
    private TextView tvNoInternetAttachments;
    private TextView tvDsn;
    private TextView tvResend;
    private TextView tvPlainTextOnly;
    private EditTextCompose etBody;
    private TextView tvNoInternet;
    private TextView tvSignature;
    private CheckBox cbSignature;
    private ImageButton ibSignature;
    private TextView tvReference;
    private ImageButton ibCloseRefHint;
    private ImageButton ibWriteAboveBelow;
    private TextView tvLanguage;
    private ImageButton ibReferenceEdit;
    private ImageButton ibReferenceImages;
    private View vwAnchor;
    private TextViewAutoCompleteAction etSearch;
    private HorizontalScrollView style_bar;
    private ImageButton ibLink;
    private ImageButton ibAnswer;
    private BottomNavigationView media_bar;
    private BottomNavigationView bottom_navigation;
    private ContentLoadingProgressBar pbWait;
    private Group grpHeader;
    private Group grpExtra;
    private Group grpAddresses;
    private Group grpAttachments;
    private Group grpBody;
    private Group grpSignature;
    private Group grpReferenceHint;

    private ContentResolver resolver;
    private AdapterAttachment adapter;

    private boolean autoscroll_editor;
    private int compose_color;
    private String compose_font;
    private boolean compose_monospaced;
    private String display_font;
    private boolean dsn = true;
    private Integer encrypt = null;
    private boolean style = false;
    private boolean media = true;
    private boolean compact = false;
    private int zoom = 0;
    private boolean nav_color;
    private boolean lt_enabled;
    private boolean lt_sentence;
    private boolean lt_auto;

    private Long account = null;
    private long working = -1;
    private State state = State.NONE;
    private boolean show_images = false;
    private Integer last_plain_only = null;
    private List<EntityAttachment> last_attachments = null;
    private boolean saved = false;
    private String subject = null;
    private boolean chatting = false;

    private Uri photoURI = null;

    private int pickRequest;
    private Uri pickUri;

    private int searchIndex = 0;

    private static final int REQUEST_CONTACT_TO = 1;
    private static final int REQUEST_CONTACT_CC = 2;
    private static final int REQUEST_CONTACT_BCC = 3;
    private static final int REQUEST_SHARED = 4;
    private static final int REQUEST_IMAGE = 5;
    private static final int REQUEST_IMAGE_FILE = 6;
    private static final int REQUEST_ATTACHMENT = 7;
    private static final int REQUEST_TAKE_PHOTO = 8;
    private static final int REQUEST_RECORD_AUDIO = 9;
    private static final int REQUEST_OPENPGP = 10;
    private static final int REQUEST_CONTACT_GROUP = 11;
    private static final int REQUEST_SELECT_IDENTITY = 12;
    private static final int REQUEST_PRINT = 13;
    private static final int REQUEST_LINK = 14;
    private static final int REQUEST_DISCARD = 15;
    private static final int REQUEST_SEND = 16;
    private static final int REQUEST_REMOVE_ATTACHMENTS = 17;

    ActivityResultLauncher<PickVisualMediaRequest> pickImages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        autoscroll_editor = prefs.getBoolean("autoscroll_editor", false);
        compose_color = prefs.getInt("compose_color", Color.TRANSPARENT);
        compose_font = prefs.getString("compose_font", "");
        compose_monospaced = prefs.getBoolean("compose_monospaced", false);
        display_font = prefs.getString("display_font", "");
        style = prefs.getBoolean("compose_style", false);
        media = prefs.getBoolean("compose_media", true);
        compact = prefs.getBoolean("compose_compact", false);
        zoom = prefs.getInt("compose_zoom", compact ? 0 : 1);
        nav_color = prefs.getBoolean("send_nav_color", false);

        lt_enabled = LanguageTool.isEnabled(context);
        lt_sentence = LanguageTool.isSentence(context);
        lt_auto = LanguageTool.isAuto(context);

        if (compose_color != Color.TRANSPARENT && Helper.isDarkTheme(context))
            compose_color = HtmlHelper.adjustLuminance(compose_color, true, HtmlHelper.MIN_LUMINANCE_COMPOSE);

        setTitle(R.string.page_compose);
        setSubtitle(getResources().getQuantityString(R.plurals.page_message, 1));

        int max = Helper.hasPhotoPicker() ? MediaStore.getPickImagesMaxLimit() : 20;
        pickImages =
                registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(max), uris -> {
                    if (!uris.isEmpty())
                        onAddImageFile(uris, false);
                });
    }

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = (ViewGroup) inflater.inflate(R.layout.fragment_compose, container, false);

        // Get controls
        vwAnchorMenu = view.findViewById(R.id.vwAnchorMenu);
        scroll = view.findViewById(R.id.scroll);
        spIdentity = view.findViewById(R.id.spIdentity);
        etExtra = view.findViewById(R.id.etExtra);
        tvDomain = view.findViewById(R.id.tvDomain);
        etTo = view.findViewById(R.id.etTo);
        ibToAdd = view.findViewById(R.id.ibToAdd);
        etCc = view.findViewById(R.id.etCc);
        ibCcAdd = view.findViewById(R.id.ibCcAdd);
        etBcc = view.findViewById(R.id.etBcc);
        ibBccAdd = view.findViewById(R.id.ibBccAdd);
        etSubject = view.findViewById(R.id.etSubject);
        ibCcBcc = view.findViewById(R.id.ibCcBcc);
        ibRemoveAttachments = view.findViewById(R.id.ibRemoveAttachments);
        rvAttachment = view.findViewById(R.id.rvAttachment);
        tvNoInternetAttachments = view.findViewById(R.id.tvNoInternetAttachments);
        tvDsn = view.findViewById(R.id.tvDsn);
        tvResend = view.findViewById(R.id.tvResend);
        tvPlainTextOnly = view.findViewById(R.id.tvPlainTextOnly);
        etBody = view.findViewById(R.id.etBody);
        tvNoInternet = view.findViewById(R.id.tvNoInternet);
        tvSignature = view.findViewById(R.id.tvSignature);
        cbSignature = view.findViewById(R.id.cbSignature);
        ibSignature = view.findViewById(R.id.ibSignature);
        tvReference = view.findViewById(R.id.tvReference);
        ibCloseRefHint = view.findViewById(R.id.ibCloseRefHint);
        ibWriteAboveBelow = view.findViewById(R.id.ibWriteAboveBelow);
        tvLanguage = view.findViewById(R.id.tvLanguage);
        ibReferenceEdit = view.findViewById(R.id.ibReferenceEdit);
        ibReferenceImages = view.findViewById(R.id.ibReferenceImages);
        vwAnchor = view.findViewById(R.id.vwAnchor);
        etSearch = view.findViewById(R.id.etSearch);
        style_bar = view.findViewById(R.id.style_bar);
        ibLink = view.findViewById(R.id.menu_link);
        ibAnswer = view.findViewById(R.id.menu_style_insert_answer);
        media_bar = view.findViewById(R.id.media_bar);
        bottom_navigation = view.findViewById(R.id.bottom_navigation);

        pbWait = view.findViewById(R.id.pbWait);
        grpHeader = view.findViewById(R.id.grpHeader);
        grpExtra = view.findViewById(R.id.grpExtra);
        grpAddresses = view.findViewById(R.id.grpAddresses);
        grpAttachments = view.findViewById(R.id.grpAttachments);
        grpBody = view.findViewById(R.id.grpBody);
        grpSignature = view.findViewById(R.id.grpSignature);
        grpReferenceHint = view.findViewById(R.id.grpReferenceHint);

        resolver = getContext().getContentResolver();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final boolean auto_save_paragraph = prefs.getBoolean("auto_save_paragraph", true);
        final boolean auto_save_dot = prefs.getBoolean("auto_save_dot", false);
        final boolean keyboard_no_fullscreen = prefs.getBoolean("keyboard_no_fullscreen", false);
        final boolean suggest_names = prefs.getBoolean("suggest_names", true);
        final boolean suggest_sent = prefs.getBoolean("suggest_sent", true);
        final boolean suggest_received = prefs.getBoolean("suggest_received", false);
        final boolean suggest_frequently = prefs.getBoolean("suggest_frequently", false);
        final boolean suggest_account = prefs.getBoolean("suggest_account", false);
        final boolean cc_bcc = prefs.getBoolean("cc_bcc", false);
        final boolean circular = prefs.getBoolean("circular", true);

        final float dp3 = Helper.dp2pixels(getContext(), 3);

        // Wire controls
        spIdentity.setOnItemSelectedListener(identitySelected);

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    EditText et = (EditText) v;
                    int start = et.getSelectionStart();
                    int end = et.getSelectionEnd();

                    if (start < 0 || end < 0)
                        return false;

                    if (start == end)
                        return false;

                    if (start > end) {
                        int tmp = start;
                        start = end;
                        end = tmp;
                    }

                    float x = event.getX() + et.getScrollX();
                    float y = event.getY() + et.getScrollY();
                    int pos = et.getOffsetForPosition(x, y);
                    if (pos < 0)
                        return false;

                    // Undo selection to be able to select another address
                    if (pos < start || pos >= end)
                        et.setSelection(pos);
                }

                return false;
            }
        };

        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                EditText et = (EditText) v;
                int start = et.getSelectionStart();
                int end = et.getSelectionEnd();

                if (start < 0 || end < 0)
                    return false;

                if (start > end) {
                    int tmp = start;
                    start = end;
                    end = tmp;
                }

                String text = et.getText().toString();
                if (text.length() == 0)
                    return false;

                int last = text.indexOf(',', start);
                last = (last < 0 ? text.length() - 1 : last);

                int first = text.substring(0, last).lastIndexOf(',');
                first = (first < 0 ? 0 : first + 1);

                et.setSelection(first, last + 1);

                return false;
            }
        };

        View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    try {
                        updateEncryption((EntityIdentity) spIdentity.getSelectedItem(), false);
                    } catch (Throwable ex) {
                        Log.e(ex);
                    }
            }
        };

        etTo.setMaxLines(Integer.MAX_VALUE);
        etTo.setHorizontallyScrolling(false);
        etTo.setOnTouchListener(onTouchListener);
        etTo.setOnLongClickListener(longClickListener);
        etTo.setOnFocusChangeListener(focusListener);

        etCc.setMaxLines(Integer.MAX_VALUE);
        etCc.setHorizontallyScrolling(false);
        etCc.setOnTouchListener(onTouchListener);
        etCc.setOnLongClickListener(longClickListener);
        etCc.setOnFocusChangeListener(focusListener);

        etBcc.setMaxLines(Integer.MAX_VALUE);
        etBcc.setHorizontallyScrolling(false);
        etBcc.setOnTouchListener(onTouchListener);
        etBcc.setOnLongClickListener(longClickListener);
        etBcc.setOnFocusChangeListener(focusListener);

        etSubject.setMaxLines(Integer.MAX_VALUE);
        etSubject.setHorizontallyScrolling(false);

        ibCcBcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuAddresses();
            }
        });

        ibCcBcc.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onMenuAddresses();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putBoolean("cc_bcc", grpAddresses.getVisibility() == View.VISIBLE).apply();
                ToastEx.makeText(v.getContext(), R.string.title_default_changed, Toast.LENGTH_LONG).show();
                return true;
            }
        });

        View.OnClickListener onPick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int request;
                int id = view.getId();
                if (id == R.id.ibToAdd) {
                    request = REQUEST_CONTACT_TO;
                } else if (id == R.id.ibCcAdd) {
                    request = REQUEST_CONTACT_CC;
                } else if (id == R.id.ibBccAdd) {
                    request = REQUEST_CONTACT_BCC;
                } else {
                    return;
                }

                // https://developer.android.com/guide/topics/providers/contacts-provider#Intents
                Intent pick = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(Helper.getChooser(getContext(), pick), request);
            }
        };

        ibToAdd.setOnClickListener(onPick);
        ibCcAdd.setOnClickListener(onPick);
        ibBccAdd.setOnClickListener(onPick);

        View.OnLongClickListener onGroup = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int id = view.getId();
                if (id == R.id.ibToAdd) {
                    onMenuContactGroup(etTo);
                    return true;
                } else if (id == R.id.ibCcAdd) {
                    onMenuContactGroup(etCc);
                    return true;
                } else if (id == R.id.ibBccAdd) {
                    onMenuContactGroup(etBcc);
                    return true;
                } else
                    return true;
            }
        };

        ibToAdd.setOnLongClickListener(onGroup);
        ibCcAdd.setOnLongClickListener(onGroup);
        ibBccAdd.setOnLongClickListener(onGroup);

        tvPlainTextOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean("force_dialog", true);
                onAction(R.id.action_check, args, "force");
            }
        });

        setZoom();

        etBody.setInputContentListener(new EditTextCompose.IInputContentListener() {
            @Override
            public void onInputContent(Uri uri, String type) {
                Log.i("Received input uri=" + uri);
                boolean resize_paste = prefs.getBoolean("resize_paste", true);
                int resize = prefs.getInt("resize", ComposeHelper.REDUCED_IMAGE_SIZE);
                onAddAttachment(
                        Arrays.asList(uri),
                        type == null ? null : new String[]{type},
                        true,
                        resize_paste ? resize : 0,
                        false,
                        false);
            }
        });

        etBody.setSelectionListener(new EditTextCompose.ISelection() {
            private boolean hasSelection = false;

            @Override
            public void onSelected(final boolean selection) {
                if (media) {
                    getMainHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                                return;
                            if (hasSelection != selection) {
                                hasSelection = selection;
                                ibLink.setVisibility(style /* && media */ ? View.GONE : View.VISIBLE);
                                style_bar.setVisibility(style || hasSelection ? View.VISIBLE : View.GONE);
                                media_bar.setVisibility(style || !etBody.hasSelection() ? View.VISIBLE : View.GONE);
                                invalidateOptionsMenu();
                            }
                        }
                    }, 20);
                } else {
                    ibLink.setVisibility(View.VISIBLE); // no media
                    style_bar.setVisibility(style || selection ? View.VISIBLE : View.GONE);
                    media_bar.setVisibility(View.GONE);
                }
            }
        });

        etBody.addTextChangedListener(StyleHelper.getTextWatcher(etBody));

        etBody.addTextChangedListener(new TextWatcher() {
            private Integer save = null;
            private Integer added = null;
            private boolean inserted = false;
            private Pair<Integer, Integer> lt = null;

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                Activity activity = getActivity();
                if (activity != null)
                    activity.onUserInteraction();

                int index = start + before;

                if (count - before == 1 && index > 0) {
                    char c = text.charAt(index);
                    char b = text.charAt(index - 1);

                    if ((auto_save_paragraph && c == '\n' && b != '\n') ||
                            (auto_save_dot && Helper.isEndChar(c) && !Helper.isEndChar(b))) {
                        Log.i("Save=" + index);
                        save = index;
                    }

                    if (c == '\n') {
                        Log.i("Added=" + index);
                        added = index;
                    }
                }

                // Autoscroll was fixed by Android 14 beta 2
                if (autoscroll_editor) {
                    if (count - before > 1)
                        inserted = true;
                    else if (count - before == 1) {
                        char c = text.charAt(start + count - 1);
                        inserted = Character.isWhitespace(c);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (etBody == null)
                    return;

                if (added != null)
                    try {
                        if (lt_auto) {
                            int start = added;
                            while (start > 0 && text.charAt(start - 1) != '\n')
                                start--;
                            if (start < added)
                                lt = new Pair<>(start, added);
                        }
                    } catch (Throwable ex) {
                        Log.e(ex);
                    } finally {
                        added = null;
                    }

                if (save != null)
                    try {
                        if (lt == null && lt_sentence &&
                                Helper.isSentenceChar(text.charAt(save))) {
                            int start = save;
                            while (start > 0 &&
                                    text.charAt(start - 1) != '\n' &&
                                    !(Character.isWhitespace(text.charAt(start)) &&
                                            Helper.isSentenceChar(text.charAt(start - 1))))
                                start--;
                            while (start < save)
                                if (Character.isWhitespace(text.charAt(start)))
                                    start++;
                                else
                                    break;
                            if (start < save)
                                lt = new Pair<>(start, save + 1);
                        }

                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                            Bundle extras = new Bundle();
                            extras.putBoolean("silent", true);
                            onAction(R.id.action_save, extras, "paragraph");
                        }
                    } finally {
                        save = null;
                    }

                if (lt != null)
                    try {
                        onLanguageTool(lt.first, lt.second, true);
                    } finally {
                        lt = null;
                    }

                // Auto scroll is broken on Android 14 beta
                if (inserted)
                    try {
                        // Auto scroll is broken on Android 14 beta
                        view.post(new RunnableEx("autoscroll") {
                            private Rect rect = new Rect();

                            @Override
                            protected void delegate() {
                                int pos = etBody.getSelectionEnd();
                                if (pos < 0)
                                    return;

                                Layout layout = etBody.getLayout();
                                int line = layout.getLineForOffset(pos);
                                int y = layout.getLineTop(line + 1);

                                etBody.getLocalVisibleRect(rect);
                                if (y > rect.bottom)
                                    scroll.scrollBy(0, y - rect.bottom);
                            }
                        });
                    } catch (Throwable ex) {
                        Log.e(ex);
                    } finally {
                        inserted = false;
                    }
            }
        });

        if (compose_color != Color.TRANSPARENT)
            tvSignature.setTextColor(compose_color);
        tvSignature.setTypeface(StyleHelper.getTypeface(compose_font, getContext()));

        cbSignature.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                Object tag = cbSignature.getTag();
                if (tag == null || !tag.equals(checked)) {
                    cbSignature.setTag(checked);
                    tvSignature.setAlpha(checked ? 1.0f : Helper.LOW_LIGHT);
                    if (tag != null)
                        onAction(R.id.action_save, "signature");
                }
            }
        });

        ibSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EntityIdentity identity = (EntityIdentity) spIdentity.getSelectedItem();
                if (identity == null || TextUtils.isEmpty(identity.signature))
                    return;

                ClipboardManager clipboard = Helper.getSystemService(v.getContext(), ClipboardManager.class);
                if (clipboard == null)
                    return;

                ClipData clip = ClipData.newHtmlText(
                        v.getContext().getString(R.string.title_edit_signature_text),
                        HtmlHelper.getText(v.getContext(), identity.signature),
                        identity.signature);
                clipboard.setPrimaryClip(clip);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                    ToastEx.makeText(v.getContext(), R.string.title_clipboard_copied, Toast.LENGTH_LONG).show();
            }
        });

        ibCloseRefHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
                prefs.edit().putBoolean("compose_reference", false).apply();
                grpReferenceHint.setVisibility(View.GONE);
            }
        });

        ibWriteAboveBelow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putLong("id", working);

                new SimpleTask<Boolean>() {
                    @Override
                    protected Boolean onExecute(Context context, Bundle args) throws Throwable {
                        long id = args.getLong("id");

                        DB db = DB.getInstance(context);
                        EntityMessage draft = db.message().getMessage(id);
                        if (draft == null)
                            return null;

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        boolean write_below = prefs.getBoolean("write_below", false);
                        boolean wb = (draft == null || draft.write_below == null ? write_below : draft.write_below);

                        wb = !wb;
                        db.message().setMessageWriteBelow(id, wb);

                        return wb;
                    }

                    @Override
                    protected void onExecuted(Bundle args, Boolean wb) {
                        if (wb == null)
                            return;

                        ibWriteAboveBelow.setImageLevel(wb ? 1 : 0);
                        ToastEx.makeText(v.getContext(), wb
                                        ? R.string.title_advanced_write_below
                                        : R.string.title_advanced_write_above,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    protected void onException(Bundle args, Throwable ex) {
                        Log.unexpectedError(getParentFragmentManager(), ex);
                    }
                }.execute(FragmentCompose.this, args, "compose:below");
            }
        });

        ibReferenceEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReferenceEdit();
            }
        });

        ibReferenceImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(v.getContext())
                        .setMessage(R.string.title_ask_show_image)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ibReferenceImages.setVisibility(View.GONE);
                                onReferenceImages();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        if (compose_color != Color.TRANSPARENT)
            etBody.setTextColor(compose_color);
        etBody.setTypeface(StyleHelper.getTypeface(compose_font, getContext()));
        tvReference.setTypeface(StyleHelper.getTypeface(display_font, getContext()));

        tvReference.setMovementMethod(new ArrowKeyMovementMethod() {
            @Override
            public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int off = Helper.getOffset(widget, buffer, event);
                    URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
                    if (link.length > 0) {
                        String url = link[0].getURL();
                        Uri uri = Uri.parse(url);
                        if (uri.getScheme() == null)
                            uri = Uri.parse("https://" + url);

                        int start = buffer.getSpanStart(link[0]);
                        int end = buffer.getSpanEnd(link[0]);
                        String title = (start < 0 || end < 0 || end <= start
                                ? null : buffer.subSequence(start, end).toString());
                        if (url.equals(title))
                            title = null;

                        Bundle args = new Bundle();
                        args.putParcelable("uri", uri);
                        args.putString("title", title);
                        args.putBoolean("always_confirm", true);

                        FragmentDialogOpenLink fragment = new FragmentDialogOpenLink();
                        fragment.setArguments(args);
                        fragment.show(getParentFragmentManager(), "open:link");

                        return true;
                    }
                }

                return super.onTouchEvent(widget, buffer, event);
            }
        });

        etSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    endSearch();
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    endSearch();
                    return true;
                } else
                    return false;
            }
        });

        etSearch.setActionRunnable(new Runnable() {
            @Override
            public void run() {
                performSearch(true);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });

        StyleHelper.wire(getViewLifecycleOwner(), view, etBody);

        ibLink.setVisibility(View.VISIBLE);
        ibLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onActionLink();
            }
        });

        ibAnswer.setVisibility(View.VISIBLE);
        ibAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuAnswerInsert(v);
            }
        });

        media_bar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int action = item.getItemId();
                if (action == R.id.menu_record_audio) {
                    onActionRecordAudio();
                    return true;
                } else if (action == R.id.menu_take_photo) {
                    onActionImage(true, false);
                    return true;
                } else if (action == R.id.menu_image) {
                    onActionImage(false, false);
                    return true;
                } else if (action == R.id.menu_attachment) {
                    onActionAttachment();
                    return true;
                } else if (action == R.id.menu_link) {
                    onActionLink();
                    return true;
                } else
                    return false;
            }
        });

        setCompact(compact);

        bottom_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                final int action = item.getItemId();
                if (action == R.id.action_delete) {
                    onActionDiscard();
                } else if (action == R.id.action_send) {
                    onAction(R.id.action_check, "check");
                } else if (action == R.id.action_save) {
                    saved = true;
                    onAction(action, "save");
                } else {
                    onAction(action, "navigation");
                }
                return true;
            }
        });

        addKeyPressedListener(onKeyPressedListener);
        setBackPressedCallback(backPressedCallback);

        // Initialize
        setHasOptionsMenu(true);
        FragmentDialogTheme.setBackground(getContext(), view, true);

        if (keyboard_no_fullscreen) {
            // https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_FULLSCREEN
            etExtra.setImeOptions(etExtra.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etTo.setImeOptions(etTo.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etCc.setImeOptions(etCc.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etBcc.setImeOptions(etBcc.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etSubject.setImeOptions(etSubject.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etBody.setImeOptions(etBody.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
        }

        etExtra.setHint("");
        tvDomain.setText(null);
        tvDsn.setVisibility(View.GONE);
        tvResend.setVisibility(View.GONE);
        tvPlainTextOnly.setVisibility(View.GONE);
        etBody.setText(null);
        etBody.setHint(null);

        grpHeader.setVisibility(View.GONE);
        grpExtra.setVisibility(View.GONE);
        ibCcBcc.setVisibility(View.GONE);
        grpAttachments.setVisibility(View.GONE);
        tvNoInternet.setVisibility(View.GONE);
        grpBody.setVisibility(View.GONE);
        grpSignature.setVisibility(View.GONE);
        grpReferenceHint.setVisibility(View.GONE);
        ibWriteAboveBelow.setVisibility(View.GONE);
        tvLanguage.setVisibility(View.GONE);
        ibReferenceEdit.setVisibility(View.GONE);
        ibReferenceImages.setVisibility(View.GONE);
        tvReference.setVisibility(View.GONE);
        etSearch.setVisibility(View.GONE);
        style_bar.setVisibility(View.GONE);
        media_bar.setVisibility(View.GONE);
        bottom_navigation.setVisibility(View.GONE);
        pbWait.setVisibility(View.VISIBLE);

        invalidateOptionsMenu();
        Helper.setViewsEnabled(view, false);

        final DB db = DB.getInstance(getContext());

        SimpleCursorAdapter cadapter = new SimpleCursorAdapter(
                getContext(),
                R.layout.spinner_contact,
                null,
                new String[]{"name", "email", "photo"},
                new int[]{R.id.tvName, R.id.tvEmail, R.id.ivPhoto},
                0);

        cadapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            private int colName = -1;
            private int colLocal = -1;

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                try {
                    int id = view.getId();
                    if (id == R.id.tvName) {
                        if (colName < 0)
                            colName = cursor.getColumnIndex("name");

                        if (cursor.isNull(colName)) {
                            ((TextView) view).setText("-");
                            return true;
                        }
                    } else if (id == R.id.ivPhoto) {
                        if (colLocal < 0)
                            colLocal = cursor.getColumnIndex("local");

                        ImageView photo = (ImageView) view;

                        GradientDrawable bg = new GradientDrawable();
                        if (circular)
                            bg.setShape(GradientDrawable.OVAL);
                        else
                            bg.setCornerRadius(dp3);
                        photo.setBackground(bg);
                        photo.setClipToOutline(true);

                        if (cursor.getInt(colLocal) == 1)
                            photo.setImageDrawable(null);
                        else {
                            String uri = cursor.getString(columnIndex);
                            if (uri == null)
                                photo.setImageResource(R.drawable.twotone_person_24);
                            else
                                photo.setImageURI(Uri.parse(uri));
                        }
                        return true;
                    }
                } catch (Throwable ex) {
                    Log.e(ex);
                }
                return false;
            }
        });

        cadapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            private int colName = -1;
            private int colEmail = -1;

            public CharSequence convertToString(Cursor cursor) {
                try {
                    if (colName < 0)
                        colName = cursor.getColumnIndex("name");
                    if (colEmail < 0)
                        colEmail = cursor.getColumnIndex("email");

                    String name = cursor.getString(colName);
                    String email = cursor.getString(colEmail);

                    InternetAddress address = MessageHelper.buildAddress(email, name, suggest_names);
                    if (address == null)
                        return email;

                    return MessageHelper.formatAddressesCompose(new Address[]{address});
                } catch (Throwable ex) {
                    Log.e(ex);
                    return ex.toString();
                }
            }
        });

        cadapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence typed) {
                MatrixCursor result = new MatrixCursor(new String[]{"_id", "name", "email", "photo", "local"});

                try {
                    Log.i("Suggest contact=" + typed);
                    if (typed == null)
                        return result;

                    final Context context = getContext();
                    if (context == null)
                        return result;

                    String wildcard = "%" + typed + "%";
                    Map<String, EntityContact> map = new HashMap<>();

                    String glob = "*" +
                            typed.toString().toLowerCase()
                                    .replaceAll("[aáàäâãåæ]", "\\[aáàäâãåæ\\]")
                                    .replaceAll("[bß]", "\\[bß\\]")
                                    .replaceAll("[cç]", "\\[cç\\]")
                                    .replaceAll("[eéèëê]", "\\[eéèëê\\]")
                                    .replaceAll("[iíìïî]", "\\[iíìïî\\]")
                                    .replaceAll("[nñ]", "\\[nñ\\]")
                                    .replaceAll("[oóòöôõøœ]", "\\[oóòöôõøœ\\]")
                                    .replaceAll("[uúùüû]", "\\[uúùüû\\]")
                                    .replace("*", "[*]")
                                    .replace("?", "[?]") +
                            "*";

                    boolean contacts = Helper.hasPermission(context, Manifest.permission.READ_CONTACTS);
                    if (contacts) {
                        try (Cursor cursor = resolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                new String[]{
                                        ContactsContract.Contacts.DISPLAY_NAME,
                                        ContactsContract.CommonDataKinds.Email.DATA,
                                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                                        ContactsContract.Contacts.STARRED
                                },
                                ContactsContract.CommonDataKinds.Email.DATA + " <> ''" +
                                        " AND (" + ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?" +
                                        " OR LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") GLOB ?" +
                                        " OR " + ContactsContract.CommonDataKinds.Email.DATA + " LIKE ?)",
                                new String[]{wildcard, glob, wildcard},
                                null)) {

                            while (cursor != null && cursor.moveToNext()) {
                                EntityContact item = new EntityContact();
                                item.id = 0L;
                                item.name = cursor.getString(0);
                                item.email = cursor.getString(1);
                                item.avatar = cursor.getString(2);
                                item.times_contacted = (cursor.getInt(3) == 0 ? 0 : Integer.MAX_VALUE);
                                item.last_contacted = 0L;
                                EntityContact existing = map.get(item.email);
                                if (existing == null ||
                                        (existing.avatar == null && item.avatar != null))
                                    map.put(item.email, item);
                            }
                        }
                    }

                    List<EntityContact> items = new ArrayList<>();
                    if (suggest_sent)
                        items.addAll(db.contact().searchContacts(
                                suggest_account ? FragmentCompose.this.account : null, EntityContact.TYPE_TO, wildcard));
                    if (suggest_received)
                        for (EntityContact item : db.contact().searchContacts(
                                suggest_account ? FragmentCompose.this.account : null, EntityContact.TYPE_FROM, wildcard))
                            if (!MessageHelper.isNoReply(item.email))
                                items.add(item);
                    for (EntityContact item : items) {
                        EntityContact existing = map.get(item.email);
                        if (existing == null)
                            map.put(item.email, item);
                        else {
                            existing.times_contacted = Math.max(existing.times_contacted, item.times_contacted);
                            existing.last_contacted = Math.max(existing.last_contacted, item.last_contacted);
                        }
                    }

                    items = new ArrayList<>(map.values());

                    final Collator collator = Collator.getInstance(Locale.getDefault());
                    collator.setStrength(Collator.SECONDARY); // Case insensitive, process accents etc

                    Collections.sort(items, new Comparator<EntityContact>() {
                        @Override
                        public int compare(EntityContact i1, EntityContact i2) {
                            try {
                                if (suggest_frequently) {
                                    int t = -i1.times_contacted.compareTo(i2.times_contacted);
                                    if (t != 0)
                                        return t;

                                    int l = -i1.last_contacted.compareTo(i2.last_contacted);
                                    if (l != 0)
                                        return l;
                                } else {
                                    // Prefer Android contacts
                                    int a = -Boolean.compare(i1.id == 0, i2.id == 0);
                                    if (a != 0)
                                        return a;
                                }

                                if (TextUtils.isEmpty(i1.name) && TextUtils.isEmpty(i2.name))
                                    return 0;
                                if (TextUtils.isEmpty(i1.name) && !TextUtils.isEmpty(i2.name))
                                    return 1;
                                if (!TextUtils.isEmpty(i1.name) && TextUtils.isEmpty(i2.name))
                                    return -1;

                                int n = collator.compare(i1.name, i2.name);
                                if (n != 0)
                                    return n;

                                return collator.compare(i1.email, i2.email);
                            } catch (Throwable ex) {
                                Log.e(ex);
                                return 0;
                            }
                        }
                    });

                    for (int i = 0; i < items.size(); i++) {
                        EntityContact item = items.get(i);
                        result.newRow()
                                .add(i + 1) // id
                                .add(item.name)
                                .add(item.email)
                                .add(item.avatar)
                                .add(item.id == 0 ? 0 : 1);
                    }
                } catch (Throwable ex) {
                    Log.e(ex);
                }

                Log.i("Suggesting contacts=" + result.getCount());
                return result;
            }
        });

        etTo.setAdapter(cadapter);
        etCc.setAdapter(cadapter);
        etBcc.setAdapter(cadapter);

        etTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                try {
                    Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
                    if (cursor != null && cursor.getCount() > 0) {
                        int colEmail = cursor.getColumnIndex("email");
                        selectIdentityForEmail(colEmail < 0 ? null : cursor.getString(colEmail));
                    }
                } catch (Throwable ex) {
                    Log.e(ex);
                }
            }
        });

        ibCcBcc.setImageLevel(cc_bcc ? 0 : 1);
        grpAddresses.setVisibility(cc_bcc ? View.VISIBLE : View.GONE);

        ibRemoveAttachments.setVisibility(View.GONE);
        ibRemoveAttachments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("question", getString(R.string.title_ask_delete_attachments));

                FragmentDialogAsk fragment = new FragmentDialogAsk();
                fragment.setArguments(args);
                fragment.setTargetFragment(FragmentCompose.this, REQUEST_REMOVE_ATTACHMENTS);
                fragment.show(getParentFragmentManager(), "compose:discard");
            }
        });

        rvAttachment.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        rvAttachment.setLayoutManager(llm);
        rvAttachment.setItemAnimator(null);

        adapter = new AdapterAttachment(this, false, null);
        rvAttachment.setAdapter(adapter);

        tvNoInternetAttachments.setVisibility(View.GONE);

        return view;
    }

    private void selectIdentityForEmail(String email) {
        if (TextUtils.isEmpty(email))
            return;

        Bundle args = new Bundle();
        args.putString("email", email);

        new SimpleTask<Long>() {
            @Override
            protected Long onExecute(Context context, Bundle args) throws Throwable {
                String email = args.getString("email");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean auto_identity = prefs.getBoolean("auto_identity", false);
                boolean suggest_sent = prefs.getBoolean("suggest_sent", true);
                boolean suggest_received = prefs.getBoolean("suggest_received", false);

                if (!auto_identity)
                    return null;

                EntityLog.log(context, "Select identity email=" + email +
                        " sent=" + suggest_sent + " received=" + suggest_received);

                DB db = DB.getInstance(context);
                List<Long> identities = null;
                if (suggest_sent)
                    identities = db.contact().getIdentities(email, EntityContact.TYPE_TO);
                if (suggest_received && (identities == null || identities.size() == 0))
                    identities = db.contact().getIdentities(email, EntityContact.TYPE_FROM);
                EntityLog.log(context, "Selected identity email=" + email +
                        " identities=" + (identities == null ? null : identities.size()));
                if (identities != null && identities.size() == 1)
                    return identities.get(0);

                return null;
            }

            @Override
            protected void onExecuted(Bundle args, Long identity) {
                if (identity == null)
                    return;

                SpinnerAdapter adapter = spIdentity.getAdapter();
                for (int pos = 0; pos < adapter.getCount(); pos++) {
                    EntityIdentity item = (EntityIdentity) adapter.getItem(pos);
                    if (item.id.equals(identity)) {
                        spIdentity.setSelection(pos);
                        break;
                    }
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(FragmentCompose.this, args, "compose:contact");
    }

    private void updateEncryption(EntityIdentity identity, boolean selected) {
        if (identity == null)
            return;

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putLong("identity", identity.id);
        args.putBoolean("selected", selected);
        args.putString("to", etTo.getText().toString().trim());
        args.putString("cc", etCc.getText().toString().trim());
        args.putString("bcc", etBcc.getText().toString().trim());

        new SimpleTask<Integer>() {
            @Override
            protected Integer onExecute(Context context, Bundle args) {
                long id = args.getLong("id");
                long iid = args.getLong("identity");
                boolean selected = args.getBoolean("selected");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sign_default = prefs.getBoolean("sign_default", false);
                boolean encrypt_default = prefs.getBoolean("encrypt_default", false);
                boolean encrypt_auto = prefs.getBoolean("encrypt_auto", false);

                DB db = DB.getInstance(context);

                EntityMessage draft = db.message().getMessage(id);
                if (draft == null)
                    return null;

                if (draft.dsn != null && !EntityMessage.DSN_NONE.equals(draft.dsn))
                    return null;

                EntityIdentity identity = db.identity().getIdentity(iid);
                if (identity == null)
                    return draft.ui_encrypt;

                if (encrypt_auto) {
                    draft.ui_encrypt = null;
                    try {
                        InternetAddress[] to = MessageHelper.parseAddresses(context, args.getString("to"));
                        InternetAddress[] cc = MessageHelper.parseAddresses(context, args.getString("cc"));
                        InternetAddress[] bcc = MessageHelper.parseAddresses(context, args.getString("bcc"));

                        List<Address> recipients = new ArrayList<>();
                        if (to != null)
                            recipients.addAll(Arrays.asList(to));
                        if (cc != null)
                            recipients.addAll(Arrays.asList(cc));
                        if (bcc != null)
                            recipients.addAll(Arrays.asList(bcc));

                        if (identity.encrypt == 0 && PgpHelper.hasPgpKey(context, recipients, true))
                            draft.ui_encrypt = EntityMessage.PGP_SIGNENCRYPT;
                        else if (identity.encrypt == 1 && SmimeHelper.hasSmimeKey(context, recipients, true))
                            draft.ui_encrypt = EntityMessage.SMIME_SIGNENCRYPT;
                    } catch (Throwable ex) {
                        Log.w(ex);
                    }
                }

                if (selected || draft.ui_encrypt == null) {
                    if (encrypt_default || identity.encrypt_default)
                        draft.ui_encrypt = (identity.encrypt == 0
                                ? EntityMessage.PGP_SIGNENCRYPT
                                : EntityMessage.SMIME_SIGNENCRYPT);
                    else if (sign_default || identity.sign_default)
                        draft.ui_encrypt = (identity.encrypt == 0
                                ? EntityMessage.PGP_SIGNONLY
                                : EntityMessage.SMIME_SIGNONLY);
                    else if (selected)
                        draft.ui_encrypt = null;
                }

                db.message().setMessageUiEncrypt(draft.id, draft.ui_encrypt);

                db.message().setMessageSensitivity(draft.id, identity.sensitivity < 1 ? null : identity.sensitivity);

                return draft.ui_encrypt;
            }

            @Override
            protected void onExecuted(Bundle args, Integer encrypt) {
                FragmentCompose.this.encrypt = encrypt;
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(FragmentCompose.this, args, "compose:identity");
    }

    private void onReferenceEdit() {
        PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(getContext(), getViewLifecycleOwner(), ibReferenceEdit);

        popupMenu.getMenu().add(Menu.NONE, R.string.title_edit_plain_text, 1, R.string.title_edit_plain_text);
        popupMenu.getMenu().add(Menu.NONE, R.string.title_edit_formatted_text, 2, R.string.title_edit_formatted_text);
        popupMenu.getMenu().add(Menu.NONE, R.string.title_clipboard_copy, 3, R.string.title_clipboard_copy);
        popupMenu.getMenu().add(Menu.NONE, R.string.title_delete, 4, R.string.title_delete);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.string.title_edit_plain_text) {
                    convertRef(true);
                    return true;
                } else if (itemId == R.string.title_edit_formatted_text) {
                    convertRef(false);
                    return true;
                } else if (itemId == R.string.title_clipboard_copy) {
                    copyRef();
                    return true;
                } else if (itemId == R.string.title_delete) {
                    deleteRef();
                    return true;
                }
                return false;
            }

            private void convertRef(boolean plain) {
                HtmlHelper.clearComposingText(etBody);

                Bundle args = new Bundle();
                args.putLong("id", working);
                args.putBoolean("plain", plain);
                args.putString("body", HtmlHelper.toHtml(etBody.getText(), getContext()));

                new SimpleTask<String>() {
                    @Override
                    protected void onPreExecute(Bundle args) {
                        ibReferenceEdit.setEnabled(false);
                    }

                    @Override
                    protected void onPostExecute(Bundle args) {
                        ibReferenceEdit.setEnabled(true);
                    }

                    @Override
                    protected String onExecute(Context context, Bundle args) throws Throwable {
                        long id = args.getLong("id");
                        boolean plain = args.getBoolean("plain");
                        String body = args.getString("body");

                        File rfile = EntityMessage.getFile(context, id);
                        Document doc = JsoupEx.parse(rfile);
                        Elements ref = doc.select("div[fairemail=reference]");
                        ref.removeAttr("fairemail");

                        Document document = JsoupEx.parse(body);
                        if (plain) {
                            String text = HtmlHelper.getText(context, ref.outerHtml());
                            String[] line = text.split("\\r?\\n");
                            for (int i = 0; i < line.length; i++)
                                line[i] = Html.escapeHtml(line[i]);
                            Element p = document.createElement("p");
                            p.html(TextUtils.join("<br>", line));
                            document.body().appendChild(p);
                            return document.html();
                        } else {
                            for (Element element : ref)
                                document.body().appendChild(element);
                            return document.html(); // Edit-ref
                        }
                    }

                    @Override
                    protected void onExecuted(Bundle args, String html) {
                        Bundle extras = new Bundle();
                        extras.putString("html", html);
                        extras.putBoolean("show", true);
                        extras.putBoolean("refedit", true);
                        onAction(R.id.action_save, extras, "refedit");
                    }

                    @Override
                    protected void onException(Bundle args, Throwable ex) {
                        Log.unexpectedError(getParentFragmentManager(), ex);
                    }
                }.serial().execute(FragmentCompose.this, args, "compose:convert");
            }

            private void copyRef() {
                Context context = getContext();
                if (context == null)
                    return;

                ClipboardManager clipboard = Helper.getSystemService(context, ClipboardManager.class);
                if (clipboard == null)
                    return;

                String html = HtmlHelper.toHtml((Spanned) tvReference.getText(), context);

                ClipData clip = ClipData.newHtmlText(
                        etSubject.getText().toString(),
                        HtmlHelper.getText(getContext(), html),
                        html);
                clipboard.setPrimaryClip(clip);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                    ToastEx.makeText(context, R.string.title_clipboard_copied, Toast.LENGTH_LONG).show();
            }

            private void deleteRef() {
                HtmlHelper.clearComposingText(etBody);

                Bundle extras = new Bundle();
                extras.putString("html", HtmlHelper.toHtml(etBody.getText(), getContext()));
                extras.putBoolean("show", true);
                onAction(R.id.action_save, extras, "refdelete");
            }
        });

        popupMenu.show();
    }

    private void onReferenceImages() {
        show_images = true;
        Bundle extras = new Bundle();
        extras.putBoolean("show", true);
        onAction(R.id.action_save, extras, "refimages");
    }

    @Override
    public void onDestroyView() {
        adapter = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("fair:working", working);
        outState.putBoolean("fair:show_images", show_images);
        outState.putParcelable("fair:photo", photoURI);

        outState.putInt("fair:pickRequest", pickRequest);
        outState.putParcelable("fair:pickUri", pickUri);

        // Focus was lost at this point
        outState.putInt("fair:selection", etBody == null ? 0 : etBody.getSelectionStart());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        state = State.NONE;

        try {
            if (savedInstanceState == null) {
                if (working < 0) {
                    Bundle a = getArguments();
                    if (a == null) {
                        a = new Bundle();
                        a.putString("action", "new");
                        setArguments(a);
                    }

                    Bundle args = new Bundle();

                    args.putString("action", a.getString("action"));
                    args.putLong("id", a.getLong("id", -1));
                    args.putLong("account", a.getLong("account", -1));
                    args.putLong("identity", a.getLong("identity", -1));
                    args.putLong("reference", a.getLong("reference", -1));
                    args.putInt("dsn", a.getInt("dsn", -1));
                    args.putSerializable("ics", a.getSerializable("ics"));
                    args.putString("status", a.getString("status"));
                    args.putBoolean("raw", a.getBoolean("raw", false));
                    args.putLong("answer", a.getLong("answer", -1));
                    args.putString("to", a.getString("to"));
                    args.putString("cc", a.getString("cc"));
                    args.putString("bcc", a.getString("bcc"));
                    args.putString("inreplyto", a.getString("inreplyto"));
                    args.putString("subject", a.getString("subject"));
                    args.putString("body", a.getString("body"));
                    args.putString("text", a.getString("text"));
                    args.putCharSequence("selected", a.getCharSequence("selected"));

                    if (a.containsKey("attachments")) {
                        args.putParcelableArrayList("attachments", a.getParcelableArrayList("attachments"));
                        a.remove("attachments");
                        setArguments(a);
                    }

                    draftLoader.execute(FragmentCompose.this, args, "compose:new");
                } else {
                    Bundle args = new Bundle();
                    args.putString("action", "edit");
                    args.putLong("id", working);

                    draftLoader.execute(FragmentCompose.this, args, "compose:edit");
                }
            } else {
                working = savedInstanceState.getLong("fair:working");
                show_images = savedInstanceState.getBoolean("fair:show_images");
                photoURI = savedInstanceState.getParcelable("fair:photo");

                pickRequest = savedInstanceState.getInt("fair:pickRequest");
                pickUri = savedInstanceState.getParcelable("fair:pickUri");

                Bundle args = new Bundle();
                args.putString("action", working < 0 ? "new" : "edit");
                args.putLong("id", working);

                args.putInt("selection", savedInstanceState.getInt("fair:selection"));

                draftLoader.execute(FragmentCompose.this, args, "compose:instance");
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ConnectivityManager cm = Helper.getSystemService(getContext(), ConnectivityManager.class);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        cm.registerNetworkCallback(builder.build(), networkCallback);
    }

    @Override
    public void onPause() {
        final Context context = getContext();

        if (state == State.LOADED) {
            Bundle extras = new Bundle();
            extras.putBoolean("autosave", true);
            onAction(R.id.action_save, extras, "pause");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong("last_composing", working).apply();

        ConnectivityManager cm = Helper.getSystemService(context, ConnectivityManager.class);
        cm.unregisterNetworkCallback(networkCallback);

        super.onPause();
    }

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            check();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            check();
        }

        @Override
        public void onLost(Network network) {
            check();
        }

        private void check() {
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        checkInternet();
                }
            });
        }
    };

    private void checkInternet() {
        boolean suitable = ConnectionHelper.getNetworkState(getContext()).isSuitable();

        Boolean content = (Boolean) tvNoInternet.getTag();
        tvNoInternet.setVisibility(!suitable && content != null && !content ? View.VISIBLE : View.GONE);

        Boolean downloading = (Boolean) rvAttachment.getTag();
        tvNoInternetAttachments.setVisibility(!suitable && downloading != null && downloading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_compose, menu);

        final Context context = getContext();
        PopupMenuLifecycle.insertIcons(context, menu, false);

        LayoutInflater infl = LayoutInflater.from(context);

        ImageButton ibOpenAi = (ImageButton) infl.inflate(R.layout.action_button, null);
        ibOpenAi.setId(View.generateViewId());
        ibOpenAi.setImageResource(R.drawable.twotone_smart_toy_24);
        ibOpenAi.setContentDescription(getString(R.string.title_openai));
        ibOpenAi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOpenAi();
            }
        });
        menu.findItem(R.id.menu_openai).setActionView(ibOpenAi);

        View v = infl.inflate(R.layout.action_button_text, null);
        v.setId(View.generateViewId());
        ImageButton ib = v.findViewById(R.id.button);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuEncrypt();
            }
        });
        ib.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int[] pos = new int[2];
                ib.getLocationOnScreen(pos);
                int dp24 = Helper.dp2pixels(v.getContext(), 24);

                Toast toast = ToastEx.makeTextBw(v.getContext(),
                        getString(R.string.title_encrypt), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.START, pos[0], pos[1] + dp24);
                toast.show();
                return true;
            }
        });
        menu.findItem(R.id.menu_encrypt).setActionView(v);

        ImageButton ibTranslate = (ImageButton) infl.inflate(R.layout.action_button, null);
        ibTranslate.setId(View.generateViewId());
        ibTranslate.setImageResource(R.drawable.twotone_translate_24);
        ibTranslate.setContentDescription(getString(R.string.title_translate));
        ibTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTranslate(vwAnchorMenu);
            }
        });
        menu.findItem(R.id.menu_translate).setActionView(ibTranslate);

        ImageButton ibZoom = (ImageButton) infl.inflate(R.layout.action_button, null);
        ibZoom.setId(View.generateViewId());
        ibZoom.setImageResource(R.drawable.twotone_format_size_24);
        ib.setContentDescription(getString(R.string.title_legend_zoom));
        ibZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMenuZoom();
            }
        });
        menu.findItem(R.id.menu_zoom).setActionView(ibZoom);

        MenuCompat.setGroupDividerEnabled(menu, true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final Context context = getContext();

        menu.findItem(R.id.menu_encrypt).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_translate).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_translate).setVisible(DeepL.isAvailable(context));
        menu.findItem(R.id.menu_openai).setEnabled(state == State.LOADED && !chatting);
        ((ImageButton) menu.findItem(R.id.menu_openai).getActionView()).setEnabled(!chatting);
        menu.findItem(R.id.menu_openai).setVisible(OpenAI.isAvailable(context));
        menu.findItem(R.id.menu_zoom).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_style).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_media).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_compact).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_contact_group).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_manage_android_contacts).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_manage_local_contacts).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_answer_insert).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_answer_create).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_clear).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_print).setEnabled(state == State.LOADED);

        SpannableStringBuilder ssbZoom = new SpannableStringBuilder(getString(R.string.title_zoom));
        ssbZoom.append(' ');
        for (int i = 0; i <= zoom; i++)
            ssbZoom.append('+');
        menu.findItem(R.id.menu_zoom).setTitle(ssbZoom);
        PopupMenuLifecycle.insertIcon(context, menu.findItem(R.id.menu_zoom), false);

        ActionBar actionBar = getSupportActionBar();
        Context actionBarContext = (actionBar == null ? context : actionBar.getThemedContext());
        int colorEncrypt = Helper.resolveColor(context, R.attr.colorEncrypt);
        int colorActionForeground = Helper.resolveColor(actionBarContext, android.R.attr.textColorPrimary);

        View v = menu.findItem(R.id.menu_encrypt).getActionView();
        ImageButton ibEncrypt = v.findViewById(R.id.button);
        TextView tv = v.findViewById(R.id.text);

        v.setAlpha(state == State.LOADED && !dsn ? 1f : Helper.LOW_LIGHT);
        ibEncrypt.setEnabled(state == State.LOADED && !dsn);

        if (EntityMessage.PGP_SIGNONLY.equals(encrypt) || EntityMessage.SMIME_SIGNONLY.equals(encrypt)) {
            ibEncrypt.setImageResource(R.drawable.twotone_gesture_24);
            ibEncrypt.setImageTintList(ColorStateList.valueOf(colorActionForeground));
            tv.setText(EntityMessage.PGP_SIGNONLY.equals(encrypt) ? "P" : "S");
        } else if (EntityMessage.PGP_ENCRYPTONLY.equals(encrypt) ||
                EntityMessage.PGP_SIGNENCRYPT.equals(encrypt) ||
                EntityMessage.SMIME_SIGNENCRYPT.equals(encrypt)) {
            ibEncrypt.setImageResource(R.drawable.twotone_lock_24);
            ibEncrypt.setImageTintList(ColorStateList.valueOf(colorEncrypt));
            tv.setText(EntityMessage.SMIME_SIGNENCRYPT.equals(encrypt) ? "S" : "P");
        } else {
            ibEncrypt.setImageResource(R.drawable.twotone_lock_open_24);
            ibEncrypt.setImageTintList(ColorStateList.valueOf(colorActionForeground));
            tv.setText(null);
        }

        ImageButton ibTranslate = (ImageButton) menu.findItem(R.id.menu_translate).getActionView();
        ibTranslate.setAlpha(state == State.LOADED ? 1f : Helper.LOW_LIGHT);
        ibTranslate.setEnabled(state == State.LOADED);

        ImageButton ibZoom = (ImageButton) menu.findItem(R.id.menu_zoom).getActionView();
        ibZoom.setAlpha(state == State.LOADED ? 1f : Helper.LOW_LIGHT);
        ibZoom.setEnabled(state == State.LOADED);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean save_drafts = prefs.getBoolean("save_drafts", true);
        boolean send_chips = prefs.getBoolean("send_chips", true);
        boolean send_dialog = prefs.getBoolean("send_dialog", true);
        boolean image_dialog = prefs.getBoolean("image_dialog", true);

        menu.findItem(R.id.menu_save_drafts).setChecked(save_drafts);
        menu.findItem(R.id.menu_send_chips).setChecked(send_chips);
        menu.findItem(R.id.menu_send_dialog).setChecked(send_dialog);
        menu.findItem(R.id.menu_image_dialog).setChecked(image_dialog);
        menu.findItem(R.id.menu_style).setChecked(style);
        menu.findItem(R.id.menu_media).setChecked(media);
        menu.findItem(R.id.menu_compact).setChecked(compact);

        View image = media_bar.findViewById(R.id.menu_image);
        if (image != null)
            image.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onActionImage(false, true);
                    return true;
                }
            });

        if (EntityMessage.PGP_SIGNONLY.equals(encrypt) ||
                EntityMessage.SMIME_SIGNONLY.equals(encrypt))
            bottom_navigation.getMenu().findItem(R.id.action_send).setTitle(R.string.title_sign);
        else if (EntityMessage.PGP_ENCRYPTONLY.equals(encrypt) ||
                EntityMessage.PGP_SIGNENCRYPT.equals(encrypt) ||
                EntityMessage.SMIME_SIGNENCRYPT.equals(encrypt))
            bottom_navigation.getMenu().findItem(R.id.action_send).setTitle(R.string.title_encrypt);
        else
            bottom_navigation.getMenu().findItem(R.id.action_send).setTitle(R.string.title_send);

        Menu m = bottom_navigation.getMenu();
        for (int i = 0; i < m.size(); i++)
            bottom_navigation.findViewById(m.getItem(i).getItemId()).setOnLongClickListener(null);

        bottom_navigation.findViewById(R.id.action_save).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (lt_enabled) {
                    onLanguageTool(0, etBody.length(), false);
                    return true;
                } else
                    return false;
            }
        });

        bottom_navigation.findViewById(R.id.action_send).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean("force_dialog", true);
                onAction(R.id.action_check, args, "force");
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onExit();
            return true;
        } else if (itemId == R.id.menu_encrypt) {
            onMenuEncrypt();
            return true;
        } else if (itemId == R.id.menu_translate) {
            onTranslate(vwAnchorMenu);
            return true;
        } else if (itemId == R.id.menu_zoom) {
            onMenuZoom();
            return true;
        } else if (itemId == R.id.menu_save_drafts) {
            onMenuSaveDrafts();
            return true;
        } else if (itemId == R.id.menu_send_chips) {
            onMenuSendChips();
            return true;
        } else if (itemId == R.id.menu_send_dialog) {
            onMenuSendDialog();
            return true;
        } else if (itemId == R.id.menu_image_dialog) {
            onMenuImageDialog();
            return true;
        } else if (itemId == R.id.menu_style) {
            onMenuStyleBar();
            return true;
        } else if (itemId == R.id.menu_media) {
            onMenuMediaBar();
            return true;
        } else if (itemId == R.id.menu_compact) {
            onMenuCompact();
            return true;
        } else if (itemId == R.id.menu_contact_group) {
            onMenuContactGroup();
            return true;
        } else if (itemId == R.id.menu_manage_android_contacts) {
            onMenuManageAndroidContacts();
            return true;
        } else if (itemId == R.id.menu_manage_local_contacts) {
            onMenuManageLocalContacts();
            return true;
        } else if (itemId == R.id.menu_answer_insert) {
            onMenuAnswerInsert(vwAnchorMenu);
            return true;
        } else if (itemId == R.id.menu_answer_create) {
            onMenuAnswerCreate();
            return true;
        } else if (itemId == R.id.menu_select_identity) {
            onMenuIdentitySelect();
            return true;
        } else if (itemId == R.id.title_search_in_text) {
            startSearch();
            return true;
        } else if (itemId == R.id.menu_clear) {
            StyleHelper.apply(R.id.menu_clear, getViewLifecycleOwner(), null, etBody);
            return true;
        } else if (itemId == R.id.menu_print) {
            onMenuPrint();
            return true;
        } else if (itemId == R.id.menu_legend) {
            onMenuLegend();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMenuAddresses() {
        ibCcBcc.setImageLevel(grpAddresses.getVisibility() == View.GONE ? 0 : 1);
        grpAddresses.setVisibility(grpAddresses.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);

        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                    return;
                if (grpAddresses.getVisibility() == View.GONE)
                    etSubject.requestFocus();
                else
                    etCc.requestFocus();
            }
        });
    }

    private void onMenuEncrypt() {
        EntityIdentity identity = (EntityIdentity) spIdentity.getSelectedItem();
        if (identity == null || identity.encrypt == 0) {
            if (EntityMessage.ENCRYPT_NONE.equals(encrypt) || encrypt == null)
                encrypt = EntityMessage.PGP_SIGNENCRYPT;
            else if (EntityMessage.PGP_ENCRYPTONLY.equals(encrypt) ||
                    EntityMessage.PGP_SIGNENCRYPT.equals(encrypt))
                encrypt = EntityMessage.PGP_SIGNONLY;
            else
                encrypt = EntityMessage.ENCRYPT_NONE;
        } else {
            if (EntityMessage.ENCRYPT_NONE.equals(encrypt) || encrypt == null)
                encrypt = EntityMessage.SMIME_SIGNENCRYPT;
            else if (EntityMessage.SMIME_SIGNENCRYPT.equals(encrypt))
                encrypt = EntityMessage.SMIME_SIGNONLY;
            else
                encrypt = EntityMessage.ENCRYPT_NONE;
        }

        final Context context = getContext();
        if ((EntityMessage.PGP_SIGNONLY.equals(encrypt) ||
                EntityMessage.PGP_ENCRYPTONLY.equals(encrypt) ||
                EntityMessage.PGP_SIGNENCRYPT.equals(encrypt))
                && !PgpHelper.isOpenKeychainInstalled(context)) {
            encrypt = EntityMessage.ENCRYPT_NONE;

            new AlertDialog.Builder(context)
                    .setIcon(R.drawable.twotone_lock_24)
                    .setTitle(R.string.title_no_openpgp)
                    .setMessage(R.string.title_no_openpgp_remark)
                    .setPositiveButton(R.string.title_info, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Helper.viewFAQ(context, 12);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.title_reset, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new SimpleTask<Void>() {
                                @Override
                                protected Void onExecute(Context context, Bundle args) throws Throwable {
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                                    prefs.edit()
                                            .remove("sign_default")
                                            .remove("encrypt_default")
                                            .apply();

                                    DB db = DB.getInstance(context);
                                    db.identity().resetIdentityPGP();

                                    return null;
                                }

                                @Override
                                protected void onException(Bundle args, Throwable ex) {
                                    Log.unexpectedError(getParentFragmentManager(), ex);
                                }
                            }.serial().execute(FragmentCompose.this, new Bundle(), "encrypt:fix");
                        }
                    })
                    .show();
        }

        invalidateOptionsMenu();

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putInt("encrypt", encrypt);

        new SimpleTask<Void>() {
            @Override
            protected Void onExecute(Context context, Bundle args) {
                long id = args.getLong("id");
                int encrypt = args.getInt("encrypt");

                DB db = DB.getInstance(context);
                if (EntityMessage.ENCRYPT_NONE.equals(encrypt))
                    db.message().setMessageUiEncrypt(id, null);
                else
                    db.message().setMessageUiEncrypt(id, encrypt);

                return null;
            }

            @Override
            protected void onExecuted(Bundle args, Void data) {
                int encrypt = args.getInt("encrypt");
                int[] values = getResources().getIntArray(R.array.encryptValues);
                String[] names = getResources().getStringArray(R.array.encryptNames);
                for (int i = 0; i < values.length; i++)
                    if (values[i] == encrypt) {
                        ToastEx.makeText(getContext(), names[i], Toast.LENGTH_LONG).show();
                        break;
                    }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(this, args, "compose:encrypt");
    }

    private void onMenuZoom() {
        zoom = ++zoom % 3;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putInt("compose_zoom", zoom).apply();
        setZoom();
    }

    private void setZoom() {
        final Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean editor_zoom = prefs.getBoolean("editor_zoom", true);
        int message_zoom = (editor_zoom ? prefs.getInt("message_zoom", 100) : 100);
        float textSize = Helper.getTextSize(context, zoom);
        if (textSize != 0) {
            etBody.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * message_zoom / 100f);
            tvReference.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * message_zoom / 100f);
        }
    }

    private void onMenuSaveDrafts() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean save_drafts = prefs.getBoolean("save_drafts", true);
        prefs.edit().putBoolean("save_drafts", !save_drafts).apply();
    }

    private void onMenuSendChips() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean send_chips = prefs.getBoolean("send_chips", true);
        prefs.edit().putBoolean("send_chips", !send_chips).apply();

        etTo.setText(etTo.getText());
        etCc.setText(etCc.getText());
        etBcc.setText(etBcc.getText());

        etSubject.requestFocus();
    }

    private void onMenuSendDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean send_dialog = prefs.getBoolean("send_dialog", true);
        prefs.edit().putBoolean("send_dialog", !send_dialog).apply();
    }

    private void onMenuImageDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean image_dialog = prefs.getBoolean("image_dialog", true);
        prefs.edit().putBoolean("image_dialog", !image_dialog).apply();
    }

    private void onMenuStyleBar() {
        style = !style;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean("compose_style", style).apply();
        ibLink.setVisibility(style && media ? View.GONE : View.VISIBLE);
        style_bar.setVisibility(style || etBody.hasSelection() ? View.VISIBLE : View.GONE);
        media_bar.setVisibility(media && (style || !etBody.hasSelection()) ? View.VISIBLE : View.GONE);
        invalidateOptionsMenu();
    }

    private void onMenuMediaBar() {
        media = !media;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean("compose_media", media).apply();
        ibLink.setVisibility(style && media ? View.GONE : View.VISIBLE);
        style_bar.setVisibility(style || etBody.hasSelection() ? View.VISIBLE : View.GONE);
        media_bar.setVisibility(media && (style || !etBody.hasSelection()) ? View.VISIBLE : View.GONE);
        invalidateOptionsMenu();
    }

    private void onMenuCompact() {
        compact = !compact;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean("compose_compact", compact).apply();
        setCompact(compact);
    }

    private void setCompact(boolean compact) {
        bottom_navigation.setLabelVisibilityMode(compact
                ? LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED
                : LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        ViewGroup.LayoutParams params = bottom_navigation.getLayoutParams();
        params.height = Helper.dp2pixels(view.getContext(), compact ? 36 : 56);
        bottom_navigation.setLayoutParams(params);
    }

    private void onMenuLegend() {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
            getParentFragmentManager().popBackStack("legend", FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Bundle args = new Bundle();
        args.putString("tab", "compose");

        Fragment fragment = new FragmentLegend();
        fragment.setArguments(args);

        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("legend");
        fragmentTransaction.commit();
    }

    private void onMenuContactGroup() {
        onMenuContactGroup(view.findFocus());
    }

    private void onMenuManageAndroidContacts() {
        Intent intent = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
        startActivity(intent);
    }

    private void onMenuManageLocalContacts() {
        FragmentContacts fragment = new FragmentContacts();
        fragment.setArguments(new Bundle()); // all accounts

        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("contacts");
        fragmentTransaction.commit();
    }

    private void onMenuContactGroup(View v) {
        int focussed = 0;
        if (v != null) {
            if (v.getId() == R.id.etCc)
                focussed = 1;
            else if (v.getId() == R.id.etBcc)
                focussed = 2;
        }

        Bundle args = new Bundle();
        args.putLong("working", working);
        args.putInt("focussed", focussed);

        Helper.hideKeyboard(view);

        FragmentDialogContactGroup fragment = new FragmentDialogContactGroup();
        fragment.setArguments(args);
        fragment.setTargetFragment(this, REQUEST_CONTACT_GROUP);
        fragment.show(getParentFragmentManager(), "compose:groups");
    }

    private void onMenuAnswerInsert(View anchor) {
        new SimpleTask<List<EntityAnswer>>() {
            @Override
            protected List<EntityAnswer> onExecute(Context context, Bundle args) {
                List<EntityAnswer> answers = DB.getInstance(context).answer().getAnswers(false);
                return (answers == null ? new ArrayList<>() : answers);
            }

            @Override
            protected void onExecuted(Bundle args, final List<EntityAnswer> answers) {
                final Context context = getContext();

                if (answers.size() == 0) {
                    ToastEx.makeText(context, R.string.title_no_answers, Toast.LENGTH_LONG).show();
                    return;
                }

                PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(context, getViewLifecycleOwner(), anchor);
                EntityAnswer.fillMenu(popupMenu.getMenu(), true, answers, context);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem target) {
                        Intent intent = target.getIntent();
                        if (intent == null)
                            return false;

                        if (!ActivityBilling.isPro(context)) {
                            startActivity(new Intent(context, ActivityBilling.class));
                            return true;
                        }

                        if (target.getGroupId() == 999) {
                            CharSequence config = intent.getCharSequenceExtra("config");
                            int start = etBody.getSelectionStart();
                            etBody.getText().insert(start, config);
                            return true;
                        }

                        long id = intent.getLongExtra("id", -1);

                        Bundle args = new Bundle();
                        args.putLong("id", id);
                        args.putString("to", etTo.getText().toString());

                        new SimpleTask<EntityAnswer>() {
                            @Override
                            protected EntityAnswer onExecute(Context context, Bundle args) throws Throwable {
                                long id = args.getLong("id");
                                String to = args.getString("to");

                                DB db = DB.getInstance(context);
                                EntityAnswer answer = db.answer().getAnswer(id);
                                if (answer != null) {
                                    InternetAddress[] tos = null;
                                    try {
                                        tos = MessageHelper.parseAddresses(context, to);
                                    } catch (AddressException ignored) {
                                    }

                                    String html = EntityAnswer.replacePlaceholders(context, answer.text, tos);

                                    Document d = HtmlHelper.sanitizeCompose(context, html, true);
                                    Spanned spanned = HtmlHelper.fromDocument(context, d, new HtmlHelper.ImageGetterEx() {
                                        @Override
                                        public Drawable getDrawable(Element element) {
                                            String source = element.attr("src");
                                            if (source.startsWith("cid:"))
                                                element.attr("src", "cid:");
                                            return ImageHelper.decodeImage(context,
                                                    working, element, true, zoom, 1.0f, etBody);
                                        }
                                    }, null);
                                    args.putCharSequence("spanned", spanned);

                                    db.answer().applyAnswer(answer.id, new Date().getTime());
                                }

                                return answer;
                            }

                            @Override
                            protected void onExecuted(Bundle args, EntityAnswer answer) {
                                if (answer == null)
                                    return;

                                if (etSubject.getText().length() == 0)
                                    etSubject.setText(answer.name);

                                Spanned spanned = (Spanned) args.getCharSequence("spanned");

                                int start = etBody.getSelectionStart();
                                int end = etBody.getSelectionEnd();
                                if (start > end) {
                                    int tmp = start;
                                    start = end;
                                    end = tmp;
                                }

                                if (start >= 0 && start < end)
                                    etBody.getText().replace(start, end, spanned);
                                else {
                                    if (start < 0) {
                                        start = etBody.length();
                                        etBody.getText().append(spanned);
                                    } else
                                        etBody.getText().insert(start, spanned);

                                    int pos = getAutoPos(start, spanned.length());
                                    if (pos >= 0)
                                        etBody.setSelection(pos);
                                }

                                StyleHelper.markAsInserted(etBody.getText(), start, start + spanned.length());
                            }

                            @Override
                            protected void onException(Bundle args, Throwable ex) {
                                Log.unexpectedError(getParentFragmentManager(), ex);
                            }
                        }.serial().execute(FragmentCompose.this, args, "compose:answer");

                        return true;
                    }
                });

                popupMenu.show();
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(getContext(), getViewLifecycleOwner(), new Bundle(), "compose:answer");
    }

    private void onMenuAnswerCreate() {
        HtmlHelper.clearComposingText(etBody);

        Bundle args = new Bundle();
        args.putString("subject", etSubject.getText().toString());
        args.putString("html", HtmlHelper.toHtml(etBody.getText(), getContext()));

        FragmentAnswer fragment = new FragmentAnswer();
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("compose:answer");
        fragmentTransaction.commit();
    }

    private void onMenuIdentitySelect() {
        Bundle args = new Bundle();
        args.putBoolean("add", true);

        FragmentDialogSelectIdentity fragment = new FragmentDialogSelectIdentity();
        fragment.setArguments(args);
        fragment.setTargetFragment(this, REQUEST_SELECT_IDENTITY);
        fragment.show(getParentFragmentManager(), "select:identity");
    }

    private void onMenuPrint() {
        Bundle extras = new Bundle();
        extras.putBoolean("silent", true);
        onAction(R.id.action_save, extras, "paragraph");

        CharSequence selected = null;
        int start = etBody.getSelectionStart();
        int end = etBody.getSelectionEnd();

        if (start < 0)
            start = 0;
        if (end < 0)
            end = 0;

        if (start != end) {
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }

            selected = etBody.getText().subSequence(start, end);
        }

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putBoolean("headers", false);
        args.putCharSequence("selected", selected);
        args.putBoolean("draft", true);

        new SimpleTask<Void>() {
            @Override
            protected Void onExecute(Context context, Bundle args) throws Throwable {
                // Do nothing: serialize
                return null;
            }

            @Override
            protected void onExecuted(Bundle args, Void dummy) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (prefs.getBoolean("print_html_confirmed", false)) {
                    Intent data = new Intent();
                    data.putExtra("args", args);
                    onActivityResult(REQUEST_PRINT, RESULT_OK, data);
                    return;
                }

                FragmentDialogPrint ask = new FragmentDialogPrint();
                ask.setArguments(args);
                ask.setTargetFragment(FragmentCompose.this, REQUEST_PRINT);
                ask.show(getParentFragmentManager(), "compose:print");
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(this, args, "compose:print");
    }

    private void onOpenAi() {
        int start = etBody.getSelectionStart();
        int end = etBody.getSelectionEnd();
        boolean selection = (start >= 0 && end > start);
        Editable edit = etBody.getText();
        String body = (selection ? edit.subSequence(start, end) : edit).toString().trim();

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putString("body", body);
        args.putBoolean("selection", selection);

        new SimpleTask<OpenAI.Message[]>() {
            @Override
            protected void onPreExecute(Bundle args) {
                chatting = true;
                invalidateOptionsMenu();
            }

            @Override
            protected void onPostExecute(Bundle args) {
                chatting = false;
                invalidateOptionsMenu();
            }

            @Override
            protected OpenAI.Message[] onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                String body = args.getString("body");
                boolean selection = args.getBoolean("selection");

                DB db = DB.getInstance(context);
                EntityMessage draft = db.message().getMessage(id);
                if (draft == null)
                    return null;

                List<EntityMessage> inreplyto;
                if (selection || TextUtils.isEmpty(draft.inreplyto))
                    inreplyto = new ArrayList<>();
                else
                    inreplyto = db.message().getMessagesByMsgId(draft.account, draft.inreplyto);

                List<OpenAI.Message> result = new ArrayList<>();

                if (inreplyto.size() > 0 && inreplyto.get(0).content) {
                    String role = (MessageHelper.equalEmail(draft.from, inreplyto.get(0).from) ? "assistant" : "user");
                    Document parsed = JsoupEx.parse(inreplyto.get(0).getFile(context));
                    Document document = HtmlHelper.sanitizeView(context, parsed, false);
                    Spanned spanned = HtmlHelper.fromDocument(context, document, null, null);
                    result.add(new OpenAI.Message(role, OpenAI.truncateParagraphs(spanned.toString())));
                }

                if (!TextUtils.isEmpty(body))
                    result.add(new OpenAI.Message("assistant", OpenAI.truncateParagraphs(body)));

                if (result.size() == 0)
                    return null;

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String model = prefs.getString("openai_model", "gpt-3.5-turbo");
                float temperature = prefs.getFloat("openai_temperature", 0.5f);
                boolean moderation = prefs.getBoolean("openai_moderation", false);

                if (moderation)
                    for (OpenAI.Message message : result)
                        OpenAI.checkModeration(context, message.getContent());

                OpenAI.Message[] completions =
                        OpenAI.completeChat(context, model, result.toArray(new OpenAI.Message[0]), temperature, 1);

                try {
                    Pair<Double, Double> usage = OpenAI.getGrants(context);
                    args.putDouble("used", usage.first);
                    args.putDouble("granted", usage.second);
                } catch (Throwable ex) {
                    Log.w(ex);
                }

                return completions;
            }

            @Override
            protected void onExecuted(Bundle args, OpenAI.Message[] messages) {
                if (messages == null || messages.length == 0)
                    return;

                String text = messages[0].getContent()
                        .replaceAll("^\\n+", "").replaceAll("\\n+$", "");

                Editable edit = etBody.getText();
                int start = etBody.getSelectionStart();
                int end = etBody.getSelectionEnd();

                int index;
                if (etBody.hasSelection()) {
                    edit.delete(start, end);
                    index = start;
                } else
                    index = end;

                if (index < 0)
                    index = 0;
                if (index > 0 && edit.charAt(index - 1) != '\n')
                    edit.insert(index++, "\n");

                edit.insert(index, text + "\n");
                etBody.setSelection(index + text.length() + 1);

                StyleHelper.markAsInserted(edit, index, index + text.length() + 1);

                if (args.containsKey("used") && args.containsKey("granted")) {
                    double used = args.getDouble("used");
                    double granted = args.getDouble("granted");
                    ToastEx.makeText(getContext(), String.format("$%.2f/%.2f", used, granted), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex, !(ex instanceof IOException));
            }
        }.serial().execute(this, args, "openai");
    }

    private void onTranslate(View anchor) {
        final Context context = anchor.getContext();

        boolean grouped = BuildConfig.DEBUG;
        List<DeepL.Language> languages = DeepL.getTargetLanguages(context, grouped);
        if (languages == null)
            languages = new ArrayList<>();

        boolean subjectHasFocus = etSubject.hasFocus();

        boolean canTranslate;
        if (subjectHasFocus) {
            CharSequence text = etSubject.getText();
            canTranslate = (DeepL.canTranslate(context) &&
                    text != null && !TextUtils.isEmpty(text.toString().trim()));
        } else {
            int s = etBody.getSelectionStart();
            Editable edit = etBody.getText();
            if (s > 1 && s <= edit.length() &&
                    edit.charAt(s - 1) == '\n' &&
                    edit.charAt(s - 2) != '\n' &&
                    (s == edit.length() || edit.charAt(s) == '\n'))
                etBody.setSelection(s - 1, s - 1);

            Pair<Integer, Integer> paragraph = StyleHelper.getParagraph(etBody);
            canTranslate = (DeepL.canTranslate(context) &&
                    paragraph != null);
        }

        PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(context, getViewLifecycleOwner(), anchor);

        popupMenu.getMenu().add(Menu.NONE, 1, 1, R.string.title_translate_configure);

        NumberFormat NF = NumberFormat.getNumberInstance();
        for (int i = 0; i < languages.size(); i++) {
            DeepL.Language lang = languages.get(i);

            SpannableStringBuilder ssb = new SpannableStringBuilderEx(lang.name);
            if (grouped && lang.frequency > 0) {
                int start = ssb.length();
                ssb.append(" (").append(NF.format(lang.frequency)).append(")");
                ssb.setSpan(new RelativeSizeSpan(HtmlHelper.FONT_SMALL),
                        start, ssb.length(), 0);
            }

            MenuItem item = popupMenu.getMenu()
                    .add(lang.favorite ? Menu.FIRST : Menu.NONE, i + 2, i + 2, ssb)
                    .setIntent(new Intent().putExtra("target", lang.target));
            if (lang.icon != null)
                item.setIcon(lang.icon);
            item.setEnabled(canTranslate);
        }

        if (grouped)
            MenuCompat.setGroupDividerEnabled(popupMenu.getMenu(), true);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    DeepL.FragmentDialogDeepL fragment = new DeepL.FragmentDialogDeepL();
                    fragment.show(getParentFragmentManager(), "deepl:configure");
                } else {
                    String target = item.getIntent().getStringExtra("target");
                    onMenuTranslate(target);
                }
                return true;
            }

            private void onMenuTranslate(String target) {
                if (subjectHasFocus) {
                    CharSequence text = etSubject.getText();
                    if (text == null && TextUtils.isEmpty(text.toString().trim()))
                        return;

                    Bundle args = new Bundle();
                    args.putString("target", target);
                    args.putCharSequence("text", text);

                    new SimpleTask<DeepL.Translation>() {
                        @Override
                        protected DeepL.Translation onExecute(Context context, Bundle args) throws Throwable {
                            String target = args.getString("target");
                            CharSequence text = args.getCharSequence("text");
                            return DeepL.translate(text, true, target, context);
                        }

                        @Override
                        protected void onExecuted(Bundle args, DeepL.Translation translation) {
                            if (etSubject == null)
                                return;
                            etSubject.setText(translation.translated_text);
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            Log.unexpectedError(getParentFragmentManager(), ex, !(ex instanceof IOException));
                        }
                    }.execute(FragmentCompose.this, args, "compose:translate");
                } else {
                    final Pair<Integer, Integer> paragraph = StyleHelper.getParagraph(etBody);
                    if (paragraph == null)
                        return;

                    HtmlHelper.clearComposingText(etBody);

                    Editable edit = etBody.getText();
                    CharSequence text = edit.subSequence(paragraph.first, paragraph.second);

                    Bundle args = new Bundle();
                    args.putString("target", target);
                    args.putCharSequence("text", text);

                    new SimpleTask<DeepL.Translation>() {
                        private HighlightSpan highlightSpan;
                        private Toast toast = null;

                        @Override
                        protected void onPreExecute(Bundle args) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                            boolean deepl_highlight = prefs.getBoolean("deepl_highlight", true);
                            if (deepl_highlight) {
                                int textColorHighlight = Helper.resolveColor(getContext(), android.R.attr.textColorHighlight);
                                highlightSpan = new HighlightSpan(textColorHighlight);
                                etBody.getText().setSpan(highlightSpan, paragraph.first, paragraph.second,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
                            }
                            toast = ToastEx.makeText(context, R.string.title_translating, Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        @Override
                        protected void onPostExecute(Bundle args) {
                            if (highlightSpan != null)
                                etBody.getText().removeSpan(highlightSpan);
                            if (toast != null)
                                toast.cancel();
                        }

                        @Override
                        protected DeepL.Translation onExecute(Context context, Bundle args) throws Throwable {
                            String target = args.getString("target");
                            CharSequence text = args.getCharSequence("text");
                            return DeepL.translate(text, true, target, context);
                        }

                        @Override
                        protected void onExecuted(Bundle args, DeepL.Translation translation) {
                            if (paragraph.second > edit.length())
                                return;

                            FragmentActivity activity = getActivity();
                            if (activity == null)
                                return;

                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            boolean small = prefs.getBoolean("deepl_small", false);
                            boolean replace = (!small && prefs.getBoolean("deepl_replace", false));

                            // Insert translated text
                        /*
                            java.lang.IndexOutOfBoundsException: charAt: -1 < 0
                             at android.text.SpannableStringBuilder.charAt(SpannableStringBuilder.java:123)
                             at java.lang.Character.codePointBefore(Character.java:5002)
                             at android.widget.SpellChecker.spellCheck(SpellChecker.java:317)
                             at android.widget.SpellChecker.access$900(SpellChecker.java:48)
                             at android.widget.SpellChecker$SpellParser.parse(SpellChecker.java:760)
                             at android.widget.SpellChecker$SpellParser.parse(SpellChecker.java:649)
                             at android.widget.SpellChecker.spellCheck(SpellChecker.java:263)
                             at android.widget.SpellChecker.spellCheck(SpellChecker.java:229)
                             at android.widget.Editor.updateSpellCheckSpans(Editor.java:1015)
                             at android.widget.Editor.sendOnTextChanged(Editor.java:1610)
                             at android.widget.TextView.sendOnTextChanged(TextView.java:10793)
                             at android.widget.TextView.handleTextChanged(TextView.java:10904)
                             at android.widget.TextView$ChangeWatcher.onTextChanged(TextView.java:13798)
                             at android.text.SpannableStringBuilder.sendTextChanged(SpannableStringBuilder.java:1268)
                             at android.text.SpannableStringBuilder.replace(SpannableStringBuilder.java:577)
                             at android.text.SpannableStringBuilder.insert(SpannableStringBuilder.java:226)
                             at android.text.SpannableStringBuilder.insert(SpannableStringBuilder.java:38)
                         */
                            int len = translation.translated_text.length();

                            edit.insert(paragraph.second, translation.translated_text);

                            if (!replace) {
                                edit.insert(paragraph.second, "\n\n");
                                len += 2;
                            }

                            StyleHelper.markAsInserted(edit, paragraph.second, paragraph.second + len);
                            etBody.setSelection(paragraph.second + len);

                            if (small) {
                                RelativeSizeSpan[] spans = edit.getSpans(
                                        paragraph.first, paragraph.second, RelativeSizeSpan.class);
                                for (RelativeSizeSpan span : spans)
                                    edit.removeSpan(span);
                                edit.setSpan(new RelativeSizeSpan(HtmlHelper.FONT_SMALL),
                                        paragraph.first, paragraph.second,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else if (replace) {
                                edit.delete(paragraph.first, paragraph.second);
                            }

                            // Updated frequency
                            String key = "translated_" + args.getString("target");
                            int count = prefs.getInt(key, 0);
                            prefs.edit().putInt(key, count + 1).apply();

                            activity.invalidateOptionsMenu();
                        }

                        @Override
                        protected void onDestroyed(Bundle args) {
                            if (toast != null) {
                                toast.cancel();
                                toast = null;
                            }
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            etBody.setSelection(paragraph.second);
                            Log.unexpectedError(getParentFragmentManager(), ex, !(ex instanceof IOException));
                        }
                    }.execute(FragmentCompose.this, args, "compose:translate");
                }
            }
        });

        popupMenu.showWithIcons(context, anchor);
    }

    private void onLanguageTool(int start, int end, boolean silent) {
        HtmlHelper.clearComposingText(etBody);

        Log.i("LT running enabled=" + etBody.isSuggestionsEnabled());

        Bundle args = new Bundle();
        args.putCharSequence("text", etBody.getText().subSequence(start, end));

        new SimpleTask<List<LanguageTool.Suggestion>>() {
            private Toast toast = null;
            private HighlightSpan highlightSpan = null;

            @Override
            protected void onPreExecute(Bundle args) {
                if (silent) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    boolean lt_highlight = prefs.getBoolean("lt_highlight", !BuildConfig.PLAY_STORE_RELEASE);
                    if (lt_highlight) {
                        int textColorHighlight = Helper.resolveColor(getContext(), android.R.attr.textColorHighlight);
                        highlightSpan = new HighlightSpan(textColorHighlight);
                        etBody.getText().setSpan(highlightSpan, start, end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
                    }
                } else {
                    toast = ToastEx.makeText(getContext(), R.string.title_suggestions_check, Toast.LENGTH_LONG);
                    toast.show();
                    setBusy(true);
                }
            }

            @Override
            protected void onPostExecute(Bundle args) {
                if (silent) {
                    if (highlightSpan != null)
                        etBody.getText().removeSpan(highlightSpan);
                } else {
                    if (toast != null)
                        toast.cancel();
                    setBusy(false);
                }
            }

            @Override
            protected List<LanguageTool.Suggestion> onExecute(Context context, Bundle args) throws Throwable {
                CharSequence text = args.getCharSequence("text").toString();
                return LanguageTool.getSuggestions(context, text);
            }

            @Override
            protected void onExecuted(Bundle args, List<LanguageTool.Suggestion> suggestions) {
                LanguageTool.applySuggestions(etBody, start, end, suggestions);

                if (!silent &&
                        (suggestions == null || suggestions.size() == 0))
                    ToastEx.makeText(getContext(), R.string.title_suggestions_none, Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onDestroyed(Bundle args) {
                if (toast != null) {
                    toast.cancel();
                    toast = null;
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (!silent) {
                    Throwable exex = new Throwable("LanguageTool", ex);
                    Log.unexpectedError(getParentFragmentManager(), exex, !(ex instanceof IOException));
                }
            }
        }.execute(this, args, "compose:lt");
    }

    private void onActionRecordAudio() {
        // https://developer.android.com/reference/android/provider/MediaStore.Audio.Media.html#RECORD_SOUND_ACTION
        PackageManager pm = getContext().getPackageManager();
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        if (intent.resolveActivity(pm) == null) { // action whitelisted
            Snackbar snackbar = Snackbar.make(view, getString(R.string.title_no_recorder), Snackbar.LENGTH_INDEFINITE)
                    .setGestureInsetBottomIgnored(true);
            snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Helper.viewFAQ(v.getContext(), 158);
                }
            });
            snackbar.show();
        } else
            try {
                startActivityForResult(intent, REQUEST_RECORD_AUDIO);
            } catch (Throwable ex) {
                Helper.reportNoViewer(getContext(), intent, ex);
            }
    }

    private void onActionImage(boolean photo, boolean force) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean image_dialog = prefs.getBoolean("image_dialog", true);
        if (image_dialog || force) {
            Helper.hideKeyboard(view);

            Bundle args = new Bundle();
            args.putInt("title", photo
                    ? R.string.title_attachment_photo
                    : R.string.title_add_image_select);

            FragmentDialogAddImage fragment = new FragmentDialogAddImage();
            fragment.setArguments(args);
            fragment.setTargetFragment(this, REQUEST_IMAGE);
            fragment.show(getParentFragmentManager(), "compose:image");
        } else
            onAddImage(photo);
    }

    private void onActionAttachment() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        PackageManager pm = getContext().getPackageManager();
        if (intent.resolveActivity(pm) == null) // system whitelisted
            ComposeHelper.noStorageAccessFramework(view);
        else
            startActivityForResult(Helper.getChooser(getContext(), intent), REQUEST_ATTACHMENT);
    }

    private void onActionLink() {
        FragmentDialogInsertLink fragment = new FragmentDialogInsertLink();
        fragment.setArguments(FragmentDialogInsertLink.getArguments(etBody));
        fragment.setTargetFragment(this, REQUEST_LINK);
        fragment.show(getParentFragmentManager(), "compose:link");
    }

    private void onActionDiscard() {
        if (isEmpty())
            onAction(R.id.action_delete, "discard");
        else {
            Bundle args = new Bundle();
            args.putString("question", getString(R.string.title_ask_discard));
            args.putBoolean("warning", true);

            FragmentDialogAsk fragment = new FragmentDialogAsk();
            fragment.setArguments(args);
            fragment.setTargetFragment(this, REQUEST_DISCARD);
            fragment.show(getParentFragmentManager(), "compose:discard");
        }
    }

    private void onEncrypt(final EntityMessage draft, final int action, final Bundle extras, final boolean interactive) {
        if (EntityMessage.SMIME_SIGNONLY.equals(draft.ui_encrypt) ||
                EntityMessage.SMIME_SIGNENCRYPT.equals(draft.ui_encrypt)) {
            Bundle args = new Bundle();
            args.putLong("id", draft.id);
            args.putInt("type", draft.ui_encrypt);

            new SimpleTask<EntityIdentity>() {
                @Override
                protected EntityIdentity onExecute(Context context, Bundle args) {
                    long id = args.getLong("id");

                    DB db = DB.getInstance(context);
                    EntityMessage draft = db.message().getMessage(id);
                    if (draft == null || draft.identity == null)
                        return null;

                    EntityIdentity identity = db.identity().getIdentity(draft.identity);
                    if (identity != null && identity.sign_key_alias != null)
                        try {
                            PrivateKey key = KeyChain.getPrivateKey(context, identity.sign_key_alias);
                            args.putBoolean("available", key != null);
                        } catch (Throwable ex) {
                            Log.w(ex);
                        }

                    return identity;
                }

                @Override
                protected void onExecuted(final Bundle args, EntityIdentity identity) {
                    if (identity == null)
                        return;

                    boolean available = args.getBoolean("available");
                    if (available) {
                        args.putString("alias", identity.sign_key_alias);
                        onSmime(args, action, extras);
                        return;
                    }

                    if (interactive)
                        Helper.selectKeyAlias(getActivity(), getViewLifecycleOwner(), identity.sign_key_alias, new Helper.IKeyAlias() {
                            @Override
                            public void onSelected(String alias) {
                                args.putString("alias", alias);
                                if (alias != null)
                                    onSmime(args, action, extras);
                            }

                            @Override
                            public void onNothingSelected() {
                                Snackbar snackbar = Snackbar.make(view, R.string.title_no_key, Snackbar.LENGTH_LONG)
                                        .setGestureInsetBottomIgnored(true);
                                final Intent intent = (Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                                        ? KeyChain.createInstallIntent()
                                        : new Intent(Settings.ACTION_SECURITY_SETTINGS));
                                PackageManager pm = getContext().getPackageManager();
                                if (intent.resolveActivity(pm) != null) // package whitelisted
                                    snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            startActivity(intent);
                                        }
                                    });
                                snackbar.show();
                            }
                        });
                }

                @Override
                protected void onException(Bundle args, Throwable ex) {
                    Log.unexpectedError(getParentFragmentManager(), ex);
                }
            }.serial().execute(this, args, "compose:alias");
        } else {
            try {
                List<Address> recipients = new ArrayList<>();
                if (draft.from != null)
                    recipients.addAll(Arrays.asList(draft.from));
                if (draft.to != null)
                    recipients.addAll(Arrays.asList(draft.to));
                if (draft.cc != null)
                    recipients.addAll(Arrays.asList(draft.cc));
                if (draft.bcc != null)
                    recipients.addAll(Arrays.asList(draft.bcc));

                if (recipients.size() == 0)
                    throw new IllegalArgumentException(getString(R.string.title_to_missing));

                List<String> emails = new ArrayList<>();
                for (int i = 0; i < recipients.size(); i++) {
                    InternetAddress recipient = (InternetAddress) recipients.get(i);
                    String email = recipient.getAddress();
                    if (!emails.contains(email))
                        emails.add(email);
                }
                String[] pgpUserIds = emails.toArray(new String[0]);

                Intent intent;
                if (EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt))
                    intent = new Intent(OpenPgpApi.ACTION_GET_SIGN_KEY_ID);
                else if (EntityMessage.PGP_ENCRYPTONLY.equals(draft.ui_encrypt) ||
                        EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt)) {
                    intent = new Intent(OpenPgpApi.ACTION_GET_KEY_IDS);
                    intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, pgpUserIds);
                } else
                    throw new IllegalArgumentException("Invalid encrypt=" + draft.ui_encrypt);

                Bundle largs = new Bundle();
                largs.putLong("id", working);
                largs.putString("session", UUID.randomUUID().toString());
                largs.putInt("action", action);
                largs.putBundle("extras", extras);
                largs.putBoolean("interactive", interactive);
                intent.putExtra(BuildConfig.APPLICATION_ID, largs);

                onPgp(intent);
            } catch (Throwable ex) {
                if (ex instanceof IllegalArgumentException)
                    Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_LONG)
                            .setGestureInsetBottomIgnored(true).show();
                else {
                    Log.e(ex);
                    Log.unexpectedError(FragmentCompose.this, ex);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            switch (requestCode) {
                case REQUEST_CONTACT_TO:
                case REQUEST_CONTACT_CC:
                case REQUEST_CONTACT_BCC:
                    if (resultCode == RESULT_OK && data != null)
                        onPickContact(requestCode, data);
                    break;
                case REQUEST_SHARED:
                    if (resultCode == RESULT_OK && data != null) {
                        Bundle args = data.getBundleExtra("args");
                        onAddImageFile(args.getParcelableArrayList("images"), true);
                    }
                    break;
                case REQUEST_IMAGE:
                    if (resultCode == RESULT_OK) {
                        int title = data.getBundleExtra("args").getInt("title");
                        onAddImage(title == R.string.title_attachment_photo);
                    }
                    break;
                case REQUEST_IMAGE_FILE:
                    if (resultCode == RESULT_OK && data != null)
                        onAddImageFile(ComposeHelper.getUris(data), false);
                    break;
                case REQUEST_TAKE_PHOTO:
                    if (resultCode == RESULT_OK) {
                        if (photoURI != null)
                            onAddImageFile(Arrays.asList(photoURI), false);
                    }
                    break;
                case REQUEST_ATTACHMENT:
                case REQUEST_RECORD_AUDIO:
                    if (resultCode == RESULT_OK && data != null)
                        onAddAttachment(ComposeHelper.getUris(data), null, false, 0, false, false);
                    break;
                case REQUEST_OPENPGP:
                    if (resultCode == RESULT_OK && data != null)
                        onPgp(data);
                    break;
                case REQUEST_CONTACT_GROUP:
                    if (resultCode == RESULT_OK && data != null)
                        onContactGroupSelected(data.getBundleExtra("args"));
                    break;
                case REQUEST_SELECT_IDENTITY:
                    if (resultCode == RESULT_OK && data != null)
                        onSelectIdentity(data.getBundleExtra("args"));
                    break;
                case REQUEST_PRINT:
                    if (resultCode == RESULT_OK && data != null)
                        onPrint(data.getBundleExtra("args"));
                    break;
                case REQUEST_LINK:
                    if (resultCode == RESULT_OK && data != null)
                        onLinkSelected(data.getBundleExtra("args"));
                    break;
                case REQUEST_DISCARD:
                    if (resultCode == RESULT_OK)
                        onActionDiscardConfirmed();
                    break;
                case REQUEST_SEND:
                    Bundle args = data.getBundleExtra("args");
                    Bundle extras = new Bundle();
                    extras.putBoolean("archive", args.getBoolean("archive"));
                    if (resultCode == RESULT_OK)
                        onAction(R.id.action_send, extras, "send");
                    else if (resultCode == RESULT_FIRST_USER) {
                        extras.putBoolean("now", true);
                        onAction(R.id.action_send, extras, "sendnow");
                    }
                    break;
                case REQUEST_REMOVE_ATTACHMENTS:
                    if (resultCode == RESULT_OK)
                        onRemoveAttachments();
                    break;
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    private void onPickContact(int requestCode, Intent data) {
        Uri uri = data.getData();
        if (uri == null)
            return;

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putString("to", etTo.getText().toString().trim());
        args.putString("cc", etCc.getText().toString().trim());
        args.putString("bcc", etBcc.getText().toString().trim());
        args.putInt("requestCode", requestCode);
        args.putParcelable("uri", uri);

        new SimpleTask<EntityMessage>() {
            @Override
            protected EntityMessage onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                String to = args.getString("to");
                String cc = args.getString("cc");
                String bcc = args.getString("bcc");
                int requestCode = args.getInt("requestCode");
                Uri uri = args.getParcelable("uri");

                if (uri == null)
                    throw new FileNotFoundException();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean suggest_names = prefs.getBoolean("suggest_names", true);

                EntityMessage draft = null;
                DB db = DB.getInstance(context);

                try (Cursor cursor = context.getContentResolver().query(
                        uri,
                        new String[]{
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                ContactsContract.Contacts.DISPLAY_NAME
                        },
                        null, null, null)) {
                    // https://issuetracker.google.com/issues/118400813
                    // https://developer.android.com/guide/topics/providers/content-provider-basics#DisplayResults
                    if (cursor != null && cursor.getCount() == 0)
                        throw new SecurityException("Could not retrieve selected contact");

                    if (cursor != null && cursor.moveToFirst()) {
                        int colEmail = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                        int colName = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                        if (colEmail >= 0 && colName >= 0) {
                            String email = cursor.getString(colEmail);
                            String name = cursor.getString(colName);

                            InternetAddress selected = MessageHelper.buildAddress(email, name, suggest_names);
                            if (selected == null)
                                return null;

                            args.putString("email", selected.getAddress());

                            try {
                                db.beginTransaction();

                                draft = db.message().getMessage(id);
                                if (draft == null)
                                    return null;

                                draft.to = MessageHelper.parseAddresses(context, to);
                                draft.cc = MessageHelper.parseAddresses(context, cc);
                                draft.bcc = MessageHelper.parseAddresses(context, bcc);

                                Address[] address = null;
                                if (requestCode == REQUEST_CONTACT_TO)
                                    address = draft.to;
                                else if (requestCode == REQUEST_CONTACT_CC)
                                    address = draft.cc;
                                else if (requestCode == REQUEST_CONTACT_BCC)
                                    address = draft.bcc;

                                List<Address> list = new ArrayList<>();
                                if (address != null)
                                    list.addAll(Arrays.asList(address));

                                list.add(selected);

                                if (requestCode == REQUEST_CONTACT_TO)
                                    draft.to = list.toArray(new Address[0]);
                                else if (requestCode == REQUEST_CONTACT_CC)
                                    draft.cc = list.toArray(new Address[0]);
                                else if (requestCode == REQUEST_CONTACT_BCC)
                                    draft.bcc = list.toArray(new Address[0]);

                                db.message().updateMessage(draft);

                                db.setTransactionSuccessful();
                            } finally {
                                db.endTransaction();
                            }
                        }
                    }
                }

                return draft;
            }

            @Override
            protected void onExecuted(Bundle args, EntityMessage draft) {
                if (draft == null)
                    return;

                if (requestCode == REQUEST_CONTACT_TO)
                    selectIdentityForEmail(args.getString("email"));

                etTo.setText(MessageHelper.formatAddressesCompose(draft.to));
                etCc.setText(MessageHelper.formatAddressesCompose(draft.cc));
                etBcc.setText(MessageHelper.formatAddressesCompose(draft.bcc));

                // After showDraft/setFocus
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (requestCode == REQUEST_CONTACT_TO)
                                etTo.setSelection(etTo.length());
                            else if (requestCode == REQUEST_CONTACT_CC)
                                etCc.setSelection(etCc.length());
                            else if (requestCode == REQUEST_CONTACT_BCC)
                                etBcc.setSelection(etBcc.length());
                        } catch (Throwable ex) {
                            Log.e(ex);
                        }
                    }
                });
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (ex instanceof SecurityException)
                    try {
                        pickRequest = requestCode;
                        pickUri = uri;
                        String permission = Manifest.permission.READ_CONTACTS;
                        requestPermissions(new String[]{permission}, REQUEST_PERMISSIONS);
                    } catch (Throwable ex1) {
                        Log.unexpectedError(getParentFragmentManager(), ex1);
                    }
                else
                    Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(this, args, "compose:picked");
    }

    void onAddTo(String email) {
        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putString("to", etTo.getText().toString().trim());
        args.putString("email", email);

        new SimpleTask<EntityMessage>() {
            @Override
            protected EntityMessage onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                String to = args.getString("to");
                String email = args.getString("email");

                EntityMessage draft;
                DB db = DB.getInstance(context);

                try {
                    db.beginTransaction();

                    draft = db.message().getMessage(id);
                    if (draft == null)
                        return null;

                    List<Address> list = new ArrayList<>();
                    Address[] _to = MessageHelper.parseAddresses(context, to);
                    if (_to != null)
                        list.addAll(Arrays.asList(_to));
                    list.add(new InternetAddress(email, null, StandardCharsets.UTF_8.name()));

                    draft.to = list.toArray(new Address[0]);

                    db.message().updateMessage(draft);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                return draft;
            }

            @Override
            protected void onExecuted(Bundle args, EntityMessage draft) {
                if (draft == null)
                    return;

                selectIdentityForEmail(args.getString("email"));
                etTo.setText(MessageHelper.formatAddressesCompose(draft.to));

                // After showDraft/setFocus
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            etTo.setSelection(etTo.length());
                        } catch (Throwable ex) {
                            Log.e(ex);
                        }
                    }
                });
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(this, args, "compose:to");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (pickUri == null)
            return;
        for (int i = 0; i < permissions.length; i++)
            if (Manifest.permission.READ_CONTACTS.equals(permissions[i]))
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    onPickContact(pickRequest, new Intent().setData(pickUri));
    }

    private void onAddImage(boolean photo) {
        Context context = getContext();
        PackageManager pm = context.getPackageManager();
        if (photo) {
            // https://developer.android.com/training/camera/photobasics
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(pm) == null) { // action whitelisted
                Snackbar snackbar = Snackbar.make(view, getString(R.string.title_no_camera), Snackbar.LENGTH_LONG)
                        .setGestureInsetBottomIgnored(true);
                snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Helper.viewFAQ(v.getContext(), 158);
                    }
                });
                snackbar.show();
            } else {
                File dir = Helper.ensureExists(new File(context.getFilesDir(), "photo"));
                File file = new File(dir, working + "_" + new Date().getTime() + ".jpg");
                try {
                    photoURI = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                } catch (Throwable ex) {
                    // java.lang.IllegalArgumentException: Failed to resolve canonical path for ...
                    Helper.reportNoViewer(context, intent, ex);
                }
            }
        } else {
            // https://developer.android.com/training/data-storage/shared/photopicker#device-availability
            // https://developer.android.com/reference/android/provider/MediaStore#ACTION_PICK_IMAGES
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean photo_picker = prefs.getBoolean("photo_picker", true);
            if (photo_picker && Helper.hasPhotoPicker())
                try {
                    Log.i("Using photo picker");
                    pickImages.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                    return;
                } catch (Throwable ex) {
                    Log.e(ex);
                }

            Log.i("Using file picker");
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            if (intent.resolveActivity(pm) == null) // GET_CONTENT whitelisted
                ComposeHelper.noStorageAccessFramework(view);
            else
                startActivityForResult(Helper.getChooser(context, intent), REQUEST_IMAGE_FILE);
        }
    }

    private void onAddImageFile(List<Uri> uri, boolean focus) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean add_inline = prefs.getBoolean("add_inline", true);
        boolean resize_images = prefs.getBoolean("resize_images", true);
        boolean privacy_images = prefs.getBoolean("privacy_images", false);
        int resize = prefs.getInt("resize", ComposeHelper.REDUCED_IMAGE_SIZE);
        onAddAttachment(uri, null, add_inline, resize_images ? resize : 0, privacy_images, focus);
    }

    private void onAddAttachment(List<Uri> uris, String[] types, boolean image, int resize, boolean privacy, boolean focus) {
        HtmlHelper.clearComposingText(etBody);

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putParcelableArrayList("uris", new ArrayList<>(uris));
        args.putStringArray("types", types);
        args.putBoolean("image", image);
        args.putInt("resize", resize);
        args.putInt("zoom", zoom);
        args.putBoolean("privacy", privacy);
        args.putCharSequence("body", etBody.getText());
        args.putInt("start", etBody.getSelectionStart());
        args.putBoolean("focus", focus);

        new SimpleTask<Spanned>() {
            @Override
            protected Spanned onExecute(Context context, Bundle args) throws IOException {
                final long id = args.getLong("id");
                List<Uri> uris = args.getParcelableArrayList("uris");
                String[] types = args.getStringArray("types");
                boolean image = args.getBoolean("image");
                int resize = args.getInt("resize");
                int zoom = args.getInt("zoom");
                boolean privacy = args.getBoolean("privacy");
                CharSequence body = args.getCharSequence("body");
                int start = args.getInt("start");

                SpannableStringBuilder s = new SpannableStringBuilderEx(body);
                if (start < 0)
                    start = 0;
                if (start > s.length())
                    start = s.length();

                for (int i = 0; i < uris.size(); i++) {
                    Uri uri = uris.get(i);
                    String type = (types != null && i < types.length ? types[i] : null);

                    EntityAttachment attachment = ComposeHelper.addAttachment(context, id, uri, type, image, resize, privacy);
                    if (attachment == null)
                        continue;
                    if (!image || !attachment.isImage())
                        continue;

                    File file = attachment.getFile(context);
                    Uri cid = Uri.parse("cid:" + BuildConfig.APPLICATION_ID + "." + attachment.id);

                    Drawable d;
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(file.getAbsoluteFile());
                            d = ImageDecoder.decodeDrawable(source);
                        } else
                            d = Drawable.createFromPath(file.getAbsolutePath());
                    } catch (Throwable ex) {
                        Log.w(ex);
                        d = Drawable.createFromPath(file.getAbsolutePath());
                    }

                    if (d == null) {
                        int px = Helper.dp2pixels(context, (zoom + 1) * 24);
                        d = ContextCompat.getDrawable(context, R.drawable.twotone_broken_image_24);
                        d.setBounds(0, 0, px, px);
                    }

                    s.insert(start, "\n\uFFFC\n"); // Object replacement character
                    ImageSpan is = new ImageSpan(context, cid);
                    s.setSpan(is, start + 1, start + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    start += 3;
                }

                if (!image)
                    return null;

                args.putInt("start", start);

                DB db = DB.getInstance(context);
                db.message().setMessagePlainOnly(id, 0);

                String html = HtmlHelper.toHtml(s, context);

                EntityMessage draft = db.message().getMessage(id);
                if (draft != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean write_below = prefs.getBoolean("write_below", false);

                    boolean wb = (draft == null || draft.write_below == null ? write_below : draft.write_below);

                    File file = draft.getFile(context);
                    Elements ref = JsoupEx.parse(file).select("div[fairemail=reference]");

                    Document doc = JsoupEx.parse(html);

                    for (Element e : ref)
                        if (wb && draft.wasforwardedfrom == null)
                            doc.body().prependChild(e);
                        else
                            doc.body().appendChild(e);

                    EntityIdentity identity = db.identity().getIdentity(draft.identity);
                    ComposeHelper.addSignature(context, doc, draft, identity);

                    Helper.writeText(file, doc.html());
                }

                Document d = HtmlHelper.sanitizeCompose(context, html, true);
                return HtmlHelper.fromDocument(context, d, new HtmlHelper.ImageGetterEx() {
                    @Override
                    public Drawable getDrawable(Element element) {
                        return ImageHelper.decodeImage(context,
                                id, element, true, zoom, 1.0f, etBody);
                    }
                }, null);
            }

            @Override
            protected void onExecuted(Bundle args, final Spanned body) {
                // Update text
                if (body != null)
                    etBody.setText(body);

                // Restore cursor/keyboard
                int start = args.getInt("start");
                boolean focus = args.getBoolean("focus");
                if (focus)
                    setFocus(null, start, start, true);
                else if (body != null) {
                    if (start <= body.length())
                        etBody.setSelection(start);
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                ComposeHelper.handleException(FragmentCompose.this, view, ex);
            }
        }.serial().execute(this, args, "compose:attachment:add");
    }

    void onSharedAttachments(ArrayList<Uri> uris) {
        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putParcelableArrayList("uris", uris);

        new SimpleTask<ArrayList<Uri>>() {
            @Override
            protected ArrayList<Uri> onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                List<Uri> uris = args.getParcelableArrayList("uris");

                ArrayList<Uri> images = new ArrayList<>();
                for (Uri uri : uris)
                    try {
                        ComposeHelper.UriInfo info = ComposeHelper.getUriInfo(uri, context);
                        if (info.isImage())
                            images.add(uri);
                        else
                            ComposeHelper.addAttachment(context, id, uri, null, false, 0, false);
                    } catch (IOException ex) {
                        Log.e(ex);
                    }

                return images;
            }

            @Override
            protected void onExecuted(Bundle args, ArrayList<Uri> images) {
                if (images.size() == 0)
                    return;

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean image_dialog = prefs.getBoolean("image_dialog", true);

                if (image_dialog) {
                    Helper.hideKeyboard(view);

                    Bundle aargs = new Bundle();
                    aargs.putInt("title", android.R.string.ok);
                    aargs.putParcelableArrayList("images", images);

                    FragmentDialogAddImage fragment = new FragmentDialogAddImage();
                    fragment.setArguments(aargs);
                    fragment.setTargetFragment(FragmentCompose.this, REQUEST_SHARED);
                    fragment.show(getParentFragmentManager(), "compose:shared");
                } else
                    onAddImageFile(images, false);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                ComposeHelper.handleException(FragmentCompose.this, view, ex);
            }
        }.serial().execute(this, args, "compose:shared");
    }

    private void onPgp(Intent data) {
        final Bundle args = new Bundle();
        args.putParcelable("data", data);

        pgpLoader.serial().execute(this, args, "compose:pgp");
    }

    private final SimpleTask<Object> pgpLoader = new LoaderComposePgp() {
        @Override
        protected void onPreExecute(Bundle args) {
            setBusy(true);
        }

        @Override
        protected void onPostExecute(Bundle args) {
            setBusy(false);
        }

        @Override
        protected void onExecuted(Bundle args, Object result) {
            Log.i("Result= " + result);
            if (result == null) {
                int action = args.getInt("action");
                Bundle extras = args.getBundle("extras");
                extras.putBoolean("encrypted", true);
                onAction(action, extras, "pgp");
            } else if (result instanceof Intent) {
                Intent intent = (Intent) result;
                onPgp(intent);
            } else if (result instanceof PendingIntent)
                if (args.getBoolean("interactive"))
                    try {
                        ToastEx.makeText(getContext(), R.string.title_user_interaction, Toast.LENGTH_SHORT).show();
                        PendingIntent pi = (PendingIntent) result;
                        startIntentSenderForResult(
                                pi.getIntentSender(),
                                REQUEST_OPENPGP,
                                null, 0, 0, 0,
                                Helper.getBackgroundActivityOptions());
                    } catch (IntentSender.SendIntentException ex) {
                        Log.unexpectedError(getParentFragmentManager(), ex);
                    }
                else {
                    if (BuildConfig.DEBUG)
                        ToastEx.makeText(getContext(), "Non interactive", Toast.LENGTH_SHORT).show();
                }
        }

        @Override
        protected void onException(Bundle args, Throwable ex) {
            if (ex instanceof IllegalArgumentException
                    || ex instanceof GeneralSecurityException /* InvalidKeyException */) {
                Log.i(ex);
                Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_LONG)
                        .setGestureInsetBottomIgnored(true).show();
            } else if (ex instanceof OperationCanceledException) {
                Snackbar snackbar = Snackbar.make(view, R.string.title_no_openpgp, Snackbar.LENGTH_INDEFINITE)
                        .setGestureInsetBottomIgnored(true);
                snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackbar.dismiss();
                        Helper.viewFAQ(v.getContext(), 12);
                    }
                });
                snackbar.show();
            } else
                Log.unexpectedError(getParentFragmentManager(), ex);
        }
    };

    private void onSmime(Bundle args, final int action, final Bundle extras) {
        new SimpleTask<Void>() {
            @Override
            protected void onPreExecute(Bundle args) {
                setBusy(true);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                setBusy(false);
            }

            @Override
            protected Void onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                int type = args.getInt("type");
                String alias = args.getString("alias");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean check_certificate = prefs.getBoolean("check_certificate", true);

                File tmp = Helper.ensureExists(new File(context.getFilesDir(), "encryption"));

                DB db = DB.getInstance(context);

                // Get data
                EntityMessage draft = db.message().getMessage(id);
                if (draft == null)
                    throw new MessageRemovedException("S/MIME");
                EntityIdentity identity = db.identity().getIdentity(draft.identity);
                if (identity == null)
                    throw new IllegalArgumentException(context.getString(R.string.title_from_missing));

                // Get/clean attachments
                List<EntityAttachment> attachments = db.attachment().getAttachments(id);
                for (EntityAttachment attachment : new ArrayList<>(attachments))
                    if (attachment.encryption != null) {
                        db.attachment().deleteAttachment(attachment.id);
                        attachments.remove(attachment);
                    }

                // Build message to sign
                //   openssl smime -verify <xxx.eml
                Properties props = MessageHelper.getSessionProperties(true);
                Session isession = Session.getInstance(props, null);
                MimeMessage imessage = new MimeMessage(isession);
                MessageHelper.build(context, draft, attachments, identity, true, imessage);
                imessage.saveChanges();
                BodyPart bpContent = new MimeBodyPart() {
                    @Override
                    public void setContent(Object content, String type) throws MessagingException {
                        super.setContent(content, type);

                        // https://javaee.github.io/javamail/FAQ#howencode
                        updateHeaders();
                        if (content instanceof Multipart) {
                            try {
                                MessageHelper.overrideContentTransferEncoding((Multipart) content);
                            } catch (IOException ex) {
                                Log.e(ex);
                            }
                        } else
                            setHeader("Content-Transfer-Encoding", "base64");
                    }
                };
                bpContent.setContent(imessage.getContent(), imessage.getContentType());

                if (alias == null)
                    throw new IllegalArgumentException("Key alias missing");

                // Get private key
                PrivateKey privkey = KeyChain.getPrivateKey(context, alias);
                if (privkey == null)
                    throw new IllegalArgumentException("Private key missing");

                // Get public key
                X509Certificate[] chain = KeyChain.getCertificateChain(context, alias);
                if (chain == null || chain.length == 0)
                    throw new IllegalArgumentException("Certificate missing");

                if (check_certificate) {
                    // Check public key validity
                    try {
                        chain[0].checkValidity();
                        // TODO: check digitalSignature/nonRepudiation key usage
                        // https://datatracker.ietf.org/doc/html/rfc3850#section-4.4.2
                    } catch (CertificateException ex) {
                        String msg = ex.getMessage();
                        throw new IllegalArgumentException(
                                TextUtils.isEmpty(msg) ? Log.formatThrowable(ex) : msg);
                    }

                    // Check public key email
                    boolean known = false;
                    List<String> emails = EntityCertificate.getEmailAddresses(chain[0]);
                    for (String email : emails)
                        if (email.equalsIgnoreCase(identity.email)) {
                            known = true;
                            break;
                        }

                    if (!known && emails.size() > 0) {
                        String message = identity.email + " (" + TextUtils.join(", ", emails) + ")";
                        throw new IllegalArgumentException(
                                context.getString(R.string.title_certificate_missing, message),
                                new CertificateException());
                    }
                }

                // Store selected alias
                db.identity().setIdentitySignKeyAlias(identity.id, alias);

                // Build content
                File sinput = new File(tmp, draft.id + ".smime_sign");
                if (EntityMessage.SMIME_SIGNONLY.equals(type))
                    try (OutputStream os = new MessageHelper.CanonicalizingStream(
                            new BufferedOutputStream(new FileOutputStream(sinput)), EntityAttachment.SMIME_CONTENT, null)) {
                        bpContent.writeTo(os);
                    }
                else
                    try (FileOutputStream fos = new FileOutputStream(sinput)) {
                        bpContent.writeTo(fos);
                    }

                if (EntityMessage.SMIME_SIGNONLY.equals(type)) {
                    EntityAttachment cattachment = new EntityAttachment();
                    cattachment.message = draft.id;
                    cattachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                    cattachment.name = "content.asc";
                    cattachment.type = "text/plain";
                    cattachment.disposition = Part.INLINE;
                    cattachment.encryption = EntityAttachment.SMIME_CONTENT;
                    cattachment.id = db.attachment().insertAttachment(cattachment);

                    File content = cattachment.getFile(context);
                    Helper.copy(sinput, content);

                    db.attachment().setDownloaded(cattachment.id, content.length());
                }

                // Sign
                Store store = new JcaCertStore(Arrays.asList(chain));
                CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
                cmsGenerator.addCertificates(store);

                String signAlgorithm = prefs.getString("sign_algo_smime", "SHA-256");

                String algorithm = privkey.getAlgorithm();
                if (TextUtils.isEmpty(algorithm) || "RSA".equals(algorithm))
                    Log.i("Private key algorithm=" + algorithm);
                else
                    Log.e("Private key algorithm=" + algorithm);

                if (TextUtils.isEmpty(algorithm))
                    algorithm = "RSA";
                else if ("EC".equals(algorithm))
                    algorithm = "ECDSA";

                algorithm = signAlgorithm.replace("-", "") + "with" + algorithm;
                Log.i("Sign algorithm=" + algorithm);

                ContentSigner contentSigner = new JcaContentSignerBuilder(algorithm)
                        .build(privkey);
                DigestCalculatorProvider digestCalculator = new JcaDigestCalculatorProviderBuilder()
                        .build();
                SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculator)
                        .build(contentSigner, chain[0]);
                cmsGenerator.addSignerInfoGenerator(signerInfoGenerator);

                CMSTypedData cmsData = new CMSProcessableFile(sinput);
                CMSSignedData cmsSignedData = cmsGenerator.generate(cmsData);
                byte[] signedMessage = cmsSignedData.getEncoded();

                Helper.secureDelete(sinput);

                // Build signature
                if (EntityMessage.SMIME_SIGNONLY.equals(type)) {
                    ContentType ct = new ContentType("application/pkcs7-signature");
                    ct.setParameter("micalg", signAlgorithm.toLowerCase(Locale.ROOT));

                    EntityAttachment sattachment = new EntityAttachment();
                    sattachment.message = draft.id;
                    sattachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                    sattachment.name = "smime.p7s";
                    sattachment.type = ct.toString();
                    sattachment.disposition = Part.INLINE;
                    sattachment.encryption = EntityAttachment.SMIME_SIGNATURE;
                    sattachment.id = db.attachment().insertAttachment(sattachment);

                    File file = sattachment.getFile(context);
                    try (OutputStream os = new FileOutputStream(file)) {
                        os.write(signedMessage);
                    }

                    db.attachment().setDownloaded(sattachment.id, file.length());

                    return null;
                }

                List<Address> addresses = new ArrayList<>();
                if (draft.to != null)
                    addresses.addAll(Arrays.asList(draft.to));
                if (draft.cc != null)
                    addresses.addAll(Arrays.asList(draft.cc));
                if (draft.bcc != null)
                    addresses.addAll(Arrays.asList(draft.bcc));

                List<X509Certificate> certs = new ArrayList<>();

                boolean own = true;
                for (Address address : addresses) {
                    boolean found = false;
                    Throwable cex = null;
                    String email = ((InternetAddress) address).getAddress();
                    List<EntityCertificate> acertificates = db.certificate().getCertificateByEmail(email);
                    if (acertificates != null)
                        for (EntityCertificate acertificate : acertificates) {
                            X509Certificate cert = acertificate.getCertificate();
                            try {
                                cert.checkValidity();
                                certs.add(cert);
                                found = true;
                                if (cert.equals(chain[0]))
                                    own = false;
                            } catch (CertificateException ex) {
                                Log.w(ex);
                                cex = ex;
                            }
                        }

                    if (!found)
                        if (cex == null)
                            throw new IllegalArgumentException(
                                    context.getString(R.string.title_certificate_missing, email));
                        else
                            throw new IllegalArgumentException(
                                    context.getString(R.string.title_certificate_invalid, email), cex);
                }

                // Allow sender to decrypt own message
                if (own)
                    certs.add(chain[0]);

                // Build signature
                BodyPart bpSignature = new MimeBodyPart();
                bpSignature.setFileName("smime.p7s");
                bpSignature.setDataHandler(new DataHandler(new ByteArrayDataSource(signedMessage, "application/pkcs7-signature")));
                bpSignature.setDisposition(Part.INLINE);

                // Build message
                ContentType ct = new ContentType("multipart/signed");
                ct.setParameter("micalg", signAlgorithm.toLowerCase(Locale.ROOT));
                ct.setParameter("protocol", "application/pkcs7-signature");
                ct.setParameter("smime-type", "signed-data");
                String ctx = ct.toString();
                int slash = ctx.indexOf("/");
                Multipart multipart = new MimeMultipart(ctx.substring(slash + 1));
                multipart.addBodyPart(bpContent);
                multipart.addBodyPart(bpSignature);
                imessage.setContent(multipart);
                imessage.saveChanges();

                // Encrypt
                CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
                if ("EC".equals(privkey.getAlgorithm())) {
                    // https://datatracker.ietf.org/doc/html/draft-ietf-smime-3278bis
                    JceKeyAgreeRecipientInfoGenerator gen = new JceKeyAgreeRecipientInfoGenerator(
                            CMSAlgorithm.ECCDH_SHA256KDF,
                            privkey,
                            chain[0].getPublicKey(),
                            CMSAlgorithm.AES128_WRAP);
                    for (X509Certificate cert : certs)
                        gen.addRecipient(cert);
                    cmsEnvelopedDataGenerator.addRecipientInfoGenerator(gen);
                    // https://security.stackexchange.com/a/53960
                    // throw new IllegalArgumentException("ECDSA cannot be used for encryption");
                } else {
                    for (X509Certificate cert : certs) {
                        RecipientInfoGenerator gen = new JceKeyTransRecipientInfoGenerator(cert);
                        cmsEnvelopedDataGenerator.addRecipientInfoGenerator(gen);
                    }
                }

                File einput = new File(tmp, draft.id + ".smime_encrypt");
                try (FileOutputStream fos = new FileOutputStream(einput)) {
                    imessage.writeTo(fos);
                }
                CMSTypedData msg = new CMSProcessableFile(einput);

                ASN1ObjectIdentifier encryptionOID;
                String encryptAlgorithm = prefs.getString("encrypt_algo_smime", "AES-128");
                switch (encryptAlgorithm) {
                    case "AES-128":
                        encryptionOID = CMSAlgorithm.AES128_CBC;
                        break;
                    case "AES-192":
                        encryptionOID = CMSAlgorithm.AES192_CBC;
                        break;
                    case "AES-256":
                        encryptionOID = CMSAlgorithm.AES256_CBC;
                        break;
                    default:
                        encryptionOID = CMSAlgorithm.AES128_CBC;
                }
                Log.i("Encryption algorithm=" + encryptAlgorithm + " OID=" + encryptionOID);

                OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(encryptionOID)
                        .build();
                CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator
                        .generate(msg, encryptor);

                EntityAttachment attachment = new EntityAttachment();
                attachment.message = draft.id;
                attachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                attachment.name = "smime.p7m";
                attachment.type = "application/pkcs7-mime";
                attachment.disposition = Part.INLINE;
                attachment.encryption = EntityAttachment.SMIME_MESSAGE;
                attachment.id = db.attachment().insertAttachment(attachment);

                File encrypted = attachment.getFile(context);
                try (OutputStream os = new FileOutputStream(encrypted)) {
                    cmsEnvelopedData.toASN1Structure().encodeTo(os);
                }

                Helper.secureDelete(einput);

                db.attachment().setDownloaded(attachment.id, encrypted.length());

                return null;
            }

            @Override
            protected void onExecuted(Bundle args, Void result) {
                extras.putBoolean("encrypted", true);
                onAction(action, extras, "smime");
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (ex instanceof IllegalArgumentException) {
                    Log.i(ex);
                    Snackbar snackbar = Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_INDEFINITE)
                            .setGestureInsetBottomIgnored(true);
                    Helper.setSnackbarLines(snackbar, 7);
                    snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ex.getCause() instanceof CertificateException)
                                v.getContext().startActivity(new Intent(v.getContext(), ActivitySetup.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        .putExtra("tab", "encryption"));
                            else {
                                EntityIdentity identity = (EntityIdentity) spIdentity.getSelectedItem();

                                PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(getContext(), getViewLifecycleOwner(), vwAnchor);
                                popupMenu.getMenu().add(Menu.NONE, R.string.title_send_dialog, 1, R.string.title_send_dialog);
                                if (identity != null)
                                    popupMenu.getMenu().add(Menu.NONE, R.string.title_reset_sign_key, 2, R.string.title_reset_sign_key);
                                popupMenu.getMenu().add(Menu.NONE, R.string.title_advanced_manage_certificates, 3, R.string.title_advanced_manage_certificates);

                                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        int itemId = item.getItemId();
                                        if (itemId == R.string.title_send_dialog) {
                                            Helper.hideKeyboard(view);

                                            FragmentDialogSend fragment = new FragmentDialogSend();
                                            fragment.setArguments(args);
                                            fragment.setTargetFragment(FragmentCompose.this, REQUEST_SEND);
                                            fragment.show(getParentFragmentManager(), "compose:send");
                                            return true;
                                        } else if (itemId == R.string.title_reset_sign_key) {
                                            Bundle args = new Bundle();
                                            args.putLong("id", identity.id);

                                            new SimpleTask<Void>() {
                                                @Override
                                                protected void onPostExecute(Bundle args) {
                                                    ToastEx.makeText(getContext(), R.string.title_completed, Toast.LENGTH_LONG).show();
                                                }

                                                @Override
                                                protected Void onExecute(Context context, Bundle args) throws Throwable {
                                                    long id = args.getLong("id");

                                                    DB db = DB.getInstance(context);
                                                    try {
                                                        db.beginTransaction();

                                                        db.identity().setIdentitySignKey(id, null);
                                                        db.identity().setIdentitySignKeyAlias(id, null);
                                                        db.identity().setIdentityEncrypt(id, 0);

                                                        db.setTransactionSuccessful();
                                                    } finally {
                                                        db.endTransaction();
                                                    }

                                                    return null;
                                                }

                                                @Override
                                                protected void onException(Bundle args, Throwable ex) {
                                                    Log.unexpectedError(getParentFragmentManager(), ex);
                                                }
                                            }.execute(FragmentCompose.this, args, "identity:reset");
                                        } else if (itemId == R.string.title_advanced_manage_certificates) {
                                            startActivity(new Intent(getContext(), ActivitySetup.class)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    .putExtra("tab", "encryption"));
                                            return true;
                                        }
                                        return false;
                                    }
                                });

                                popupMenu.show();
                            }
                        }
                    });
                    snackbar.show();
                } else {
                    if (ex instanceof RuntimeOperatorException &&
                            ex.getMessage() != null &&
                            ex.getMessage().contains("Memory allocation failed")) {
                        /*
                            org.bouncycastle.operator.RuntimeOperatorException: exception obtaining signature: android.security.KeyStoreException: Memory allocation failed
                                at org.bouncycastle.operator.jcajce.JcaContentSignerBuilder$1.getSignature(Unknown Source:31)
                                at org.bouncycastle.cms.SignerInfoGenerator.generate(Unknown Source:95)
                                at org.bouncycastle.cms.CMSSignedDataGenerator.generate(SourceFile:2)
                                at org.bouncycastle.cms.CMSSignedDataGenerator.generate(SourceFile:1)
                                at eu.faircode.email.FragmentCompose$62.onExecute(SourceFile:74)
                                at eu.faircode.email.FragmentCompose$62.onExecute(SourceFile:1)
                                at eu.faircode.email.SimpleTask$2.run(SourceFile:72)
                                at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:462)
                                at java.util.concurrent.FutureTask.run(FutureTask.java:266)
                                at eu.faircode.email.Helper$PriorityFuture.run(Unknown Source:2)
                                at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
                                at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
                                at java.lang.Thread.run(Thread.java:919)
                            Caused by: java.security.SignatureException: android.security.KeyStoreException: Memory allocation failed
                                at android.security.keystore.AndroidKeyStoreSignatureSpiBase.engineSign(AndroidKeyStoreSignatureSpiBase.java:333)
                                at java.security.Signature$Delegate.engineSign(Signature.java:1418)
                                at java.security.Signature.sign(Signature.java:739)
                                at org.bouncycastle.operator.jcajce.JcaContentSignerBuilder$1.getSignature(Unknown Source:2)
                                ... 12 more
                            Caused by: android.security.KeyStoreException: Memory allocation failed
                                at android.security.KeyStore.getKeyStoreException(KeyStore.java:1303)
                                at android.security.keystore.KeyStoreCryptoOperationChunkedStreamer.doFinal(KeyStoreCryptoOperationChunkedStreamer.java:224)
                                at android.security.keystore.AndroidKeyStoreSignatureSpiBase.engineSign(AndroidKeyStoreSignatureSpiBase.java:328)
                         */
                        // https://issuetracker.google.com/issues/199605614
                        Log.unexpectedError(getParentFragmentManager(), new IllegalArgumentException("Key too large for Android", ex));
                    } else {
                        boolean expected =
                                (ex instanceof OperatorCreationException &&
                                        ex.getCause() instanceof InvalidKeyException);
                        Log.unexpectedError(getParentFragmentManager(), ex, !expected);
                    }
                }
            }
        }.serial().execute(this, args, "compose:s/mime");
    }

    private void onContactGroupSelected(Bundle args) {
        final int target = args.getInt("target");
        if (target > 0) {
            ibCcBcc.setImageLevel(0);
            grpAddresses.setVisibility(View.VISIBLE);
        }

        args.putString("to", etTo.getText().toString().trim());
        args.putString("cc", etCc.getText().toString().trim());
        args.putString("bcc", etBcc.getText().toString().trim());

        new SimpleTask<EntityMessage>() {
            @Override
            protected EntityMessage onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                int target = args.getInt("target");
                long group = args.getLong("group");
                String gname = args.getString("name");
                int type = args.getInt("type");
                String to = args.getString("to");
                String cc = args.getString("cc");
                String bcc = args.getString("bcc");

                EntityLog.log(context, "Selected group=" + group + "/" + gname);

                List<Address> selected = new ArrayList<>();

                if (group < 0) {
                    DB db = DB.getInstance(context);
                    List<EntityContact> contacts = db.contact().getContacts(gname);
                    if (contacts != null)
                        for (EntityContact contact : contacts) {
                            Address address = new InternetAddress(contact.email, contact.name, StandardCharsets.UTF_8.name());
                            selected.add(address);
                        }
                } else
                    try (Cursor cursor = context.getContentResolver().query(
                            ContactsContract.Data.CONTENT_URI,
                            new String[]{ContactsContract.Data.CONTACT_ID},
                            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "= ?" + " AND "
                                    + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + "='"
                                    + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'",
                            new String[]{String.valueOf(group)}, null)) {
                        while (cursor != null && cursor.moveToNext()) {
                            // https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Email
                            try (Cursor contact = getContext().getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                    new String[]{
                                            ContactsContract.Contacts.DISPLAY_NAME,
                                            ContactsContract.CommonDataKinds.Email.DATA,
                                            ContactsContract.CommonDataKinds.Email.TYPE,
                                    },
                                    ContactsContract.Data.CONTACT_ID + " = ?",
                                    new String[]{cursor.getString(0)},
                                    null)) {
                                if (contact != null && contact.moveToNext()) {
                                    String name = contact.getString(0);
                                    String email = contact.getString(1);
                                    int etype = contact.getInt(2);
                                    Address address = new InternetAddress(email, name, StandardCharsets.UTF_8.name());
                                    EntityLog.log(context, "Selected group=" + group + ":" + type +
                                            " address=" + MessageHelper.formatAddresses(new Address[]{address}) + ":" + etype);
                                    if (type == 0 || etype == type)
                                        selected.add(address);
                                }
                            }
                        }
                    }

                EntityMessage draft;
                DB db = DB.getInstance(context);

                try {
                    db.beginTransaction();

                    draft = db.message().getMessage(id);
                    if (draft == null)
                        return null;

                    draft.to = MessageHelper.parseAddresses(context, to);
                    draft.cc = MessageHelper.parseAddresses(context, cc);
                    draft.bcc = MessageHelper.parseAddresses(context, bcc);

                    Address[] address = null;
                    if (target == 0)
                        address = draft.to;
                    else if (target == 1)
                        address = draft.cc;
                    else if (target == 2)
                        address = draft.bcc;

                    List<Address> list = new ArrayList<>();
                    if (address != null)
                        list.addAll(Arrays.asList(address));

                    list.addAll(selected);

                    if (target == 0)
                        draft.to = list.toArray(new Address[0]);
                    else if (target == 1)
                        draft.cc = list.toArray(new Address[0]);
                    else if (target == 2)
                        draft.bcc = list.toArray(new Address[0]);

                    db.message().updateMessage(draft);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                return draft;
            }

            @Override
            protected void onExecuted(Bundle args, EntityMessage draft) {
                if (draft == null)
                    return;

                EditText edit;
                String text;

                if (target == 0) {
                    edit = etTo;
                    text = MessageHelper.formatAddressesCompose(draft.to);
                } else if (target == 1) {
                    edit = etCc;
                    text = MessageHelper.formatAddressesCompose(draft.cc);
                } else if (target == 2) {
                    edit = etBcc;
                    text = MessageHelper.formatAddressesCompose(draft.bcc);
                } else
                    return;

                edit.setText(text);
                edit.setSelection(text.length());
                edit.requestFocus();
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(this, args, "compose:picked");
    }

    private void onSelectIdentity(Bundle args) {
        long id = args.getLong("id");
        if (id < 0) {
            getContext().startActivity(new Intent(getContext(), ActivitySetup.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("manual", true)
                    .putExtra("scroll", true));
            return;
        }

        new SimpleTask<EntityIdentity>() {
            @Override
            protected EntityIdentity onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");

                EntityIdentity identity;
                DB db = DB.getInstance(context);
                try {
                    db.beginTransaction();

                    identity = db.identity().getIdentity(id);
                    if (identity != null) {
                        db.account().resetPrimary();
                        db.account().setAccountPrimary(identity.account, true);
                        db.identity().resetPrimary(identity.account);
                        db.identity().setIdentityPrimary(identity.id, true);
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                return identity;
            }

            @Override
            protected void onExecuted(Bundle args, EntityIdentity identity) {
                ToastEx.makeText(getContext(), R.string.title_completed, Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(this, args, "select:identity");
    }

    private void onPrint(Bundle args) {
        FragmentDialogPrint.print((ActivityBase) getActivity(), getParentFragmentManager(), args);
    }

    private void onLinkSelected(Bundle args) {
        String link = args.getString("link");
        boolean image = args.getBoolean("image");
        int start = args.getInt("start");
        int end = args.getInt("end");
        String title = args.getString("title");
        etBody.setSelection(start, end);
        StyleHelper.apply(R.id.menu_link, getViewLifecycleOwner(), null, etBody, working, zoom, link, image, title);
    }

    private void onActionDiscardConfirmed() {
        onAction(R.id.action_delete, "delete");
    }

    private void onRemoveAttachments() {
        Bundle args = new Bundle();
        args.putLong("id", working);

        new SimpleTask<Void>() {
            @Override
            protected Void onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");

                DB db = DB.getInstance(context);
                db.attachment().deleteAttachments(id);

                return null;
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(FragmentCompose.this, args, "attachments:remove");
    }

    private void onExit() {
        if (state == State.LOADED) {
            state = State.NONE;
            if (!saved && isEmpty())
                onAction(R.id.action_delete, "empty");
            else {
                Bundle extras = new Bundle();
                extras.putBoolean("autosave", true);
                onAction(R.id.action_save, extras, "exit");
                finish();
            }
        } else
            finish();
    }

    private boolean isEmpty() {
        if (!etSubject.getText().toString().equals(subject))
            return false;

        if (!TextUtils.isEmpty(JsoupEx.parse(HtmlHelper.toHtml(etBody.getText(), getContext())).text().trim()))
            return false;

        if (rvAttachment.getAdapter().getItemCount() > 0)
            return false;

        return true;
    }

    private void onAction(int action, String reason) {
        onAction(action, new Bundle(), reason);
    }

    private void onAction(int action, @NonNull Bundle extras, String reason) {
        EntityIdentity identity = (EntityIdentity) spIdentity.getSelectedItem();

        View focus = view.findFocus();
        boolean ime = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            try {
                ime = view.getRootWindowInsets().isVisible(WindowInsetsCompat.Type.ime());
            } catch (Throwable ex) {
                Log.e(ex);
            }

        // Workaround underlines left by Android
        HtmlHelper.clearComposingText(etBody);

        Editable e = etBody.getText();
        boolean notext = e.toString().trim().isEmpty();

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putInt("action", action);
        args.putLong("account", identity == null ? -1 : identity.account);
        args.putLong("identity", identity == null ? -1 : identity.id);
        args.putString("extra", etExtra.getText().toString().trim());
        args.putString("to", etTo.getText().toString().trim());
        args.putString("cc", etCc.getText().toString().trim());
        args.putString("bcc", etBcc.getText().toString().trim());
        args.putString("subject", etSubject.getText().toString().trim());
        args.putCharSequence("loaded", (Spanned) etBody.getTag());
        args.putCharSequence("spanned", etBody.getText());
        args.putBoolean("signature", cbSignature.isChecked());
        args.putBoolean("empty", isEmpty());
        args.putBoolean("notext", notext);
        args.putBoolean("interactive", getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED));
        args.putInt("focus", focus == null ? -1 : focus.getId());
        if (focus instanceof EditText) {
            args.putInt("start", ((EditText) focus).getSelectionStart());
            args.putInt("end", ((EditText) focus).getSelectionEnd());
        }
        args.putBoolean("ime", ime);
        args.putBundle("extras", extras);

        Log.i("Run execute id=" + working + " reason=" + reason);
        actionLoader.execute(this, args, "compose:action:" + LoaderComposeAction.getActionName(action));
    }

    private final SimpleTask<ComposeHelper.DraftData> draftLoader = new LoaderComposeDraft() {
        @Override
        protected void onExecuted(Bundle args, final ComposeHelper.DraftData data) {
            final String action = getArguments().getString("action");
            Log.i("Loaded draft id=" + data.draft.id + " action=" + action);

            FragmentActivity activity = getActivity();
            if (activity != null) {
                Intent intent = activity.getIntent();
                if (intent != null) {
                    intent.putExtra("id", data.draft.id);
                    intent.putExtra("action", "edit");
                }
            }

            working = data.draft.id;
            dsn = (data.draft.dsn != null && !EntityMessage.DSN_NONE.equals(data.draft.dsn));
            encrypt = data.draft.ui_encrypt;
            invalidateOptionsMenu();

            subject = data.draft.subject;
            saved = args.getBoolean("saved");

            // Show identities
            AdapterIdentitySelect iadapter = new AdapterIdentitySelect(getContext(), data.identities);
            spIdentity.setAdapter(iadapter);

            // Select identity
            if (data.draft.identity != null)
                for (int pos = 0; pos < data.identities.size(); pos++) {
                    if (data.identities.get(pos).id.equals(data.draft.identity)) {
                        spIdentity.setTag(pos);
                        spIdentity.setSelection(pos);
                        break;
                    }
                }

            etExtra.setText(data.draft.extra);
            etTo.setText(MessageHelper.formatAddressesCompose(data.draft.to));
            etCc.setText(MessageHelper.formatAddressesCompose(data.draft.cc));
            etBcc.setText(MessageHelper.formatAddressesCompose(data.draft.bcc));
            etSubject.setText(data.draft.subject);

            long reference = args.getLong("reference", -1);
            etTo.setTag(reference < 0 ? "" : etTo.getText().toString());
            etSubject.setTag(reference < 0 ? "" : etSubject.getText().toString());
            cbSignature.setTag(data.draft.signature);

            grpHeader.setVisibility(View.VISIBLE);
            if ("reply_all".equals(action) ||
                    (data.draft.cc != null && data.draft.cc.length > 0) ||
                    (data.draft.bcc != null && data.draft.bcc.length > 0)) {
                ibCcBcc.setImageLevel(0);
                grpAddresses.setVisibility(View.VISIBLE);
            }
            ibCcBcc.setVisibility(View.VISIBLE);

            bottom_navigation.getMenu().findItem(R.id.action_undo).setVisible(data.draft.revision > 1);
            bottom_navigation.getMenu().findItem(R.id.action_redo).setVisible(data.draft.revision < data.draft.revisions);

            if (args.getBoolean("incomplete")) {
                final Snackbar snackbar = Snackbar.make(
                                view, R.string.title_attachments_incomplete, Snackbar.LENGTH_INDEFINITE)
                        .setGestureInsetBottomIgnored(true);
                snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        snackbar.dismiss();
                    }
                });
                snackbar.show();
            }

            DB db = DB.getInstance(getContext());

            db.attachment().liveAttachments(data.draft.id).observe(getViewLifecycleOwner(),
                    new Observer<List<EntityAttachment>>() {
                        private Integer count = null;

                        @Override
                        public void onChanged(@Nullable List<EntityAttachment> attachments) {
                            if (attachments == null)
                                attachments = new ArrayList<>();

                            List<EntityAttachment> a = new ArrayList<>(attachments);
                            rvAttachment.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (adapter != null)
                                            adapter.set(a);
                                    } catch (Throwable ex) {
                                        Log.e(ex);
                                        /*
                                            java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling androidx.recyclerview.widget.RecyclerView{f9baa84 VFED..... ........ 0,245-720,1445 #7f0a03fd app:id/rvAttachment}, adapter:eu.faircode.email.AdapterAttachment@954026d, layout:androidx.recyclerview.widget.LinearLayoutManager@ed06ea2, context:eu.faircode.email.ActivityCompose@d14c627
                                              at androidx.recyclerview.widget.RecyclerView.assertNotInLayoutOrScroll(SourceFile:3)
                                              at androidx.recyclerview.widget.RecyclerView$RecyclerViewDataObserver.onItemRangeChanged(SourceFile:1)
                                              at androidx.recyclerview.widget.RecyclerView$AdapterDataObservable.notifyItemRangeChanged(SourceFile:2)
                                              at androidx.recyclerview.widget.RecyclerView$Adapter.notifyItemRangeChanged(SourceFile:3)
                                              at androidx.recyclerview.widget.AdapterListUpdateCallback.onChanged(SourceFile:1)
                                              at androidx.recyclerview.widget.BatchingListUpdateCallback.dispatchLastEvent(SourceFile:2)
                                              at androidx.recyclerview.widget.DiffUtil$DiffResult.dispatchUpdatesTo(SourceFile:36)
                                              at eu.faircode.email.AdapterAttachment.set(SourceFile:6)
                                              at eu.faircode.email.FragmentCompose$38$3.onChanged(SourceFile:3)
                                              at eu.faircode.email.FragmentCompose$38$3.onChanged(SourceFile:1)
                                              at androidx.lifecycle.LiveData.considerNotify(SourceFile:6)
                                              at androidx.lifecycle.LiveData.dispatchingValue(SourceFile:8)
                                              at androidx.lifecycle.LiveData.setValue(SourceFile:4)
                                              at androidx.lifecycle.LiveData$1.run(SourceFile:5)
                                              at android.os.Handler.handleCallback(Handler.java:751)
                                         */
                                    }
                                }
                            });

                            ibRemoveAttachments.setVisibility(attachments.size() > 2 ? View.VISIBLE : View.GONE);
                            grpAttachments.setVisibility(attachments.size() > 0 ? View.VISIBLE : View.GONE);

                            boolean downloading = false;
                            for (EntityAttachment attachment : attachments) {
                                if (attachment.isEncryption())
                                    continue;
                                if (attachment.progress != null)
                                    downloading = true;
                            }

                            Log.i("Attachments=" + attachments.size() + " downloading=" + downloading);

                            rvAttachment.setTag(downloading);
                            checkInternet();

                            if (count != null && count > attachments.size()) {
                                boolean updated = false;
                                Editable edit = etBody.getEditableText();

                                ImageSpan[] spans = edit.getSpans(0, edit.length(), ImageSpan.class);
                                for (int i = 0; i < spans.length && !updated; i++) {
                                    ImageSpan span = spans[i];
                                    String source = span.getSource();
                                    if (source != null && source.startsWith("cid:")) {
                                        String cid = "<" + source.substring(4) + ">";
                                        boolean found = false;
                                        for (EntityAttachment attachment : attachments)
                                            if (cid.equals(attachment.cid)) {
                                                found = true;
                                                break;
                                            }

                                        if (!found) {
                                            updated = true;
                                            int start = edit.getSpanStart(span);
                                            int end = edit.getSpanEnd(span);
                                            edit.removeSpan(span);
                                            edit.delete(start, end);
                                        }
                                    }
                                }

                                if (updated)
                                    etBody.setText(edit);
                            }

                            count = attachments.size();
                        }
                    });

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            final boolean plain_only = prefs.getBoolean("plain_only", false);

            db.message().liveMessage(data.draft.id).observe(getViewLifecycleOwner(), new Observer<EntityMessage>() {
                @Override
                public void onChanged(EntityMessage draft) {
                    // Draft was deleted
                    if (draft == null || draft.ui_hide)
                        finish();
                    else {
                        encrypt = draft.ui_encrypt;
                        invalidateOptionsMenu();

                        Log.i("Draft content=" + draft.content);
                        if (draft.content && state == State.NONE) {
                            Runnable postShow = null;
                            if (args.containsKey("images")) {
                                ArrayList<Uri> images = args.getParcelableArrayList("images");
                                args.remove("images"); // once

                                postShow = new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            boolean image_dialog = prefs.getBoolean("image_dialog", true);
                                            if (image_dialog) {
                                                Helper.hideKeyboard(view);

                                                Bundle aargs = new Bundle();
                                                aargs.putInt("title", android.R.string.ok);
                                                aargs.putParcelableArrayList("images", images);

                                                FragmentDialogAddImage fragment = new FragmentDialogAddImage();
                                                fragment.setArguments(aargs);
                                                fragment.setTargetFragment(FragmentCompose.this, REQUEST_SHARED);
                                                fragment.show(getParentFragmentManager(), "compose:shared");
                                            } else
                                                onAddImageFile(images, true);
                                        } catch (Throwable ex) {
                                            Log.e(ex);
                                        }
                                    }
                                };
                            }

                            showDraft(draft, false, postShow, args.getInt("selection"));
                        }

                        tvDsn.setVisibility(
                                draft.dsn != null && !EntityMessage.DSN_NONE.equals(draft.dsn)
                                        ? View.VISIBLE : View.GONE);

                        tvResend.setVisibility(
                                draft.headers != null && Boolean.TRUE.equals(draft.resend)
                                        ? View.VISIBLE : View.GONE);

                        tvPlainTextOnly.setVisibility(
                                draft.isPlainOnly() && !plain_only
                                        ? View.VISIBLE : View.GONE);

                        if (compose_monospaced) {
                            if (draft.isPlainOnly())
                                etBody.setTypeface(Typeface.MONOSPACE);
                            else {
                                Typeface tf = etBody.getTypeface();
                                if (tf == Typeface.MONOSPACE)
                                    etBody.setTypeface(StyleHelper.getTypeface(compose_font, etBody.getContext()));
                            }
                        }

                        tvNoInternet.setTag(draft.content);
                        checkInternet();
                    }
                }
            });

            boolean threading = prefs.getBoolean("threading", true);
            if (threading)
                db.message().liveUnreadThread(data.draft.account, data.draft.thread).observe(getViewLifecycleOwner(), new Observer<List<Long>>() {
                    private int lastDiff = 0;
                    private List<Long> base = null;

                    @Override
                    public void onChanged(List<Long> ids) {
                        if (ids == null)
                            return;

                        if (base == null) {
                            base = ids;
                            return;
                        }

                        int diff = (ids.size() - base.size());
                        if (diff > lastDiff) {
                            lastDiff = diff;
                            String msg = getResources().getQuantityString(
                                    R.plurals.title_notification_unseen, diff, diff);

                            Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_INDEFINITE)
                                    .setGestureInsetBottomIgnored(true);
                            snackbar.setAction(R.string.title_show, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Bundle args = new Bundle();
                                    args.putLong("id", ids.get(0));

                                    new SimpleTask<EntityMessage>() {
                                        @Override
                                        protected EntityMessage onExecute(Context context, Bundle args) throws Throwable {
                                            long id = args.getLong("id");

                                            DB db = DB.getInstance(context);
                                            EntityMessage message = db.message().getMessage(id);
                                            if (message != null) {
                                                EntityFolder folder = db.folder().getFolder(message.id);
                                                if (folder != null)
                                                    args.putString("type", folder.type);
                                            }

                                            return message;
                                        }

                                        @Override
                                        protected void onExecuted(Bundle args, EntityMessage message) {
                                            boolean notify_remove = prefs.getBoolean("notify_remove", true);

                                            String type = args.getString("type");

                                            Intent thread = new Intent(v.getContext(), ActivityView.class);
                                            thread.setAction("thread:" + message.id);
                                            // No group
                                            thread.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            thread.putExtra("account", message.account);
                                            thread.putExtra("folder", message.folder);
                                            thread.putExtra("type", type);
                                            thread.putExtra("thread", message.thread);
                                            thread.putExtra("filter_archive", !EntityFolder.ARCHIVE.equals(type));
                                            thread.putExtra("ignore", notify_remove);

                                            v.getContext().startActivity(thread);
                                            getActivity().finish();
                                        }

                                        @Override
                                        protected void onException(Bundle args, Throwable ex) {
                                            Log.unexpectedError(getParentFragmentManager(), ex);
                                        }
                                    }.execute(FragmentCompose.this, args, "compose:unread");
                                }
                            });
                            snackbar.show();
                        }
                    }
                });
        }

        @Override
        protected void onException(Bundle args, Throwable ex) {
            pbWait.setVisibility(View.GONE);

            if (ex instanceof MessageRemovedException)
                finish();
            else if (ex instanceof OperationCanceledException) {
                Snackbar snackbar = Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_INDEFINITE)
                        .setGestureInsetBottomIgnored(true);
                snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.getContext().startActivity(new Intent(v.getContext(), ActivitySetup.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .putExtra("manual", true));
                        getActivity().finish();
                    }
                });
                snackbar.show();
            } else
                ComposeHelper.handleException(FragmentCompose.this, view, ex);
        }

        @Override
        protected void set(Integer plain_only, List<EntityAttachment> attachments) {
            last_plain_only = plain_only;
            last_attachments = attachments;
        }
    }.serial();

    private final SimpleTask<EntityMessage> actionLoader = new LoaderComposeAction() {
        @Override
        protected void onPreExecute(Bundle args) {
            if (args.getBundle("extras").getBoolean("silent"))
                return;

            setBusy(true);
        }

        @Override
        protected void onPostExecute(Bundle args) {
            if (args.getBundle("extras").getBoolean("silent"))
                return;

            int action = args.getInt("action");
            boolean needsEncryption = args.getBoolean("needsEncryption");
            if (action != R.id.action_check || needsEncryption)
                setBusy(false);
        }

        @Override
        protected void onExecuted(Bundle args, EntityMessage draft) {
            if (draft == null)
                return;

            bottom_navigation.getMenu().findItem(R.id.action_undo).setVisible(draft.revision > 1);
            bottom_navigation.getMenu().findItem(R.id.action_redo).setVisible(draft.revision < draft.revisions);

            if (args.getBoolean("large"))
                ToastEx.makeText(getContext(), R.string.title_large_body, Toast.LENGTH_LONG).show();

            if (args.getBundle("extras").getBoolean("silent")) {
                etBody.setTag(null);
                return;
            }

            boolean needsEncryption = args.getBoolean("needsEncryption");
            int action = args.getInt("action");
            Log.i("Loaded action id=" + draft.id +
                    " action=" + LoaderComposeAction.getActionName(action) + " encryption=" + needsEncryption);

            int toPos = etTo.getSelectionStart();
            int ccPos = etCc.getSelectionStart();
            int bccPos = etBcc.getSelectionStart();

            etTo.setText(MessageHelper.formatAddressesCompose(draft.to));
            etCc.setText(MessageHelper.formatAddressesCompose(draft.cc));
            etBcc.setText(MessageHelper.formatAddressesCompose(draft.bcc));

            if (toPos >= 0 && toPos <= etTo.getText().length())
                etTo.setSelection(toPos);
            if (ccPos >= 0 && ccPos <= etCc.getText().length())
                etCc.setSelection(ccPos);
            if (bccPos >= 0 && bccPos <= etBcc.getText().length())
                etBcc.setSelection(bccPos);

            boolean dirty = args.getBoolean("dirty");
            if (dirty)
                etBody.setTag(null);

            Bundle extras = args.getBundle("extras");
            boolean show = extras.getBoolean("show");
            boolean refedit = extras.getBoolean("refedit");
            if (show)
                showDraft(draft, refedit, null, -1);

            if (needsEncryption) {
                if (ActivityBilling.isPro(getContext()) ||
                        EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt) ||
                        EntityMessage.PGP_ENCRYPTONLY.equals(draft.ui_encrypt) ||
                        EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt)) {
                    boolean interactive = args.getBoolean("interactive");
                    onEncrypt(draft, action, extras, interactive);
                } else
                    startActivity(new Intent(getContext(), ActivityBilling.class));
                return;
            }

            if (action == R.id.action_delete) {
                state = State.NONE;
                finish();

            } else if (action == R.id.action_undo || action == R.id.action_redo) {
                showDraft(draft, false, null, -1);

            } else if (action == R.id.action_save) {
                boolean autosave = extras.getBoolean("autosave");
                setFocus(
                        args.getInt("focus"),
                        args.getInt("start", -1),
                        args.getInt("end", -1),
                        args.getBoolean("ime") && !autosave);

            } else if (action == R.id.action_check) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean send_dialog = prefs.getBoolean("send_dialog", true);
                boolean send_reminders = prefs.getBoolean("send_reminders", true);

                boolean force_dialog = extras.getBoolean("force_dialog", false);
                boolean sent_missing = args.getBoolean("sent_missing", false);
                String address_error = args.getString("address_error");
                String mx_error = args.getString("mx_error");
                boolean remind_dsn = args.getBoolean("remind_dsn", false);
                boolean remind_size = args.getBoolean("remind_size", false);
                boolean remind_pgp = args.getBoolean("remind_pgp", false);
                boolean remind_smime = args.getBoolean("remind_smime", false);
                boolean remind_to = args.getBoolean("remind_to", false);
                boolean remind_extra = args.getBoolean("remind_extra", false);
                boolean remind_noreply = args.getBoolean("remind_noreply", false);
                boolean remind_external = args.getBoolean("remind_external", false);
                boolean remind_subject = args.getBoolean("remind_subject", false);
                boolean remind_text = args.getBoolean("remind_text", false);
                boolean remind_attachment = args.getBoolean("remind_attachment", false);
                String remind_extension = args.getString("remind_extension");
                boolean remind_internet = args.getBoolean("remind_internet", false);
                boolean styled = args.getBoolean("styled", false);

                int recipients = (draft.to == null ? 0 : draft.to.length) +
                        (draft.cc == null ? 0 : draft.cc.length) +
                        (draft.bcc == null ? 0 : draft.bcc.length);
                if (send_dialog || force_dialog ||
                        sent_missing || address_error != null || mx_error != null ||
                        remind_dsn || remind_size || remind_pgp || remind_smime ||
                        remind_to || remind_noreply || remind_external ||
                        recipients > FragmentDialogSend.RECIPIENTS_WARNING ||
                        (styled && draft.isPlainOnly()) ||
                        (send_reminders &&
                                (remind_extra || remind_subject || remind_text ||
                                        remind_attachment || remind_extension != null ||
                                        remind_internet))) {
                    setBusy(false);

                    Helper.hideKeyboard(view);

                    FragmentDialogSend fragment = new FragmentDialogSend();
                    fragment.setArguments(args);
                    fragment.setTargetFragment(FragmentCompose.this, REQUEST_SEND);
                    fragment.show(getParentFragmentManager(), "compose:send");
                } else
                    onAction(R.id.action_send, "dialog");

            } else if (action == R.id.action_send) {
                state = State.NONE;
                finish();
            }
        }

        @Override
        protected void onException(Bundle args, Throwable ex) {
            if (ex instanceof MessageRemovedException)
                finish();
            else {
                setBusy(false);
                if (ex instanceof IllegalArgumentException)
                    Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_LONG)
                            .setGestureInsetBottomIgnored(true).show();
                else
                    Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }

        @Override
        protected void set(Integer plain_only, List<EntityAttachment> attachments) {
            last_plain_only = plain_only;
            last_attachments = attachments;
        }

        @Override
        protected Pair<Integer, List<EntityAttachment>> get() {
            return new Pair<>(last_plain_only, last_attachments);
        }

        @Override
        protected void toast(String feedback) {
            getMainHandler().post(new RunnableEx("compose:toast") {
                public void delegate() {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        Helper.performHapticFeedback(view, HapticFeedbackConstants.CONFIRM);
                    ToastEx.makeText(getContext(), feedback, Toast.LENGTH_LONG).show();
                }
            });
        }
    }.serial();

    private void setBusy(boolean busy) {
        state = (busy ? State.LOADING : State.LOADED);
        Helper.setViewsEnabled(view, !busy);
        invalidateOptionsMenu();
    }

    private void showDraft(final EntityMessage draft, boolean refedit, Runnable postShow, int selection) {
        Bundle args = new Bundle();
        args.putLong("id", draft.id);
        args.putBoolean("show_images", show_images);

        new SimpleTask<Spanned[]>() {
            @Override
            protected void onPreExecute(Bundle args) {
                // Needed to get width for images
                grpBody.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                pbWait.setVisibility(View.GONE);
                ibLink.setVisibility(style && media ? View.GONE : View.VISIBLE);
                style_bar.setVisibility(style || etBody.hasSelection() ? View.VISIBLE : View.GONE);
                media_bar.setVisibility(media && (style || !etBody.hasSelection()) ? View.VISIBLE : View.GONE);
                bottom_navigation.getMenu().findItem(R.id.action_undo).setVisible(draft.revision > 1);
                bottom_navigation.getMenu().findItem(R.id.action_redo).setVisible(draft.revision < draft.revisions);
                bottom_navigation.setVisibility(View.VISIBLE);

                Helper.setViewsEnabled(view, true);

                invalidateOptionsMenu();
            }

            @Override
            protected Spanned[] onExecute(final Context context, Bundle args) throws Throwable {
                final long id = args.getLong("id");
                final boolean show_images = args.getBoolean("show_images", false);

                int colorPrimary = Helper.resolveColor(context, androidx.appcompat.R.attr.colorPrimary);
                final int colorBlockquote = Helper.resolveColor(context, R.attr.colorBlockquote, colorPrimary);
                int quoteGap = context.getResources().getDimensionPixelSize(R.dimen.quote_gap_size);
                int quoteStripe = context.getResources().getDimensionPixelSize(R.dimen.quote_stripe_width);

                DB db = DB.getInstance(context);
                EntityMessage draft = db.message().getMessage(id);
                if (draft == null || !draft.content)
                    throw new IllegalArgumentException(context.getString(R.string.title_no_body));

                Document doc = JsoupEx.parse(draft.getFile(context));
                doc.select("div[fairemail=signature]").remove();
                Elements ref = doc.select("div[fairemail=reference]");
                ref.remove();

                HtmlHelper.clearAnnotations(doc); // Legacy left-overs

                doc = HtmlHelper.sanitizeCompose(context, doc.html(), true);

                Spanned spannedBody = HtmlHelper.fromDocument(context, doc, new HtmlHelper.ImageGetterEx() {
                    @Override
                    public Drawable getDrawable(Element element) {
                        return ImageHelper.decodeImage(context,
                                id, element, true, zoom, 1.0f, etBody);
                    }
                }, null);

                SpannableStringBuilder bodyBuilder = new SpannableStringBuilderEx(spannedBody);
                QuoteSpan[] bodySpans = bodyBuilder.getSpans(0, bodyBuilder.length(), QuoteSpan.class);
                for (QuoteSpan quoteSpan : bodySpans) {
                    QuoteSpan q;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                        q = new QuoteSpan(colorBlockquote);
                    else
                        q = new QuoteSpan(colorBlockquote, quoteStripe, quoteGap);
                    bodyBuilder.setSpan(q,
                            bodyBuilder.getSpanStart(quoteSpan),
                            bodyBuilder.getSpanEnd(quoteSpan),
                            bodyBuilder.getSpanFlags(quoteSpan));
                    bodyBuilder.removeSpan(quoteSpan);
                }

                spannedBody = bodyBuilder;

                Spanned spannedRef = null;
                if (!ref.isEmpty()) {
                    Document dref = JsoupEx.parse(ref.outerHtml());
                    HtmlHelper.autoLink(dref);
                    Document quote = HtmlHelper.sanitizeView(context, dref, show_images);
                    spannedRef = HtmlHelper.fromDocument(context, quote,
                            new HtmlHelper.ImageGetterEx() {
                                @Override
                                public Drawable getDrawable(Element element) {
                                    return ImageHelper.decodeImage(context,
                                            id, element, show_images, zoom, 1.0f, tvReference);
                                }
                            },
                            null);

                    // Strip newline of reply header
                    if (spannedRef.length() > 0 && spannedRef.charAt(0) == '\n')
                        spannedRef = (Spanned) spannedRef.subSequence(1, spannedRef.length());

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean language_detection = prefs.getBoolean("language_detection", false);

                    Locale ref_lang = (language_detection
                            ? TextHelper.detectLanguage(context, spannedRef.toString())
                            : null);
                    args.putSerializable("ref_lang", ref_lang);
                }

                args.putBoolean("ref_has_images", spannedRef != null &&
                        spannedRef.getSpans(0, spannedRef.length(), ImageSpan.class).length > 0);

                return new Spanned[]{spannedBody, spannedRef};
            }

            @Override
            protected void onExecuted(Bundle args, Spanned[] text) {
                etBody.setText(text[0]);
                etBody.setTag(text[0]);

                SpannableStringBuilder hint = new SpannableStringBuilderEx();
                hint.append(getString(R.string.title_body_hint));
                hint.append("\n");
                int start = hint.length();
                hint.append(getString(R.string.title_body_hint_style));
                hint.setSpan(new RelativeSizeSpan(0.7f), start, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                etBody.setHint(hint);

                grpBody.setVisibility(View.VISIBLE);

                cbSignature.setChecked(draft.signature);
                tvSignature.setAlpha(draft.signature ? 1.0f : Helper.LOW_LIGHT);

                boolean ref_has_images = args.getBoolean("ref_has_images");

                Locale ref_lang = (Locale) args.getSerializable("ref_lang");
                if (ref_lang != null) {
                    String dl = ref_lang.getDisplayLanguage();
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        Locale l = Locale.getDefault();
                        if (Objects.equals(dl, l.getDisplayLanguage()))
                            ref_lang = null;
                    } else {
                        LocaleList ll = getResources().getConfiguration().getLocales();
                        for (int i = 0; i < ll.size(); i++) {
                            Locale l = ll.get(i);
                            if (Objects.equals(dl, l.getDisplayLanguage())) {
                                ref_lang = null;
                                break;
                            }
                        }
                    }
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean ref_hint = prefs.getBoolean("compose_reference", true);
                boolean write_below = prefs.getBoolean("write_below", false);

                boolean wb = (draft == null || draft.write_below == null ? write_below : draft.write_below);

                tvReference.setText(text[1]);
                tvReference.setVisibility(text[1] == null ? View.GONE : View.VISIBLE);
                grpReferenceHint.setVisibility(text[1] == null || !ref_hint ? View.GONE : View.VISIBLE);
                ibWriteAboveBelow.setImageLevel(wb ? 1 : 0);
                ibWriteAboveBelow.setVisibility(text[1] == null ||
                        draft.wasforwardedfrom != null || BuildConfig.PLAY_STORE_RELEASE
                        ? View.GONE : View.VISIBLE);
                tvLanguage.setText(ref_lang == null ? null : ref_lang.getDisplayLanguage());
                tvLanguage.setVisibility(ref_lang == null ? View.GONE : View.VISIBLE);
                ibReferenceEdit.setVisibility(text[1] == null ? View.GONE : View.VISIBLE);
                ibReferenceImages.setVisibility(ref_has_images && !show_images ? View.VISIBLE : View.GONE);

                setBodyPadding();

                if (refedit && wb)
                    etBody.setSelection(etBody.length());

                if (state == State.LOADED)
                    return;
                state = State.LOADED;

                int selStart = (selection == 0 ? -1 : selection);

                if (selStart < 0) {
                    int pos = getAutoPos(0, etBody.length());
                    if (pos < 0)
                        pos = 0;
                    etBody.setSelection(pos);
                }

                setFocus(selStart < 0 ? null : R.id.etBody, selStart, selStart, postShow == null);
                if (postShow != null)
                    getMainHandler().post(postShow);

                if (lt_sentence || lt_auto)
                    onLanguageTool(0, etBody.length(), true);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.serial().execute(this, args, "compose:show");
    }

    private void setFocus(Integer v, int start, int end, boolean restore) {
        final View target;
        if (v != null)
            target = view.findViewById(v);
        else if (TextUtils.isEmpty(etTo.getText().toString().trim()))
            target = etTo;
        else if (TextUtils.isEmpty(etSubject.getText().toString()))
            target = etSubject;
        else
            target = etBody;

        if (target == null)
            return;

        int s = (start < end ? start : end);
        int e = (start < end ? end : start);

        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        return;

                    if (target instanceof EditText) {
                        EditText et = (EditText) target;
                        int len = et.length();
                        if (s >= 0 && s <= len && e <= len)
                            if (e < 0)
                                et.setSelection(s);
                            else
                                et.setSelection(s, e);
                    }

                    target.requestFocus();

                    Context context = target.getContext();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean keyboard = prefs.getBoolean("keyboard", true);
                    if (keyboard && restore)
                        Helper.showKeyboard(target);

                } catch (Throwable ex) {
                    Log.e(ex);
                }
            }
        });
    }

    private void setBodyPadding() {
        // Keep room for the style toolbar
        boolean pad =
                (grpSignature.getVisibility() == View.GONE &&
                        tvReference.getVisibility() == View.GONE);
        etBody.setPadding(0, 0, 0, pad ? Helper.dp2pixels(getContext(), 36) : 0);
    }

    private int getAutoPos(int start, int end) {
        if (start > end || end == 0)
            return -1;

        CharSequence text = etBody.getText();
        if (text == null)
            return -1;

        int lc = 0;
        int nl = 0;
        int pos = 0;
        String[] lines = text.subSequence(start, end).toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (TextUtils.isEmpty(lines[i]))
                nl++;
            else {
                lc++;
                nl = 0;
            }
            if (lc > 1)
                return -1;
            if (nl > 2)
                return start + pos - 1;
            pos += lines[i].length() + 1;
        }
        return -1;
    }

    private void startSearch() {
        etSearch.setText(null);
        etSearch.setVisibility(View.VISIBLE);
        etSearch.requestFocus();
        Helper.showKeyboard(etSearch);
    }

    private void endSearch() {
        if (etSearch == null)
            return;

        Helper.hideKeyboard(etSearch);
        etSearch.setVisibility(View.GONE);
        clearSearch();
    }

    private void performSearch(boolean next) {
        clearSearch();

        searchIndex = (next ? searchIndex + 1 : 1);
        String query = etSearch.getText().toString().toLowerCase();
        String text = etBody.getText().toString().toLowerCase();

        int pos = -1;
        for (int i = 0; i < searchIndex; i++)
            pos = (pos < 0 ? text.indexOf(query) : text.indexOf(query, pos + 1));

        // Wrap around
        if (pos < 0 && searchIndex > 1) {
            searchIndex = 1;
            pos = text.indexOf(query);
        }

        // Scroll to found text
        if (pos >= 0) {
            Context context = etBody.getContext();
            int color = Helper.resolveColor(context, R.attr.colorHighlight);
            SpannableString ss = new SpannableString(etBody.getText());
            ss.setSpan(new HighlightSpan(color),
                    pos, pos + query.length(), Spannable.SPAN_COMPOSING);
            ss.setSpan(new RelativeSizeSpan(HtmlHelper.FONT_LARGE),
                    pos, pos + query.length(), Spannable.SPAN_COMPOSING);
            etBody.setText(ss);

            Layout layout = etBody.getLayout();
            if (layout != null) {
                int line = layout.getLineForOffset(pos);
                int y = layout.getLineTop(line);
                int dy = context.getResources().getDimensionPixelSize(R.dimen.search_in_text_margin);

                Rect rect = new Rect();
                etBody.getDrawingRect(rect);
                ScrollView scroll = view.findViewById(R.id.scroll);
                scroll.offsetDescendantRectToMyCoords(etBody, rect);
                scroll.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            scroll.scrollTo(0, rect.top + y - dy);
                        } catch (Throwable ex) {
                            Log.e(ex);
                        }
                    }
                });
            }
        }

        boolean hasNext = (pos >= 0 &&
                (text.indexOf(query) != pos ||
                        text.indexOf(query, pos + 1) >= 0));
        etSearch.setActionEnabled(hasNext);
    }

    private void clearSearch() {
        HtmlHelper.clearComposingText(etBody);
    }

    private AdapterView.OnItemSelectedListener identitySelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final Context context = parent.getContext();
            TupleIdentityEx identity = (TupleIdentityEx) parent.getAdapter().getItem(position);
            FragmentCompose.this.account = (identity == null ? null : identity.account);

            int at = (identity == null ? -1 : identity.email.indexOf('@'));
            etExtra.setHint(at < 0 ? null : identity.email.substring(0, at));
            tvDomain.setText(at < 0 ? null : identity.email.substring(at));
            grpExtra.setVisibility(identity != null && identity.sender_extra ? View.VISIBLE : View.GONE);

            if (identity != null && nav_color) {
                Integer color = (identity.color == null ? identity.accountColor : identity.color);
                bottom_navigation.setBackgroundColor(color == null
                        ? Helper.resolveColor(context, androidx.appcompat.R.attr.colorPrimary) : color);

                ColorStateList itemColor;
                if (color == null)
                    itemColor = ContextCompat.getColorStateList(context, R.color.action_foreground);
                else {
                    Integer icolor = null;
                    float lum = (float) ColorUtils.calculateLuminance(color);
                    if (lum > Helper.BNV_LUMINANCE_THRESHOLD)
                        icolor = Color.BLACK;
                    else if ((1.0f - lum) > Helper.BNV_LUMINANCE_THRESHOLD)
                        icolor = Color.WHITE;
                    if (icolor == null)
                        itemColor = ContextCompat.getColorStateList(context, R.color.action_foreground);
                    else
                        itemColor = new ColorStateList(
                                new int[][]{
                                        new int[]{android.R.attr.state_enabled},
                                        new int[]{}
                                },
                                new int[]{
                                        icolor,
                                        Color.GRAY
                                }
                        );
                }
                bottom_navigation.setItemIconTintList(itemColor);
                bottom_navigation.setItemTextColor(itemColor);
            }

            Spanned signature = null;
            if (identity != null && !TextUtils.isEmpty(identity.signature)) {
                Document d = HtmlHelper.sanitizeCompose(context, identity.signature, true);
                signature = HtmlHelper.fromDocument(context, d, new HtmlHelper.ImageGetterEx() {
                    @Override
                    public Drawable getDrawable(Element element) {
                        String source = element.attr("src");
                        if (source.startsWith("cid:"))
                            element.attr("src", "cid:");
                        return ImageHelper.decodeImage(context,
                                working, element, true, 0, 1.0f, tvSignature);
                    }
                }, null);
            }
            tvSignature.setText(signature);
            grpSignature.setVisibility(signature == null ? View.GONE : View.VISIBLE);

            setBodyPadding();

            if (!Objects.equals(spIdentity.getTag(), position)) {
                spIdentity.setTag(position);
                updateEncryption(identity, true);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            etExtra.setHint("");
            tvDomain.setText(null);

            tvSignature.setText(null);
            grpSignature.setVisibility(View.GONE);

            setBodyPadding();

            updateEncryption(null, true);
        }
    };

    private ActivityBase.IKeyPressedListener onKeyPressedListener = new ActivityBase.IKeyPressedListener() {
        @Override
        public boolean onKeyPressed(KeyEvent event) {
            if (event.isCtrlPressed() && event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_S:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER:
                        onAction(R.id.action_check, "key");
                        return true;
                    case KeyEvent.KEYCODE_B:
                        if (etBody.hasSelection())
                            return StyleHelper.apply(R.id.menu_bold, getViewLifecycleOwner(), null, etBody);
                        else
                            return false;
                    case KeyEvent.KEYCODE_I:
                        if (etBody.hasSelection())
                            return StyleHelper.apply(R.id.menu_italic, getViewLifecycleOwner(), null, etBody);
                        else
                            return false;
                    case KeyEvent.KEYCODE_U:
                        if (etBody.hasSelection())
                            return StyleHelper.apply(R.id.menu_underline, getViewLifecycleOwner(), null, etBody);
                        else
                            return false;
                }
            }

            return false;
        }
    };

    private OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (Helper.isKeyboardVisible(view))
                Helper.hideKeyboard(view);
            else
                onExit();
        }
    };
}
