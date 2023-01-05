# -*- coding:utf-8 -*-


import easydict
import numpy as np
from PIL import Image
import os
import sys
import typing
import multiprocessing
import time
import logging
import argparse
import json

parser = argparse.ArgumentParser()


parser.add_argument("--apk_name", required=True,
                    help="APK name such as com.xx.xx")
parser.add_argument("--model_name", required=True,
                    help="model name ends with .pb")
parser.add_argument("--img_name", required=False, default="",
                    help="img name. Recomanded using png/jpg")
parser.add_argument("--nr_iter", required=False, default=0, type=int,
                    help="iter times, e.g. 10, 20. If nnot set or set to 0, then try 10 and 20.")
parser.add_argument("--attack_cost", required=False, default=0, type=int,
                    help="attack cost, e.g. 6, 8, 10, ... 16. If nnot set or set to 0, then try 8~16.")
parser.add_argument("--control_file", required=False,
                    default="control.json", type=str, help="path to control file(json)")
parser.add_argument("--logs_file", required=False,
                    default="log.txt", type=str, help="path to log file")
parser.add_argument("--gpu", required=False,
                    default="", type=str, help="visible gpu for current session. Default is all")
parser.add_argument("--each_model_img", required=False,
                    default=False, action="store_true", help="if true, then for one apk, each model has its own image path")
parser.add_argument("--skip_exist", required=False,
                    default=False, action="store_true", help="if true, skip existing image test")
parser.add_argument("--keras", required=False,
                    default=False, action="store_true", help="if true, test keras model")
parser.add_argument("--keras_origin_model", required=False,
                    default=False, action="store_true", help="if true, also test the origin keras model")

args = parser.parse_args()

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '1'
if len(args.gpu):
    os.environ['CUDA_VISIBLE_DEVICES'] = args.gpu

import tensorflow as tf
os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.getcwd())

from src.advtool.WhiteBoxAttacks.config import AdvAttackConfig
from src.advtool.attack import AdvTool

MODEL_DIR = os.path.join("/tf", "data", 'apk', 'tensorflow_models')
MEDIA_DIR = os.path.join("statistics", "media")
LOG_DIR = "logs"
CONTROL_DIR = os.path.join("statistics", "controls")

all_control = json.load(
    open(os.path.join(CONTROL_DIR, args.control_file), 'r'))

if not os.path.exists(LOG_DIR):
    os.makedirs(LOG_DIR)

logging.basicConfig(filename=os.path.join(LOG_DIR, args.logs_file), level=logging.INFO,
                    format='%(asctime)s %(filename)s [line:%(lineno)d]-%(levelname)s:%(message)s',
                    datefmt='%m-%d %H:%M:%S'
                    )

pixel_type_mapping = {
    "float32": np.float32,
    "uint8":np.uint8, 
}


def load_pb(path_to_pb):
    """Load the .pb file and return graph

    :param path_to_pb: The path of the .pb file
    :type path_to_pb: str
    :return: graph
    :rtype: GraphDef
    """
    with tf.compat.v1.gfile.GFile(path_to_pb, "rb") as f:
        graph_def = tf.compat.v1.GraphDef()
        graph_def.ParseFromString(f.read())
    with tf.compat.v1.Graph().as_default() as graph:
        tf.compat.v1.import_graph_def(graph_def, name='')
        return graph, graph_def

import imp
# dynamic import
def dynamic_imp(name, class_name):
    
    # find_module() method is used
    # to find the module and return
    # its description and path
    try:
        #name = name.replace("-","_")
        fp, path, desc = imp.find_module(name)
        
    except ImportError:
        print ("module not found: " + name)
        
    try:
    # load_modules loads the module
    # dynamically ans takes the filepath
    # module and description as parameter
        example_package = imp.load_module(name, fp,
                                        path, desc)
    except Exception as e:
        print(e)

    try:
        myclass = imp.load_module("% s.% s" % (name,
                                            class_name),
                                fp, path, desc)
    except Exception as e:
        print(e)
        
    return example_package, myclass

