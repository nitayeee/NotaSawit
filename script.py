import re

with open('app/src/main/res/layout/activity_profil_petani.xml', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace TextViews with font family
def replace_font(match):
    tag = match.group(0)
    if 'android:fontFamily' not in tag:
        if 'textStyle=\"bold\"' in tag:
            tag = tag.replace('<TextView', '<TextView\n        android:fontFamily=\"@font/poppins_bold\"')
        else:
            tag = tag.replace('<TextView', '<TextView\n        android:fontFamily=\"@font/poppins_regular\"')
    return tag

content = re.sub(r'<TextView[^>]*>', replace_font, content)

# Also apply font to button/edit if it's a TextView

# Write back
with open('app/src/main/res/layout/activity_profil_petani.xml', 'w', encoding='utf-8') as f:
    f.write(content)
