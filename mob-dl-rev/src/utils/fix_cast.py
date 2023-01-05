def run():
    f = open(
        "/opt/Anonymous/workspace/mob-dl-rev/data/processed/tflite_model/4ca5287b7495786b726a099530d6bd63_62611500/fixed.py",
        "w",
    )
    old_f = open(
        "/opt/Anonymous/workspace/mob-dl-rev/data/processed/tflite_model/4ca5287b7495786b726a099530d6bd63_62611500/lane_detection_v2.2.0_float32.tflite.py",
        "r",
    )
    # prevline = None
    for line in old_f.readlines():
        if "=" in line and "cast16" in line:
            continue

        # if "relu_" in line and "x_" in line:
        #     f.write(line)
        #     if prevline and "add_" not in prevline:
        #         continue
        #     var = line.split("=")[0].strip()
        #     f.write("{} = cast16({})\n".format(var, var))
        #     continue
        # prevline = line

        # if "add_" in line and "x_" in line:
        #     tmpline = line.replace("[", "")
        #     tmpline = tmpline.replace("]", "")
        #     var1, var2 = tmpline.split("(")[-1][:-2].split(", ")
        #     f.write("{} = cast16int({})\n".format(var1, var1))
        #     f.write("{} = cast16int({})\n".format(var2, var2))
        #     f.write(line)
        #     var = line.split("=")[0].strip()
        #     f.write("{} = cast16({})\n".format(var, var))
        #     continue

        f.write(line)
        # if "add_" in line and "x_" in line:
        #     var = line.split("=")[0].strip()
        #     f.write("{} = cast16({})\n".format(var, var))


if __name__ == "__main__":
    run()
