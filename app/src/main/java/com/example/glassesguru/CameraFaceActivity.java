package com.example.glassesguru;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Html;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.glassesguru.common.helpers.CameraPermissionHelper;
import com.example.glassesguru.common.helpers.DisplayRotationHelper;
import com.example.glassesguru.common.helpers.FullScreenHelper;
import com.example.glassesguru.common.helpers.SnackbarHelper;
import com.example.glassesguru.common.helpers.TrackingStateHelper;
import com.example.glassesguru.common.rendering.BackgroundRenderer;
import com.example.glassesguru.common.rendering.ObjectRenderer;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.AugmentedFace.RegionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import yuku.ambilwarna.AmbilWarnaDialog;

// Ml kit for face shape detection
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CameraFaceActivity extends AppCompatActivity implements GLSurfaceView.Renderer, GlassesItemCustomAdapter.OnItemClickListener {

    private static final String TAG = AugmentedFaceRenderer.class.getSimpleName();
    private int primaryColorBackgroundTransparent = R.color.gray_blue_semi_transparent;
    private int primaryOnColor = R.color.white;
    public static final int REFRESH_ITEMS = 4;
    private boolean capture_image = false;
    private boolean capture_image_ai = false;
    private FaceShapeClassifier faceShapeClassifier;
    ProgressDialog progressDialog;
    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private boolean installRequested;
    PrefManager prefManager;
    private Session session;
    private static final int REQUEST_INSTALL_UNKNOWN_APK = 2;
    private static final int REQUEST_READ_STORAGE_PERMISSION = 3;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final AugmentedFaceRenderer augmentedFaceRenderer = new AugmentedFaceRenderer();
    private final ObjectRenderer noseObject = new ObjectRenderer();
    private final ObjectRenderer rightEarObject = new ObjectRenderer();
    private final ObjectRenderer leftEarObject = new ObjectRenderer();
    private final ObjectRenderer rightTempleObject = new ObjectRenderer();
    private final ObjectRenderer leftTempleObject = new ObjectRenderer();
    private final ObjectRenderer rightTempleTipObject = new ObjectRenderer();
    private final ObjectRenderer leftTempleTipObject = new ObjectRenderer();
    private final ObjectRenderer rightHingeObject = new ObjectRenderer();
    private final ObjectRenderer leftHingeObject = new ObjectRenderer();
    private final ObjectRenderer lensesObject = new ObjectRenderer();
    private final ObjectRenderer padsObject = new ObjectRenderer();
    private final ObjectRenderer padsArmsObject = new ObjectRenderer();
    private final ObjectRenderer eyesObject = new ObjectRenderer();
    // Temporary matrix allocated here to reduce number of allocations for each frame.
    float[] offsetMatrix = new float[16];
    float[] rotatedMatrix = new float[16];
    private final float[] noseMatrix = new float[16];
    private final float[] rightEarMatrix = new float[16];
    private final float[] leftEarMatrix = new float[16];
    private final float[] eyesMatrix = new float[16];
    private final float[] leftHingeMatrix = new float[16];
    private final float[] rightHingeMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};
    private static final int DEFAULT_COLOR_INT = 0x00000000;
    public float scaleFactor = 1.0f;
    private boolean shownGlassesOptions = false;
    private boolean shownMoreSettings = true;
    private boolean glassesVisible = true;
    private boolean lensesVisible = true;
    private boolean lensesFlareVisible = true;
    private boolean templeVisible = true;
    private boolean rightTempleVisible = true;
    private boolean leftTempleVisible = true;
    private boolean rightTempleTipVisible = true;
    private boolean leftTempleTipVisible = true;
    private boolean autoHideTemple = true;
    TextView debug_x, debug_y, debug_z;
    TextView dashedFaceText;
    private float temple_right_rotate_offset_x = 0.0f;
    private float temple_right_rotate_offset_y = 0.0f;
    private float temple_right_rotate_offset_z = 0.0f;
    private String glassesModel = "glasses_1_frame.obj";
    // Original lists of glasses data
    private ArrayList<String> originalGlassesId = new ArrayList<>();
    private ArrayList<Integer> originalGlassesImage = new ArrayList<>();
    private ArrayList<String> originalGlassesTitle = new ArrayList<>();
    private ArrayList<String> originalGlassesObjName = new ArrayList<>();
    private ArrayList<String> originalTempleObjName = new ArrayList<>();
    private ArrayList<String> originalLensesObjName = new ArrayList<>();
    private ArrayList<String> originalGlassesFrameType = new ArrayList<>();
    private ArrayList<String> originalGlassesType = new ArrayList<>();
    private ArrayList<String> originalPadsObjName = new ArrayList<>();
    private ArrayList<String> originalDescription = new ArrayList<>();
    private ArrayList<String> originalStacks = new ArrayList<>();
    private ArrayList<String> originalGlassesPrice = new ArrayList<>();
    private ArrayList<String> originalTransparency = new ArrayList<>();
    private ArrayList<Boolean> originalIsDownloaded = new ArrayList<>();

    // Filtered lists of glasses data
    private ArrayList<String> filteredGlassesId = new ArrayList<>();
    private ArrayList<Integer> filteredGlassesImage = new ArrayList<>();
    private ArrayList<String> filteredGlassesTitle = new ArrayList<>();
    private ArrayList<String> filteredGlassesObjName = new ArrayList<>();
    private ArrayList<String> filteredTempleObjName = new ArrayList<>();
    private ArrayList<String> filteredLensesObjName = new ArrayList<>();
    private ArrayList<String> filteredGlassesFrameType = new ArrayList<>();
    private ArrayList<String> filteredGlassesType = new ArrayList<>();
    private ArrayList<String> filteredPadsObjName = new ArrayList<>();
    private ArrayList<String> filteredDescription = new ArrayList<>();
    private ArrayList<String> filteredStacks = new ArrayList<>();
    private ArrayList<String> filteredGlassesPrice = new ArrayList<>();
    private ArrayList<String> filteredTransparency = new ArrayList<>();
    private ArrayList<Boolean> filteredIsDownloaded = new ArrayList<>();
    GlassesItemCustomAdapter glassesItemCustomAdapter;
    RecyclerView glassesRecyclerView;
    private float glasses_offset_y = 0.0299f;
    private boolean eyesObjectNeedsCreation;
    public int selectedColor = DEFAULT_COLOR_INT;
    private int previousColor = DEFAULT_COLOR_INT;
    private ImageButton colorPickerButton;
    RotateAnimation rotateAnimation;

    ImageView loading_icon_ImageView;
    LinearLayout loading_screen, dashedFaceAreaLayout;
    boolean showLoadingScreen = false;
    ConstraintLayout main, slider_layout;
    LinearLayout more_settings_layout;
    public static CaptureButton capture_button;
    LinearLayout capture_LinearLayout;
    ImageView ai_recommendation_Button;
    private boolean capture_image_recommendation = false;
    FaceDetectorOptions face_detector_options;
    private FaceMaskView faceMaskView;
    private Spinner frameTypeSpinner;
    private MultiSelectSpinnerAdapter frameTypeAdapter;
    private List<String> frameTypes = Arrays.asList("All", "Rectangular frame", "Square frame", "Angular frame", "Wayfarer frame", "Round frame", "Oval frame", "Oversize frame", "Aviator frame", "Browline frame", "Sunglasses", "Favorites");

    private Spinner face_type_Spinner;
    private ArrayAdapter<CharSequence> face_type_adapter;
    private ImageButton lastPhotoImageView;
    ImageView capture_image_ImageView;
    private ImageButton replay_tutorial_Button;
    private ImageButton more_settings_button;
    private float rotationAngle = 0.0f;
    private String templeModel = "glasses_1_temple, glasses_1_temple_tip";
    private String lensesModel = "glasses_1_lenses.obj:albedo_5";
    private String glassesType = "";
    private String padsModel = "";
    private String defaultLensesTransparency = "lensesObject=0.1f";
    private String defaultSunglassesLensesTransparency = "lensesObject=0.6f";
    private String transparencyObj = defaultLensesTransparency;
    private float maxScaleFactor = 0.5f;
    private float maxTranslation = 0.05f;
    private ConstraintLayout recommendation_pop_up;
    private ImageView face_shape_ImageView;
    private TextView face_shape_TextView;
    private TextView face_shape_description_TextView;
    private ImageButton close_recommendation_Button;
    private boolean isRecommendationPopupVisible = false;
    private boolean isFrameColorSelected = true;
    private boolean isLensesColorSelected = false;
    private boolean isTempleColorSelected = true;
    private boolean isTempleTipColorSelected = false;
    private boolean isTempleHingeColorSelected = false;
    private boolean isPadArmsColorSelected = false;
    String[] templeParts = new String[]{""};
    String[] padParts = new String[]{""};
    private boolean isPadArmsExists = false;
    public int lensesObjectCustomColor = DEFAULT_COLOR_INT;
    public int templeObjectCustomColor = DEFAULT_COLOR_INT;
    public int templeTipObjectCustomColor = DEFAULT_COLOR_INT;
    public int eyesObjectCustomColor;
    private float lensesVisibilitySliderValue = 0f;
    private boolean adjustLensesTransparency = false;
    private ImageButton chatSupportButton;
    private int currentGlassesPosition = 0;
    private TextView notification_count_TextView;
    private int unseenCount = 0;
    private ImageButton settingsAppButton;
    private LinearLayout loadingIndicator;
    private ImageView fullscreen_Button;
    private RelativeLayout linearLayout;
    private boolean fullscreenRecyclerView = false;
    private RelativeLayout bottom_buttons_Layout;
    private boolean shownBottomButtons = true;
    private boolean shownColorPaletteSettings = false;
    private boolean shownGlassesVisibilitySettings = false;
    private ConstraintLayout color_picker_settings_layout, showGlasses_settings_layout;
    private ImageButton showMoreGlassesDescriptionButton;
    private boolean isDownloaded = false;
    public String modelBasePath = "models/glasses/";
    private ImageButton color_picker_settings_button, color_picker_reset_button, showGlassesButton, showGlasses_settings_Button;
    private Bitmap croppedFaceBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefManager = new PrefManager(this);

        String theme = prefManager.getThemeColor();

        switch (theme) {
            case "slate":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                primaryColorBackgroundTransparent = R.color.pale_black_semi_transparent;
                break;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_face);

        main = findViewById(R.id.main);

        loadingIndicator = findViewById(R.id.loadingIndicator);

        loadingIndicator.setVisibility(View.VISIBLE);

        faceShapeClassifier = new FaceShapeClassifier(this);

        replay_tutorial_Button = findViewById(R.id.replay_tutorial_Button);
        more_settings_button = findViewById(R.id.more_settings_button);
        recommendation_pop_up = findViewById(R.id.recommendation_pop_up);
        close_recommendation_Button = findViewById(R.id.close_recommendation_Button);
        close_recommendation_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeRecommendationPopup();
            }
        });
        face_shape_ImageView = findViewById(R.id.face_shape_ImageView);
        face_shape_TextView = findViewById(R.id.face_shape_TextView);
        face_shape_description_TextView = findViewById(R.id.face_shape_description_TextView);

        replay_tutorial_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTutorial();
                prefManager.setFirstTimeLaunchMoreOptions(true);

                shownGlassesOptions = false;
                slider_layout.setVisibility(View.INVISIBLE);
            }
        });

        lastPhotoImageView = findViewById(R.id.last_photo_image_view);
        lastPhotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CameraFaceActivity.this, PhotoManagerActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        loadLastPhoto();

        loading_screen = findViewById(R.id.loading_screen);
        loading_icon_ImageView = findViewById(R.id.loading_icon_ImageView);
        rotateAnimation = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );

        dashedFaceAreaLayout = findViewById(R.id.dashedFaceAreaLayout);

        capture_image_ImageView = findViewById(R.id.captured_image);

        faceMaskView = findViewById(R.id.face_mask_view);

        ai_recommendation_Button = findViewById(R.id.ai_recommendation_Button);

        // [START set_detector_options]
        face_detector_options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();
        // [END set_detector_options]

        bottom_buttons_Layout = findViewById(R.id.bottom_buttons_Layout);

        capture_LinearLayout = findViewById(R.id.capture_LinearLayout);

        slider_layout = (ConstraintLayout) findViewById(R.id.sliderLayout);
        more_settings_layout = (LinearLayout) findViewById(R.id.more_settings_layout);
        ImageButton showMoreGlassesButton = (ImageButton) findViewById(R.id.showMoreGlassesButton);
        showMoreGlassesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(prefManager.isFirstTimeLaunchMoreOptions()) {
                    showMoreOptionsTutorial();
                }
                toggleGlassesOptions();
            }
        });

        more_settings_button.setOnClickListener(v -> {
            toggleMoreSettings();
        });

        String senderRoom = "1" + prefManager.getUserUID();

        notification_count_TextView = findViewById(R.id.notification_count_TextView);

        DatabaseReference dbReferenceSender = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(senderRoom);

        dbReferenceSender.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MessageModel> messages = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    messages.add(message);
                }

                // Sort messages by timestamp
                Collections.sort(messages, new Comparator<MessageModel>() {
                    @Override
                    public int compare(MessageModel m1, MessageModel m2) {
                        return Long.compare(m1.getTimestamp(), m2.getTimestamp());
                    }
                });

                // Count unseen messages that are not from senderId
                unseenCount = countUnseenMessages(messages, prefManager.getUserUID());
                notification_count_TextView.setText(String.valueOf(unseenCount));
                if(unseenCount != 0) {
                    notification_count_TextView.setVisibility(View.VISIBLE);
                } else {
                    notification_count_TextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });

        chatSupportButton = findViewById(R.id.chatSupportButton);
        chatSupportButton.setOnClickListener(v -> {
            Intent intentLenses = new Intent(CameraFaceActivity.this, ChatActivity.class);
            intentLenses.putExtra("Price", filteredGlassesPrice.get(currentGlassesPosition));
            intentLenses.putExtra("Title", filteredGlassesTitle.get(currentGlassesPosition));
            intentLenses.putExtra("Image", filteredGlassesImage.get(currentGlassesPosition));
            intentLenses.putExtra("FrameType", filteredGlassesFrameType.get(currentGlassesPosition));
            intentLenses.putExtra("Type", filteredGlassesType.get(currentGlassesPosition));
            intentLenses.putExtra("Price", filteredGlassesPrice.get(currentGlassesPosition));
            intentLenses.putExtra("Function", filteredGlassesType.get(currentGlassesPosition));
            intentLenses.putExtra("Size", filteredStacks.get(currentGlassesPosition));
            intentLenses.putExtra("Description", filteredDescription.get(currentGlassesPosition));
            intentLenses.putExtra("Color", eyesObjectCustomColor);
            intentLenses.putExtra("LensesColor", lensesObjectCustomColor);
            intentLenses.putExtra("TempleColor", templeObjectCustomColor);
            intentLenses.putExtra("TempleTipColor", templeTipObjectCustomColor);
            intentLenses.putExtra("ID", filteredGlassesId.get(currentGlassesPosition));
            intentLenses.putExtra("IsDownloaded", filteredIsDownloaded.get(currentGlassesPosition));
            updateAllMessagesAsSeen(senderRoom, prefManager.getUserUID());

            startActivity(intentLenses);
        });

        showMoreGlassesDescriptionButton = findViewById(R.id.showMoreGlassesDescriptionButton);
        showMoreGlassesDescriptionButton.setOnClickListener(v -> {
            Intent intent = new Intent(CameraFaceActivity.this, GlassesActivity.class);
            intent.putExtra("ID", filteredGlassesId.get(currentGlassesPosition));
            intent.putExtra("Image", filteredGlassesImage.get(currentGlassesPosition));
            intent.putExtra("Title", filteredGlassesTitle.get(currentGlassesPosition));
            intent.putExtra("FrameType", filteredGlassesFrameType.get(currentGlassesPosition));
            intent.putExtra("Type", filteredGlassesType.get(currentGlassesPosition));
            intent.putExtra("Price", filteredGlassesPrice.get(currentGlassesPosition));
            intent.putExtra("Size", filteredStacks.get(currentGlassesPosition));
            intent.putExtra("Description", filteredDescription.get(currentGlassesPosition));
            intent.putExtra("Color", eyesObjectCustomColor);
            intent.putExtra("LensesColor", lensesObjectCustomColor);
            intent.putExtra("TempleColor", templeObjectCustomColor);
            intent.putExtra("TempleTipColor", templeTipObjectCustomColor);
            intent.putExtra("IsDownloaded", filteredIsDownloaded.get(currentGlassesPosition));
            startActivityForResult(intent, CameraFaceActivity.REFRESH_ITEMS);
        });

        settingsAppButton = findViewById(R.id.settingsAppButton);
        settingsAppButton.setOnClickListener(v -> {
            Intent intent = new Intent(CameraFaceActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        rotateAnimation.setDuration(1000); // Duration of the animation in milliseconds
        rotateAnimation.setRepeatCount(Animation.INFINITE); // Repeat the animation infinitely
        rotateAnimation.setInterpolator(new LinearInterpolator());

        initializeGlassesData();

        colorPickerButton = (ImageButton) findViewById(R.id.color_picker_button);

        /*colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousColor = selectedColor;
                selectedColor = -1;
                eyesObject.clearCustomColor();
                rightTempleObject.clearCustomColor();
                leftTempleObject.clearCustomColor();
                openColorPickerDialog();
            }
        });*/

        colorPickerButton.setOnLongClickListener(v -> {
            showCheckboxColorPickerDialog();
            return true; // Consume the long click event
        });

        colorPickerButton.setOnClickListener(v -> {
            showColorPickerDialog();
            showColorPickerSettings();
        });

        color_picker_settings_layout = findViewById(R.id.color_picker_settings_layout);

        color_picker_settings_button = findViewById(R.id.color_picker_settings_button);
        color_picker_settings_button.setOnClickListener(v -> {
            showCheckboxColorPickerDialog();
        });

        color_picker_reset_button = findViewById(R.id.color_picker_reset_button);
        color_picker_reset_button.setOnClickListener(v -> {
            resetColor();
        });


        glassesItemCustomAdapter = new GlassesItemCustomAdapter(CameraFaceActivity.this, this, filteredGlassesId, filteredGlassesImage, filteredGlassesTitle, filteredGlassesObjName, filteredTempleObjName, filteredLensesObjName, filteredGlassesFrameType, filteredGlassesType, filteredPadsObjName, filteredDescription, filteredStacks, filteredGlassesPrice, filteredTransparency, filteredIsDownloaded);

        glassesRecyclerView = (RecyclerView) findViewById(R.id.glassesRecyclerVIew);
        glassesItemCustomAdapter.setOnItemClickListener(this);
        glassesRecyclerView.setAdapter(glassesItemCustomAdapter);
        glassesRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        surfaceView = findViewById(R.id.surface_view_GLSurfaceView);
        displayRotationHelper = new DisplayRotationHelper(this);

        debug_x = findViewById(R.id.debug_x);
        debug_y = findViewById(R.id.debug_y);
        debug_z = findViewById(R.id.debug_z);
        dashedFaceText = findViewById(R.id.dashedFaceText);

        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        installRequested = false;

        capture_button = findViewById(R.id.capture_button);

        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capture_image = true;
                capture_LinearLayout.setVisibility(View.VISIBLE);
            }
        });

        fullscreen_Button = findViewById(R.id.fullscreen_Button);
        linearLayout = findViewById(R.id.linearLayout);

        fullscreen_Button.setOnClickListener(view -> {
            toggleRecyclerViewFullscreen();
        });

        ai_recommendation_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ai_recommendation_Button.setClickable(false);
                dashedFaceAreaLayout.setVisibility(View.VISIBLE);
                dashedFaceText.setText("CHIN UP");
                capture_button.startLoadingAnimation();
                capture_image_recommendation = true;
                //capture_LinearLayout.setVisibility(View.VISIBLE);
            }
        });

        frameTypeSpinner = findViewById(R.id.frameTypeSpinner);

        frameTypeAdapter = new MultiSelectSpinnerAdapter(this, android.R.layout.simple_spinner_item, frameTypes, "Frame Types");
        frameTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        frameTypeSpinner.setAdapter(frameTypeAdapter);

        frameTypeAdapter.setOnItemSelectedListener(new MultiSelectSpinnerAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(List<String> selectedItemsList) {
                filterGlasses();
            }
        });

        frameTypeSpinner.setSelection(0);

        filterGlasses();

        face_type_Spinner = findViewById(R.id.face_type_Spinner);
        face_type_adapter = ArrayAdapter.createFromResource(this,
                R.array.face_types_array, R.layout.spinner_item);
        face_type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        face_type_Spinner.setAdapter(face_type_adapter);
        face_type_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0) {
                    frameTypeAdapter.clearSelectedItems();
                    filterGlasses();
                } else {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    recommendGlassesType(selectedItem);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        SeekBar slider_scale_bar = (SeekBar) findViewById(R.id.slider_scale_bar);
        slider_scale_bar.setOnSeekBarChangeListener(sliderScaleChangeListener);

        SeekBar slider_y_bar = (SeekBar) findViewById(R.id.slider_y_bar);
        slider_y_bar.setOnSeekBarChangeListener(sliderYChangeListener);

        SeekBar slider_rotation_bar = (SeekBar) findViewById(R.id.slider_rotation_bar);
        slider_rotation_bar.setOnSeekBarChangeListener(sliderRotationChangeListener);

        slider_scale_bar.setProgress((int) ((scaleFactor - 0.5f) * 100));
        slider_y_bar.setProgress((int) ((glasses_offset_y + 1) * 100));

        // Set the maximum value of the SeekBar to 2000
        slider_rotation_bar.setMax(2000);

        // Set the progress of the SeekBar to its middle value
        slider_rotation_bar.setProgress(1000);

        // Set the initial rotation angle to 0
        rotationAngle = 0.0f;

        ImageView scaleResetIcon = (ImageView) findViewById(R.id.scaleResetIcon);
        scaleResetIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scaleFactor = 1.0f;
                slider_scale_bar.setProgress((int) ((scaleFactor - 0.5f) * 100));
            }
        });

        ImageView yResetIcon = (ImageView) findViewById(R.id.yResetIcon);
        yResetIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                glasses_offset_y = 0.0200f;
                slider_y_bar.setProgress((int) ((glasses_offset_y + 1) * 100));
            }
        });

        ImageView rotationResetIcon = (ImageView) findViewById(R.id.rotationResetIcon);
        rotationResetIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*rotationOnProgress = false;
                rotationAngle = 0.0; // Reset rotation angle*/
                rotationAngle = 360;
                slider_rotation_bar.setProgress(1000);
            }
        });

        showGlassesButton = (ImageButton) findViewById(R.id.showGlassesButton);
        showGlassesButton.setOnClickListener(v -> {
                toggleGlassesVisibility();
                showGlassesVisibilitySettings();
        });

        showGlassesButton.setOnLongClickListener(v -> {
            showCheckboxShowGlassesDialog();
            return true;
        });

        showGlasses_settings_layout = findViewById(R.id.showGlasses_settings_layout);

        showGlasses_settings_Button = findViewById(R.id.showGlasses_settings_Button);
        showGlasses_settings_Button.setOnClickListener(v -> {
            showCheckboxShowGlassesDialog();
        });

        main.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                boolean isColorPickerVisible = color_picker_settings_layout.getVisibility() == View.VISIBLE;
                boolean isGlassesSettingsVisible = showGlasses_settings_layout.getVisibility() == View.VISIBLE;

                // Cache positions only if either settings layout is visible
                int[] colorPickerLocation = new int[2];
                int[] settingsLocation = new int[2];
                int[] glassesSettingsLocation = new int[2];

                if (isColorPickerVisible) {
                    colorPickerButton.getLocationOnScreen(colorPickerLocation);
                    color_picker_settings_layout.getLocationOnScreen(settingsLocation);
                }

                if (isGlassesSettingsVisible) {
                    showGlasses_settings_Button.getLocationOnScreen(glassesSettingsLocation);
                }

                // Calculate bounds for the color picker button
                int colorPickerLeft = colorPickerLocation[0];
                int colorPickerTop = colorPickerLocation[1];
                int colorPickerRight = colorPickerLeft + colorPickerButton.getWidth();
                int colorPickerBottom = colorPickerTop + colorPickerButton.getHeight();

                // Calculate bounds for the settings layout
                int settingsLeft = settingsLocation[0];
                int settingsTop = settingsLocation[1];
                int settingsRight = settingsLeft + color_picker_settings_layout.getWidth();
                int settingsBottom = settingsTop + color_picker_settings_layout.getHeight();

                // Calculate bounds for the glasses settings layout
                int glassesSettingsLeft = glassesSettingsLocation[0];
                int glassesSettingsTop = glassesSettingsLocation[1];
                int glassesSettingsRight = glassesSettingsLeft + showGlasses_settings_layout.getWidth();
                int glassesSettingsBottom = glassesSettingsTop + showGlasses_settings_layout.getHeight();

                // Check if touch is outside all views
                float touchX = event.getRawX();
                float touchY = event.getRawY();
                boolean isOutsideButton = (touchX < colorPickerLeft || touchX > colorPickerRight || touchY < colorPickerTop || touchY > colorPickerBottom);
                boolean isOutsideSettings = (touchX < settingsLeft || touchX > settingsRight || touchY < settingsTop || touchY > settingsBottom);
                boolean isOutsideGlassesSettings = (touchX < glassesSettingsLeft || touchX > glassesSettingsRight || touchY < glassesSettingsTop || touchY > glassesSettingsBottom);

                // Hide settings if touch is outside all
                if (isOutsideButton && isOutsideSettings && isOutsideGlassesSettings) {
                    hideColorPickerSettings();
                    // Optionally, hide the show glasses settings if needed
                    hideGlassesVisibilitySettings(); // Implement this method as needed
                }
            }
            return false; // Return false to allow other views to process the touch event
        });
    }

    private void showGlassesVisibilitySettings() {
        showGlasses_settings_layout.setVisibility(View.VISIBLE);
        showGlasses_settings_layout.setAlpha(0f);
        showGlasses_settings_layout.animate()
                .alpha(1f)
                .withEndAction(() -> shownGlassesVisibilitySettings = true);
    }

    private void hideGlassesVisibilitySettings() {
        showGlasses_settings_layout.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                   showGlasses_settings_layout.setVisibility(View.GONE);
                   shownGlassesVisibilitySettings = false;
                });
    }
    private void toggleColorPickerSettings() {
        shownColorPaletteSettings = !shownColorPaletteSettings;
        if(shownColorPaletteSettings) {
            color_picker_settings_layout.setVisibility(View.VISIBLE);
            color_picker_settings_layout.setAlpha(0f);
            color_picker_settings_layout.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction(() -> shownColorPaletteSettings = true);
        } else {
            color_picker_settings_layout.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        color_picker_settings_layout.setVisibility(View.GONE);
                        shownColorPaletteSettings = false;
                    });
        }
    }

    public void showColorPickerSettings() {
        color_picker_settings_layout.setVisibility(View.VISIBLE);
        color_picker_settings_layout.setAlpha(0f);
        color_picker_settings_layout.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> shownColorPaletteSettings = true);
    }

    public void hideColorPickerSettings() {
        color_picker_settings_layout.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    color_picker_settings_layout.setVisibility(View.GONE);
                    shownColorPaletteSettings = false;
                });
    }

    private void toggleGlassesOptions() {
        shownGlassesOptions = !shownGlassesOptions;
        if(shownGlassesOptions) {
            slider_layout.setVisibility(View.VISIBLE);
            slider_layout.setAlpha(0f);
            slider_layout.animate()
                    .alpha(1f)
                    .setDuration(300);
            if(shownBottomButtons) {
                bottom_buttons_Layout.setVisibility(View.GONE);
                shownBottomButtons = false;
            }
        } else {
            slider_layout.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> slider_layout.setVisibility(View.INVISIBLE));
            if(!shownBottomButtons) {
                bottom_buttons_Layout.setVisibility(View.VISIBLE);
                shownBottomButtons = true;
            }
        }
    }

    private void toggleMoreSettings() {
        shownMoreSettings = !shownMoreSettings;
        if(shownMoreSettings) {
            more_settings_layout.setVisibility(View.VISIBLE);
            more_settings_layout.setAlpha(0f);
            more_settings_layout.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction(() -> shownMoreSettings = true);
        } else {
            more_settings_layout.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        more_settings_layout.setVisibility(View.INVISIBLE);
                        shownMoreSettings = false;
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_UNKNOWN_APK) {
            if (resultCode == RESULT_OK) {
                // Permission to install unknown apps granted
                installArCore();
            } else {
                // Permission to install unknown apps denied
                // Toast.makeText(this, "Permission to install unknown apps denied", Toast.LENGTH_LONG).show();
            }
        } else if(requestCode == 1) {
            loadLastPhoto();
        } else if(requestCode == REFRESH_ITEMS) {
            glassesItemCustomAdapter.notifyDataSetChanged();
        }
    }

    private void initializeGlassesData() {
        // Populate the original glasses data lists
        addOriginalGlasses();

        if (prefManager.isAllFilesDownloaded()) {
            addDownloadedGlasses();
        }

        addAllFilteredGlasses();

        Log.d("Last Filtered Id", "" + filteredGlassesObjName.get(filteredGlassesId.size() - 1));
    }

    private void addOriginalGlasses() {
        originalGlassesId.add("1");
        originalGlassesImage.add(R.drawable.glasses_1);
        originalGlassesTitle.add("Timeless Rectangular Eyeglasses");
        originalGlassesObjName.add("glasses_1_frame.obj");
        originalTempleObjName.add("glasses_1_temple, glasses_1_temple_tip");
        originalLensesObjName.add("glasses_1_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");
        originalDescription.add("These classic, rectangular eyeglasses frames feature a bold, black outline for a modern, streamlined look. The durable construction and full UV protection make them a versatile choice for any style. Available in multiple sizes to flatter a variety of face shapes. Add a touch of refined sophistication to your look with these affordable, high-quality frames.");
        originalStacks.add("5");
        originalGlassesPrice.add("2000.95");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("2");
        originalGlassesImage.add(R.drawable.glasses_2);
        originalGlassesTitle.add("Sophisticated Rectangular Frames");
        originalGlassesObjName.add("glasses_2_frame.obj");
        originalTempleObjName.add("glasses_2_temple, glasses_2_temple_tip");
        originalLensesObjName.add("glasses_2_lenses.obj:albedo_4");
        originalGlassesFrameType.add("Wayfarer frame");
        originalGlassesType.add("Sunglasses");
        originalPadsObjName.add("");
        originalDescription.add("These classic rectangular eyeglasses frames feature a bold, modern silhouette with a sleek, black outline. Crafted from durable materials, they provide full UV protection to keep your eyes comfortable. The versatile design flatters a variety of face shapes, making these frames a stylish and practical choice for everyday wear. Available in multiple sizes to ensure a customized fit.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultSunglassesLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("3");
        originalGlassesImage.add(R.drawable.glasses_3);
        originalGlassesTitle.add("Vintage-Inspired Round Frames");
        originalGlassesObjName.add("glasses_3_frame.obj");
        originalTempleObjName.add("glasses_3_temple, glasses_3_temple_tip");
        originalLensesObjName.add("glasses_3_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Round frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");
        originalDescription.add("Transport your look to a bygone era with these classic round eyeglasses frames. Featuring a bold, black outline, they evoke a vintage aesthetic while providing modern functionality. The durable construction and full UV protection make these frames a practical choice for everyday wear. The versatile round shape flatters a variety of face types. Available in multiple sizes to ensure a comfortable, customized fit.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("4");
        originalGlassesImage.add(R.drawable.glasses_4);
        originalGlassesTitle.add("Sophisticated Round Eyeglasses");
        originalGlassesObjName.add("glasses_4_frame.obj");
        originalTempleObjName.add("glasses_4_temple, glasses_4_temple_tip");
        originalLensesObjName.add("glasses_4_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Round frame, Oval frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");
        originalDescription.add("These classic round eyeglasses frames exude a refined, vintage-inspired look. The bold, black outline and sleek silhouette give them a modern edge. Crafted from durable materials, they provide full UV protection to keep your eyes comfortable. The versatile round shape flatters a variety of face types. Available in multiple sizes to ensure a customized, comfortable fit. Elevate your style with these sophisticated, timeless frames.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("5");
        originalGlassesImage.add(R.drawable.glasses_5);
        originalGlassesTitle.add("Refined Rectangular Frames");
        originalGlassesObjName.add("glasses_5_frame.obj");
        originalTempleObjName.add("glasses_5_temple, glasses_5_temple_tip");
        originalLensesObjName.add("glasses_5_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");
        originalDescription.add("These classic rectangular eyeglasses offer a timeless, sophisticated look. The bold, black outline creates a sleek, modern silhouette. Crafted from durable materials, they provide full UV protection for all-day comfort. The rectangular shape flatters a variety of face types. Available in multiple sizes to ensure a customized, comfortable fit. Elevate your style with these refined, versatile frames.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("6");
        originalGlassesImage.add(R.drawable.glasses_6);
        originalGlassesTitle.add("Sophisticated Round Eyeglasses");
        originalGlassesObjName.add("glasses_6_frame.obj:albedo_2");
        originalTempleObjName.add("glasses_6_temple:albedo_2, glasses_6_temple_tip:albedo_2");
        originalLensesObjName.add("glasses_6_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Round frame, Oval frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");
        originalDescription.add("These classic round eyeglasses frames exude a refined, vintage-inspired look. The transparent, light-colored acetate material and sleek silhouette give them a modern, sophisticated edge. Crafted from durable materials, they provide full UV protection to keep your eyes comfortable. The versatile round shape flatters a variety of face types. Available in a range of sizes to ensure a customized, comfortable fit. Elevate your style with these timeless, versatile frames.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency + ", eyesObject=0.5f, rightTempleObject=0.5f, leftTempleObject=0.5f, rightTempleTipObject=0.5f, leftTempleTipObject=0.5f");
        originalIsDownloaded.add(false);

        originalGlassesId.add("7");
        originalGlassesImage.add(R.drawable.glasses_7);
        originalGlassesTitle.add("Classic Rectangular Frames");
        originalGlassesObjName.add("glasses_7_frame.obj");
        originalTempleObjName.add("glasses_7_temple, glasses_7_temple_tip");
        originalLensesObjName.add("glasses_7_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Wayfarer frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");
        originalDescription.add("These sleek, rectangular eyeglasses frames exude a refined, sophisticated look. The bold, black acetate material provides a modern, statement-making silhouette. Crafted with durable construction, they offer full UV protection to keep your eyes comfortable. The versatile rectangular shape flatters a variety of face types. Available in an assortment of sizes to ensure a customized, comfortable fit. Elevate your style with these timeless, high-quality frames.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("8");
        originalGlassesImage.add(R.drawable.glasses_8);
        originalGlassesTitle.add("Sophisticated Metal Frames");
        originalGlassesObjName.add("glasses_8_frame.obj");
        originalTempleObjName.add("glasses_8_temple, glasses_8_temple_tip");
        originalLensesObjName.add("glasses_8_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("glasses_8_pads.obj");
        originalDescription.add("These sleek, metal eyeglasses frames exude a refined, modern style. The clean, minimal silhouette features a bold, black finish that creates a striking, statement-making look. Crafted from durable, high-quality materials, they provide full UV protection for all-day comfort and clarity. The versatile, semi-rimless design flatters a variety of face shapes. Available in multiple sizes to ensure a customized, secure fit. Elevate your eyewear wardrobe with these sophisticated, versatile frames.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("9");
        originalGlassesImage.add(R.drawable.glasses_9);
        originalGlassesTitle.add("Durable Performance Frames");
        originalGlassesObjName.add("glasses_9_frame.obj");
        originalTempleObjName.add("glasses_9_temple, glasses_9_temple_tip");
        originalLensesObjName.add("glasses_9_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("glasses_9_pads.obj");
        originalDescription.add("These athletic eyeglasses from Under Armour feature a sleek, modern silhouette designed for an active lifestyle. The sturdy metal construction provides a lightweight, comfortable fit while offering full UV protection. The semi-rimless design with vibrant red accents delivers a bold, performance-focused aesthetic. Adjustable nose pads and temples ensure a secure, customized wear for all-day comfort during sports or everyday activities. Elevate your eyewear with these versatile, high-quality frames built to keep up with your active pursuits.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("10");
        originalGlassesImage.add(R.drawable.glasses_10);
        originalGlassesTitle.add("Chic Round Optical Frames");
        originalGlassesObjName.add("glasses_10_frame.obj");
        originalTempleObjName.add("glasses_10_temple, glasses_10_temple_tip");
        originalLensesObjName.add("glasses_10_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Round frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("glasses_10_pads.obj");
        originalDescription.add("These stylish round eyeglasses feature a classic silhouette with a modern twist. The sleek, rose gold metal construction creates a sophisticated, eye-catching look. The full-rim design provides a secure, comfortable fit while the transparent lenses offer clear, unobstructed vision. The versatile round shape flatters a variety of face types. With adjustable nose pads, these frames can be customized for all-day wearability. Elevate your everyday style with these timeless, high-quality optical frames.");
        originalStacks.add("0");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);

        originalGlassesId.add("11");
        originalGlassesImage.add(R.drawable.glasses_11);
        originalGlassesTitle.add("Sophisticated Square Optical Frames");
        originalGlassesObjName.add("glasses_11_frame.obj");
        originalTempleObjName.add("glasses_11_temple:albedo_3, glasses_11_temple_tip, glasses_11_temple_hinge:albedo_3");
        originalLensesObjName.add("glasses_11_lenses.obj:albedo_5");
        originalGlassesFrameType.add("Round frame, Oval frame, Oversize frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("glasses_11_pads.obj, glasses_11_pads_arms.obj:albedo_3");
        originalDescription.add("These sleek, square eyeglasses exude a bold, contemporary style. The sturdy black acetate construction creates a striking silhouette, while the thin metal accents add a refined, premium touch. The full-rim design provides a secure, comfortable fit, and the transparent lenses offer clear, unobstructed vision. The versatile square shape flatters a variety of face types. With adjustable nose pads, these frames can be customized for all-day wearability. Elevate your everyday look with these high-quality, sophisticated optical frames.");
        originalStacks.add("5");
        originalGlassesPrice.add("20");
        originalTransparency.add(defaultLensesTransparency);
        originalIsDownloaded.add(false);
    }

    private void addDownloadedGlasses() {
        int glassesCountDownloaded = prefManager.getGlassesCountDownloaded();

        // Read and parse data from the JSON file
        try {
            FileInputStream fileInputStream = new FileInputStream(getExternalFilesDir(null) + "/data.json");
            String jsonString = convertStreamToString(fileInputStream); // Converts InputStream to String
            JSONObject jsonData = new JSONObject(jsonString);

            // Get the glasses array from the JSON
            JSONArray glassesArray = jsonData.getJSONArray("glasses");

            int i = 0;

            for (int count = originalGlassesId.size() + 1; count <= glassesCountDownloaded; count++) {
                // Get the data for each glass
                JSONObject glass = glassesArray.getJSONObject(i);  // Adjust for index

                // Add the data to the lists
                originalGlassesId.add(String.valueOf(glass.getInt("glassesId")));
                originalGlassesImage.add(R.drawable.glasses_1);
                originalGlassesTitle.add(glass.getString("glassesTitle"));
                originalGlassesObjName.add(glass.getString("glassesObjName"));
                originalTempleObjName.add(glass.getString("templeObjName"));
                originalLensesObjName.add(glass.getString("lensesObjName"));
                originalGlassesFrameType.add(glass.getString("glassesFrameType"));
                originalGlassesType.add(glass.getString("glassesType"));
                originalPadsObjName.add(glass.getString("padsObjName"));
                originalDescription.add(glass.getString("description"));
                originalStacks.add(String.valueOf(glass.getInt("stacks")));
                originalGlassesPrice.add(String.valueOf(glass.getInt("price")));
                String transparency = glass.getString("transparency").equals("defaultLensesTransparency") ? defaultLensesTransparency : glass.getString("transparency");
                originalTransparency.add(transparency);
                originalIsDownloaded.add(true);

                i++;
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addAllFilteredGlasses() {
        // Initially show all glasses
        filteredGlassesId.addAll(originalGlassesId);
        filteredGlassesImage.addAll(originalGlassesImage);
        filteredGlassesTitle.addAll(originalGlassesTitle);
        filteredGlassesObjName.addAll(originalGlassesObjName);
        filteredTempleObjName.addAll(filteredTempleObjName);
        filteredLensesObjName.addAll(filteredLensesObjName);
        filteredGlassesFrameType.addAll(originalGlassesFrameType);
        filteredGlassesType.addAll(originalGlassesType);
        filteredPadsObjName.addAll(originalPadsObjName);
        filteredDescription.addAll(originalDescription);
        filteredStacks.addAll(originalStacks);
        filteredGlassesPrice.addAll(originalGlassesPrice);
        filteredTransparency.addAll(originalTransparency);
        filteredIsDownloaded.addAll(originalIsDownloaded);
    }

    private String convertStreamToString(InputStream is) throws IOException {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void filterGlasses() {
        List<String> selectedFrameTypes = frameTypeAdapter.getSelectedItemsList();

        // Clear the filtered lists
        filteredGlassesId.clear();
        filteredGlassesImage.clear();
        filteredGlassesTitle.clear();
        filteredGlassesObjName.clear();
        filteredTempleObjName.clear();
        filteredLensesObjName.clear();
        filteredGlassesFrameType.clear();
        filteredGlassesType.clear();
        filteredPadsObjName.clear();
        filteredDescription.clear();
        filteredStacks.clear();
        filteredGlassesPrice.clear();
        filteredTransparency.clear();
        filteredIsDownloaded.clear();

        if (selectedFrameTypes.contains("All")) {
            // If "All" is selected, show all glasses
            filteredGlassesId.addAll(originalGlassesId);
            filteredGlassesImage.addAll(originalGlassesImage);
            filteredGlassesTitle.addAll(originalGlassesTitle);
            filteredGlassesObjName.addAll(originalGlassesObjName);
            filteredTempleObjName.addAll(originalTempleObjName);
            filteredLensesObjName.addAll(originalLensesObjName);
            filteredGlassesFrameType.addAll(originalGlassesFrameType);
            filteredGlassesType.addAll(originalGlassesType);
            filteredPadsObjName.addAll(originalPadsObjName);
            filteredDescription.addAll(originalDescription);
            filteredStacks.addAll(originalStacks);
            filteredGlassesPrice.addAll(originalGlassesPrice);
            filteredTransparency.addAll(originalTransparency);
            filteredIsDownloaded.addAll(originalIsDownloaded);
        } else {
            for (int i = 0; i < originalGlassesId.size(); i++) {
                boolean isFavorite = prefManager != null && prefManager.isFavorite(originalGlassesId.get(i));
                boolean isSunglasses = originalGlassesType.get(i).contains("Sunglasses");
                boolean matchesType = false;

                for (String frameType : selectedFrameTypes) {
                    if (frameType.equals("Favorites") && !isFavorite) {
                        continue;
                    }
                    if (frameType.equals("Sunglasses") && !isSunglasses) {
                        continue;
                    }
                    if (originalGlassesFrameType.get(i).contains(frameType)) {
                        matchesType = true;
                        break;
                    }
                }

                if (selectedFrameTypes.size() == 1) {
                    if (selectedFrameTypes.contains("Favorites") && isFavorite) {
                        // Only "Favorites" selected, add all favorites
                        addToFilteredLists(i);
                    } else if (selectedFrameTypes.contains("Sunglasses") && isSunglasses) {
                        // Only "Sunglasses" selected
                        addToFilteredLists(i);
                    } else if (matchesType) {
                        // Only one other frame type selected
                        addToFilteredLists(i);
                    }
                } else {
                    boolean favoritesSelected = selectedFrameTypes.contains("Favorites");
                    boolean sunglassesSelected = selectedFrameTypes.contains("Sunglasses");

                    if (favoritesSelected && sunglassesSelected && isFavorite && isSunglasses) {
                        // Both "Favorites" and "Sunglasses" selected
                        addToFilteredLists(i);
                    } else if (favoritesSelected && isFavorite && matchesType) {
                        // "Favorites" and other frame types selected
                        addToFilteredLists(i);
                    } else if (sunglassesSelected && isSunglasses && matchesType) {
                        // "Sunglasses" and other frame types selected
                        addToFilteredLists(i);
                    } else if (!favoritesSelected && !sunglassesSelected && matchesType) {
                        // Only other frame types selected
                        addToFilteredLists(i);
                    }
                }
            }
        }

        // Notify the adapter of the changes
        glassesItemCustomAdapter.notifyDataSetChanged();
    }

    private void addToFilteredLists(int index) {
        filteredGlassesId.add(originalGlassesId.get(index));
        filteredGlassesImage.add(originalGlassesImage.get(index));
        filteredGlassesTitle.add(originalGlassesTitle.get(index));
        filteredGlassesObjName.add(originalGlassesObjName.get(index));
        filteredTempleObjName.add(originalTempleObjName.get(index));
        filteredLensesObjName.add(originalLensesObjName.get(index));
        filteredGlassesFrameType.add(originalGlassesFrameType.get(index));
        filteredGlassesType.add(originalGlassesType.get(index));
        filteredPadsObjName.add(originalPadsObjName.get(index));
        filteredDescription.add(originalDescription.get(index));
        filteredStacks.add(originalStacks.get(index));
        filteredGlassesPrice.add(originalGlassesPrice.get(index));
        filteredTransparency.add(originalTransparency.get(index));
        filteredIsDownloaded.add(originalIsDownloaded.get(index));
    }

    @Override
    protected void onDestroy() {
        if(session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }

    private static final String APK_URL = "https://github.com/CharlesCraft50/GlassesGuru/releases/download/arcore/arcore.apk";

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;

            // Ensure ARCore is installed via custom APK
            if (!isArCoreInstalled()) {
                installArCore();
                return;
            }

            // ARCore is now installed, proceed with session creation
            try {
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session and configure it to use a front-facing (selfie) camera.
                session = new Session(/* context= */ this, EnumSet.noneOf(Session.Feature.class));
                CameraConfigFilter cameraConfigFilter = new CameraConfigFilter(session);
                cameraConfigFilter.setFacingDirection(CameraConfig.FacingDirection.FRONT);
                List<CameraConfig> cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter);
                if (!cameraConfigs.isEmpty()) {
                    // Element 0 contains the camera config that best matches the session feature
                    // and filter settings.
                    session.setCameraConfig(cameraConfigs.get(0));
                } else {
                    message = "This device does not have a front-facing (selfie) camera";
                    exception = new UnavailableDeviceNotCompatibleException(message);
                }
                configureSession();

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

            } catch (UnavailableArcoreNotInstalledException e) {
                // Attempt to install ARCore via custom APK
                if (!isArCoreInstalled()) {
                    installArCore();
                    return;
                }
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();

            if (prefManager.isFirstTimeLaunch()) {
                if (areViewsInitialized()) {
                    showWelcomeDialog();
                } else {
                    // Handle the case where views might not be initialized
                    Log.e(TAG, "Views are not properly initialized for tutorial");
                }
            }

        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    // Check if all necessary views are initialized
    private boolean areViewsInitialized() {
        return findViewById(R.id.glassesCardView) != null &&
                findViewById(R.id.ai_recommendation_Button) != null &&
                findViewById(R.id.showMoreGlassesButton) != null &&
                findViewById(R.id.more_settings_button) != null &&
                findViewById(R.id.color_picker_button) != null &&
                findViewById(R.id.showGlassesButton) != null &&
                findViewById(R.id.chatSupportButton) != null &&
                findViewById(R.id.face_type_Spinner) != null &&
                findViewById(R.id.frameTypeSpinner) != null &&
                findViewById(R.id.capture_button) != null &&
                findViewById(R.id.last_photo_image_view) != null &&
                findViewById(R.id.replay_tutorial_Button) != null;
    }

    private boolean isArCoreInstalled() {
        try {
            getPackageManager().getPackageInfo("com.google.ar.core", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void installArCore() {
        // Check if the app has permission to install unknown apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                // Request permission to install unknown apps
                requestInstallUnknownAppsPermission();
            } else {
                // Download and install the APK
                downloadAndInstallArCore();
            }
        }
    }

    private void requestInstallUnknownAppsPermission() {
        // Show a dialog to prompt the user to grant permission
        new AlertDialog.Builder(this)
                .setMessage("GlassesGuru requires permission to install ARCore from an unknown source.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    // Launch the settings activity to request permission
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_INSTALL_UNKNOWN_APK);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled the permission request
                    Toast.makeText(this, "Permission denied. ARCore cannot be installed.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void downloadAndInstallArCore() {
        if(progressDialog == null || !progressDialog.isShowing()) {
            // Show progress dialog while downloading ARCore
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Downloading ARCore...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Start the installation process
            installArCore(new OnInstallationListener() {
                @Override
                public void onInstallationComplete() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onInstallationFailed() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(CameraFaceActivity.this, "Failed to install ARCore", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void installArCore(OnInstallationListener listener) {
        // Execute the task to download and install ARCore
        new DownloadAndInstallApkTask(listener).execute(APK_URL);
    }

    interface OnInstallationListener {
        void onInstallationComplete();
        void onInstallationFailed();
    }

    private class DownloadAndInstallApkTask extends AsyncTask<String, Void, File> {
        private OnInstallationListener listener;

        public DownloadAndInstallApkTask(OnInstallationListener listener) {
            this.listener = listener;
        }

        @Override
        protected File doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                File tempFile = new File(getExternalFilesDir(null), "arcore.apk");
                FileOutputStream outputStream = new FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.close();
                inputStream.close();
                return tempFile;
            } catch (Exception e) {
                Log.e(TAG, "Failed to download APK", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File apkFile) {
            if (apkFile != null) {
                installApk(apkFile);
            } else {
                listener.onInstallationFailed();
            }
        }

        private void installApk(File apkFile) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
                // Request permission to install unknown apps
                requestInstallUnknownAppsPermission();
            } else {
                // Proceed with installation
                Uri apkUri = FileProvider.getUriForFile(CameraFaceActivity.this, "com.example.glassesguru.provider", apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(apkUri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // Call the installation complete callback
                listener.onInstallationComplete();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                new DownloadAndInstallApkTask(new OnInstallationListener() {
                    @Override
                    public void onInstallationComplete() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    }

                    @Override
                    public void onInstallationFailed() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(CameraFaceActivity.this, "Failed to install ARCore", Toast.LENGTH_LONG).show();
                    }
                }).execute(APK_URL);
            } else {
                Toast.makeText(this, "Permission to read storage denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            augmentedFaceRenderer.createOnGlThread(this, "models/freckles.png");
            augmentedFaceRenderer.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            /*noseObject.createOnGlThread(*//*context=*//* this, "models/nose.obj", "models/nose_fur.png");
            noseObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            noseObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
            rightEarObject.createOnGlThread(this, "models/forehead_right.obj", "models/ear_fur.png");
            rightEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            rightEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
            leftEarObject.createOnGlThread(this, "models/forehead_left.obj", "models/ear_fur.png");
            leftEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            leftEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);*/

            // Glasses render
            eyesObject.createOnGlThread(this, "models/glasses/" + glassesModel, "models/glasses/albedo.png", false);
            eyesObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            eyesObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

            rightTempleObject.createOnGlThread(this, "models/glasses/glasses_1_temple_right.obj", "models/glasses/albedo.png", false);
            rightTempleObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            rightTempleObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

            leftTempleObject.createOnGlThread(this, "models/glasses/glasses_1_temple_left.obj", "models/glasses/albedo.png", false);
            leftTempleObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            leftTempleObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

            rightTempleTipObject.createOnGlThread(this, "models/glasses/glasses_1_temple_tip_right.obj", "models/glasses/albedo.png", false);
            rightTempleTipObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            rightTempleTipObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

            leftTempleTipObject.createOnGlThread(this, "models/glasses/glasses_1_temple_tip_left.obj", "models/glasses/albedo.png", false);
            leftTempleTipObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            leftTempleTipObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

            lensesObject.createOnGlThread(this, "models/glasses/glasses_1_lenses.obj", "models/glasses/albedo_5.png", false);
            lensesObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            lensesObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }

        hideLoadingIndicator();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }
    private Handler recommendationPopUpHandler = new Handler();
    private Runnable hideRecommendationPopupRunnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recommendation_pop_up.getVisibility() == View.VISIBLE) {
                        recommendation_pop_up.setVisibility(View.GONE);
                        isRecommendationPopupVisible = false;
                    }
                }
            });
        }
    };

    @Override
    public void onDrawFrame(GL10 gl) {
// Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);



        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Get projection matrix.
            float[] projectionMatrix = new float[16];
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewMatrix = new float[16];
            camera.getViewMatrix(viewMatrix, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

            if(capture_image_recommendation) {
                Bitmap capturedBitmap = createBitmapFromGLSurface(0, 0, surfaceView.getWidth(), surfaceView.getHeight(), gl);
                InputImage image = InputImage.fromBitmap(capturedBitmap, 0);
                if (image != null) {
                    /*runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageView imageView = findViewById(R.id.captured_image);
                            imageView.setImageBitmap(capturedBitmap);
                        }
                    });*/
                    capture_image_recommendation = false;

                    detectFaces(image, capturedBitmap);

                    // Hide the capture_LinearLayout after 2 seconds
                    /*new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            capture_LinearLayout.setVisibility(View.GONE);
                        }
                    }, 1000);*/
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CameraFaceActivity.this, "Error capturing frame", Toast.LENGTH_SHORT).show();
                        }
                    });
                    capture_button.completeLoading();

                    capture_image_recommendation = false;

                    // Hide the capture_LinearLayout after 2 seconds
                    /*new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            capture_LinearLayout.setVisibility(View.GONE);
                        }
                    }, 1000);*/
                }

            }

            if (isRecommendationPopupVisible) {
                isRecommendationPopupVisible = false; // Reset the flag to prevent repeated triggers

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (recommendation_pop_up.getVisibility() == View.GONE) {
                            recommendation_pop_up.setVisibility(View.VISIBLE);
                            recommendationPopUpHandler.postDelayed(hideRecommendationPopupRunnable, 5000);
                        }
                    }
                });
            }

            // ARCore's face detection works best on upright faces, relative to gravity.
            // If the device cannot determine a screen side aligned with gravity, face
            // detection may not work optimally.
            Collection<AugmentedFace> faces = session.getAllTrackables(AugmentedFace.class);
            for (AugmentedFace face : faces) {
                if (face.getTrackingState() != TrackingState.TRACKING) {
                    break;
                }

                // Face objects use transparency so they must be rendered back to front without depth write.
                GLES20.glDepthMask(false);



                // Each face's region poses, mesh vertices, and mesh normals are updated every frame.

                // 1. Render the face mesh first, behind any 3D objects attached to the face regions.
                float[] modelMatrix = new float[16];
                face.getCenterPose().toMatrix(modelMatrix, 0);
                augmentedFaceRenderer.draw(
                        projectionMatrix, viewMatrix, modelMatrix, colorCorrectionRgba, face);

                /*// 2. Next, render the 3D objects attached to the forehead.
                face.getRegionPose(RegionType.FOREHEAD_RIGHT).toMatrix(rightEarMatrix, 0);
                rightEarObject.updateModelMatrix(rightEarMatrix, scaleFactor);
                rightEarObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);

                face.getRegionPose(RegionType.FOREHEAD_LEFT).toMatrix(leftEarMatrix, 0);
                leftEarObject.updateModelMatrix(leftEarMatrix, scaleFactor);
                leftEarObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);

                // 3. Render the nose last so that it is not occluded by face mesh or by 3D objects attached
                // to the forehead regions.
                face.getRegionPose(RegionType.NOSE_TIP).toMatrix(noseMatrix, 0);
                noseObject.updateModelMatrix(noseMatrix, scaleFactor);
                noseObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);*/

                // This is for the glasses

                /*face.getRegionPose(RegionType.NOSE_TIP).toMatrix(eyesMatrix, 0);
                eyesObject.updateModelMatrix(eyesMatrix, scaleFactor);
                eyesObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);*/

                // For the Glasses Frame

                float headRotationAngle = getHeadRotationAngle(face);

                if (eyesObjectNeedsCreation) {
                    try {



                        // Create the eyesObject
                        String glassesModelPath = modelBasePath + glassesModel.trim();
                        String glassesModelAlbedo = "models/glasses/albedo.png";
                        if (glassesModel.contains(":")) {
                            String[] glassesModelComponents = glassesModel.split(":");
                            glassesModelPath = modelBasePath + glassesModelComponents[0].trim();
                            glassesModelAlbedo = "models/glasses/" + glassesModelComponents[1].trim() + ".png";
                        }


                        if(isDownloaded) {
                            File glassesModelFile = new File(glassesModelPath);

                            if(glassesModelFile.exists()) {
                                eyesObject.createOnGlThread(this, glassesModelFile.getAbsolutePath(), glassesModelAlbedo, true);
                            }
                        } else {
                            eyesObject.createOnGlThread(this, glassesModelPath, glassesModelAlbedo, false);
                        }



                        if (!templeModel.isEmpty()) {
                            templeParts = templeModel.contains(",") ? templeModel.split(",") : new String[]{templeModel};

                            String templePart = templeParts[0].trim();
                            String templeTipPart = templeParts.length > 1 ? templeParts[1].trim() : null;
                            String hingePart = templeParts.length > 2 ? templeParts[2].trim() : null;

                            // Handle temple part
                            String templePartPath = modelBasePath + templePart + "_right.obj";
                            String templePartAlbedo = "models/glasses/albedo.png";
                            if (templePart.contains(":")) {
                                String[] templePartComponents = templePart.split(":");
                                templePartPath = modelBasePath + templePartComponents[0].trim() + "_right.obj";
                                templePartAlbedo = "models/glasses/" + templePartComponents[1].trim() + ".png";
                            }

                            if(isDownloaded) {
                                File templePartFile = new File(templePartPath);
                                File templePartLeftFile = new File(templePartPath.replace("_right", "_left"));

                                if(templePartFile.exists()) {
                                    rightTempleObject.createOnGlThread(this, templePartFile.getAbsolutePath(), templePartAlbedo, true);
                                    leftTempleObject.createOnGlThread(this, templePartLeftFile.getAbsolutePath(), templePartAlbedo, true);
                                }
                            } else {
                                rightTempleObject.createOnGlThread(this, templePartPath, templePartAlbedo, false);
                                leftTempleObject.createOnGlThread(this, templePartPath.replace("_right", "_left"), templePartAlbedo, false);
                            }

                            // Handle temple tip part
                            if (templeTipPart != null) {

                                String templeTipPartPath = modelBasePath + templeTipPart + "_right.obj";
                                String templeTipPartAlbedo = "models/glasses/albedo.png";
                                if (templeTipPart.contains(":")) {
                                    String[] templeTipPartComponents = templeTipPart.split(":");
                                    templeTipPartPath = modelBasePath + templeTipPartComponents[0].trim() + "_right.obj";
                                    templeTipPartAlbedo = "models/glasses/" + templeTipPartComponents[1].trim() + ".png";
                                }

                                if(isDownloaded) {
                                    File templeTipPartFile = new File(templeTipPartPath);
                                    File templeTipPartLeftFile = new File(templeTipPartPath.replace("_right", "_left"));

                                    if (templeTipPartFile.exists()) {
                                        rightTempleTipObject.createOnGlThread(this, templeTipPartFile.getAbsolutePath(), templeTipPartAlbedo, true);

                                        leftTempleTipObject.createOnGlThread(this, templeTipPartLeftFile.getAbsolutePath(), templeTipPartAlbedo, true);
                                    }
                                } else {
                                    rightTempleTipObject.createOnGlThread(this, templeTipPartPath, templeTipPartAlbedo, false);

                                    leftTempleTipObject.createOnGlThread(this, templeTipPartPath.replace("_right", "_left"), templeTipPartAlbedo, false);
                                }

                            }

                            // Handle hinge part
                            if (hingePart != null) {
                                String hingePartRightPath = modelBasePath + hingePart + "_right.obj";
                                String hingePartLeftPath = modelBasePath + hingePart + "_left.obj";
                                String hingePartAlbedo = "models/glasses/albedo.png";
                                if (hingePart.contains(":")) {
                                    String[] hingePartComponents = hingePart.split(":");
                                    hingePartRightPath = modelBasePath + hingePartComponents[0].trim() + "_right.obj";
                                    hingePartLeftPath = modelBasePath + hingePartComponents[0].trim() + "_left.obj";
                                    hingePartAlbedo = "models/glasses/" + hingePartComponents[1].trim() + ".png";
                                }

                                if(isDownloaded) {
                                    File hingePartRightFile = new File(hingePartRightPath);
                                    File hingePartLeftFile = new File(hingePartLeftPath);

                                    if(hingePartRightFile.exists()) {
                                        rightHingeObject.createOnGlThread(this, hingePartRightFile.getAbsolutePath(), hingePartAlbedo, true);
                                        leftHingeObject.createOnGlThread(this, hingePartLeftFile.getAbsolutePath(), hingePartAlbedo, true);
                                    }
                                } else {
                                    rightHingeObject.createOnGlThread(this, hingePartRightPath, hingePartAlbedo, false);
                                    leftHingeObject.createOnGlThread(this, hingePartLeftPath, hingePartAlbedo, false);
                                }

                            }
                        }

                        if (!padsModel.isEmpty()) {
                            isPadArmsExists = true;
                            padParts = padsModel.contains(",") ? padsModel.split(",") : new String[]{padsModel};

                            // Create the pads object
                            String padModelPath = modelBasePath + padParts[0].trim();
                            String padModelAlbedo = "models/glasses/albedo_2.png";
                            if (padParts[0].contains(":")) {
                                String[] padModelComponents = padParts[0].split(":");
                                padModelPath = modelBasePath + padModelComponents[0].trim();
                                padModelAlbedo = "models/glasses/" + padModelComponents[1].trim() + ".png";
                            }

                            if(isDownloaded) {
                                File padModelFile = new File(padModelPath);

                                if(padModelFile.exists()) {
                                    padsObject.createOnGlThread(this, padModelFile.getAbsolutePath(), padModelAlbedo, true);
                                }
                            } else {
                                padsObject.createOnGlThread(this, padModelPath, padModelAlbedo, false);
                            }

                            padsObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
                            padsObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

                            // Create the pads arms object if specified
                            if (padParts.length > 1) {
                                String padArmsModelPath = modelBasePath + padParts[1].trim();
                                String padArmsModelAlbedo = "models/glasses/albedo_2.png";
                                if (padParts[1].contains(":")) {
                                    String[] padArmsModelComponents = padParts[1].split(":");
                                    padArmsModelPath = modelBasePath + padArmsModelComponents[0].trim();
                                    padArmsModelAlbedo = "models/glasses/" + padArmsModelComponents[1].trim() + ".png";
                                }

                                if(isDownloaded) {
                                    File padArmsModelFile = new File(padArmsModelPath);

                                    if(padArmsModelFile.exists()) {
                                        padsArmsObject.createOnGlThread(this, padArmsModelFile.getAbsolutePath(), padArmsModelAlbedo, true);
                                    }
                                } else {
                                    padsArmsObject.createOnGlThread(this, padArmsModelPath, padArmsModelAlbedo, false);
                                }

                                padsArmsObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
                                padsArmsObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
                            } else {
                                isPadArmsExists = false;
                            }
                        }

                        if(!lensesModel.isEmpty()) {

                            // Create the lenses object
                            String lensesModelPath = modelBasePath + lensesModel.trim();
                            String lensesModelAlbedo = "models/glasses/albedo_2.png";
                            if (lensesModel.contains(":")) {
                                String[] lensesModelComponents = lensesModel.split(":");
                                lensesModelPath = modelBasePath + lensesModelComponents[0].trim();
                                lensesModelAlbedo = "models/glasses/" + lensesModelComponents[1].trim() + ".png";
                            }

                            if(isDownloaded) {
                                File lensesModelFile = new File(lensesModelPath);

                                if (lensesModelFile.exists()) {
                                    lensesObject.createOnGlThread(this, lensesModelFile.getAbsolutePath(), lensesModelAlbedo, true);
                                }
                            } else {
                                lensesObject.createOnGlThread(this, lensesModelPath, lensesModelAlbedo, false);
                            }

                        }

                        eyesObjectNeedsCreation = false;
                        runOnUiThread(() -> {
                                    capture_button.completeLoading();
                                });
                        //loading_screen.setVisibility(View.GONE);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to create eyesObject", e);
                    }
                }

                if(glassesVisible) {
                    Matrix.setIdentityM(offsetMatrix, 0);
                    Matrix.translateM(offsetMatrix, 0, 0.0f, glasses_offset_y, -0.05f); // Adjust the y-offset as needed
                    face.getRegionPose(RegionType.NOSE_TIP).toMatrix(noseMatrix, 0);

                    Matrix.rotateM(noseMatrix, 0, rotationAngle, 1.0f, 0.0f, 0.0f);

                    // Apply the offset to position the eyeglasses slightly above the nose tip.
                    Matrix.multiplyMM(eyesMatrix, 0, noseMatrix, 0, offsetMatrix, 0);

                    eyesObject.updateModelMatrix(eyesMatrix, scaleFactor);
                    eyesObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);

                    updateTransparency("eyesObject", eyesObject);

                    if (!padsModel.isEmpty()) {
                        padsObject.updateModelMatrix(eyesMatrix, scaleFactor);
                        padsObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                        if(padsModel.length() > 1) {
                            if(isPadArmsExists) {
                                padsArmsObject.updateModelMatrix(eyesMatrix, scaleFactor);
                                padsArmsObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                            }
                        }
                    }

                    if(lensesVisible) {
                        if(!lensesModel.isEmpty()) {
                            lensesObject.updateModelMatrix(eyesMatrix, scaleFactor);
                            lensesObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                            if(updateTransparency("lensesObject", eyesObject)) {
                                if(lensesFlareVisible) {
                                    lensesObject.addLensFlare(0.3f);
                                } else {
                                    lensesObject.addLensFlare(0f);
                                }
                            } else {
                                if(lensesFlareVisible) {
                                    lensesObject.addLensFlare(0.001f);
                                } else {
                                    lensesObject.addLensFlare(0f);
                                }
                            }

                            if(adjustLensesTransparency) {
                                lensesObject.setTransparency(lensesVisibilitySliderValue);
                            }

                        }
                    }

                    // For the Glasses Temples

                    if (templeVisible) {
                        if (!templeModel.isEmpty()) {
                            templeParts = templeModel.contains(",") ? templeModel.split(",") : new String[] {templeModel};
                            // Right temple calculations for Auto Hide
                            /*float rightTempleScaleFactor = 1.0f;
                            float rightTempleOffsetFactor = 0.0f;
                            if (headRotationAngle > 0 && headRotationAngle <= 84) {
                                rightTempleScaleFactor = 1.0f - (headRotationAngle / 170f);
                                if (headRotationAngle <= 90f && !templeModel.equals("glasses_3_temple")) {
                                    rightTempleOffsetFactor = glasses_offset_y;
                                    rightTempleScaleFactor = 0.0f;
                                }
                            }*/

                            // Right Temple Draw
                            if (rightTempleVisible) {
                                rightTempleObject.updateModelMatrix(eyesMatrix, scaleFactor);
                                if (autoHideTemple) {
                                    //rightTempleObject.adjustTempleTransform(rightTempleScaleFactor, rightTempleOffsetFactor);
                                }
                                rightTempleObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                                updateTransparency("rightTempleObject", rightTempleObject);
                            }

                            // Right Temple Tip Draw
                            if (rightTempleTipVisible) {
                                if (templeParts.length > 1) {
                                    rightTempleTipObject.updateModelMatrix(eyesMatrix, scaleFactor);
                                    if (autoHideTemple) {
                                        //rightTempleTipObject.adjustTempleTransform(rightTempleScaleFactor, rightTempleOffsetFactor);
                                    }
                                    rightTempleTipObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                                    updateTransparency("rightTempleTipObject", rightTempleTipObject);
                                }
                            }

                            // Left temple calculations for Auto Hide
                            /*float leftTempleScaleFactor = 1.0f;
                            float leftTempleOffsetFactor = 0.0f;
                            if (headRotationAngle >= 93) {
                                leftTempleScaleFactor = 1.0f - ((headRotationAngle - 100) / 20f);
                                if (!templeModel.equals("glasses_3_temple")) {
                                    leftTempleOffsetFactor = glasses_offset_y;
                                    leftTempleScaleFactor = 0.0f;
                                }
                            }*/

                            // Left Temple Draw
                            if (leftTempleVisible) {
                                leftTempleObject.updateModelMatrix(eyesMatrix, scaleFactor);
                                if (autoHideTemple) {
                                    ///leftTempleObject.adjustTempleTransform(leftTempleScaleFactor, leftTempleOffsetFactor);
                                }
                                leftTempleObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                                updateTransparency("leftTempleObject", leftTempleObject);
                            }

                            // Left Temple Tip Draw
                            if (leftTempleTipVisible) {
                                if (templeParts.length > 1) {
                                    leftTempleTipObject.updateModelMatrix(eyesMatrix, scaleFactor);
                                    if (autoHideTemple) {
                                        //leftTempleTipObject.adjustTempleTransform(leftTempleScaleFactor, leftTempleOffsetFactor);
                                    }
                                    leftTempleTipObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                                    updateTransparency("leftTempleTipObject", leftTempleTipObject);
                                }
                            }
                        }
                    }

                    // If hinges is available
                    if(templeParts.length > 2) {
                        leftHingeObject.updateModelMatrix(eyesMatrix, scaleFactor);
                        leftHingeObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                        updateTransparency("leftHingeObject", leftHingeObject);

                        rightHingeObject.updateModelMatrix(eyesMatrix, scaleFactor);
                        rightHingeObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                        updateTransparency("rightHingeObject", rightHingeObject);
                    }


                }
            }

            if(capture_image) {

                Bitmap capturedBitmap = createBitmapFromGLSurface(0, 0, surfaceView.getWidth(), surfaceView.getHeight(), gl);
                if (capturedBitmap != null) {
                    runOnUiThread(() -> {
                        capture_image_ImageView.setImageBitmap(capturedBitmap);
                        lastPhotoImageView.setImageBitmap(capturedBitmap);
                    });
                    capture_image = false;
                    saveBitmapToDevice(capturedBitmap);


                    // Hide the capture_LinearLayout after 2 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            capture_LinearLayout.setVisibility(View.GONE);
                        }
                    }, 1000);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CameraFaceActivity.this, "Error capturing frame", Toast.LENGTH_SHORT).show();
                        }
                    });
                    capture_image = false;
                    capture_button.completeLoading();

                    // Hide the capture_LinearLayout after 2 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            capture_LinearLayout.setVisibility(View.GONE);
                        }
                    }, 1000);
                }
            }

            if(capture_image_ai) {
                //Bitmap capturedBitmap = createBitmapFromGLSurface(0, 0, surfaceView.getWidth(), surfaceView.getHeight(), gl);

                if (croppedFaceBitmap != null) {
                    croppedFaceBitmap = Bitmap.createScaledBitmap(croppedFaceBitmap, 224, 224, true);
                    croppedFaceBitmap = adjustBrightnessContrast(croppedFaceBitmap, 50, 1.2f);
                    detectFaceShape(croppedFaceBitmap);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dashedFaceAreaHide();
                        }
                    });

                    capture_image_ai = false;
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CameraFaceActivity.this, "Error capturing frame", Toast.LENGTH_SHORT).show();
                        }
                    });
                    capture_image = false;
                    capture_button.completeLoading();

                    // Hide the capture_LinearLayout after 2 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            capture_LinearLayout.setVisibility(View.GONE);
                        }
                    }, 1000);
                }
            }
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        } finally {
            GLES20.glDepthMask(true);
        }
    }

    private void dashedFaceAreaHide() {
        dashedFaceAreaLayout.setVisibility(View.GONE);
        ai_recommendation_Button.setClickable(true);
    }

    private void loadLastPhoto() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    return Long.compare(file2.lastModified(), file1.lastModified());
                }
            });
            File lastPhoto = files[0];
            Glide.with(this).load(lastPhoto).into(lastPhotoImageView);
        } else {
            //Toast.makeText(this, "No photos found", Toast.LENGTH_SHORT).show();
        }
    }

    // Function to save the Bitmap to the device with a random filename
    private void saveBitmapToDevice(Bitmap bitmap) {
        // Generate a random filename with the prefix "glasses_guru_image_"
        // String fileName = "glasses_guru_image_" + UUID.randomUUID().toString() + ".png";
        // Generate a filename with the current date and time in the format IMGyyyyMMddHHmmss
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        String fileName = "GLASSES_GURU_IMG" + currentDateAndTime + ".png";
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        // File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!directory.exists()) {
            directory.mkdirs();  // Create the directory if it doesn't exist
        }
        File file = new File(directory, fileName);

        // Save the Bitmap to the file
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            // Optionally, notify the user that the image has been saved
            // Request the media scanner to scan the file so it appears in the Photos app
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CameraFaceActivity.this, "Image saved to: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                            capture_button.completeLoading();
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            // Notify the user if there was an error saving the image
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CameraFaceActivity.this, "Error saving image", Toast.LENGTH_SHORT).show();
                    capture_button.completeLoading();
                }
            });
        }
    }

    private void configureSession() {
        Config config = new Config(session);
        config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
        session.configure(config);
    }

    SeekBar.OnSeekBarChangeListener sliderScaleChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//            scaleFactor = initialScaleFactor + (progress / 100.0f);
            scaleFactor = (progress / 100.0f) + 0.5f;
            debug_x.setText("Scale: " + scaleFactor);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener sliderYChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//            yFactor = (progress - 100) / 100.0f;
            glasses_offset_y = (progress - 100) / 100.0f;
            debug_y.setText("glasses_offset_y: " + glasses_offset_y);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener sliderRotationChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            rotationAngle = progress * 0.36f;
            debug_z.setText("Rotation angle: " + rotationAngle);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            /*rotationOnProgress = true;*/
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            /*rotationOnProgress = true;
            previousRotationAngle = rotationAngle;*/
        }
    };

    private void toggleGlassesVisibility() {
        glassesVisible = !glassesVisible;
        templeVisible = !templeVisible;
        if(templeParts.length > 1) {
            rightTempleTipVisible = !rightTempleTipVisible;
            leftTempleTipVisible = !leftTempleTipVisible;
        }
        rightTempleVisible = !rightTempleVisible;
        leftTempleVisible = !leftTempleVisible;
    }

    private void toggleTempleVisibility() {
        templeVisible = !templeVisible;
    }

    public void updateGlassesModel(String newModel, String newTemple, String newLenses, String newGlassesType, String newPads, String newTransparency, boolean newIsDownloaded) {
        templeParts = new String[]{""};
        padParts = new String[]{""};
        glassesModel = newModel;
        templeModel = newTemple;
        lensesModel = newLenses;
        glassesType = newGlassesType;
        padsModel = newPads;
        transparencyObj = newTransparency;
        isDownloaded = newIsDownloaded;

        if(lensesModel.isEmpty()) {
            lensesObjectCustomColor = DEFAULT_COLOR_INT;

        } else {
            lensesObjectCustomColor = fromFloatToIntColor(lensesObject.getCustomColor());
        }

        templeObjectCustomColor = DEFAULT_COLOR_INT;
        templeTipObjectCustomColor = DEFAULT_COLOR_INT;

        // Set the flag to indicate that eyesObject needs to be created
        eyesObjectNeedsCreation = true;
    }

    private void showCheckboxColorPickerDialog() {
        View checkboxDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);
        SwitchMaterial frameCheckbox = checkboxDialogView.findViewById(R.id.frame_checkbox);
        SwitchMaterial templeCheckbox = checkboxDialogView.findViewById(R.id.temple_checkbox);
        SwitchMaterial templeTipCheckbox = checkboxDialogView.findViewById(R.id.temple_tip_checkbox);
        LinearLayout templeTipCheckboxLayout = checkboxDialogView.findViewById(R.id.temple_tip_layout);
        SwitchMaterial templeHingeCheckbox = checkboxDialogView.findViewById(R.id.temple_hinge_checkbox);
        LinearLayout templeHingeCheckboxLayout = checkboxDialogView.findViewById(R.id.temple_hinge_layout);
        SwitchMaterial padArmsCheckbox = checkboxDialogView.findViewById(R.id.pad_arms_checkbox);
        LinearLayout padArmsCheckboxLayout = checkboxDialogView.findViewById(R.id.pad_arms_layout);
        SwitchMaterial lensesCheckbox = checkboxDialogView.findViewById(R.id.lenses_checkbox);
        LinearLayout lensesCheckboxLayout = checkboxDialogView.findViewById(R.id.lenses_layout);
        Button okButton = checkboxDialogView.findViewById(R.id.ok_button);
        Button resetButton = checkboxDialogView.findViewById(R.id.reset_button);

        AlertDialog checkboxDialog = new AlertDialog.Builder(this)
                .setView(checkboxDialogView)
                .create();

        frameCheckbox.setChecked(isFrameColorSelected);

        if(!lensesModel.isEmpty()) {
            lensesCheckboxLayout.setVisibility(View.VISIBLE);
            lensesCheckbox.setChecked(isLensesColorSelected);
        }

        templeCheckbox.setChecked(isTempleColorSelected);
        templeHingeCheckbox.setChecked(isTempleHingeColorSelected);
        padArmsCheckbox.setChecked(isPadArmsColorSelected);

        if(templeParts.length > 1) {
            templeTipCheckboxLayout.setVisibility(View.VISIBLE);
            templeTipCheckbox.setChecked(isTempleTipColorSelected);
        }

        if(templeParts.length > 2) {
            templeHingeCheckboxLayout.setVisibility(View.VISIBLE);
            templeHingeCheckbox.setChecked(isTempleHingeColorSelected);
        }

        if(padParts.length > 1) {
            padArmsCheckboxLayout.setVisibility(View.VISIBLE);
            padArmsCheckbox.setChecked(isPadArmsColorSelected);
        }

        okButton.setOnClickListener(v -> {
            isFrameColorSelected = frameCheckbox.isChecked();
            if(!lensesModel.isEmpty()) {
                isLensesColorSelected = lensesCheckbox.isChecked();
            }
            isTempleColorSelected = templeCheckbox.isChecked();
            if(templeParts.length > 1) {
                isTempleTipColorSelected = templeTipCheckbox.isChecked();
            }
            if(templeParts.length > 2) {
                isTempleHingeColorSelected = templeHingeCheckbox.isChecked();
            }
            if(padParts.length > 1) {
                isPadArmsColorSelected = padArmsCheckbox.isChecked();
            }
            checkboxDialog.dismiss();
            showColorPickerDialog();
        });

        resetButton.setOnClickListener(v -> {
            resetColor();

            isFrameColorSelected = frameCheckbox.isChecked();
            if(!lensesModel.isEmpty()) {
                isLensesColorSelected = lensesCheckbox.isChecked();
            }
            isTempleColorSelected = templeCheckbox.isChecked();
            if(templeParts.length > 1) {
                isTempleTipColorSelected = templeTipCheckbox.isChecked();
            }
            if(templeParts.length > 2) {
                isTempleHingeColorSelected = templeHingeCheckbox.isChecked();
            }
            if(padParts.length > 1) {
                isPadArmsColorSelected = padArmsCheckbox.isChecked();
            }
            checkboxDialog.dismiss();
        });

        checkboxDialog.show();
    }

    private void resetColor() {
        clearCustomColor();
        previousColor = DEFAULT_COLOR_INT;
        eyesObjectCustomColor = DEFAULT_COLOR_INT;
        lensesObjectCustomColor = DEFAULT_COLOR_INT;
        templeObjectCustomColor = DEFAULT_COLOR_INT;
        templeTipObjectCustomColor = DEFAULT_COLOR_INT;
    }


    private void showColorPickerDialog() {
        int initialColor = (previousColor == DEFAULT_COLOR_INT) ? selectedColor : previousColor;
        AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(this, initialColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // Clear custom color
                clearCustomColor();
                previousColor = DEFAULT_COLOR_INT;
                eyesObjectCustomColor = DEFAULT_COLOR_INT;
                lensesObjectCustomColor = DEFAULT_COLOR_INT;
                templeObjectCustomColor = DEFAULT_COLOR_INT;
                templeTipObjectCustomColor = DEFAULT_COLOR_INT;
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                selectedColor = color;
                // Update previousColor after setting the new selectedColor
                previousColor = selectedColor;

                float[] customColor = new float[]{Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f};
                // Set custom color for selected parts
                if (isFrameColorSelected) {
                    eyesObject.setCustomColor(customColor);
                    eyesObjectCustomColor = fromFloatToIntColor(eyesObject.getCustomColor());
                }

                if(isLensesColorSelected) {
                    lensesObject.setCustomColor(customColor);
                    lensesObjectCustomColor = fromFloatToIntColor(lensesObject.getCustomColor());
                }

                if(isTempleTipColorSelected && templeParts.length > 1) {
                    rightTempleTipObject.setCustomColor(customColor);
                    leftTempleTipObject.setCustomColor(customColor);
                    templeTipObjectCustomColor = fromFloatToIntColor(rightTempleTipObject.getCustomColor());
                }


                if(isTempleHingeColorSelected && templeParts.length > 2) {
                    rightHingeObject.setCustomColor(customColor);
                    leftHingeObject.setCustomColor(customColor);
                }

                if(isPadArmsColorSelected && padParts.length > 1) {
                    padsArmsObject.setCustomColor(customColor);
                }

                if (isTempleColorSelected) {
                    rightTempleObject.setCustomColor(customColor);
                    leftTempleObject.setCustomColor(customColor);
                    templeObjectCustomColor = fromFloatToIntColor(rightTempleObject.getCustomColor());
                }

            }
        });

        colorPickerDialog.show();
    }

    private void clearCustomColor() {
        previousColor = selectedColor;
        selectedColor = DEFAULT_COLOR_INT;
        eyesObject.clearCustomColor();
        if(isLensesColorSelected) {
            lensesObject.clearCustomColor();
        }
        if(templeParts.length > 1 && isTempleTipColorSelected) {
                rightTempleTipObject.clearCustomColor();
                leftTempleTipObject.clearCustomColor();
        }
        if(templeParts.length > 2 && isTempleHingeColorSelected) {
            rightHingeObject.clearCustomColor();
            leftHingeObject.clearCustomColor();
        }
        if(padParts.length > 1) {
            padsArmsObject.clearCustomColor();
        }
        rightTempleObject.clearCustomColor();
        leftTempleObject.clearCustomColor();
        isFrameColorSelected = true;
        isTempleColorSelected = true;
        isTempleTipColorSelected = false;
        isTempleHingeColorSelected = false;
        isPadArmsColorSelected = false;
    }

    /*public void toggleLoadingScreen() {
        showLoadingScreen = !showLoadingScreen;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(showLoadingScreen) {
                    loading_screen.setVisibility(View.VISIBLE);
                    loading_icon_ImageView.startAnimation(rotateAnimation);
                } else {
                    loading_screen.setVisibility(View.GONE);
                }
            }
        });
    }*/

    private void showCheckboxShowGlassesDialog() {
        View checkboxDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_show_glasses, null);
        SwitchMaterial frameCheckbox = checkboxDialogView.findViewById(R.id.frame_checkbox);
        SwitchMaterial templeCheckbox = checkboxDialogView.findViewById(R.id.temple_checkbox);
        MaterialCheckBox rightTempleCheckbox = checkboxDialogView.findViewById(R.id.temple_right_checkbox);
        MaterialCheckBox leftTempleCheckbox = checkboxDialogView.findViewById(R.id.temple_left_checkbox);
        MaterialCheckBox rightTempleTipCheckbox = checkboxDialogView.findViewById(R.id.temple_tip_right_checkbox);
        MaterialCheckBox leftTempleTipCheckbox = checkboxDialogView.findViewById(R.id.temple_tip_left_checkbox);
        LinearLayout templeTipCheckboxLayout = checkboxDialogView.findViewById(R.id.temple_tip_layout);
        SwitchMaterial templeTipCheckbox = checkboxDialogView.findViewById(R.id.temple_tip_checkbox);
        MaterialCheckBox autoHideTemplesCheckbox = checkboxDialogView.findViewById(R.id.auto_hide_temple_checkbox);
        SwitchMaterial lensesCheckbox = checkboxDialogView.findViewById(R.id.lenses_checkbox);
        LinearLayout lensesCheckboxLayout = checkboxDialogView.findViewById(R.id.lenses_layout);
        ConstraintLayout lensesVisibilityLayout = checkboxDialogView.findViewById(R.id.lensesVisibilityLayout);
        SeekBar sliderLensesVisibilityBar = checkboxDialogView.findViewById(R.id.slider_lenses_visibility_bar);
        MaterialCheckBox lensesFlareCheckbox = checkboxDialogView.findViewById(R.id.lenses_flare_checkbox);
        ImageView lensesVisibilityIcon = checkboxDialogView.findViewById(R.id.lensesVisibilityIcon);
        Button okButton = checkboxDialogView.findViewById(R.id.ok_button);

        AlertDialog checkboxDialog = new AlertDialog.Builder(this)
                .setView(checkboxDialogView)
                .create();

        frameCheckbox.setChecked(glassesVisible);
        if(!lensesModel.isEmpty()) {
            lensesCheckboxLayout.setVisibility(View.VISIBLE);
            lensesCheckbox.setChecked(lensesVisible);
            lensesFlareCheckbox.setVisibility(View.VISIBLE);
            lensesFlareCheckbox.setChecked(lensesFlareVisible);
            lensesVisibilityLayout.setVisibility(View.VISIBLE);
            int progress = (int) ((lensesVisibilitySliderValue - 0.1f) / 0.9f * 90);
            sliderLensesVisibilityBar.setProgress(progress);
        }

        templeCheckbox.setChecked(templeVisible);

        if(templeParts.length > 1) {
            templeTipCheckboxLayout.setVisibility(View.VISIBLE);

            rightTempleTipCheckbox.setChecked(rightTempleTipVisible);
            leftTempleTipCheckbox.setChecked(leftTempleTipVisible);
        }

        templeTipCheckbox.setChecked(rightTempleTipCheckbox.isChecked() || leftTempleTipCheckbox.isChecked());

        templeTipCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rightTempleTipCheckbox.setChecked(isChecked);
            leftTempleTipCheckbox.setChecked(isChecked);
        });

        rightTempleCheckbox.setChecked(rightTempleVisible);
        leftTempleCheckbox.setChecked(leftTempleVisible);
        autoHideTemplesCheckbox.setChecked(autoHideTemple);

        rightTempleCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rightTempleTipCheckbox.setChecked(isChecked);
        });

        leftTempleCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            leftTempleTipCheckbox.setChecked(isChecked);
        });

        frameCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            templeCheckbox.setChecked(isChecked);
            if(templeParts.length > 1) {
                rightTempleTipCheckbox.setChecked(isChecked);
                leftTempleTipCheckbox.setChecked(isChecked);
            }
            rightTempleCheckbox.setChecked(isChecked);
            leftTempleCheckbox.setChecked(isChecked);
        });

        // Add listeners to sync the temple checkboxes with the main temple checkbox
        templeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rightTempleCheckbox.setChecked(isChecked);
            leftTempleCheckbox.setChecked(isChecked);
        });

        lensesVisibilityIcon.setOnClickListener(v -> {
            sliderLensesVisibilityBar.setProgress(0);
        });

        lensesCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            lensesFlareCheckbox.setChecked(isChecked);
        });

        sliderLensesVisibilityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lensesVisibilitySliderValue = 0.1f + (0.9f * (progress / 90.0f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        okButton.setOnClickListener(v -> {
            glassesVisible = frameCheckbox.isChecked();
            if(!lensesModel.isEmpty()) {
                lensesVisible = lensesCheckbox.isChecked();
                lensesFlareVisible = lensesFlareCheckbox.isChecked();
                if (lensesVisibilitySliderValue == 0) {
                    defaultLensesTransparency = "lensesObject=0.1f";
                    lensesVisibilitySliderValue = 0.1f;
                    adjustLensesTransparency = true;
                } else {
                    defaultLensesTransparency = "lensesObject=" + String.valueOf(lensesVisibilitySliderValue);
                    adjustLensesTransparency = true;
                }
            }
            templeVisible = templeCheckbox.isChecked();
            rightTempleVisible = rightTempleCheckbox.isChecked();
            if(templeParts.length > 1) {
                rightTempleTipVisible = rightTempleTipCheckbox.isChecked();
                leftTempleTipVisible = leftTempleTipCheckbox.isChecked();
            }
            leftTempleVisible = leftTempleCheckbox.isChecked();
            autoHideTemple = autoHideTemplesCheckbox.isChecked();
            checkboxDialog.dismiss();
        });

        checkboxDialog.show();
    }



    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl)
            throws OutOfMemoryError {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    public boolean isBitmapValid(Bitmap bitmap) {
        // Check if the bitmap is not null and has non-zero width and height
        return bitmap != null && bitmap.getWidth() > 0 && bitmap.getHeight() > 0;
    }

    private void detectFaces(InputImage image, Bitmap capturedBitmap) {

        // [START get_detector]
        FaceDetector detector = FaceDetection.getClient(face_detector_options);
        // Or use the default options:
        // FaceDetector detector = FaceDetection.getClient();
        // [END get_detector]

        // [START run_detector]
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        Log.d(TAG, "Face detection successful. Number of faces detected: " + faces.size());
                                        if(faces.size() == 0) {
                                            dashedFaceText.setText("NO FACE DETECTED");
                                            new Handler().postDelayed(() -> {
                                                dashedFaceAreaHide();
                                            }, 1000);
                                            capture_button.completeLoading();
                                        }
                                        // [START_EXCLUDE]
                                        // [START get_face_info]
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            // Use the bounding box information here
//                                            int left = bounds.left;
//                                            int top = bounds.top;
//                                            int right = bounds.right;
//                                            int bottom = bounds.bottom;

                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            /*FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                PointF leftEarPos = leftEar.getPosition();
                                                Log.d(TAG, "Left ear position: " + leftEarPos.toString());
                                            }

                                            // Handle null case for landmarks
                                            if (face.getAllLandmarks() != null) {
                                                List<FaceLandmark> allLandmarks = face.getAllLandmarks();
                                                Log.d(TAG, "Number of landmarks detected: " + allLandmarks.size());
                                                // Process landmarks here
                                            } else {
                                                Log.d(TAG, "No landmarks detected for this face.");
                                            }

                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != null) {
                                                float smileProb = face.getSmilingProbability();
                                                Log.d(TAG, "Smiling probability: " + smileProb);
                                            }
                                            if (face.getRightEyeOpenProbability() != null) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                                Log.d(TAG, "Right eye open probability: " + rightEyeOpenProb);
                                            }*/

                                            // If face tracking was enabled no padding:
//                                            if (face.getTrackingId() != null) {
//                                                int id = face.getTrackingId();
//
//                                                // Clamp bounding box dimensions to the bitmap size
//                                                int left = Math.max(0, bounds.left);
//                                                int top = Math.max(0, bounds.top);
//                                                int right = Math.min(capturedBitmap.getWidth(), bounds.right);
//                                                int bottom = Math.min(capturedBitmap.getHeight(), bounds.bottom);
//
//                                                int width = right - left;
//                                                int height = bottom - top;
//
//                                                // Only create the cropped bitmap if width and height are positive
//                                                if (width > 0 && height > 0) {
//                                                    croppedFaceBitmap = Bitmap.createBitmap(
//                                                            capturedBitmap,
//                                                            left,
//                                                            top,
//                                                            width,
//                                                            height
//                                                    );
//                                                    Log.d(TAG, "Face tracking ID: " + id);
//                                                } else {
//                                                    Log.e(TAG, "Invalid bounding box dimensions for cropping: width=" + width + ", height=" + height);
//                                                }
//                                            }

                                            // With Padding:
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();

                                                // Define padding as a percentage of face dimensions
                                                int padding = (int) (0.1 * Math.min(bounds.width(), bounds.height())); // 10% padding

                                                // Clamp and adjust the bounding box
                                                int left = Math.max(0, bounds.left - padding);
                                                int top = Math.max(0, bounds.top - padding);
                                                int right = Math.min(capturedBitmap.getWidth(), bounds.right + padding);
                                                int bottom = Math.min(capturedBitmap.getHeight(), bounds.bottom + padding);

                                                int width = right - left;
                                                int height = bottom - top;

                                                // Only create the cropped bitmap if width and height are valid
                                                if (width > 0 && height > 0) {
                                                    croppedFaceBitmap = Bitmap.createBitmap(
                                                            capturedBitmap,
                                                            left,
                                                            top,
                                                            width,
                                                            height
                                                    );
                                                    Log.d(TAG, "Face tracking ID: " + id);
                                                } else {
                                                    Log.e(TAG, "Invalid bounding box dimensions for cropping: width=" + width + ", height=" + height);
                                                }
                                            }

                                            analyzeFaceShape(face);
                                        }
                                        // [END get_face_info]
                                        // [END_EXCLUDE]
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.e(TAG, "Face detection failed: " + e.getMessage());
                                        // Handle failure
                                    }
                                });
        // [END run_detector]
    }


    // Method to analyze face shape
    private void analyzeFaceShape(Face face) {
        // Retrieve face contour
        FaceContour faceContour = face.getContour(FaceContour.FACE);
        if (faceContour == null) {
            // Contour not available
            Log.d(TAG, "Face contour not available");
            return;
        }

        // Retrieve additional landmarks
        FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
        FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
        FaceLandmark leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK);
        FaceLandmark rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK);
        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark noseBase = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark leftLipCorner = face.getLandmark(FaceLandmark.MOUTH_LEFT);
        FaceLandmark rightLipCorner = face.getLandmark(FaceLandmark.MOUTH_RIGHT);
        // Add more landmarks as needed

        // Combine face contour points and additional landmarks
        List<PointF> allPoints = new ArrayList<>();
        allPoints.addAll(faceContour.getPoints());
        if (leftEar != null) {
            allPoints.add(leftEar.getPosition());
        }
        if (rightEar != null) {
            allPoints.add(rightEar.getPosition());
        }
        if (leftCheek != null) {
            allPoints.add(leftCheek.getPosition());
        }
        if (rightCheek != null) {
            allPoints.add(rightCheek.getPosition());
        }
        if (leftEye != null) {
            allPoints.add(leftEye.getPosition());
        }
        if (rightEye != null) {
            allPoints.add(rightEye.getPosition());
        }
        if (noseBase != null) {
            allPoints.add(noseBase.getPosition());
        }
        if (leftLipCorner != null) {
            allPoints.add(leftLipCorner.getPosition());
        }
        if (rightLipCorner != null) {
            allPoints.add(rightLipCorner.getPosition());
        }

        // Apply face mask on the custom view
        faceMaskView.setFacePoints(allPoints);

        // Calculate face width and height
        /*float faceWidth = getDistance(allPoints.get(0), allPoints.get(allPoints.size() / 2));
        float faceHeight = getDistance(allPoints.get(0), allPoints.get(allPoints.size() - 1));

        // Infer face shape based on width-to-height ratio and other landmarks
        String faceShape = classifyFaceShape(allPoints, faceWidth, faceHeight);
        String recommendedGlassesType = recommendGlassesType(faceShape);
        face_type_Spinner.setSelection(face_type_adapter.getPosition(faceShape));


        // Display a toast message with the detected face shape
        //Toast.makeText(this, "Recommended glasses type for " + faceShape + " face shape: " + recommendedGlassesType, Toast.LENGTH_SHORT).show();

        // Update glasses based on the selected frame types
        // Check if filteredGlassesObjName is not empty
        if (filteredGlassesObjName.size() > 0) {
            // Check if there's more than one item
            if (filteredGlassesObjName.size() > 1) {
                updateGlassesModel(filteredGlassesObjName.get(1).toString(), filteredTempleObjName.get(1), filteredLensesObjName.get(1), filteredGlassesType.get(1), filteredPadsObjName.get(1));
            } else {
                updateGlassesModel(filteredGlassesObjName.get(0).toString(), filteredTempleObjName.get(0), filteredLensesObjName.get(0), filteredGlassesType.get(0), filteredPadsObjName.get(0));
            }
        } else {
            // Handle the case when there are no items in filteredGlassesObjName
            // For example, you might want to show a default model or a message
            // updateGlassesModel(defaultModel);
            Toast.makeText(this, "No glasses available for the selected frame type.", Toast.LENGTH_SHORT).show();
        }*/

        capture_image_ai = true;

        // Complete any additional actions, such as updating UI elements
        capture_button.completeLoading();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                faceMaskView.clearFaceMask();
            }
        }, 1000);
    }

    // Method to classify face shape
    private String classifyFaceShape(List<PointF> faceContour, float faceWidth, float faceHeight) {
        float ratio = faceWidth / faceHeight;
        float jawWidth = getDistance(faceContour.get(faceContour.size() / 4), faceContour.get(3 * faceContour.size() / 4));

        if (ratio > 0.9 && ratio < 1.1) {
            return "Round";
        } else if (ratio < 0.9) {
            if (jawWidth / faceWidth < 0.8) {
                return "Heart";
            } else {
                return "Oval";
            }
        } else if (ratio > 1.1) {
            if (jawWidth / faceWidth > 0.8) {
                return "Square";
            } else {
                return "Long";
            }
        }
        return "Unknown";
    }

    private String recommendGlassesType(String faceShape) {
        List<String> recommendedFrames = new ArrayList<>();

        switch (faceShape) {
            case "Round":
                recommendedFrames.add("Rectangular frame");
                recommendedFrames.add("Angular frame");
                recommendedFrames.add("Square frame");
                face_shape_ImageView.setImageResource(R.drawable.face_shape_round);
                break;
            case "Heart":
                recommendedFrames.add("Cat-eye frame");
                recommendedFrames.add("Oval frame");
                recommendedFrames.add("Square frame");
                face_shape_ImageView.setImageResource(R.drawable.face_shape_heart);
                break;
            case "Oval":
                recommendedFrames.add("Wayfarer frame");
                recommendedFrames.add("Aviator frame");
                recommendedFrames.add("Browline frame");
                recommendedFrames.add("Square frame");
                face_shape_ImageView.setImageResource(R.drawable.face_shape_oval);
                break;
            case "Square":
                recommendedFrames.add("Round frame");
                recommendedFrames.add("Oval frame");
                recommendedFrames.add("Aviator frame");
                face_shape_ImageView.setImageResource(R.drawable.face_shape_square);
                break;
            case "Long":
                recommendedFrames.add("Wide frame");
                recommendedFrames.add("Oversize frame");
                recommendedFrames.add("Browline frame");
                face_shape_ImageView.setImageResource(R.drawable.face_shape_long);
                break;
            default:
                return "Unknown";
        }

        // Set selected items on the spinner adapter
        frameTypeAdapter.setSelectedItems(recommendedFrames);

        // Filter the glasses based on the selected frame types
        filterGlasses();
        //String response = String.join(" or ", recommendedFrames) + " frames";

        StringBuilder responseBuilder = new StringBuilder();
        for (String frame : recommendedFrames) {
            responseBuilder.append("\u2713 ").append(frame).append("\n");
        }
        String response = responseBuilder.toString().trim();

        face_shape_TextView.setText(faceShape.replace("Long", "Long/Oblong"));
        face_shape_description_TextView.setText(response);
        isRecommendationPopupVisible = true;
        return response;
    }

    // Helper method to calculate distance between two points
    private float getDistance(PointF point1, PointF point2) {
        return (float) Math.sqrt(Math.pow(point1.x - point2.x, 2) + Math.pow(point1.y - point2.y, 2));
    }

    private void showTutorial() {
        TapTargetSequence sequence = new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(findViewById(R.id.glassesCardView), "Select Through Glasses", "Tap to try on different glasses. Long press to see the product.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.showMoreGlassesDescriptionButton), "Glasses Description", "See the glasses in full details, and inquire its availability.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.ai_recommendation_Button), "AI Button", "This button analyzes your face shape and chooses the best frame for you.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.showMoreGlassesButton), "More Options", "This is where you can customize your glasses in real-time: scale, move up and down, and rotation.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.more_settings_button), "Hide Settings", "Show/Hide settings.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.color_picker_button), "Color Palette", "Change the glasses color. Long press to change specific glasses parts color (E.g. Frame, Temple)")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.showGlassesButton), "Eye Button", "Toggle glasses on and off. Long press to hide/show specific glasses parts (E.g. Temples, Temple Left, Temple Right)")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.chatSupportButton), "Chat Support", "Allows you to directly connect with our support team to inquire about product availability, book appointments, and get answers to any questions you might have.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.face_type_Spinner), "Face Type Dropdown", "Filter glasses based on the face type.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.frameTypeSpinner), "Select Frame Types", "Filter glasses by type.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.capture_button), "Capture Button", "This circle of magic captures your moment of glory. Click it, and say 'Cheese!'... or 'I look fabulous!'")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.last_photo_image_view), "Photo Manager", "Lastly, the Photo Manager. View and manage your photos here. Browse through your saved images and delete unwanted photos effortlessly.")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.replay_tutorial_Button), "Replay Tutorial", "Missed out on becoming a glasses guru the first time? Fear not! Hit this button to replay the tutorial and level up your glasses game!")
                                .drawShadow(true)
                                .outerCircleColor(primaryColorBackgroundTransparent)
                                .textColor(primaryOnColor)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true)
                ).listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Save the preference to not show the tutorial again
                        prefManager.setFirstTimeLaunch(false);
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        // Called when each target is clicked
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        prefManager.setFirstTimeLaunch(false);
                    }
                });

        sequence.start();
    }

    private void showWelcomeDialog() {
        View welcomeDialogView = getLayoutInflater().inflate(R.layout.dialog_text_view, null);
        TextView content_TextView = welcomeDialogView.findViewById(R.id.content_TextView);
        ImageView icon_ImageView = welcomeDialogView.findViewById(R.id.icon_ImageView);
        icon_ImageView.setVisibility(View.VISIBLE);

        content_TextView.setText(Html.fromHtml(getString(R.string.welcome_message), Html.FROM_HTML_MODE_LEGACY));

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(welcomeDialogView)
                .setPositiveButton("Begin Tutorial", (dialog, which) -> showTutorial())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    prefManager.setFirstTimeLaunch(false);
                });

        androidx.appcompat.app.AlertDialog aboutDialog = builder.create();
        aboutDialog.show();
    }

    private void showMoreOptionsTutorial() {
        TapTargetSequence sequence = new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(findViewById(R.id.scaleIcon), "Adjust Size", "Resize the glasses to fit perfectly on your face.")
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.yIcon), "Adjust Height", "Move the glasses up or down to align with your face.")
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.rotationIcon), "Rotate", "Rotate the glasses to match the angle of your face.")
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true)
                ).listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Save the preference to not show the tutorial again
                        prefManager.setFirstTimeLaunchMoreOptions(false);
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        // Called when each target is clicked
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        prefManager.setFirstTimeLaunchMoreOptions(false);
                    }
                });

        sequence.start();
    }

    private float getHeadRotationAngle(AugmentedFace augmentedFace) {
        float[] noseTipPos = augmentedFace.getRegionPose(RegionType.NOSE_TIP).getTranslation();
        float[] foreheadLeftPos = augmentedFace.getRegionPose(RegionType.FOREHEAD_LEFT).getTranslation();
        float[] foreheadRightPos = augmentedFace.getRegionPose(RegionType.FOREHEAD_RIGHT).getTranslation();

        // Calculate the angle between the forehead positions and the nose tip
        float dx1 = foreheadLeftPos[0] - noseTipPos[0];
        float dy1 = foreheadLeftPos[1] - noseTipPos[1];
        float dx2 = foreheadRightPos[0] - noseTipPos[0];
        float dy2 = foreheadRightPos[1] - noseTipPos[1];

        float angle1 = (float) Math.toDegrees(Math.atan2(dy1, dx1));
        float angle2 = (float) Math.toDegrees(Math.atan2(dy2, dx2));

        // Take the average of the two angles
        float headRotationAngle = (angle1 + angle2) / 2.0f;

        return headRotationAngle;
    }

    private void triggerRecommendationPopup() {
        isRecommendationPopupVisible = true;
    }

    private void closeRecommendationPopup() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (recommendation_pop_up.getVisibility() == View.VISIBLE) {
                    recommendation_pop_up.setVisibility(View.GONE);
                    isRecommendationPopupVisible = false;
                    recommendationPopUpHandler.removeCallbacks(hideRecommendationPopupRunnable); // Remove any pending hide callbacks
                }
            }
        });
    }

    private void detectFaceShape(Bitmap bitmap) {
        if (faceShapeClassifier != null) {

            String faceShape = faceShapeClassifier.classifyFace(bitmap);
            runOnUiThread(() -> {
                if(!faceShapeClassifier.noToast) {
                    Toast.makeText(CameraFaceActivity.this, faceShape, Toast.LENGTH_LONG).show();
                } else {
                    try {
                        face_type_Spinner.setSelection(face_type_adapter.getPosition(faceShape));
                        capture_button.startLoadingAnimation();
                        new Handler().postDelayed(() -> {
                            if(face_type_Spinner.getSelectedItemPosition() != 0) {
                                if (!filteredGlassesObjName.isEmpty()) {
                                    if (filteredGlassesObjName.size() > 1) {
                                        updateGlassesModel(filteredGlassesObjName.get(1), filteredTempleObjName.get(1), filteredLensesObjName.get(1), filteredGlassesType.get(1), filteredPadsObjName.get(1), filteredTransparency.get(1), filteredIsDownloaded.get(1));
                                    } else {
                                        updateGlassesModel(filteredGlassesObjName.get(0), filteredTempleObjName.get(0), filteredLensesObjName.get(0), filteredGlassesType.get(0), filteredPadsObjName.get(0), filteredTransparency.get(0), filteredIsDownloaded.get(0));
                                    }
                                } else {
                                    Toast.makeText(this, "No glasses available for the selected frame type.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            capture_button.completeLoading();
                        }, 100);

                    } catch (ArrayIndexOutOfBoundsException e) {

                    }
                }
            });
        } else {
            runOnUiThread(() -> Toast.makeText(CameraFaceActivity.this, "FaceShapeClassifier not initialized", Toast.LENGTH_SHORT).show());
        }
    }

    private boolean isFavorite(String glassesId) {
        return prefManager.isFavorite(glassesId);
    }

    private boolean updateTransparency(String objectName, ObjectRenderer object) {
        boolean enableLensFlare = false;
        if(!transparencyObj.isEmpty()) {
            String[] transparencyParts = transparencyObj.split(",");
            for(String part : transparencyParts) {
                if(part.contains(objectName)) {
                    String[] keyValue = part.split("=");
                    float value = Float.parseFloat(keyValue[1].trim());
                    object.setTransparency(value);
                    if(value < 0.5f) {
                        enableLensFlare = true;
                    }
                }
            }
        }

        return enableLensFlare;
    }

    private int fromFloatToIntColor(float[] color) {
        return Color.argb(
                (int)(color[3] * 255),
                (int)(color[0] * 255),
                (int)(color[1] * 255),
                (int)(color[2] * 255)
        );
    }

    private void updateAllMessagesAsSeen(String senderRoom, String currentUserId) {
        DatabaseReference dbReferenceSender = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(senderRoom);
        dbReferenceSender.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    if (message != null && !message.getSenderId().equals(currentUserId)) {
                        String messageId = dataSnapshot.getKey();
                        dbReferenceSender.child(messageId).child("seen").setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    // Successfully updated message as seen
                                })
                                .addOnFailureListener(e -> {
                                    // Failed to update message
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private int countUnseenMessages(List<MessageModel> messages, String senderId) {
        int count = 0;
        for (MessageModel message : messages) {
            if (!message.isSeen() && !message.getSenderId().equals(senderId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onItemClick(int position) {
        currentGlassesPosition = position;
    }

    public void hideLoadingIndicator() {
        runOnUiThread(() -> {
            loadingIndicator.setVisibility(View.GONE);
        });
    }

    public void toggleRecyclerViewFullscreen() {
        fullscreenRecyclerView = !fullscreenRecyclerView;

        if(fullscreenRecyclerView) {
            int heightInDp = 500; // Desired height in dp
            float scale = getResources().getDisplayMetrics().density; // Get display density
            int targetHeight = (int) (heightInDp * scale + 0.5f); // Convert dp to pixels

            // Create a TransitionManager to handle the layout transition
            TransitionManager.beginDelayedTransition((ViewGroup) linearLayout.getParent(), new ChangeBounds());

            // Update the layout parameters
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) linearLayout.getLayoutParams();
            params.height = targetHeight;
            linearLayout.setLayoutParams(params);

            fullscreen_Button.setImageResource(R.drawable.circle_fullscreen_exit_button);

            if(shownMoreSettings) {
                more_settings_layout.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            more_settings_layout.setVisibility(View.GONE);
                            shownMoreSettings = false;
                        });
            }
        } else {
            int heightInDp = 100; // Desired height in dp
            float scale = getResources().getDisplayMetrics().density; // Get display density
            int targetHeight = (int) (heightInDp * scale + 0.5f); // Convert dp to pixels

            // Create a TransitionManager to handle the layout transition
            TransitionManager.beginDelayedTransition((ViewGroup) linearLayout.getParent(), new ChangeBounds());

            // Update the layout parameters
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) linearLayout.getLayoutParams();
            params.height = targetHeight;
            linearLayout.setLayoutParams(params);

            fullscreen_Button.setImageResource(R.drawable.circle_fullscreen_button);

            if(!shownMoreSettings) {
                more_settings_layout.setVisibility(View.VISIBLE);
                shownMoreSettings = true;
                more_settings_layout.setVisibility(View.VISIBLE);
                more_settings_layout.setAlpha(0f);
                more_settings_layout.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .withEndAction(() -> shownMoreSettings = true);
            }
        }
    }

    public Bitmap adjustBrightnessContrast(Bitmap bitmap, float brightness, float contrast) {
        Bitmap adjustedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(adjustedBitmap);
        Paint paint = new Paint();

        ColorMatrix colorMatrix = new ColorMatrix();

        // Adjust contrast
        float scale = contrast; // Contrast factor
        colorMatrix.set(new float[]{
                scale, 0, 0, 0, 0,  // Red channel
                0, scale, 0, 0, 0,  // Green channel
                0, 0, scale, 0, 0,  // Blue channel
                0, 0, 0, 1, 0       // Alpha channel
        });

        // Adjust brightness (add offset to RGB channels)
        float translate = brightness; // Brightness offset
        colorMatrix.postConcat(new ColorMatrix(new float[]{
                1, 0, 0, 0, translate,  // Red channel
                0, 1, 0, 0, translate,  // Green channel
                0, 0, 1, 0, translate,  // Blue channel
                0, 0, 0, 1, 0          // Alpha channel
        }));

        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return adjustedBitmap;
    }
}