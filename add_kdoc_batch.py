import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
import add_kdoc

# Process remaining small packages
ROOT = r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc"
dirs = [
    "bootstrap", "context", "database", "utils", "environment",
    "settlement", "addons", "topology", "integration", "reputation",
    "util", "version",
]
total = 0
for d in dirs:
    fp = os.path.join(ROOT, d)
    if os.path.isdir(fp):
        add_kdoc.ENGINE_DIR = fp
        count = 0
        for root, _, files in os.walk(fp):
            for f in sorted(files):
                if f.endswith('.kt'):
                    try:
                        if add_kdoc.add_kdoc_to_file(os.path.join(root, f)):
                            count += 1
                    except Exception as e:
                        print(f"Error processing {f}: {e}")
        print(f"{d}: {count} files")
        total += count
print(f"Total: {total} files")