def analyze_inputs_outputs(graph):
    """Analyzing the graph and return the input node and output node

    :param graph: The computation graph
    :type graph: GraphDef
    :return: [Input Node List, Output Node List]
    :rtype: List
    """
    ops = graph.get_operations()
    outputs_set = set(ops)
    inputs = []
    for op in ops:
        if len(op.inputs) == 0 and op.type != 'Const':
            inputs.append(op)
        else:
            for input_tensor in op.inputs:
                if input_tensor.op in outputs_set:
                    outputs_set.remove(input_tensor.op)
    outputs = list(outputs_set)
    return (inputs, outputs)


def getRandomImage():
    imgDir = "/opt/Anonymous/workspace/mob-dl-rev/data/external/adv_test_imgs/hotdogs"
    length = len(os.listdir(imgDir))
    idx = np.random.randint(low=0, high=length)
    img = Image.open("{}/{}".format(imgDir, os.listdir(imgDir)[idx]))
    # img.show()
    return img


def get_img(img_path):
    return Image.open(img_path)


def attack_img_one_config(sess, graph, graph_def, input_tensor, output_tensor, old_x, clip_range: typing.Tuple[float, float],
                          eps_dividend: float, nr_iter: int, target = None, other_inputs=None):
    """
        @param `input_tensor`: place holder of input 
        @param `output_tensor`: place holder of output
        @param `old_x`: the np array of origin input
        @param `clip_range`: range of clip treshold
        @param `eps_dividend`: number of dividend. One of (6,8,10,12,..., 16)
        @param `nr_iter`: number of Iter times when generating adversary example.
                    usually 10 ~ 100.
    """
    config = AdvAttackConfig()
    config.clip_range = clip_range
    config.eps = eps_dividend / 255. * \
        (config.clip_range[1] - config.clip_range[0])
    # config.eps = 0.02
    config.nb_iter = nr_iter
    pgd_attack = AdvTool.getWhiteBoxAttack(attack_name="pgd", config=config)
    model = (graph, graph_def, input_tensor, output_tensor)
    adv_x = pgd_attack.run(model, old_x, target,other_inputs)
    # sess = tf.compat.v1.Session(graph=graph)
    feed_dict={input_tensor: adv_x}
    if other_inputs is not None and len(other_inputs):
        feed_dict.update(other_inputs)
    out = sess.run(output_tensor, feed_dict=feed_dict)
    # print(out)
    return out, adv_x

def attack_img_one_config_keras(model, old_x, clip_range: typing.Tuple[float, float],
                          eps_dividend: float, nr_iter: int, target = None,):
    config = AdvAttackConfig()
    config.clip_range = clip_range
    config.eps = eps_dividend / 255. * \
        (config.clip_range[1] - config.clip_range[0])
    # config.eps = 0.02
    config.nb_iter = nr_iter
    pgd_attack = AdvTool.getWhiteBoxAttack(attack_name="pgd", config=config)
    adv_x = pgd_attack.run(model.model, old_x, target)
    out = model.model(adv_x)
    if isinstance(out, tuple):
        out = out[0]
    return out.numpy(), adv_x.numpy()

def run_old_model(interpreter, input_tensor:np.ndarray):
    # interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    # get input and output tensors
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # set the tensor to point to the input data to be inferred
    input_index = interpreter.get_input_details()[0]["index"]
    try:
        interpreter.set_tensor(input_index, input_tensor)
    except ValueError:
        if input_tensor.dtype == np.float32:
            input_tensor = input_tensor.astype(np.uint8)
        else:
            input_tensor = input_tensor.astype(np.float32)
        interpreter.set_tensor(input_index, input_tensor)
    # Run the inference
    interpreter.invoke()
    output_details = interpreter.get_output_details()
    output_data = interpreter.get_tensor(output_details[0]['index'])

    return output_data


def _get_save_img_name(img_path, suffix="", ext_name='bmp'):
    base_name = os.path.basename(img_path)
    _p = os.path.join(os.path.dirname(img_path), 'adversary', base_name.split(
        '.')[0] + suffix + ext_name)
    return _p


def make_dir_if_not_exists(dir_name):
    if not os.path.exists(dir_name):
        os.makedirs(dir_name)

