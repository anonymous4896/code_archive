import tensorflow as tf
import pickle as pk
import numpy as np
weight_data = pk.load(open("/tf/mob-dl-rev/keras_data/tflite_model/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite-weights.pkl", "rb"))
conv2d_1 = tf.keras.layers.Conv2D(filters=16, kernel_size=(3, 3), strides=(2,2), padding="same", activation=None, use_bias=True)
dw_conv2d_1 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_2 = tf.keras.layers.Conv2D(filters=8, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
conv2d_3 = tf.keras.layers.Conv2D(filters=48, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_2 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(2,2), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_4 = tf.keras.layers.Conv2D(filters=12, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
conv2d_5 = tf.keras.layers.Conv2D(filters=72, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_3 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_6 = tf.keras.layers.Conv2D(filters=12, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_1 = tf.keras.layers.Add()
conv2d_7 = tf.keras.layers.Conv2D(filters=72, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_4 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(2,2), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_8 = tf.keras.layers.Conv2D(filters=16, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
conv2d_9 = tf.keras.layers.Conv2D(filters=96, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_5 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_10 = tf.keras.layers.Conv2D(filters=16, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_2 = tf.keras.layers.Add()
conv2d_11 = tf.keras.layers.Conv2D(filters=96, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_6 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_12 = tf.keras.layers.Conv2D(filters=16, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_3 = tf.keras.layers.Add()
conv2d_13 = tf.keras.layers.Conv2D(filters=96, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_7 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_14 = tf.keras.layers.Conv2D(filters=32, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
conv2d_15 = tf.keras.layers.Conv2D(filters=192, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_8 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_16 = tf.keras.layers.Conv2D(filters=32, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_4 = tf.keras.layers.Add()
conv2d_17 = tf.keras.layers.Conv2D(filters=192, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_9 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_18 = tf.keras.layers.Conv2D(filters=32, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_5 = tf.keras.layers.Add()
conv2d_19 = tf.keras.layers.Conv2D(filters=192, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_10 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_20 = tf.keras.layers.Conv2D(filters=32, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_6 = tf.keras.layers.Add()
conv2d_21 = tf.keras.layers.Conv2D(filters=192, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_11 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_22 = tf.keras.layers.Conv2D(filters=48, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
conv2d_23 = tf.keras.layers.Conv2D(filters=288, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_12 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_24 = tf.keras.layers.Conv2D(filters=48, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_7 = tf.keras.layers.Add()
conv2d_25 = tf.keras.layers.Conv2D(filters=288, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_13 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_26 = tf.keras.layers.Conv2D(filters=48, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_8 = tf.keras.layers.Add()
conv2d_27 = tf.keras.layers.Conv2D(filters=288, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_14 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_28 = tf.keras.layers.Conv2D(filters=80, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
conv2d_29 = tf.keras.layers.Conv2D(filters=480, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_15 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_30 = tf.keras.layers.Conv2D(filters=80, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_9 = tf.keras.layers.Add()
conv2d_31 = tf.keras.layers.Conv2D(filters=480, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_16 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_32 = tf.keras.layers.Conv2D(filters=80, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
add_10 = tf.keras.layers.Add()
conv2d_33 = tf.keras.layers.Conv2D(filters=480, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
dw_conv2d_17 = tf.keras.layers.DepthwiseConv2D(kernel_size=(3, 3), strides=(1,1), padding="same", activation=None, depth_multiplier=1, use_bias=True)
conv2d_34 = tf.keras.layers.Conv2D(filters=160, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
avgp2d_1 = tf.keras.layers.AveragePooling2D(pool_size=33, padding="valid", strides=33)
conv2d_35 = tf.keras.layers.Conv2D(filters=256, kernel_size=(1, 1), strides=(3,3), padding="valid", activation=None, use_bias=True)
rzbi_1 = tf.keras.layers.Resizing(height=33, width=33, interpolation="bilinear")
conv2d_36 = tf.keras.layers.Conv2D(filters=256, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
concat_1 = tf.keras.layers.Concatenate(axis=3)
conv2d_37 = tf.keras.layers.Conv2D(filters=256, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
conv2d_38 = tf.keras.layers.Conv2D(filters=21, kernel_size=(1, 1), strides=(1,1), padding="valid", activation=None, use_bias=True)
rzbi_2 = tf.keras.layers.Resizing(height=33, width=33, interpolation="bilinear")
rzbi_3 = tf.keras.layers.Resizing(height=257, width=257, interpolation="bilinear")
relu_1 = tf.keras.layers.ReLU(max_value=6)
relu_2 = tf.keras.layers.ReLU(max_value=6)
relu_3 = tf.keras.layers.ReLU(max_value=6)
relu_4 = tf.keras.layers.ReLU(max_value=6)
relu_5 = tf.keras.layers.ReLU(max_value=6)
relu_6 = tf.keras.layers.ReLU(max_value=6)
relu_7 = tf.keras.layers.ReLU(max_value=6)
relu_8 = tf.keras.layers.ReLU(max_value=6)
relu_9 = tf.keras.layers.ReLU(max_value=6)
relu_10 = tf.keras.layers.ReLU(max_value=6)
relu_11 = tf.keras.layers.ReLU(max_value=6)
relu_12 = tf.keras.layers.ReLU(max_value=6)
relu_13 = tf.keras.layers.ReLU(max_value=6)
relu_14 = tf.keras.layers.ReLU(max_value=6)
relu_15 = tf.keras.layers.ReLU(max_value=6)
relu_16 = tf.keras.layers.ReLU(max_value=6)
relu_17 = tf.keras.layers.ReLU(max_value=6)
relu_18 = tf.keras.layers.ReLU(max_value=6)
relu_19 = tf.keras.layers.ReLU(max_value=6)
relu_20 = tf.keras.layers.ReLU(max_value=6)
relu_21 = tf.keras.layers.ReLU(max_value=6)
relu_22 = tf.keras.layers.ReLU(max_value=6)
relu_23 = tf.keras.layers.ReLU(max_value=6)
relu_24 = tf.keras.layers.ReLU(max_value=6)
relu_25 = tf.keras.layers.ReLU(max_value=6)
relu_26 = tf.keras.layers.ReLU(max_value=6)
relu_27 = tf.keras.layers.ReLU(max_value=6)
relu_28 = tf.keras.layers.ReLU(max_value=6)
relu_29 = tf.keras.layers.ReLU(max_value=6)
relu_30 = tf.keras.layers.ReLU(max_value=6)
relu_31 = tf.keras.layers.ReLU(max_value=6)
relu_32 = tf.keras.layers.ReLU(max_value=6)
relu_33 = tf.keras.layers.ReLU(max_value=6)
relu_34 = tf.keras.layers.ReLU(max_value=6)
relu_35 = tf.keras.layers.ReLU(max_value=None)
input_0 = tf.keras.Input(shape=(257, 257, 3))
x_2 = conv2d_1(input_0)
x_3 = relu_1(x_2)
x_4 = dw_conv2d_1(x_3)
x_5 = relu_2(x_4)
x_6 = conv2d_2(x_5)
x_7 = conv2d_3(x_6)
x_8 = relu_4(x_7)
x_9 = dw_conv2d_2(x_8)
x_10 = relu_3(x_9)
x_11 = conv2d_4(x_10)
x_12 = conv2d_5(x_11)
x_13 = relu_20(x_12)
x_14 = dw_conv2d_3(x_13)
x_15 = relu_19(x_14)
x_16 = conv2d_6(x_15)
x_17 = add_1([x_11, x_16])
x_18 = conv2d_7(x_17)
x_19 = relu_22(x_18)
x_20 = dw_conv2d_4(x_19)
x_21 = relu_21(x_20)
x_22 = conv2d_8(x_21)
x_23 = conv2d_9(x_22)
x_24 = relu_24(x_23)
x_25 = dw_conv2d_5(x_24)
x_26 = relu_23(x_25)
x_27 = conv2d_10(x_26)
x_28 = add_2([x_22, x_27])
x_29 = conv2d_11(x_28)
x_30 = relu_26(x_29)
x_31 = dw_conv2d_6(x_30)
x_32 = relu_25(x_31)
x_33 = conv2d_12(x_32)
x_34 = add_3([x_28, x_33])
x_35 = conv2d_13(x_34)
x_36 = relu_28(x_35)
x_37 = dw_conv2d_7(x_36)
x_38 = relu_27(x_37)
x_39 = conv2d_14(x_38)
x_40 = conv2d_15(x_39)
x_41 = relu_30(x_40)
x_42 = dw_conv2d_8(x_41)
x_43 = relu_29(x_42)
x_44 = conv2d_16(x_43)
x_45 = add_4([x_39, x_44])
x_46 = conv2d_17(x_45)
x_47 = relu_32(x_46)
x_48 = dw_conv2d_9(x_47)
x_49 = relu_31(x_48)
x_50 = conv2d_18(x_49)
x_51 = add_5([x_45, x_50])
x_52 = conv2d_19(x_51)
x_53 = relu_34(x_52)
x_54 = dw_conv2d_10(x_53)
x_55 = relu_33(x_54)
x_56 = conv2d_20(x_55)
x_57 = add_6([x_51, x_56])
x_58 = conv2d_21(x_57)
x_59 = relu_6(x_58)
x_60 = dw_conv2d_11(x_59)
x_61 = relu_5(x_60)
x_62 = conv2d_22(x_61)
x_63 = conv2d_23(x_62)
x_64 = relu_8(x_63)
x_65 = dw_conv2d_12(x_64)
x_66 = relu_7(x_65)
x_67 = conv2d_24(x_66)
x_68 = add_7([x_62, x_67])
x_69 = conv2d_25(x_68)
x_70 = relu_10(x_69)
x_71 = dw_conv2d_13(x_70)
x_72 = relu_9(x_71)
x_73 = conv2d_26(x_72)
x_74 = add_8([x_68, x_73])
x_75 = conv2d_27(x_74)
x_76 = relu_12(x_75)
x_77 = dw_conv2d_14(x_76)
x_78 = relu_11(x_77)
x_79 = conv2d_28(x_78)
x_80 = conv2d_29(x_79)
x_81 = relu_14(x_80)
x_82 = dw_conv2d_15(x_81)
x_83 = relu_13(x_82)
x_84 = conv2d_30(x_83)
x_85 = add_9([x_79, x_84])
x_86 = conv2d_31(x_85)
x_87 = relu_16(x_86)
x_88 = dw_conv2d_16(x_87)
x_89 = relu_15(x_88)
x_90 = conv2d_32(x_89)
x_91 = add_10([x_85, x_90])
x_92 = conv2d_33(x_91)
x_93 = relu_18(x_92)
x_94 = dw_conv2d_17(x_93)
x_95 = relu_17(x_94)
x_96 = conv2d_34(x_95)
x_97 = avgp2d_1(x_96)
x_98 = conv2d_35(x_97)
x_99 = rzbi_1(x_98)
x_100 = conv2d_36(x_96)
x_101 = relu_35(x_100)
x_102 = concat_1([x_99, x_101])
x_103 = conv2d_37(x_102)
x_104 = conv2d_38(x_103)
x_105 = rzbi_2(x_104)
x_106 = rzbi_3(x_105)
model = tf.keras.Model(inputs=input_0, outputs=x_106)
conv2d_1.set_weights([np.transpose(weight_data["MobilenetV2/Conv/weights-3"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/Conv/Conv2D_bias-1"]])
dw_conv2d_1.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv/depthwise/depthwise_weights-6"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv/depthwise/depthwise_bias-5"]])
conv2d_2.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv/project/weights-9"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv/project/Conv2D_bias-8"]])
conv2d_3.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_1/expand/weights-15"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_1/expand/Conv2D_bias-13"]])
dw_conv2d_2.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_1/depthwise/depthwise_weights-12"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_1/depthwise/depthwise_bias-11"]])
conv2d_4.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_1/project/weights-18"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_1/project/Conv2D_bias-17"]])
conv2d_5.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_2/expand/weights-92"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_2/expand/Conv2D_bias-90"]])
dw_conv2d_3.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_2/depthwise/depthwise_weights-89"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_2/depthwise/depthwise_bias-88"]])
conv2d_6.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_2/project/weights-95"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_2/project/Conv2D_bias-94"]])
conv2d_7.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_3/expand/weights-101"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_3/expand/Conv2D_bias-99"]])
dw_conv2d_4.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_3/depthwise/depthwise_weights-98"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_3/depthwise/depthwise_bias-97"]])
conv2d_8.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_3/project/weights-104"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_3/project/Conv2D_bias-103"]])
conv2d_9.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_4/expand/weights-111"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_4/expand/Conv2D_bias-109"]])
dw_conv2d_5.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_4/depthwise/depthwise_weights-108"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_4/depthwise/depthwise_bias-107"]])
conv2d_10.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_4/project/weights-114"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_4/project/Conv2D_bias-113"]])
conv2d_11.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_5/expand/weights-121"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_5/expand/Conv2D_bias-119"]])
dw_conv2d_6.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_5/depthwise/depthwise_weights-118"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_5/depthwise/depthwise_bias-117"]])
conv2d_12.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_5/project/weights-124"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_5/project/Conv2D_bias-123"]])
conv2d_13.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_6/expand/weights-130"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_6/expand/Conv2D_bias-128"]])
dw_conv2d_7.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_6/depthwise/depthwise_weights-127"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_6/depthwise/depthwise_bias-126"]])
conv2d_14.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_6/project/weights-133"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_6/project/Conv2D_bias-132"]])
conv2d_15.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_7/expand/weights-140"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_7/expand/Conv2D_bias-138"]])
dw_conv2d_8.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_7/depthwise/depthwise_weights-137"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_7/depthwise/depthwise_bias-136"]])
conv2d_16.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_7/project/weights-143"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_7/project/Conv2D_bias-142"]])
conv2d_17.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_8/expand/weights-150"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_8/expand/Conv2D_bias-148"]])
dw_conv2d_9.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_8/depthwise/depthwise_weights-147"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_8/depthwise/depthwise_bias-146"]])
conv2d_18.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_8/project/weights-153"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_8/project/Conv2D_bias-152"]])
conv2d_19.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_9/expand/weights-160"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_9/expand/Conv2D_bias-158"]])
dw_conv2d_10.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_9/depthwise/depthwise_weights-157"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_9/depthwise/depthwise_bias-156"]])
conv2d_20.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_9/project/weights-163"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_9/project/Conv2D_bias-162"]])
conv2d_21.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_10/expand/weights-24"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_10/expand/Conv2D_bias-22"]])
dw_conv2d_11.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_10/depthwise/depthwise_weights-21"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_10/depthwise/depthwise_bias-20"]])
conv2d_22.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_10/project/weights-27"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_10/project/Conv2D_bias-26"]])
conv2d_23.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_11/expand/weights-34"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_11/expand/Conv2D_bias-32"]])
dw_conv2d_12.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_11/depthwise/depthwise_weights-31"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_11/depthwise/depthwise_bias-30"]])
conv2d_24.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_11/project/weights-37"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_11/project/Conv2D_bias-36"]])
conv2d_25.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_12/expand/weights-44"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_12/expand/Conv2D_bias-42"]])
dw_conv2d_13.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_12/depthwise/depthwise_weights-41"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_12/depthwise/depthwise_bias-40"]])
conv2d_26.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_12/project/weights-47"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_12/project/Conv2D_bias-46"]])
conv2d_27.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_13/expand/weights-53"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_13/expand/Conv2D_bias-51"]])
dw_conv2d_14.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_13/depthwise/depthwise_weights-50"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_13/depthwise/depthwise_bias-49"]])
conv2d_28.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_13/project/weights-56"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_13/project/Conv2D_bias-55"]])
conv2d_29.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_14/expand/weights-63"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_14/expand/Conv2D_bias-61"]])
dw_conv2d_15.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_14/depthwise/depthwise_weights-60"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_14/depthwise/depthwise_bias-59"]])
conv2d_30.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_14/project/weights-66"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_14/project/Conv2D_bias-65"]])
conv2d_31.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_15/expand/weights-73"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_15/expand/Conv2D_bias-71"]])
dw_conv2d_16.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_15/depthwise/depthwise_weights-70"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_15/depthwise/depthwise_bias-69"]])
conv2d_32.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_15/project/weights-76"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_15/project/Conv2D_bias-75"]])
conv2d_33.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_16/expand/weights-82"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_16/expand/Conv2D_bias-80"]])
dw_conv2d_17.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_16/depthwise/depthwise_weights-79"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_16/depthwise/depthwise_bias-78"]])
conv2d_34.set_weights([np.transpose(weight_data["MobilenetV2/expanded_conv_16/project/weights-85"], axes=[1, 2, 3, 0]), weight_data["MobilenetV2/expanded_conv_16/project/Conv2D_bias-84"]])
conv2d_35.set_weights([np.transpose(weight_data["image_pooling/weights-178"], axes=[1, 2, 3, 0]), weight_data["image_pooling/Conv2D_bias-176"]])
conv2d_36.set_weights([np.transpose(weight_data["aspp0/weights-171"], axes=[1, 2, 3, 0]), weight_data["aspp0/Conv2D_bias-169"]])
conv2d_37.set_weights([np.transpose(weight_data["concat_projection/weights-175"], axes=[1, 2, 3, 0]), weight_data["concat_projection/Conv2D_bias-173"]])
conv2d_38.set_weights([np.transpose(weight_data["logits/semantic/weights-181"], axes=[1, 2, 3, 0]), weight_data["logits/semantic/Conv2D_bias-180"]])
