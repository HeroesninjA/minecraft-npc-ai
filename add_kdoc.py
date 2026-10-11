import re
import os

BASE_DIRS = {
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\engine": "engine",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\world": "world",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\gui": "gui",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\debug": "debug",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\commands": "commands",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\spawn": "spawn",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\progression": "progression",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\routine": "routine",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\managers": "managers",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\mcp": "mcp",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\story": "story",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\listeners": "listeners",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\economy": "economy",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\npc": "npc",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\platform": "platform",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\worldgen": "worldgen",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\bootstrap": "bootstrap",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\context": "context",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\database": "database",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\utils": "utils",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\environment": "environment",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\settlement": "settlement",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\addons": "addons",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\topology": "topology",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\integration": "integration",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\reputation": "reputation",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\util": "util",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-core-plugin\src\main\kotlin\ro\ainpc\version": "version",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-api\src\main\kotlin\ro\ainpc\api": "api",
    r"C:\Users\HeroesninjA\IdeaProjects\test\ainpc-scenario-medieval\src\main\kotlin\ro\ainpc\addons\medieval": "scenario-medieval",
}

# Only match class/object declarations (NOT functions)
CLASS_PATTERN = re.compile(r'^(\s*)(?:public\s+)?(?:open\s+)?(?:class|object|enum class|interface|data class)\s+(\w+)')

def add_kdoc_to_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    lines = content.split('\n')
    modified = False
    result_lines = []
    i = 0
    
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        
        # Skip existing KDoc blocks
        if stripped.startswith("/**"):
            result_lines.append(line)
            i += 1
            while i < len(lines) and not lines[i].strip().endswith("*/"):
                result_lines.append(lines[i])
                i += 1
            if i < len(lines):
                result_lines.append(lines[i])
                i += 1
            continue
        
        # Class/object/enum/interface declarations
        m = CLASS_PATTERN.match(stripped)
        if m:
            name = m.group(2)
            desc = name.replace('_', ' ').title() + '.'
            indent = m.group(1)
            result_lines.append(f"{indent}/**")
            result_lines.append(f"{indent} * {desc}")
            result_lines.append(f"{indent} */")
            result_lines.append(line)
            modified = True
            i += 1
            continue
        
        result_lines.append(line)
        i += 1
    
    if modified:
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.write('\n'.join(result_lines))
        return True
    return False

total = 0
for dir_path, label in sorted(BASE_DIRS.items(), key=lambda x: x[1]):
    count = 0
    for root, dirs, files in os.walk(dir_path):
        for f in sorted(files):
            if f.endswith('.kt'):
                fp = os.path.join(root, f)
                try:
                    if add_kdoc_to_file(fp):
                        count += 1
                except Exception as e:
                    print(f"Error: {f}: {e}")
    print(f"{label}: {count} files")
    total += count

print(f"\nTotal: {total} files modified")
