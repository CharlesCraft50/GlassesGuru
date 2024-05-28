package com.example.glassesguru;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.ar.core.ArCoreApk;
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
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.core.AugmentedFace.RegionType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

public class CameraFaceActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    private static final String TAG = AugmentedFaceRenderer.class.getSimpleName();
    private boolean capture_image = false;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested;

    private Session session;
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
    private final ObjectRenderer lensesObject = new ObjectRenderer();
    private final ObjectRenderer padsObject = new ObjectRenderer();
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
    private float scaleFactor = 1.0f;
    private boolean shownGlassesOptions = false;
    private boolean glassesVisible = true;
    private boolean templeVisible = true;
    TextView debug_x;
    TextView debug_y;
    TextView debug_z;
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
    GlassesItemCustomAdapter glassesItemCustomAdapter;
    RecyclerView glassesRecyclerView;
    private float glasses_offset_y = 0.0299f;
    private boolean eyesObjectNeedsCreation;
    private int selectedColor = -1;
    private int previousColor;
    private ImageButton colorPickerButton;
    RotateAnimation rotateAnimation;

    ImageView loading_icon_ImageView;
    LinearLayout loading_screen;
    boolean showLoadingScreen = false;
    ConstraintLayout slider_layout;
    CaptureButton capture_button;
    LinearLayout capture_LinearLayout;
    ImageButton ai_recommendation_Button;
    private boolean capture_image_recommendation = false;
    FaceDetectorOptions face_detector_options;
    private FaceMaskView faceMaskView;
    private Spinner frameTypeSpinner;
    private MultiSelectSpinnerAdapter frameTypeAdapter;
    private List<String> frameTypes = Arrays.asList("All", "Rectangular frame", "Angular frame", "Wayfarer frame", "Round frame", "Oval frame", "Sunglasses");

    private Spinner face_type_Spinner;
    private ArrayAdapter<CharSequence> face_type_adapter;
    private ImageButton lastPhotoImageView;
    ImageView capture_image_ImageView;
    private ImageButton replay_tutorial_Button;
    private float rotationAngle = 0.0f;
    private String templeModel = "glasses_1_temple";
    private String lensesModel = "";
    private String glassesType = "";
    private String padsModel = "";
    private float maxScaleFactor = 0.5f;
    private float maxTranslation = 0.05f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_face);

        PrefManager prefManager = new PrefManager(this);

        if (prefManager.isFirstTimeLaunch()) {
            showTutorial();
        }

        replay_tutorial_Button = findViewById(R.id.replay_tutorial_Button);

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

        capture_LinearLayout = findViewById(R.id.capture_LinearLayout);

        slider_layout = (ConstraintLayout) findViewById(R.id.sliderLayout);
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

        rotateAnimation.setDuration(1000); // Duration of the animation in milliseconds
        rotateAnimation.setRepeatCount(Animation.INFINITE); // Repeat the animation infinitely
        rotateAnimation.setInterpolator(new LinearInterpolator());

        initializeGlassesData();

        colorPickerButton = (ImageButton) findViewById(R.id.color_picker_button);

        colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousColor = selectedColor;
                selectedColor = -1;
                eyesObject.clearCustomColor();
                openColorPickerDialog();
            }
        });

        glassesItemCustomAdapter = new GlassesItemCustomAdapter(CameraFaceActivity.this, this, filteredGlassesId, filteredGlassesImage, filteredGlassesTitle, filteredGlassesObjName, filteredTempleObjName, filteredLensesObjName, filteredGlassesFrameType, filteredGlassesType, filteredPadsObjName);

        glassesRecyclerView = (RecyclerView) findViewById(R.id.glassesRecyclerVIew);
        glassesRecyclerView.setAdapter(glassesItemCustomAdapter);
        glassesRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        surfaceView = findViewById(R.id.surface_view_GLSurfaceView);
        displayRotationHelper = new DisplayRotationHelper(this);

        debug_x = findViewById(R.id.debug_x);
        debug_y = findViewById(R.id.debug_y);
        debug_z = findViewById(R.id.debug_z);

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

        ai_recommendation_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        ImageView scaleIcon = (ImageView) findViewById(R.id.scaleIcon);
        scaleIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scaleFactor = 1.0f;
                slider_scale_bar.setProgress((int) ((scaleFactor - 0.5f) * 100));
            }
        });

        ImageView yIcon = (ImageView) findViewById(R.id.yIcon);
        yIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                glasses_offset_y = 0.0200f;
                slider_y_bar.setProgress((int) ((glasses_offset_y + 1) * 100));
            }
        });

        ImageView rotationIcon = (ImageView) findViewById(R.id.rotationIcon);
        rotationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*rotationOnProgress = false;
                rotationAngle = 0.0; // Reset rotation angle*/
                rotationAngle = 360;
                slider_rotation_bar.setProgress(1000);
            }
        });

        ImageButton showGlassesButton = (ImageButton) findViewById(R.id.showGlassesButton);
        showGlassesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleGlassesVisibility();
            }
        });

        ImageButton showTempleButton = (ImageButton) findViewById(R.id.showTempleButton);
        showTempleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTempleVisibility();
            }
        });
    }

    private void toggleGlassesOptions() {
        shownGlassesOptions = !shownGlassesOptions;
        if(shownGlassesOptions) {
            slider_layout.setVisibility(View.VISIBLE);
        } else {
            slider_layout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1) {
            loadLastPhoto();
        }
    }

    private void initializeGlassesData() {
        // Populate the original glasses data lists
        originalGlassesId.add("1");
        originalGlassesImage.add(R.drawable.glasses_1);
        originalGlassesTitle.add("Glasses 1");
        originalGlassesObjName.add("glasses_1_frame.obj");
        originalTempleObjName.add("glasses_1_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");

        originalGlassesId.add("2");
        originalGlassesImage.add(R.drawable.glasses_2);
        originalGlassesTitle.add("Glasses 2");
        originalGlassesObjName.add("glasses_2_frame.obj");
        originalTempleObjName.add("");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Sunglasses, Wayfarer frame");
        originalGlassesType.add("Sunglasses");
        originalPadsObjName.add("");

        originalGlassesId.add("3");
        originalGlassesImage.add(R.drawable.glasses_3);
        originalGlassesTitle.add("Glasses 3");
        originalGlassesObjName.add("glasses_3_frame.obj");
        originalTempleObjName.add("glasses_3_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Round frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");

        originalGlassesId.add("4");
        originalGlassesImage.add(R.drawable.glasses_4);
        originalGlassesTitle.add("Glasses 4");
        originalGlassesObjName.add("glasses_4_frame.obj");
        originalTempleObjName.add("glasses_4_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Round frame, Oval frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");

        originalGlassesId.add("5");
        originalGlassesImage.add(R.drawable.glasses_5);
        originalGlassesTitle.add("Glasses 5");
        originalGlassesObjName.add("glasses_5_frame.obj");
        originalTempleObjName.add("glasses_5_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");

        originalGlassesId.add("6");
        originalGlassesImage.add(R.drawable.glasses_6);
        originalGlassesTitle.add("Glasses 6");
        originalGlassesObjName.add("glasses_6_frame.obj");
        originalTempleObjName.add("glasses_6_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Round frame, Oval frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");

        originalGlassesId.add("7");
        originalGlassesImage.add(R.drawable.glasses_7);
        originalGlassesTitle.add("Glasses 7");
        originalGlassesObjName.add("glasses_7_frame.obj");
        originalTempleObjName.add("glasses_7_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Wayfarer frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");

        originalGlassesId.add("8");
        originalGlassesImage.add(R.drawable.glasses_8);
        originalGlassesTitle.add("Glasses 8");
        originalGlassesObjName.add("glasses_8_frame.obj");
        originalTempleObjName.add("glasses_8_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("glasses_8_pads.obj");

        originalGlassesId.add("9");
        originalGlassesImage.add(R.drawable.glasses_9);
        originalGlassesTitle.add("Glasses 9");
        originalGlassesObjName.add("glasses_9_frame.obj");
        originalTempleObjName.add("glasses_9_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Rectangular frame, Angular frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("glasses_9_pads.obj");

        originalGlassesId.add("10");
        originalGlassesImage.add(R.drawable.glasses_10);
        originalGlassesTitle.add("Glasses 10");
        originalGlassesObjName.add("glasses_10_frame.obj");
        originalTempleObjName.add("glasses_10_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Round frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("glasses_10_pads.obj");

        originalGlassesId.add("11");
        originalGlassesImage.add(R.drawable.glasses_11);
        originalGlassesTitle.add("Glasses 11");
        originalGlassesObjName.add("glasses_11_frame.obj");
        originalTempleObjName.add("glasses_11_temple");
        originalLensesObjName.add("");
        originalGlassesFrameType.add("Round frame, Oval frame");
        originalGlassesType.add("Eyeglasses");
        originalPadsObjName.add("");

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

        if (selectedFrameTypes.contains("All")) {
            // If "All" is selected or no frame type is selected, show all glasses
            filteredGlassesId.addAll(originalGlassesId);
            filteredGlassesImage.addAll(originalGlassesImage);
            filteredGlassesTitle.addAll(originalGlassesTitle);
            filteredGlassesObjName.addAll(originalGlassesObjName);
            filteredTempleObjName.addAll(originalTempleObjName);
            filteredLensesObjName.addAll(originalLensesObjName);
            filteredGlassesFrameType.addAll(originalGlassesFrameType);
            filteredGlassesType.addAll(originalGlassesType);
            filteredPadsObjName.addAll(originalPadsObjName);
        } else if(selectedFrameTypes.contains("Sunglasses")) {

                // Filter the glasses based on the selected frame types
                for (int i = 0; i < originalGlassesId.size(); i++) {
                    if(originalGlassesType.get(i) == "Sunglasses") {
                        boolean isAdded = false;
                        for (String frameType : selectedFrameTypes) {
                            if (originalGlassesFrameType.get(i).contains(frameType)) {
                                filteredGlassesId.add(originalGlassesId.get(i));
                                filteredGlassesImage.add(originalGlassesImage.get(i));
                                filteredGlassesTitle.add(originalGlassesTitle.get(i));
                                filteredGlassesObjName.add(originalGlassesObjName.get(i));
                                filteredTempleObjName.add(originalTempleObjName.get(i));
                                filteredLensesObjName.add(originalLensesObjName.get(i));
                                filteredGlassesFrameType.add(originalGlassesFrameType.get(i));
                                filteredGlassesType.add(originalGlassesType.get(i));
                                filteredPadsObjName.add(originalPadsObjName.get(i));
                                isAdded = true;
                                break;
                            }
                        }
                        // If no glasses match, filtered lists will remain empty
                        if (!isAdded && selectedFrameTypes.isEmpty()) {
                            filteredGlassesId.clear();
                            filteredGlassesImage.clear();
                            filteredGlassesTitle.clear();
                            filteredGlassesObjName.clear();
                            filteredTempleObjName.clear();
                            filteredLensesObjName.clear();
                            filteredGlassesFrameType.clear();
                            filteredGlassesType.clear();
                            filteredPadsObjName.clear();
                        }
                    }
            }
        } else {
            // Filter the glasses based on the selected frame types
            for (int i = 0; i < originalGlassesId.size(); i++) {
                boolean isAdded = false;
                for (String frameType : selectedFrameTypes) {
                    if (originalGlassesFrameType.get(i).contains(frameType)) {
                        filteredGlassesId.add(originalGlassesId.get(i));
                        filteredGlassesImage.add(originalGlassesImage.get(i));
                        filteredGlassesTitle.add(originalGlassesTitle.get(i));
                        filteredGlassesObjName.add(originalGlassesObjName.get(i));
                        filteredTempleObjName.add(originalTempleObjName.get(i));
                        filteredLensesObjName.add(originalLensesObjName.get(i));
                        filteredGlassesFrameType.add(originalGlassesFrameType.get(i));
                        filteredGlassesType.add(originalGlassesType.get(i));
                        filteredPadsObjName.add(originalPadsObjName.get(i));
                        isAdded = true;
                        break;
                    }
                }
                // If no glasses match, filtered lists will remain empty
                if (!isAdded && selectedFrameTypes.isEmpty()) {
                    filteredGlassesId.clear();
                    filteredGlassesImage.clear();
                    filteredGlassesTitle.clear();
                    filteredGlassesObjName.clear();
                    filteredTempleObjName.clear();
                    filteredLensesObjName.clear();
                    filteredGlassesFrameType.clear();
                    filteredGlassesType.clear();
                    filteredPadsObjName.clear();
                }
            }
        }

        // Notify the adapter of the changes
        glassesItemCustomAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        if(session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

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

            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
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
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
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
            eyesObject.createOnGlThread(this, "models/glasses/" + glassesModel, "models/glasses/albedo.png");
            eyesObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            eyesObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

            rightTempleObject.createOnGlThread(this, "models/glasses/glasses_1_temple_right.obj", "models/glasses/albedo.png");
            rightTempleObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            rightTempleObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

            leftTempleObject.createOnGlThread(this, "models/glasses/glasses_1_temple_left.obj", "models/glasses/albedo.png");
            leftTempleObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            leftTempleObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);



        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

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

                    detectFaces(image);

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
                        eyesObject.createOnGlThread(this, "models/glasses/" + glassesModel, "models/glasses/albedo.png");
                        if(templeModel != "") {
                            rightTempleObject.createOnGlThread(this, "models/glasses/" + templeModel + "_right.obj", "models/glasses/albedo.png");
                            leftTempleObject.createOnGlThread(this, "models/glasses/" + templeModel + "_left.obj", "models/glasses/albedo.png");
                        }

                        if(padsModel != "") {
                            padsObject.createOnGlThread(this, "models/glasses/" + padsModel, "models/glasses/albedo_2.png");
                            padsObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
                            padsObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
                        }

                        eyesObjectNeedsCreation = false;
                        toggleLoadingScreen();
                        showLoadingScreen = false;
                        loading_screen.setVisibility(View.GONE);
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

                    if (padsModel != "") {
                        padsObject.updateModelMatrix(eyesMatrix, scaleFactor);
                        padsObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                    }

                    // For the Glasses Hinges

                    if (templeVisible) {
                        if (!templeModel.isEmpty()) {

                            // Calculate the scaling factor for the right temple
                            float rightTempleScaleFactor = 1.0f;
                            float rightTempleOffsetFactor = 0.0f;
                            if (headRotationAngle > 0 && headRotationAngle <= 84) {
                                rightTempleScaleFactor = 1.0f - (headRotationAngle / 170f); // Scale down based on head rotation
                                if (headRotationAngle <= 90f) {
                                    if (templeModel != "glasses_3_temple") {
                                        rightTempleOffsetFactor = glasses_offset_y;
                                    }
                                    rightTempleScaleFactor = 0.0f;
                                }
                            }

                            rightTempleObject.updateModelMatrix(eyesMatrix, scaleFactor);
                            // Scale the right temple based on the head rotation angle
                            rightTempleObject.adjustTempleTransform(rightTempleScaleFactor, rightTempleOffsetFactor);
                            rightTempleObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);

                            // Calculate the scaling factor for the left temple
                            float leftTempleScaleFactor = 1.0f;
                            float leftTempleOffsetFactor = 0.0f;
                            if (headRotationAngle >= 93) {
                                leftTempleScaleFactor = 1.0f - ((headRotationAngle - 100) / 20f); // Scale down based on head rotation
                                if (templeModel != "glasses_3_temple") {
                                    leftTempleOffsetFactor = glasses_offset_y;
                                }
                                leftTempleScaleFactor = 0.0f;
                            }

                            leftTempleObject.updateModelMatrix(eyesMatrix, scaleFactor);
                            // Scale the left temple based on the head rotation angle
                            leftTempleObject.adjustTempleTransform(leftTempleScaleFactor, leftTempleOffsetFactor);
                            leftTempleObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
                        }
                    }
                }
            }

            if(capture_image) {

                Bitmap capturedBitmap = createBitmapFromGLSurface(0, 0, surfaceView.getWidth(), surfaceView.getHeight(), gl);
                if (capturedBitmap != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            capture_image_ImageView.setImageBitmap(capturedBitmap);
                            lastPhotoImageView.setImageBitmap(capturedBitmap);
                        }
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
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        } finally {
            GLES20.glDepthMask(true);
        }
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
            Toast.makeText(this, "No photos found", Toast.LENGTH_SHORT).show();
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
            debug_x.setText("scaleFactor: " + scaleFactor);
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
    }

    private void toggleTempleVisibility() {
        templeVisible = !templeVisible;
    }

    public void updateGlassesModel(String newModel, String newHinges, String newLenses, String newGlassesType, String newPads) {
        glassesModel = newModel;
        templeModel = newHinges;
        lensesModel = newLenses;
        glassesType = newGlassesType;
        padsModel = newPads;
        // Set the flag to indicate that eyesObject needs to be created
        eyesObjectNeedsCreation = true;
    }

    private void openColorPickerDialog() {
        AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(this, previousColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                selectedColor = color;
                float[] customColor = new float[]{Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f};
                // Set the custom color directly without further processing
                eyesObject.setCustomColor(customColor);
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }
        });

        colorPickerDialog.show();
    }

    public void toggleLoadingScreen() {
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

    private void detectFaces(InputImage image) {

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
                                            Toast.makeText(CameraFaceActivity.this, "No face detected!", Toast.LENGTH_SHORT).show();
                                            capture_button.completeLoading();
                                        }
                                        // [START_EXCLUDE]
                                        // [START get_face_info]
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            // Use the bounding box information here
                                            int left = bounds.left;
                                            int top = bounds.top;
                                            int right = bounds.right;
                                            int bottom = bounds.bottom;

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

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                                Log.d(TAG, "Face tracking ID: " + id);
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
        float faceWidth = getDistance(allPoints.get(0), allPoints.get(allPoints.size() / 2));
        float faceHeight = getDistance(allPoints.get(0), allPoints.get(allPoints.size() - 1));

        // Infer face shape based on width-to-height ratio and other landmarks
        String faceShape = classifyFaceShape(allPoints, faceWidth, faceHeight);
        String recommendedGlassesType = recommendGlassesType(faceShape);
        face_type_Spinner.setSelection(face_type_adapter.getPosition(faceShape));

        // Display a toast message with the detected face shape
        Toast.makeText(this, "Recommended glasses type for " + faceShape + " face shape: " + recommendedGlassesType, Toast.LENGTH_SHORT).show();

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
        }
        toggleLoadingScreen();

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
                break;
            case "Heart":
                recommendedFrames.add("Cat-eye frame");
                recommendedFrames.add("Oval frame");
                break;
            case "Oval":
                recommendedFrames.add("Wayfarer frame");
                recommendedFrames.add("Aviator frame");
                break;
            case "Square":
                recommendedFrames.add("Round frame");
                recommendedFrames.add("Oval frame");
                break;
            case "Long":
                recommendedFrames.add("Wide frame");
                recommendedFrames.add("Oversized frame");
                break;
            default:
                return "Unknown";
        }

        // Set selected items on the spinner adapter
        frameTypeAdapter.setSelectedItems(recommendedFrames);

        // Filter the glasses based on the selected frame types
        filterGlasses();

        return String.join(" or ", recommendedFrames) + " frames";
    }

    // Helper method to calculate distance between two points
    private float getDistance(PointF point1, PointF point2) {
        return (float) Math.sqrt(Math.pow(point1.x - point2.x, 2) + Math.pow(point1.y - point2.y, 2));
    }

    private void showTutorial() {
        TapTargetSequence sequence = new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(findViewById(R.id.ai_recommendation_Button), "AI Button", "This button analyzes your face shape and chooses the best frame for you.")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.showMoreGlassesButton), "More Options", "This is where you can customize your glasses in real-time: scale, move up and down, and rotation.")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.showGlassesButton), "Eye Button", "Toggle glasses on and off.")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.color_picker_button), "Color Palette", "Change the glasses color.")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.face_type_Spinner), "Face Type Dropdown", "Filter glasses based on the face type.")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.frameTypeSpinner), "Select Frame Types", "Filter glasses by type.")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.capture_button), "Capture Button", "This circle of magic captures your moment of glory. Click it, and say 'Cheese!'... or 'I look fabulous!'")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.last_photo_image_view), "Photo Manager", "Lastly, the Photo Manager. View and manage your photos here. Browse through your saved images and delete unwanted photos effortlessly.")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.replay_tutorial_Button), "Replay Tutorial", "Missed out on becoming a glasses guru the first time? Fear not! Hit this button to replay the tutorial and level up your glasses game!")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true)
                ).listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Save the preference to not show the tutorial again
                        PrefManager prefManager = new PrefManager(CameraFaceActivity.this);
                        prefManager.setFirstTimeLaunch(false);
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        // Called when each target is clicked
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        // Called when the sequence is canceled
                    }
                });

        sequence.start();
    }

    private void showMoreOptionsTutorial() {
        TapTargetSequence sequence = new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(findViewById(R.id.scaleIcon), "Reset Buttons: Scale", "You can reset the sliders to default for each buttons here!")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.yIcon), "Reset Buttons: Move up and down", "You can reset the sliders to default for each buttons here!")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true),
                        TapTarget.forView(findViewById(R.id.rotationIcon), "Reset Buttons: Rotation", "You can reset the sliders to default for each buttons here!")
                                .cancelable(false)
                                .drawShadow(true)
                                .outerCircleColor(R.color.gray_blue_semi_transparent)
                                .tintTarget(true)
                                .transparentTarget(true)
                                .cancelable(true)
                ).listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        // Save the preference to not show the tutorial again
                        PrefManager prefManager = new PrefManager(CameraFaceActivity.this);
                        prefManager.setFirstTimeLaunchMoreOptions(false);
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        // Called when each target is clicked
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        // Called when the sequence is canceled
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
}