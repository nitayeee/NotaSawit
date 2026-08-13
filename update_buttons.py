import os, re

target_files = []
for root, _, files in os.walk(r'd:\Notasawit\app\src\main\res\layout'):
    for file in files:
        if file.startswith('fragment_section') or file.startswith('fragment_kl_') or file == 'activity_audit_internal.xml' or file == 'activity_coba.xml':
            target_files.append(os.path.join(root, file))

for filepath in target_files:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Replace backgroundTint="@color/agri_green_dark" with "@color/secondary"
        content = re.sub(r'(<Button[^>]*?app:backgroundTint=")@color/agri_green_dark("[^>]*?>)', r'\1@color/secondary\2', content)
        content = re.sub(r'(<Button[^>]*?app:backgroundTint=")@color/primary("[^>]*?>)', r'\1@color/secondary\2', content)
        
        # Change text color of these buttons to tertiary for contrast on Gold
        def replace_text_color_on_secondary(match):
            button_content = match.group(0)
            if 'app:backgroundTint="@color/secondary"' in button_content:
                return re.sub(r'android:textColor="#[A-Fa-f0-9]+"', 'android:textColor="@color/tertiary"', button_content)
            return button_content

        content = re.sub(r'<Button[^>]*>', replace_text_color_on_secondary, content)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
    except Exception as e:
        print(f'Error on {filepath}: {e}')
print('Updated buttons successfully.')
