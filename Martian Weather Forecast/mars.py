import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.model_selection import KFold, cross_validate, cross_val_predict
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.metrics import confusion_matrix, classification_report

# ==========================================
# 1. DATA LOADING & PRE-PROCESSING
# ==========================================
print("Loading and cleaning Mars Weather dataset...")
df = pd.read_csv('mars-weather.csv')

# Drop irrelevant/empty columns and drop missing values (NaNs)
df = df.drop(columns=['id', 'terrestrial_date', 'wind_speed', 'atmo_opacity']).dropna()

# Feature Extraction: Convert 'Month X' to integer X
df['month'] = df['month'].str.replace('Month ', '').astype(int)

print(f"Dataset ready. Total valid rows: {len(df)}\n")

# Setup the 5-Fold split (shuffled for randomness)
kf = KFold(n_splits=5, shuffle=True, random_state=42)

# ==========================================
# EXPERIMENT 1: REGRESSION (5-Fold CV)
# ==========================================
print("-" * 50)
print("EXPERIMENT 1: PREDICTING MAXIMUM TEMPERATURE (5-FOLD CV)")
print("-" * 50)

# Define Features and Target
X_reg = df.drop(columns=['max_temp'])
y_reg = df['max_temp']
reg_model = RandomForestRegressor(n_estimators=100, random_state=42)


scoring_reg = {
    'mae': 'neg_mean_absolute_error',
    'mse': 'neg_mean_squared_error',
    'r2': 'r2'
}

# Run 5-Fold Cross-Validation
print("Running 5-Fold Cross Validation for Regression...")
cv_reg_results = cross_validate(reg_model, X_reg, y_reg, cv=kf, scoring=scoring_reg)

# Calculate the average (mean) scores across all 5 folds

avg_mae = -cv_reg_results['test_mae'].mean()
avg_mse = -cv_reg_results['test_mse'].mean()
avg_rmse = np.mean(np.sqrt(-cv_reg_results['test_mse']))
avg_r2 = cv_reg_results['test_r2'].mean()

print(f"Average MAE across 5 folds:  {avg_mae:.2f} °C")
print(f"Average MSE across 5 folds:  {avg_mse:.2f}")
print(f"Average RMSE across 5 folds: {avg_rmse:.2f} °C")
print(f"Average R2 across 5 folds:   {avg_r2:.2f}\n")

# Explainability: Train once on full data just to extract Feature Importances
reg_model.fit(X_reg, y_reg)
feature_importance_df = pd.DataFrame({
    'Feature': X_reg.columns, 
    'Importance': reg_model.feature_importances_
}).sort_values(by='Importance', ascending=False)

# Plot Feature Importances
plt.figure(figsize=(8, 5))
sns.barplot(x='Importance', y='Feature', data=feature_importance_df, palette='viridis', hue='Feature', legend=False)
plt.title('Explainability: Drivers of Mars Maximum Temp')
plt.xlabel('Importance Score')
plt.ylabel('Features')
plt.tight_layout()
plt.savefig('regression_feature_importance_cv.png')
print("-> Saved feature importance graph as 'regression_feature_importance_cv.png'\n")


# ==========================================
# EXPERIMENT 2: CLASSIFICATION (5-Fold CV)
# ==========================================
print("-" * 50)
print("EXPERIMENT 2: IDENTIFYING SEASONAL WEATHER PATTERNS (5-FOLD CV)")
print("-" * 50)


X_clf = df[['min_temp', 'max_temp', 'pressure']]
y_clf = df['month']
clf_model = RandomForestClassifier(n_estimators=100, random_state=42)

# Run Cross-Validation to get average accuracy, precision, recall, and f1
scoring_clf = ['accuracy', 'precision_weighted', 'recall_weighted', 'f1_weighted']
print("Running 5-Fold Cross Validation for Classification...")
cv_clf_results = cross_validate(clf_model, X_clf, y_clf, cv=kf, scoring=scoring_clf)

print(f"Average Accuracy:  {cv_clf_results['test_accuracy'].mean() * 100:.2f}%")
print(f"Average Precision: {cv_clf_results['test_precision_weighted'].mean() * 100:.2f}%")
print(f"Average Recall:    {cv_clf_results['test_recall_weighted'].mean() * 100:.2f}%")
print(f"Average F1-Score:  {cv_clf_results['test_f1_weighted'].mean() * 100:.2f}%\n")


yc_pred_cv = cross_val_predict(clf_model, X_clf, y_clf, cv=kf)

print("Comprehensive Classification Report (All 5 Folds Combined):")
print(classification_report(y_clf, yc_pred_cv))

# Confusion Matrix built from all 5 folds
cm_cv = confusion_matrix(y_clf, yc_pred_cv)

# Plot Confusion Matrix
plt.figure(figsize=(10, 8))
sns.heatmap(cm_cv, annot=True, fmt='d', cmap='Blues', 
            xticklabels=range(1, 13), yticklabels=range(1, 13))
plt.title('5-Fold CV Confusion Matrix: Predicting Martian Month')
plt.ylabel('Actual Martian Month')
plt.xlabel('Predicted Martian Month')
plt.tight_layout()
plt.savefig('classification_confusion_matrix_cv.png')
print("-> Saved cross-validated confusion matrix graph as 'classification_confusion_matrix_cv.png'")