def calc_image_target(obj, origin_output=None):
    if isinstance(obj,list):
        return np.array(obj)
    if isinstance(obj,str) and obj == "argmax":
        idx = np.unravel_index(origin_output.argmax(), origin_output.shape)
        ret = np.zeros(shape=origin_output.shape)
        ret[idx] = 1.0
        return ret 
    if isinstance(obj,str) and obj == "zeros":
        return np.zeros(shape=origin_output.shape)
    else:
        assert 0, str(obj)

def preprocess_color_mode(img, mode):
    if mode == "RGB":
        return img 
    if mode == "BGR":
        return img[...,(2,1,0)]
    elif mode == "L":
        return img
    elif mode == "RBG":
        return img[..., (0,2,1)]
    elif mode == "BRG":
        return img[...,(2,0,1)]

def postprocess_color_mode(img, mode):
    if mode == "RGB":
        return img 
    if mode == "BGR":
        return img[...,(2,1,0)]
    elif mode == "L":
        return img
    elif mode == "RBG":
        return img[..., (0,2,1)]
    elif mode == "BRG":
        return img[...,(1,2,3)]

def preprocess_pixel_value(x, process_config:dict):

    def sub_and_div(input,sub,div,as_type,reverse=False):
        if reverse:
            input = (input / div ).astype(as_type)
            input = (input - sub ).astype(as_type)
        else:
            input = (input - sub ).astype(as_type)
            input = (input / div ).astype(as_type)
        return input


    pixel_type = process_config['pixel_type']
    if isinstance(process_config['div'], (int, float)):
        div = process_config['div']
        sub = process_config['sub']
        x = sub_and_div(x, sub, div, pixel_type_mapping[pixel_type], reverse=process_config['reverse'])
    elif isinstance(process_config['div'], list):
        channel = 0
        for sub, div in zip(process_config['sub'],process_config['div']):
            x[...,channel] = sub_and_div(x[...,channel], sub, div, pixel_type_mapping[pixel_type], reverse=process_config['reverse'])
            channel += 1
    else:
        assert 0, process_config['div']
    return x

def postprocess_pixel_value(x, process_config:dict):
    def mul_and_add(input, add, mul , as_type, reverse=False):
        if reverse:
            input = (input + add ).astype(as_type)
            input = (input * mul ).astype(as_type)
        else:
            input = (input * mul ).astype(as_type)
            input = (input + add ).astype(as_type)
        return input 
    pixel_type = process_config['pixel_type']
    if isinstance(process_config['div'], (int, float)):
        mul = process_config['div']
        add = process_config['sub']
        x = mul_and_add(x, add, mul, pixel_type_mapping[pixel_type], reverse=process_config['reverse'])
    elif isinstance(process_config['div'], list):
        channel = 0
        for add, mul in zip(process_config['sub'],process_config['div']):
            x[...,channel] = mul_and_add(x[...,channel], add, mul, pixel_type_mapping[pixel_type], reverse=process_config['reverse'])
            channel += 1
    else:
        assert 0, process_config['div']
    return x



