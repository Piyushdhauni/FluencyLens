import pandas as pd
import joblib

from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import LeaveOneOut, cross_val_predict
from sklearn.metrics import classification_report

data = pd.read_csv("dataset.csv")
print(data.columns.to_list())

X = data[["wpm", "slow_words", "fillers", "filler_percent"]]
y = data["label"]

model = RandomForestClassifier(n_estimators=100 , random_state=42)

predictions = cross_val_predict(model, X , y , cv=LeaveOneOut())
print(classification_report(y, predictions))

print("Correct :", (predictions == y).sum(), "out of ", len(y))

model.fit(X,y)

for name, score in zip (X.columns, model.feature_importances_):
    print(name, round(score, 2))

joblib.dump(model , "fluency_model.joblib")
print("Model saved")