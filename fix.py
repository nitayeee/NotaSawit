import os
import re
import glob

def fix_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    def deduplicate_attrs(match):
        tag_content = match.group(0)
        font_families = re.findall(r'android:fontFamily="[^"]+"', tag_content)
        if len(font_families) > 1:
            first_font = font_families[0]
            clean_tag = re.sub(r'\s*android:fontFamily="[^"]+"', '', tag_content)
            if clean_tag.endswith('/>'):
                clean_tag = clean_tag[:-2] + f' {first_font} />'
            else:
                clean_tag = clean_tag[:-1] + f' {first_font}>'
            return clean_tag
        return tag_content

    new_content = re.sub(r'<[^>]+>', deduplicate_attrs, content)
    
    if new_content != content:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Fixed {filepath}")

for file in glob.glob("d:/Notasawit/app/src/main/res/layout/**/*.xml", recursive=True):
    fix_file(file)
