package hunter.Preprocess.Adapters;

public abstract class Adapter {
    boolean verbose = false;
    String adapterName = "Default Adapter";

    public Adapter(boolean verbose) {
        this.verbose = verbose;
    }

    public abstract void go();

    void PrintAdapterLog(String s) {
        PrintAdapterLogFormat(s);
    }

    void PrintAdapterLogFormat(String format, Object... args) {
        System.out.println(adapterName + ": " + String.format(format, args));
    }
}
