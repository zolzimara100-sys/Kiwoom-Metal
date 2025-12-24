import pandas as pd
import os

# Define the input path
input_file = 'classified_stock_list.csv'

# Ensure the input file exists
if not os.path.exists(input_file):
    print(f"Error: {input_file} not found.")
    exit(1)

# Read the CSV file
df = pd.read_csv(input_file)

# Fill NaN with empty string
df['categories'] = df['categories'].fillna('')

# Filter rows where categories are empty
missing_df = df[df['categories'] == '']

total_count = len(df)
missing_count = len(missing_df)
classified_count = total_count - missing_count

print(f"Total Stocks: {total_count}")
print(f"Classified: {classified_count}")
print(f"Unclassified (Missing Sector): {missing_count}")

if missing_count > 0:
    print("\n[Unclassified Stocks List]")
    # Print max 100 rows
    print(missing_df[['code', 'name']].to_string(index=False))
else:
    print("\nAll stocks have been classified!")