def test_one_img(img_path: str, control: dict, graph, graph_def, input_tensor, output_tensor,skip_exist=False, keras_model=False, modCl=None, origin_model=None):

    # prepare dir. Common
    adv_dir = os.path.join(os.path.dirname(img_path), 'adversary')
    if not os.path.exists(adv_dir):
        os.makedirs(adv_dir)
    origin_img_name = _get_save_img_name(img_path, "origin.")
    if skip_exist and os.path.exists(origin_img_name):
        return False

    # image target for classification task 
    process_config = control['process_config']
    img_target = control['img_target']
    base_name = os.path.basename(img_path)

    # other inputs for multi input. Only for tensorflow models 
    if 'other_inputs' not in control:
        other_inputs = None 
    elif not len(control['other_inputs']):
        other_inputs = None
    else:
        other_inputs = {}
        for key in control['other_inputs']:
            a = graph.get_tensor_by_name("%s:0"% key)
            other_inputs[a] = np.array(control['other_inputs'][key])

    img = get_img(img_path)

    # preprocess. Common
    reshape = process_config['reshape']
    if reshape[-1] == 3:
        if img.mode != "RGB":
            img = img.convert("RGB")
    elif reshape[-1] == 4:
        if img.mode != "RGBA":
            img = img.convert("RGBA")
    elif reshape[-1] == 1:
        if img.mode != "L":
            img = img.convert("L")
    else:
        assert 0, str(reshape[-1])
    if len(reshape) == 4:
        shape_to_resize = reshape[2], reshape[1]
    elif len(reshape) == 3:
        shape_to_resize = reshape[1], reshape[0]
    else:
        assert 0, str(reshape)
    # reshape the img according to config. Common
    if shape_to_resize == (0,0):
        shape_to_resize = img.height, img.width
    else:
        img = img.resize(size=shape_to_resize)
    # save the reshaped img for further use. Common 
    img.save(origin_img_name)

    # convert to numpy array. Common
    img = np.asarray(img).astype(pixel_type_mapping[process_config['pixel_type']])

    if len(reshape) == 4:
        x = img.reshape((reshape[0], shape_to_resize[1],shape_to_resize[0], reshape[3]))
    elif len(reshape) == 3:
        x = img.reshape((shape_to_resize[1],shape_to_resize[0], reshape[2]))
    else:
        assert 0, str(reshape)

    x = preprocess_pixel_value(x, process_config)

    x = preprocess_color_mode(x, process_config['mode'])

    minx, maxx = (np.min(x), np.max(x))
    ret = {}
    
    # run original inference session
    if not keras_model:
        tf.compat.v1.disable_eager_execution()
        sess = tf.compat.v1.Session(graph=graph)
        tf.compat.v1.import_graph_def(graph_def)
        feed_dict = {input_tensor: x}
        if other_inputs is not None:
            for key in other_inputs:
                feed_dict[key] = other_inputs[key]
        orig_out = sess.run(output_tensor, feed_dict=feed_dict)
    else:
        orig_out = modCl.model(x)
        if isinstance(orig_out,tuple):
            orig_out = orig_out[0]
        orig_out = orig_out.numpy()

        if origin_model is not None:
            _old_output = run_old_model(origin_model, x)
            ret['origin_output_old'] = _old_output

    # save the origin output. Common. 
    ret['origin_output'] = orig_out
    ret['origin_img_path'] = origin_img_name
    ret['res'] = {}

    # Get image target. Common 
    if base_name in img_target:
        target = calc_image_target(img_target[base_name],orig_out)
    elif 'default' in img_target and img_target['default'] is not None:
        target = calc_image_target(img_target['default'],orig_out)
    else:
        target = None
    
    # running config. Common
    flag_one_config = False
    if args.nr_iter and args.attack_cost:
        flag_one_config = True

    for nr in range(1, 3):
        nr_iter = nr * 10
        if flag_one_config:
            nr_iter = args.nr_iter
        ret['res'][nr_iter] = {}
        for eps_dividend in range(8, 18, 2):
            if flag_one_config:
                nr_iter, eps_dividend = args.nr_iter, args.attack_cost

            _p = _get_save_img_name(img_path, "_%d_%d." %
                        (nr_iter, eps_dividend))
            if skip_exist and os.path.exists(_p):
                logging.warning("config set (iter %d, cost %d) exsits. Skip")
                break
            logging.info("Testing img %s, nr_iter %d, eps_dividend %d" %
                         (os.path.basename(img_path), nr_iter, eps_dividend))
            ret['res'][nr_iter][eps_dividend] = {}
            start_time = time.time()
            if not keras_model:
                out, adv_x = attack_img_one_config(sess, graph, graph_def, input_tensor,
                                                   output_tensor, x, (minx,
                                                                      maxx),
                                                   float(eps_dividend), nr_iter, target, other_inputs)
            else:
                out, adv_x = attack_img_one_config_keras(
                    modCl, x, (minx, maxx), float(eps_dividend), nr_iter, target)
            end_time = time.time()
            duration = (round(end_time - start_time, 3)) * 1000
            ret['res'][nr_iter][eps_dividend]['used_time'] = duration
            ret['res'][nr_iter][eps_dividend]['output'] = out
            _p = _get_save_img_name(img_path, "_%d_%d." %
                                    (nr_iter, eps_dividend))
            ret['res'][nr_iter][eps_dividend]['adv_img'] = _p

            if origin_model is not None:
                # if we also test the original model(not converted)
                old_model_output = run_old_model(origin_model, adv_x)
                ret['res'][nr_iter][eps_dividend]['output_old'] = old_model_output

            # post process
            if len(reshape) == 4:
                new_img = adv_x[0]
            else:
                new_img = adv_x
            new_img = postprocess_color_mode(new_img, process_config['mode'])
            new_img = postprocess_pixel_value(new_img, process_config)
            if process_config['mode'] == "L":
                _mode = "L"
                new_img = new_img[...,0]
            else:
                _mode = "RGB"
            new_img = Image.fromarray(new_img.astype(np.uint8), _mode)
            new_img.save(_p)
            if flag_one_config:
                break
        if flag_one_config:
            break
    if not len(ret):
        return False 
    return ret


