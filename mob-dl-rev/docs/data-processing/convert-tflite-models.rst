Convert TF-Lite Models
=======================

Here, our pipeline likes:

1. Load tf-lite model by using tensorflow
2. Extract the computation graph and weights [**Without attributes of operators**]
3. Complete the operator attributes by using some fixed rules
4. Model Code Generation

1. Load tf-lite model by using tensorflow
"""""""""""""""""""""""""""""""""""""""""""

First, you can apply the code::
    
    import tensorflow as tf
    model_path = "../data/raw/tflite_model/a_i_glass_apkpure.com/detect.tflite"
    # Load the TFLite model and allocate tensors.
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

Such tf.lite.Interpreter can load tf-lite model directly and we can use it to get model computation graph.


2. Extract the computation graph and weights
""""""""""""""""""""""""""""""""""""""""""""

For a tf-lite model named "model.tflite", we will generate two parts at that step:

* model-graph.pkl
* model-weight.pkl

The model-graph.pkl file is from our graph object. We use pickle to dump it to a file.

You could use the shell::

    make tflite2graph


3. Complete the computation graph and weights
""""""""""""""""""""""""""""""""""""""""""""

In this step, we convert the tf-lite op type to keras type.

Then, we fill up all operator nodes with their attributes.

We will save the completed graph to "model-cmpgraph.pkl".

You could use the shell::

    make complete_graph


4. Model Code Generation
""""""""""""""""""""""""

We use the topology sort on the graph and get a ordered list of operator nodes.

Then, we generate the keras code by using that list.

You could use the shell::

    make keras_codegen
