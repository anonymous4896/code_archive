# -*- coding:utf-8 -*-

import json

key_mapping = {
    "outputNames": "output_name",
    "inputNames": "input_name",
}


def convert(input_file: str):
    """convert the intermediate control json file to our usable format"""
    ret = {}
    app_name = None
    model_name = None
    process_config = {}
    other_inputs = {}
    extra_dict = {}
    obj = json.load(open(input_file, 'r'))
    for key in obj:
        if key == 'appName':
            app_name = obj[key].replace('.apk','')
        elif key == 'modelName':
            model_name = obj[key] or '*'
        elif key == 'frameworkName':
            pass
        elif key == 'processConfig':
            for k, v in obj[key].items():
                if k == 'pixelType':
                    process_config['pixel_type'] = v
                elif k == 'colorMode':
                    process_config['mode'] = v
                else:
                    process_config[k] = v
        elif key == 'otherInputs':
            for item in obj[key]:  # obj[key] is a list
                other_inputs[item['inputName']] = item['values']
        elif key in key_mapping:
            extra_dict[key_mapping[key]] = obj[key]
    ret[app_name] = {model_name: {}}
    tmp: dict = ret[app_name][model_name]
    tmp['process_config'] = process_config
    if len(other_inputs):
        tmp['other_inputs'] = other_inputs
    if len(extra_dict):
        tmp.update(extra_dict)
    return ret


if __name__ == '__main__':
    # Testing the scripts
    import os
    import pathlib
    cur_dir = pathlib.Path(__file__)
    json_path = cur_dir.parent.parent.parent.joinpath('Hunter', 'output')
    output_obj = {}
    for file in os.listdir(str(json_path)):
        if file.endswith('.json'):
            t = convert(str(json_path.joinpath(file)))
            output_obj.update(t)
    json.dump(output_obj, open(
        cur_dir.parent.joinpath('controls', 'total.json'), 'w'), indent=2)
