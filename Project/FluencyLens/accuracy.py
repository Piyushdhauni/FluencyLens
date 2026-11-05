real_fillers = 7
detected = 6
corrected = 6

precision = corrected / detected
recall = corrected / real_fillers

print("precision", round(precision * 100,1) ,"%")
print("recall", round(recall * 100,1) ,"%")