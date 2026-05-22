import streamlit as st
import cv2
import numpy as np
import tensorflow as tf
import joblib
import matplotlib.pyplot as plt
from lime import lime_image
from skimage.segmentation import mark_boundaries


st.set_page_config(page_title="KoS3 Clinical AI", layout="wide")
st.title("🧠 KoS3: Transparent Brain Tumor Diagnostics")
st.markdown("Upload a MRI scan to receive a diagnostic prediction and visual explanation.")


@st.cache_resource
def load_models():
    cnn = tf.keras.models.load_model('kos3_cnn.keras')
    cnn.build((None, 64, 64, 1))
    rf = joblib.load('kos3_random_forest.pkl')
    return cnn, rf

try:
    cnn_model, rf_model = load_models()
except Exception as e:
    st.error("⚠️ Could not load models. Did you run the training script and save them?")
    st.stop()


def generate_gradcam(img_array, model, last_conv_layer_name="last_conv_layer"):
    
    inputs = tf.keras.Input(shape=(64, 64, 1))
    x = inputs
    last_conv_output = None
    
    
    for layer in model.layers:
        x = layer(x)
        if layer.name == last_conv_layer_name:
            last_conv_output = x
            
    # Create the clean, error-free Functional model
    grad_model = tf.keras.models.Model(inputs, [last_conv_output, x])
    
    # Calculate gradients
    with tf.GradientTape() as tape:
        last_conv_layer_output, preds = grad_model(img_array)
        class_channel = preds[:, 0] # Binary classification output

    grads = tape.gradient(class_channel, last_conv_layer_output)
    pooled_grads = tf.reduce_mean(grads, axis=(0, 1, 2))
    
    # Multiply channels by their gradients
    last_conv_layer_output = last_conv_layer_output[0]
    heatmap = last_conv_layer_output @ pooled_grads[..., tf.newaxis]
    heatmap = tf.squeeze(heatmap)
    
    # Normalize heatmap
    heatmap = tf.maximum(heatmap, 0) / tf.math.reduce_max(heatmap)
    return heatmap.numpy()


def rf_predict_proba_wrapper(images):
    """LIME passes RGB images, but RF expects flattened grayscale 64x64."""
    batch_size = images.shape[0]
    gray_images = np.array([cv2.cvtColor((img * 255).astype(np.uint8), cv2.COLOR_RGB2GRAY) for img in images])
    flat_images = gray_images.reshape(batch_size, -1)
    return rf_model.predict_proba(flat_images)


sidebar = st.sidebar
sidebar.header("Settings")
model_choice = sidebar.radio("Select Diagnostic Engine:", ["Deep Learning (CNN + Grad-CAM)", "Classical ML (Random Forest + LIME)"])

uploaded_file = st.file_uploader("Upload MRI Scan (JPEG/PNG)", type=["jpg", "jpeg", "png"])

if uploaded_file is not None:
    # Read the image
    file_bytes = np.asarray(bytearray(uploaded_file.read()), dtype=np.uint8)
    img_raw = cv2.imdecode(file_bytes, cv2.IMREAD_GRAYSCALE)
    img_resized = cv2.resize(img_raw, (64, 64))
    
    col1, col2 = st.columns(2)
    
    with col1:
        st.subheader("Original MRI Scan")
        st.image(img_raw, use_container_width=True, channels="GRAY")
        
    with col2:
        st.subheader("XAI Diagnostic Heatmap")
        
        if "CNN" in model_choice:
            # --- CNN PREDICTION & GRAD-CAM ---
            img_cnn = img_resized.reshape(1, 64, 64, 1) / 255.0
            prediction = cnn_model.predict(img_cnn)[0][0]
            
            heatmap = generate_gradcam(img_cnn, cnn_model)
            heatmap_resized = cv2.resize(heatmap, (img_raw.shape[1], img_raw.shape[0]))
            
            # Create a color overlay
            heatmap_colored = cv2.applyColorMap(np.uint8(255 * heatmap_resized), cv2.COLORMAP_JET)
            img_rgb = cv2.cvtColor(img_raw, cv2.COLOR_GRAY2RGB)
            superimposed_img = cv2.addWeighted(img_rgb, 0.6, heatmap_colored, 0.4, 0)
            
            st.image(superimposed_img, use_container_width=True, channels="BGR")
            
            confidence = prediction if prediction > 0.5 else 1 - prediction
            diagnosis = "🚨 Tumor Detected" if prediction > 0.5 else "✅ Healthy (No Tumor)"
            st.success(f"**Diagnosis:** {diagnosis} (Confidence: {confidence*100:.2f}%)")

        else:
            # --- RANDOM FOREST PREDICTION & LIME ---
            img_flat = img_resized.flatten().reshape(1, -1)
            prediction_class = rf_model.predict(img_flat)[0]
            prediction_probs = rf_model.predict_proba(img_flat)[0]
            
            # LIME needs an RGB image to work with, even if the model uses grayscale
            img_rgb_lime = cv2.cvtColor(img_resized, cv2.COLOR_GRAY2RGB)
            
            explainer = lime_image.LimeImageExplainer()
            with st.spinner("Generating LIME Super-pixels... (This takes a few seconds)"):
                explanation = explainer.explain_instance(
                    img_rgb_lime, 
                    rf_predict_proba_wrapper, 
                    top_labels=1, hide_color=0, num_samples=500
                )
                
                temp, mask = explanation.get_image_and_mask(
                    explanation.top_labels[0], positive_only=True, num_features=5, hide_rest=False
                )
                img_boundary = mark_boundaries(temp / 255.0, mask)
            
            st.image(img_boundary, use_container_width=True, clamp=True)
            
            confidence = prediction_probs[1] if prediction_class == 1 else prediction_probs[0]
            diagnosis = "🚨 Tumor Detected" if prediction_class == 1 else "✅ Healthy (No Tumor)"
            st.success(f"**Diagnosis:** {diagnosis} (Confidence: {confidence*100:.2f}%)")
            st.info("LIME Explanation: Yellow outlines indicate the 'super-pixels' the Random Forest used to make this decision.")
