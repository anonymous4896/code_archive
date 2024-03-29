mob-dl-rev
==============================

That project aims to reverse the model in some mobile applications (may be on android platform).

Project Organization
------------

    ├── LICENSE
    ├── Makefile           <- Makefile with commands like `make data` or `make train`
    ├── README.md          <- The top-level README for developers using this project.
    ├── data
    │   ├── external       <- Data from third party sources.
    │   ├── interim        <- Intermediate data that has been transformed.
    │   ├── processed      <- The final, canonical data sets for modeling.
    │   └── raw            <- The original, immutable data dump.
    │
    ├── docs               <- A default Sphinx project; see sphinx-doc.org for details
    │
    ├── models             <- Trained and serialized models, model predictions, or model summaries
    │
    ├── notebooks          <- Jupyter notebooks. Naming convention is a number (for ordering),
    │                         the creator's initials, and a short `-` delimited description, e.g.
    │                         `1.0-jqp-initial-data-exploration`.
    │
    ├── references         <- Data dictionaries, manuals, and all other explanatory materials.
    │
    ├── reports            <- Generated analysis as HTML, PDF, LaTeX, etc.
    │   └── figures        <- Generated graphics and figures to be used in reporting
    │
    ├── requirements.txt   <- The requirements file for reproducing the analysis environment, e.g.
    │                         generated with `pip freeze > requirements.txt`
    │
    ├── setup.py           <- makes project pip installable (pip install -e .) so src can be imported
    ├── src                <- Source code for use in this project.
    │   ├── __init__.py    <- Makes src a Python module
    │   │
    │   ├── data           <- Scripts to download or generate data
    │   │   └── make_dataset.py
    │   │
    │   ├── features       <- Scripts to turn raw data into features for modeling
    │   │   └── build_features.py
    │   │
    │   ├── models         <- Scripts to train models and then use trained models to make
    │   │   │                 predictions
    │   │   ├── predict_model.py
    │   │   └── train_model.py
    │   │
    │   └── visualization  <- Scripts to create exploratory and results oriented visualizations
    │       └── visualize.py
    │
    └── tox.ini            <- tox file with settings for running tox; see tox.readthedocs.io


--------

<p><small>Project based on the <a target="_blank" href="https://drivendata.github.io/cookiecutter-data-science/">cookiecutter data science project template</a>. #cookiecutterdatascience</small></p>


## Prepare env

```bash

virtualenv venv
source venv/bin/ativate
python -m pip install -r requirements.txt

```

If tensorflow-gpu version(2.8.0) is not compatible with your productivity envirenmont, change the `tensorflow-gpu` in `requirements.txt` to `tensorflow` is ok. 

## Convert tflite model to keras

### 1. get weights

Put the tflite model in path `data/keras_data/raw`, and the `*.pkl` file will be written to `data/keras_data/tflite_model`

```bash
python src/data/make_tflite_graph.py data/keras_data/raw data/keras_data/tflite_model
```

### 2. generate cmpgraph


```bash
python src/data/tflite_graph_to_keras.py data/keras_data/tflite_model/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite-graph.pkl data/keras_data/tflite_model/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite-cmpgraph.pkl
```

This will generate a complete graph `data/keras_data/tflite_model/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite-cmpgraph.pkl`

### 3. make keras code

~~~bash
python src/data/make_keras_code.py data/keras_data/tflite_model/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite-cmpgraph.pkl data/keras_data/tflite_model/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite.py
~~~

## prepare the control file

The control file gererated by Hunter previously need convertion to use. Assuming the outputs of Hunter is located in `../Hunter/output`, run the following command will result `total.json` in directory `mob-dl-rev\statistics\controls`:

```bash
cd mob-dl-rev
python statistics/script_convertion.py
```


## Running the white box test

### Example 1: Tensorflow model 

package name: `uk.tensorzoom_2018-01-18`

to test model `lv2.pb`, we need:
1. the model with ext name `.pb`. Example: `data/tf_data/uk.tensorzoom_2018-01-18/lv2.pb`
2. A control json file, which contains all white box information, such as the input layer name, input-shape, pre-processing config. An example has shown in `statistics/controls/total.json`
3. An image to test. Example: `statistics/media/uk.tensorzoom_2018-01-18/lv2.pb/000000049983.jpg`

The following command would launch a test with `iter times=10` (recommand: 10 or 20) and `attack cost = 12`(recommand: 8, 12, 16, 20)

```bash
cd mob-dl-rev
python statistics/batch_test.py --apk_name uk.tensorzoom_2018-01-18 --model_name lv2.pb --img_name 000000049983.jpg --nr_iter 10 --attack_cost 12 --each_model_img --control_file total.json
```

If everything goes well, the results can be found in :
1. the generated adversary image `statistics/media/uk.tensorzoom_2018-01-18/lv2.pb/adversary/000000049983_10_12.bmp` 
2. the model output towards original image `000000049983.jpg_origin.npz` and the model output towards the adversary image `000000049983.jpg_10_12.npz`, both in path `statistics/numpys/uk.tensorzoom_2018-01-18/lv2.pb`
3. The result will be **append** to `statistics/results/uk.tensorzoom_2018-01-18/lv2.pb.md`. Using a markdown preview tool can view the visual result.

### Example 2: tflite model

package: `com.blink.academy.nomo`


to test model `deeplabv3_257_mv_gpu.tflite`, we need:
1. the model files generated by `Convert tflite model to keras`. Example: `data/keras_data/tflite_model/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite.*`
2. A control json file, which contains all white box information, such as the input layer name, input-shape, pre-processing config. An example has shown in `statistics/controls/total.json`
3. An image to test. Example: `statistics/media/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite/2012_000060.jpg`
4. (optional) If you want to test the original model(ends with .tflite), then the original model is required. put it in `data/keras_data/raw`

```bash
cd mob-dl-rev
python /statistics/batch_test.py --apk_name com.blink.academy.nomo --model_name deeplabv3_257_mv_gpu.tflite --img_name 2012_000060.jpg --nr_iter 10 --attack_cost 12 --gpu 2 --each_model_img --keras --keras_origin_model --control_file total.json 
```

If everything goes well, the results can be found in :
1. the generated adversary image `statistics/media/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite/adversary/2012_000060_10_12.bmp` 
2. the keras model output towards original image `2012_000060.jpg_origin.npz`, the output towards the adversary image of old tflite model (that is, before convertion to keras)`2012_000060.jpg_10_12.old.npz`,  and the keras model output towards the adversary image `2012_000060.jpg_10_12.npz`, both in path `statistics/numpys/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite`
3. The result will be **append** to `statistics/results/com.blink.academy.nomo/deeplabv3_257_mv_gpu.tflite.md`. Using a markdown preview tool can view the visual result.
