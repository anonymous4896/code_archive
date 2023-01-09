package hunter.Preprocess;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import hunter.Constant;
import soot.SootMethod;
import soot.Type;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FeatureBuilder {
    private static ArrayList<String> frameworks = new ArrayList<>();
    private static ArrayList<String> classPatterns = new ArrayList<>();
    private static ArrayList<String> methodPatterns = new ArrayList<>();
    private static ArrayList<String> classNames = new ArrayList<>();
    private static ArrayList<String> methodNames = new ArrayList<>();
    private static ArrayList<ArrayList<Integer>> defValues = new ArrayList<>();
    private static ArrayList<Integer> orders = new ArrayList<>();

    private static String featureFileName = Constant.DEFAULT_SINK_SETTING;
    private static int featureNum = 0;

    public static void loadFeatures() {
        try {
            String path = FeatureBuilder.class.getClassLoader().getResource(featureFileName).getFile();
            CSVReader reader = new CSVReader(new FileReader(path));
            String[] line;

            reader.readNext(); // skip the first line

            while ((line = reader.readNext()) != null) {
                frameworks.add(line[0]);
                classPatterns.add(line[1]);
                methodPatterns.add(line[2]);
                classNames.add(line[3]);
                methodNames.add(line[4]);

                ArrayList<Integer> defValue = new ArrayList<>();
                for (String def : line[5].split(",")) {
                    int d = Integer.parseInt(def);
                    if (d > -2) {
                        defValue.add(d);
                    }
                }
                defValues.add(defValue);

                orders.add(Integer.parseInt(line[6]));
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        featureNum = methodPatterns.size();
    }

    public static void printFeatures() {
        System.out.println(String.format("We have %s features.", featureNum));
        for (int i = 0; i < featureNum; ++i) {
            System.out.println(String.format("%s: %s, %s, %s, %s, %s, %s, %s", i + 1, frameworks.get(i), classPatterns.get(i), methodPatterns.get(i), classNames.get(i), methodNames.get(i), defValues.get(i), orders.get(i)));
        }
    }

    private static boolean matchPattern(String s, String pattern) {
        for (String p : pattern.split("#")) {
            if (!s.contains(p)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isSinkPoint(SootMethod method) {
        return getSinkIndex(method) > -1;
    }

    public static int getSinkIndex(SootMethod method) {
        for (int i = 0; i < featureNum; ++i) {
            boolean isSink = matchPattern(method.getSignature(), classPatterns.get(i) + "#" + methodPatterns.get(i));

            if (isSink) {
                return i;
            }
        }

        return -1;
    }

    public static int getSinkOrder(SootMethod method) {
        int i = getSinkIndex(method);
        if (i > -1) {
            return orders.get(i);
        }

        return -1;
    }

    public static ArrayList<Integer> getSinkWriteValues(SootMethod method) {
        ArrayList<Integer> result = new ArrayList<>();

        int i = getSinkIndex(method);
        if (i > -1 && i < featureNum) {
            result.addAll(defValues.get(i));
        }

        return result;
    }

    public static String getSinkClassName(SootMethod method) {
        int i = getSinkIndex(method);
        if (i > -1) {
            return classNames.get(i);
        }
        return null;
    }

    public static String getSinkClassName(Type type) {
        for (int i = 0; i < featureNum; ++i) {
            boolean isSinkClass = matchPattern(type.toString(), classPatterns.get(i));

            if (isSinkClass) {
                return classNames.get(i);
            }
        }

        return "";
    }

    public static String getSinkMethodName(SootMethod method) {
        int i = getSinkIndex(method);
        if (i > -1) {
            return methodNames.get(i);
        }
        return null;
    }

    public static String getFramework(int i){
        return frameworks.get(i);
    }

    public static String getFrameworkInstance(String className) {
        for (int i = 0; i < featureNum; ++i) {
            if (classNames.get(i).equals(className) && frameworks.get(i).length() > 0) {
                return frameworks.get(i) + "0";
            }
        }
        return null;
    }
}