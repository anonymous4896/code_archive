package hunter.Postprocess.ControlGenerate;

import com.fasterxml.jackson.databind.ObjectMapper;
import hunter.Track.TrackInfo;
import hunter.Track.Tracker;
import soot.Value;
import soot.jimple.*;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

public class Generator {
    // inputs
    private boolean verbose;
    private TrackInfo info;

    // outputs
    public Model control;

    // variables
    private static int MAX_DIMENSION = 4;
    private static int MIN_SIZE = 16;

    public Generator(Tracker tracker, boolean verbose) {
        // inputs
        this.verbose = verbose;
        this.info = tracker.info;

        // outputs
        this.control = new Model();

        // variables
    }

    public void go() {
        control.appName = info.getApkName();
        control.modelName = info.getModelName();
        control.frameworkName = info.getFramework();

        TrackInfo.Layer mainInput = info.getInputLayers().get(0);
        if (info.getFramework().equals("tf")) {
            mainInput = info.getInputLayers().get(info.getInputLayers().size() - 1);
        }

        if (info.getFramework().equals("tf")) {
            control.processConfig.pixelType = mainInput.dataType;
        } else if (info.getFramework().equals("tflite")) {
            for (Value v : mainInput.dataOps) {
                if (v instanceof InvokeExpr) {
                    String methodName = ((InvokeExpr) v).getMethod().getName();
                    if (methodName.startsWith("put")) {
                        if (methodName.endsWith("Float")) {
                            control.processConfig.pixelType = "float";
                        } else if (methodName.endsWith("put")) {
                            control.processConfig.pixelType = "uint8";
                        }
                    }
                }
            }
        }

        for (Value v : mainInput.nameValues) {
            if (v instanceof StringConstant) {
                control.inputNames = new String[]{((StringConstant) v).value};
                break;
            }
        }

        int channel = 3;
        int width = 0;
        for (Value v : mainInput.shapeValues) {
            if (v instanceof IntConstant) {
                channel = ((IntConstant) v).value;
                break;
            } else if (v instanceof LongConstant) {
                channel = (int) ((LongConstant) v).value;
                break;
            }
        }
        for (Value v : mainInput.shapeValues) {
            if (v instanceof IntConstant && ((IntConstant) v).value > MIN_SIZE) {
                width = ((IntConstant) v).value;
            }
        }
        if (width == 0) {
            HashSet<Integer> constantsInOp = new HashSet<>();
            for (Value v : mainInput.dataOps) {
                if (v instanceof BinopExpr) {
                    Value rop = ((BinopExpr) v).getOp2();
                    if (rop instanceof IntConstant) {
                        constantsInOp.add(((IntConstant) rop).value);
                    }
                }
            }
            for (Value v : mainInput.dataValues) {
                if (v instanceof IntConstant && ((IntConstant) v).value > MIN_SIZE && !constantsInOp.contains(((IntConstant) v).value)) {
                    width = ((IntConstant) v).value > width ? ((IntConstant) v).value : width;
                }
            }
        }

        int inputDimension = 3;
        for (Value v : mainInput.indexValues) {
            if (v instanceof IntConstant) {
                if (((IntConstant) v).value + 1 > inputDimension && ((IntConstant) v).value + 1 <= MAX_DIMENSION) {
                    inputDimension = ((IntConstant) v).value + 1;
                }
            }
        }

        control.processConfig.reshape = new int[inputDimension];
        for (int i = 0; i < inputDimension; ++i) {
            control.processConfig.reshape[i] = 1;
        }

        if (inputDimension == 3) {
            control.processConfig.reshape[0] = width;
            control.processConfig.reshape[1] = width;
            control.processConfig.reshape[2] = channel;
        } else if (inputDimension == 4) {
            control.processConfig.reshape[1] = width;
            control.processConfig.reshape[2] = width;
            control.processConfig.reshape[3] = channel;
        }

        String rgbMode = "";
        boolean meetPixel = false;
        for (Value v : mainInput.dataOps) {
            if (v instanceof BinopExpr) {
                String symbol = ((BinopExpr) v).getSymbol();
                Value rop = ((BinopExpr) v).getOp2();
                if (symbol.equals(" & ")) {
                    if (rop instanceof IntConstant && ((IntConstant) rop).value == 255) {
                        if (meetPixel) {
                            rgbMode = 'R' + rgbMode;
                        } else {
                            meetPixel = true;
                        }
                    }
                } else if (symbol.equals(" >> ")) {
                    if (rop instanceof IntConstant) {
                        if (((IntConstant) rop).value == 8 && meetPixel) {
                            rgbMode = 'G' + rgbMode;
                            meetPixel = false;
                        } else if (((IntConstant) rop).value == 16 && meetPixel) {
                            rgbMode = 'B' + rgbMode;
                            meetPixel = false;
                        }
                    }
                }
            }

            if (rgbMode.length() == 3) {
                break;
            }
        }
        control.processConfig.colorMode = rgbMode;

        boolean hasSub = false;
        boolean hasDiv = false;
        for (Value v : mainInput.dataOps) {
            if (v instanceof BinopExpr) {
                String symbol = ((BinopExpr) v).getSymbol();
                Value rop = ((BinopExpr) v).getOp2();
                if (symbol.equals(" / ")) {
                    hasDiv = true;
                    if (rop instanceof FloatConstant && ((FloatConstant) rop).value < 256.0) {
                        control.processConfig.div = ((FloatConstant) rop).value;
                    } else if (rop instanceof DoubleConstant && ((DoubleConstant) rop).value < 256.0) {
                        control.processConfig.div = ((DoubleConstant) rop).value;
                    } else if (rop instanceof IntConstant && ((IntConstant) rop).value < 256) {
                        control.processConfig.div = ((IntConstant) rop).value;
                    } else if (rop instanceof LongConstant && ((LongConstant) rop).value < 256) {
                        control.processConfig.div = ((LongConstant) rop).value;
                    }
                    if (control.processConfig.sub > 0.0000000001) {
                        control.processConfig.reverse = true;
                    }
                } else if (symbol.equals(" - ")) {
                    hasSub = true;
                    if (rop instanceof FloatConstant && ((FloatConstant) rop).value < 256.0) {
                        control.processConfig.sub = ((FloatConstant) rop).value;
                    } else if (rop instanceof DoubleConstant && ((DoubleConstant) rop).value < 256.0) {
                        control.processConfig.sub = ((DoubleConstant) rop).value;
                    } else if (rop instanceof IntConstant && ((IntConstant) rop).value < 256) {
                        control.processConfig.sub = ((IntConstant) rop).value;
                    } else if (rop instanceof LongConstant && ((LongConstant) rop).value < 256) {
                        control.processConfig.sub = ((LongConstant) rop).value;
                    }
                }
            }
        }
        if (control.processConfig.div == 0) {
            for (Value v : mainInput.dataValues) {
                if (v instanceof FloatConstant) {
                    control.processConfig.div = ((FloatConstant) v).value;
                    break;
                }
            }
        }
        if (hasSub && control.processConfig.sub == 0) {
            control.processConfig.sub = control.processConfig.div;
        }

        ArrayList<Model.InputLayer> otherInputs = new ArrayList<>();
        if (info.getInputLayers().size() > 1) {
            for (int i = 0; i < info.getInputLayers().size() - 1; i++) {
                TrackInfo.Layer input = info.getInputLayers().get(i);
                String name = "";
                for (Value v : input.nameValues) {
                    if (v instanceof StringConstant) {
                        name = ((StringConstant) v).value;
                        break;
                    }
                }
                if (name.equals("input_shape")) {
                    otherInputs.add(new Model.InputLayer(name, control.processConfig.reshape));
                }
            }
        }
        control.otherInputs = otherInputs.toArray(new Model.InputLayer[otherInputs.size()]);

        ArrayList<String> outputNames = new ArrayList<>();
        for (TrackInfo.Layer l : info.getOutputLayers()) {
            for (Value v : l.nameValues) {
                if (v instanceof StringConstant) {
                    outputNames.add(((StringConstant) v).value);
                    break;
                }
            }
        }
        control.outputNames = outputNames.toArray(new String[outputNames.size()]);
    }

    public void print() {
        System.out.println(control.toString());
    }

    public void outputJson(String path){
        String fileName = control.appName.replace(".apk","") + "_control.json";
        Path filePath = Paths.get(path, fileName);

        ObjectMapper mapper = new ObjectMapper();
        try{
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(control);
            System.out.println(jsonString);


            FileWriter fileWriter = new FileWriter(filePath.toString());
            fileWriter.write(jsonString);
            fileWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