def test_one_model(apk_name: str,  model_name: str, img_name=""):
    control = all_control[apk_name][model_name]
    if isinstance(control, str): # if it is str, then we use the same config 
        if '/' in control:
            apk, m = control.split('/')
            control = all_control[apk][m] 
        else:
            control = all_control[apk_name][control] 
    if args.keras:

        model_path = os.path.join("keras_data","tflite_model")
        ignore_list = open(os.path.join(model_path, 'keras_ignore.txt'), 'r').read()
        if "%s/%s" %(apk_name, model_name) in ignore_list: 
            logging.warning("%s/%s in ignore list. Skip"%(apk_name, model_name))
            return 

        mod, modCl = dynamic_imp(os.path.join(model_path, apk_name, model_name),"model") 
        graph, graph_def = None, None 
        input_tensor, output_tensor = None, None 
    else:
        modCl = None 
        model_path = os.path.join(MODEL_DIR, apk_name, model_name)
        graph, graph_def = load_pb(model_path)
        input_nodes, output_nodes = analyze_inputs_outputs(graph)
        print("Input Number: {}".format(len(input_nodes)))
        print("Output Number: {}".format(len(output_nodes)))
        for in_node in input_nodes:
            print("Input Node Name: " + in_node.name)
        for out_node in output_nodes:
            print("Out Node Name: " + out_node.name)
        if "input_name" in control and len(control['input_name']):
            input_tensor = graph.get_tensor_by_name( "%s:0" % control['input_name'][0])
        else:
            input_tensor = graph.get_tensor_by_name(
                '{}:0'.format(input_nodes[0].name))
        if "output_name" in control and len(control['output_name']):
            output_tensor = graph.get_tensor_by_name("%s:0" % control['output_name'][0])
        else:
            output_tensor = graph.get_tensor_by_name(
                '{}:0'.format(output_nodes[0].name))
    # ret = {}
    if len(img_name):
        if args.each_model_img:
            img_path = os.path.join(MEDIA_DIR, apk_name, model_name ,img_name)
        else:
            img_path = os.path.join(MEDIA_DIR, apk_name, img_name)
        if args.keras_origin_model:
            """ load the origin(old) model """
            old_model_path = os.path.join("keras_data","raw",apk_name, model_name)
            interpreter = tf.lite.Interpreter(model_path=old_model_path)
        else:
            interpreter = None

        img_ret = test_one_img(img_path, control, graph, graph_def, input_tensor, output_tensor, args.skip_exist, args.keras,modCl, interpreter)
        if img_ret:
            write_res_one_img(img_ret, apk_name, model_name, img_name)
    # for file_base_name in os.listdir(os.path.join(MEDIA_DIR, apk_name)):
    #     filename = os.path.join(MEDIA_DIR, apk_name, file_base_name)
    #     if os.path.isdir(filename):
    #         continue
    #     img_ret = test_one_img(filename, graph, graph_def,
    #                            input_tensor, output_tensor)
    #     write_res_one_img(img_ret, apk_name, model_name, file_base_name)
        # ret[file_base_name] = img_ret

    # return ret


