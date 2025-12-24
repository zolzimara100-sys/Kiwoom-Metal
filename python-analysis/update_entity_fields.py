import re
import shutil
import sys
import os

java_file = '/Users/juhyunhwang/kiwoom/src/main/java/com/stocktrading/kiwoom/adapter/out/persistence/entity/StockInvestorMaEntity.java'
backup_file = java_file + '.bak'

if not os.path.exists(java_file):
    print(f"Error: Java file not found at {java_file}")
    sys.exit(1)

try:
    shutil.copy(java_file, backup_file)
    print(f"Backup created: {backup_file}")
except Exception as e:
    print(f"Backup failed: {e}")
    sys.exit(1)

with open(java_file, 'r', encoding='utf-8') as f:
    content = f.read()

# Regex to capture investor blocks from ma1m to ma12m
# Group 1: snake_case name (e.g. frgnr_invsr)
# Group 2: camelCase prefix (e.g. frgnrInvsr) captured from the last line (Ma12m)
pattern = re.compile(
    r'\s+@Column\(name = "([a-z_]+)_ma1m".+?private BigDecimal ([a-zA-Z0-9]+)Ma12m;\n',
    re.DOTALL
)

def replacer(match):
    snake = match.group(1)
    camel = match.group(2)
    # print(f"Replacing monthly logic for: {snake}")
    # Replacement text: adds ma120 and ma140 fields, maintaining 4-space indent
    return f"""
    @Column(name = "{snake}_ma120", precision = 15)
    private BigDecimal {camel}Ma120;

    @Column(name = "{snake}_ma140", precision = 15)
    private BigDecimal {camel}Ma140;
"""

new_content, count = pattern.subn(replacer, content)

print(f"Total blocks replaced: {count}")

if count > 0:
    with open(java_file, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Optimization successful: Monthly fields removed, 120/140 fields added.")
else:
    print("No blocks matched. Please check the regex pattern.")
