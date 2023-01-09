package hunter.Postprocess.ControlGenerate;

public class Model {
    public String appName;
    public String modelName;
    public String frameworkName;
    public String[] outputNames;
    public String[] inputNames;

    public static class InputLayer {
        public String inputName;
        public int[] values;

        public InputLayer(String name, int[] values){
            this.inputName =name;
            this.values = values;
        }

        @Override
        public String toString(){
            String res = "";
            res += String.format("%s [ ", inputName);
            if(values != null){
                for(int v : values){
                    res += String.format("%d ", v);
                }
            }
            res += "]";
            return res;
        }
    }
    public InputLayer[] otherInputs;

    public class ProcessConfig {
        public static final String MODE_RGB = "RGB";
        public static final String MODE_BGR = "BGR";
        public static final String MODE_RBG = "RBG";
        public static final String MODE_BRG = "BRG";
        public static final String MODE_GBR = "GBR";
        public static final String MODE_GRB = "GRB";
        public static final String MODE_GREY = "L";

        public String pixelType;
        public int[] reshape;
        public double sub;
        public double div;
        public boolean reverse;
        public String colorMode = MODE_RGB;

        @Override
        public String toString(){
            String res = "Process Config:\n";
            res += String.format("  Pixel Type: %s\n", pixelType);
            res += String.format("  Reshape: [ ");
            for(int i : reshape){
                res += String.format("%d ", i);
            }
            res += String.format("]\n");
            res += String.format("  Sub: %s\n", sub);
            res += String.format("  Div: %s\n", div);
            res += String.format("  Reverse: %s\n", reverse);
            res += String.format("  Color Mode: %s\n", colorMode);
            return res;
        }
    }
    public ProcessConfig processConfig;

    public Model(){
        this.processConfig = new ProcessConfig();
        this.otherInputs = new InputLayer[]{};
        this.inputNames = new String[]{};
        this.outputNames = new String[]{};
    }

    @Override
    public String toString(){
        String res = "===== control =====\n";
        res += String.format("APK Name: %s\n", appName);
        res += String.format("Framework: %s\n", frameworkName);
        res += String.format("Model Name: %s\n", modelName);
        if(inputNames.length > 0){
            res += String.format("Input Name: [");
            for(String s : inputNames){
                res += String.format(" %s", s);
            }
            res += String.format(" ]\n");
        }
        if(outputNames.length > 0){
            res += String.format("Output Name: [");
            for(String s : outputNames){
                res += String.format(" %s", s);
            }
            res += String.format(" ]\n");
        }
        if(otherInputs.length > 0){
            res += String.format("Other Inputs: \n");
            for(InputLayer l : otherInputs){
                res += String.format("  %s\n", l.toString());
            }
        }
        res += processConfig.toString();
        res += "===== control end =====";
        return res;
    }
}