def _clip_str_len(s: str):
    """
        return a shorter string
    """
    return s[:min(20, len(s))]

def write_res_check_dir(apk_name: str,  model_name: str):
    md_dir = os.path.join('statistics', 'results', apk_name)
    np_dir = os.path.join('statistics', 'numpys', apk_name, model_name)
    make_dir_if_not_exists(md_dir)
    make_dir_if_not_exists(np_dir)

def write_res_check_header(filename):
    firstline = "|image|origin_output|iter_time|attack_cost|adv_output|origin_preview|adv_preview|time used(ms)|\n"
    secondline = "|----|----|----|----|----|---|---|----|\n"
    flag1, flag2 = False, False
    if not os.path.exists(filename):
        os.system("touch " + filename)
    else:
        f_md = open(filename,'r')
        for i, line in enumerate(f_md):
            if i == 0 and line.strip() == firstline.strip() :
                flag1 = True 
                continue
            elif i==1 and line.strip()  == secondline.strip() :
                flag2 = True
            else:
                break
        f_md.close() 
    f_md = open(filename,'a')
    if not flag1:
        f_md.write(firstline)
    if not flag2:
        f_md.write(secondline)
    f_md.close()

def write_res_one_img(data: dict, apk_name: str,  model_name: str, image_name: str):

    write_res_check_dir(apk_name, model_name)

    md_dir = os.path.join('statistics', 'results', apk_name)
    np_dir = os.path.join('statistics', 'numpys', apk_name, model_name)

    md_file = os.path.join(md_dir, model_name+".md")
    write_res_check_header(md_file)
    f_md = open(md_file, 'a')

    origin_output = data['origin_output']
    origin_output_old = data['origin_output_old']
    origin_img_path = data['origin_img_path']
    np.savez(os.path.join(np_dir, "%s_origin.npz" % image_name), origin_output)
    np.savez(os.path.join(np_dir, "%s_origin.old.npz" % image_name), origin_output_old)
    cur_res = data['res']
    for nr_iter in cur_res:
        for eps_dividend in cur_res[nr_iter]:
            out = cur_res[nr_iter][eps_dividend]['output']
            out_old = cur_res[nr_iter][eps_dividend]['output_old']
            np.savez(os.path.join(np_dir, "%s_%d_%d.old.npz" %
                                  (image_name, nr_iter, eps_dividend)), out_old)
            np.savez(os.path.join(np_dir, "%s_%d_%d.npz" %
                                  (image_name, nr_iter, eps_dividend)), out)
            adv_img_path = cur_res[nr_iter][eps_dividend]['adv_img']
            f_md.write(
                "|{image_name}|`{origin_output}`|{iter_time}|{eps_dividend}|`{adv_output}`|{origin_preview}|{adv_preview}|{used}|\n".format(
                    image_name=image_name,
                    origin_output=_clip_str_len(
                        str(origin_output).replace(' ', '').replace('\n', ' ')),
                    iter_time=nr_iter,
                    eps_dividend=eps_dividend,
                    adv_output=_clip_str_len(
                        str(out).replace(' ', '').replace('\n', ' ')),
                    origin_preview="![img](%s)" % (
                        origin_img_path.replace("statistics", "../..", 1)),
                    adv_preview="![img](%s)" % (
                        adv_img_path.replace("statistics", "../..", 1)),
                    used=cur_res[nr_iter][eps_dividend]['used_time'],
                ))
            f_md.flush()
    f_md.close()


def write_res(res: dict,  apk_name: str,  model_name: str):
    md_dir = os.path.join('statistics', 'results', apk_name)
    write_res_check_dir(apk_name, model_name)

    md_file = os.path.join(md_dir, model_name+".md")
    write_res_check_header(md_file)

    for image_name in res:
        write_res_one_img(res[image_name], apk_name, model_name, image_name)


if __name__ == '__main__':
    apk_name = args.apk_name or 'com.seefoodtechnologies.nothotdog_2017-06-23'
    model_name = args.model_name or 'deepdog.pb'
    res = test_one_model(apk_name, model_name, args.img_name)
    # write_res(res,apk_name,model_name)
