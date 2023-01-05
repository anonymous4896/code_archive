import tensorflow as tf


def get_tf_gradient(x, model, top_pred_idx):
    """Computes the gradients of outputs w.r.t input image.

    Args:
        x: 4D image tensor
        top_pred_idx: Predicted label for the input image

    Returns:
        Gradients of the predictions w.r.t img_input
    """
    images = tf.cast(x, tf.float32)

    with tf.GradientTape() as tape:
        tape.watch(images)
        preds = model(images)
        top_class = preds[:, top_pred_idx]

    grads = tape.gradient(top_class, images)
    return grads
