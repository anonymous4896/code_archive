package hunter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Constant {
    public static final int DEFAULT_THREAD_NUM = 8;

    public static final String WORK_MODE_CODE = "code";
    public static final String WORK_MODE_CONTROL = "control";

    public static final String DEFAULT_MODEL_SETTING = "model.csv";
    public static final String DEFAULT_SINK_SETTING = "sinks.csv";

    public static final String SPLITTER = "/|\\\\"; //for both linux and windows

    public static final int EDGE_TYPE_FORWARD = 0;
    public static final int EDGE_TYPE_BACKWARD = 1;
    public static final int EDGE_TYPE_CALL_FORWARD = 2;
    public static final int EDGE_TYPE_CALL_BACKWARD = 3;
    public static final int EDGE_TYPE_RETURN_FORWARD = 4;
    public static final int EDGE_TYPE_RETURN_BACKWARD = 5;
    public static final int EDGE_TYPE_GLOBAL_FORWARD = 6;
    public static final int EDGE_TYPE_GLOBAL_BACKWARD = 7;

    public static final String[] EDGE_LABEL = {"NormalForward", "NormalBackward", "CallForward", "CallBackward", "ReturnForward", "ReturnBackward", "GlobalForward", "GlobalBackward"};

    public static final String EDGE_LABEL_FORWARD = "NormalForward";
    public static final String EDGE_LABEL_BACKWARD = "NormalBackward";
    public static final String EDGE_LABEL_CALL_FORWARD = "CallForward";
    public static final String EDGE_LABEL_CALL_BACKWARD = "CallBackward";
    public static final String EDGE_LABEL_RETURN_FORWARD = "ReturnForward";
    public static final String EDGE_LABEL_RETURN_BACKWARD = "ReturnBackward";
    public static final String EDGE_LABEL_GLOBAL_FORWARD = "GlobalForward";
    public static final String EDGE_LABEL_GLOBAL_BACKWARD = "GlobalBackward";

    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String CONSTANT = "constant";
    public static final String OP = "op";
    public static final String ASSIGN = "assign";
    public static final String ARRAY_INDEX = "array_index";

    public static final int DIRECTION_FORWARD = 0;
    public static final int DIRECTION_BACKWARD = 1;

    public static final String separator = "##########";


    public static final String bitmapFieldName = "picture";

    public static HashSet<String> models = new HashSet<>();

    public static ArrayList<String> inputs = new ArrayList<>();
    public static ArrayList<String> outputs = new ArrayList<>();


    public static HashMap<String, List<Integer>> specialMethods4Values = new HashMap<String, List<Integer>>() {
        {
            put("<android.graphics.Bitmap: void getPixels(int[],int,int,int,int,int,int)>", Arrays.asList(0));
            put("<android.graphics.Bitmap: void setPixels(int[],int,int,int,int,int,int)>", Arrays.asList()); // Arrays.asList(-1)
            // remove the -1 for the stop purpose
            put("<android.graphics.Bitmap: void recycle()>", Arrays.asList(-1));
            put("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>", Arrays.asList(2));
            put("<java.util.concurrent.ConcurrentHashMap: java.lang.Object put(java.lang.Object,java.lang.Object)"
                    + ">", Arrays.asList(-1));
            put("<kotlin.jvm.internal.Intrinsics: void checkParameterIsNotNull(java.lang.Object,java.lang.String)"
                    + ">", Arrays.asList());
            put("<org.opencv.imgproc.Imgproc: void resize(org.opencv.core.Mat,org.opencv.core.Mat,org.opencv.core"
                    + ".Size)>", Arrays.asList(1));
            put("<java.nio.ByteBuffer: java.nio.Buffer rewind()>", Arrays.asList(-1));
            put("<android.graphics.Bitmap: boolean compress(android.graphics.Bitmap$CompressFormat,int,java.io.OutputStream)>",
                    Arrays.asList(2));
            put("<android.util.LruCache: java.lang.Object put(java.lang.Object,java.lang.Object)>", Arrays.asList(-1));
            put("<java.util.List: boolean add(java.lang.Object)>", Arrays.asList(-1));
            put("<android.graphics.BitmapFactory: android.graphics.Bitmap decodeFile(java.lang.String,android.graphics.BitmapFactory$Options)>",
                    Arrays.asList(1));
            put("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>", Arrays.asList(-1));
            put("<java.nio.ByteBuffer: java.nio.ByteBuffer putFloat(float)>", Arrays.asList(-1));
            put("<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>", Arrays.asList(-1));
        }
    };

    public static ArrayList<String> specialMethods2Skip = new ArrayList<>(Arrays.asList("org.tensorflow.", "java.",
            "javax.", "android.", "kotlin.", "kotlinx.", "androidx.", "org.opencv.", "com.google.", "com.amazonaws.",
            "com.facebook", "com.duapps.ad.", "com.j256.", "com.crashlytics.", "de.test.", "soot.",
            "libcore.icu.", "securibench.", "sun.", "org.apache.", "org.eclipse.", "dalvik.", "com.squareup.",
            "com.mopub.", "okhttp3.", "org.json.", "com.xiaomi.push.", "com.googlecode.mp4parser.", "com.airbnb.", "org.joda.", "com.baidu.android.", "io.fabric.sdk.",
            "android.util.Log", "io.realm", "com.applovin.", "android.graphics.Canvas"));

    public static ArrayList<String> specialMethodPrefixesNotSkip = new ArrayList<>(Arrays.asList("org.tensorflow.detect", "com.google.android.gms.vision.face.",
            "org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel", "org.tensorflow.demo.TensorFlowImageListener", "org.tensorflow.yolo.TensorFlowImageRecognizer",
            "org.tensorflow.demo.TensorFlowObjectDetectionAPIModel", "org.tensorflow.yolo.model."));

    public static ArrayList<String> specialMethodsNotSkip = new ArrayList<>(Arrays.asList("org.tensorflow.demo.Classifier"));

    public static ArrayList<String> specialTypeNotFilter = new ArrayList<>(Arrays.asList(
            "org.json.JSONObject", "org.json.JSONArray",
            "java.util.", "java.nio.", "java.io.", "java.lang.", "kotlin.collections.",
            "android.os.Parcel", "android.os.Bundle", "android.util.Pair",
            "android.content.res.AssetFileDescriptor",
            "android.graphics.",
            "org.tensorflow.demo.Classifier",
            "android.content.res.AssetManager", "android.os.Environment", "android.util.Size", "android.util.Base64",
            "org.tensorflow.lite.support.image.", "org.tensorflow.lite.support.tensorbuffer.", "org.tensorflow.lite.DataType"
    ));

    public static ArrayList<String> specialType2Filter = new ArrayList<>(Arrays.asList("java.util.concurrent",
            "java.util.concurrent.atomic", "java.util.concurrent.locks", "java.util.function", "java.util.jar",
            "java.util.logging", "java.util.prefs", "java.util.regex", "java.util.spi", "java.util.stream", "java.util.zip",
            "java.lang.annotation", "java.lang.instrument", "java.lang.invoke", "java.lang.management", "java.lang.ref.",
            "java.lang.Throwable", "java.lang.StackTraceElement", "java.lang.reflect.AccessibleObject",
            "java.lang.reflect.AnnotatedElement", "java.lang.reflect.Constructor", "java.lang.reflect.Field",
            "java.lang.reflect.GenericArrayType", "java.lang.reflect.GenericDeclaration",
            "java.lang.reflect.GenericSignatureFormatError", "java.lang.reflect.InvocationHandler",
            "java.lang.reflect.InvocationTargetException", "java.lang.reflect.MalformedParameterizedTypeException",
            "java.lang.reflect.Member", "java.lang.reflect.Method", "java.lang.reflect.Modifier",
            "java.lang.reflect.ParameterizedType", "java.lang.reflect.Parameter", "java.lang.reflect.Proxy",
            "java.lang.reflect.ReflectPermission", "java.lang.reflect.Type", "java.lang.reflect.TypeVariable",
            "java.lang.reflect.UndeclaredThrowableException", "java.lang.reflect.WildcardType", "android.graphics.RectF", "android.graphics.Rect",
            "org.tensorflow.detect.tracking.ObjectTracker.TrackedObject", "org.tensorflow.detect.TensorFlowObjectDetectionAPIModel.1",
            "java.lang.StackTraceElement", "android.graphics.Paint", "java.lang.Thread", "java.lang.Comparable",
            "java.util.Iterator", "org.tensorflow.Graph", "org.tensorflow.Operation", "android.net.Uri", "java.io.InputStream","java.io.FileOutputStream",
            "org.apache.commons.math3.", "java.io.InputStreamReader", "android.graphics.Point",
            "android.graphics.Canvas", "android.graphics.Path", "android.graphics.Region"//for wallpaper
    ));

    public static HashMap<String, List<Integer>> backwardEndPointValues2Remove = new HashMap<String, List<Integer>>() {
        {
            put("<android.graphics.Bitmap: void getPixels(int[],int,int,int,int,int,int)>", Arrays.asList(-1));
            put("<android.graphics.Bitmap: android.graphics.Bitmap createScaledBitmap(android.graphics.Bitmap,int,int,boolean)>", Arrays.asList(0));
            put("<android.graphics.Bitmap: android.graphics.Bitmap createBitmap(int,int,android.graphics.Bitmap$Config)>", Arrays.asList(-2, 0, 1, 2));
        }
    };

    public static HashMap<String, List<Integer>> forwardEndPointValues2Remove = new HashMap<String, List<Integer>>() {
        {
            put("<android.graphics.Bitmap: void setPixels(int[],int,int,int,int,int,int)>", Arrays.asList()); // Arrays.asList(-1)
            put("<android.graphics.Bitmap: void setPixel(int,int,int)>", Arrays.asList()); // Arrays.asList(-1)
            put("<android.graphics.Bitmap: android.graphics.Bitmap createBitmap(int[],int,int,android.graphics.Bitmap$Config)>", Arrays.asList());

        }
    };

    public static HashMap<String, List<String>> src2FeedStmts = new HashMap<String, List<String>>() {
        {
            put("picture",
                    Arrays.asList("java.io.InputStream is = getAssets().open(\"liu.jpg\")",
                            "picture = android.graphics.BitmapFactory.decodeStream(is)"));
        }
    };

    public static HashMap<String, List<String>> frameworkInit = new HashMap<String, List<String>>() {
        {
            put("org.tensorflow.contrib.android.TensorFlowInferenceInterface",
                    Arrays.asList("java.io.InputStream model_is = context.getAssets().open(\"%MODEL_NAME%\")",
                            "tf0 = new org.tensorflow.contrib.android.TensorFlowInferenceInterface(model_is)"));
            put("org.tensorflow.lite.Interpreter",
                    Arrays.asList("android.content.res.AssetManager model_am = context.getAssets()",
                            "java.nio.MappedByteBuffer modelBuffer = Utils.getModelBuffer(\"%MODEL_NAME%\", model_am)",
                            "tflite0 = new org.tensorflow.lite.Interpreter(modelBuffer, new org.tensorflow.lite.Interpreter.Options())"));
            put("com.tencent.mobilenetssdncnn.MobilenetSSDNcnn",
                    Arrays.asList("MobilenetSSDNcnn0 = new com.tencent.mobilenetssdncnn.MobilenetSSDNcnn()",
                            "boolean ret_init = MobilenetSSDNcnn0.Init(context.getAssets())"));
            put("com.tencent.yolov5ncnn.YoloV5Ncnn",
                    Arrays.asList("YoloV5Ncnn0 = new com.tencent.yolov5ncnn.YoloV5Ncnn()",
                            "boolean ret_init = YoloV5Ncnn0.Init(context.getAssets())"));
            put("org.tensorflow.demo.TensorFlowClassifier",
                    Arrays.asList("tfClassifier0 = new org.tensorflow.demo.TensorFlowClassifier()",
                            "tfClassifier0.initializeTensorFlow(context.getAssets(), \"file:///android_asset/android_graph.pb\", \"file:///android_asset/label_strings.txt\", 1470, 448, 128, 128, \"Placeholder\", \"19_fc\")"));
        }
    };


    public static HashMap<String, String> resourcesMethod2Value = new HashMap<String, String>() {
        {
            put("<android.content.res.Resources: int getIdentifier(java.lang.String,java.lang.String,java.lang.String)>", "0");
            put("<android.content.res.Resources: int getDimensionPixelSize(int)>", "48");
            put("<android.content.res.Resources: java.lang.String getString(int)>", "\"\"");
        }
    };

    public static HashSet<String> modelSuffix = new HashSet<>(Arrays.asList(".tflite", ".pb"));


    /**
     * Method signature block list for **Translator**
     * Translator will not translate these blocked methods in slice results
     *
     * Usages:
     * 1. translate unit: InvokeExpr & InvokeStmt
     * 2. translate head methods
     * 3. translate methods
     * 4. _isStdSink
     */
    public static HashSet<String> methodBlockList = new HashSet<>(Arrays.asList(
            "<org.opencv.utils.Converters: org.opencv.core.Mat vector_float_to_Mat(java.util.List)>",
            "<com.facebook.react.bridge.Promise: void resolve(java.lang.Object)>",
            "<android.graphics.BitmapFactory: android.graphics.Bitmap decodeFile(java.lang.String,android.graphics.BitmapFactory$Options)>",
            "<org.tensorflow.Graph: org.tensorflow.Operation operation(java.lang.String)>",
            "<org.tensorflow.detect.TensorFlowYoloDetector: java.util.List recognizeImage(android.graphics.Bitmap)>",
            "<org.tensorflow.detect.TensorFlowMultiBoxDetector: java.util.List recognizeImage(android.graphics.Bitmap)>",
            "<org.tensorflow.detect.TensorFlowImageClassifier: java.util.List recognizeImage(android.graphics.Bitmap)>",
            "<org.tensorflow.detect.Classifier$Recognition: java.lang.String toString()>",
            "<org.tensorflow.detect.env.Logger: void <init>(java.lang.Class)>",
            "<org.tensorflow.detect.tracking.MultiBoxTracker: void handleDetection(byte[],long,android.util.Pair)>",
            "<org.tensorflow.detect.env.Logger: java.lang.String getCallerSimpleName()>",
            "<org.tensorflow.detect.TensorFlowMultiBoxDetector$1: int compare(org.tensorflow.detect.Classifier$Recognition,org.tensorflow.detect.Classifier$Recognition)>",
            "<android.graphics.Paint: void setColor(int)>",
            "<org.tensorflow.Graph: org.tensorflow.Operation a(java.lang.String)>",
            "<org.tensorflow.contrib.android.a: void <init>(android.content.res.AssetManager,java.lang.String)>",
            "<java.lang.Thread: java.lang.String getName()>",
//            "<java.util.Iterator: boolean hasNext()>",
            "<java.lang.StackTraceElement: java.lang.String getClassName()>",
            "<org.tensorflow.lite.examples.detection.tflite.Classifier$Recognition: void <init>(java.lang.String,java.lang.String,java.lang.Float,android.graphics.RectF)>",
            "<android.graphics.BitmapFactory: android.graphics.Bitmap decodeStream(java.io.InputStream,android.graphics.Rect,android.graphics.BitmapFactory$Options)>",
            "<org.tensorflow.demo.env.Logger: void v(java.lang.String,java.lang.Object[])>",
            "<org.tensorflow.demo.BoundingBoxView: void setResults(java.util.List)>",
            "<org.tensorflow.demo.RecognitionScoreView: void setResults(java.util.List)>",
            "<org.tensorflow.lite.examples.detection.tflite.Detector$Recognition: void <init>(java.lang.String,java.lang.String,java.lang.Float,android.graphics.RectF)>",
            "<android.graphics.Canvas: void drawBitmap(android.graphics.Bitmap,android.graphics.Rect,android.graphics.Rect,android.graphics.Paint)>"
    ));


    /**
     * Method name block list for **Translator**
     * Translator will not translate these blocked methods in slice results
     *
     * Usages:
     * 1. translate unit: InvokeExpr & InvokeStmt
     * 2. translate head methods
     * 3. translate methods
     */
    public static HashSet<String> methodNameBlockList = new HashSet<>(Arrays.asList(
            "hashCode", "equals", "toString", "iterator"
    ));

    public static HashSet<String> methodSpecialList = new HashSet<>(Arrays.asList(
            "<org.tensorflow.detect.Classifier: java.util.List recognizeImage(android.graphics.Bitmap)>"
    ));

    public static HashMap<String, String> knownObfuscatedMethods = new HashMap<>(){
        {
            put("<org.tensorflow.lite.Tensor: int[] d()>", "shape");
        }
    };
}
