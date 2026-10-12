import re
import xml.etree.ElementTree as ET

settings_path = r'app/src/main/java/com/offlinenotifier/app/ui/SettingsScreen.kt'
main_path = r'app/src/main/java/com/offlinenotifier/app/MainActivity.kt'
strings_xml_path = r'app/src/main/res/values/strings.xml'

with open(settings_path, 'r', encoding='utf-8') as f:
    settings_code = f.read()

with open(main_path, 'r', encoding='utf-8') as f:
    main_code = f.read()

tree = ET.parse(strings_xml_path)
root = tree.getroot()

existing_strings = {}
for child in root:
    if child.tag == 'string':
        name = child.attrib.get('name')
        existing_strings[child.text] = name

new_strings = {}

def get_or_add_string(text, prefix="str"):
    if text in existing_strings:
        return existing_strings[text]
    if text in new_strings:
        return new_strings[text]
    
    # generate a valid string name
    clean_name = re.sub(r'[^a-zA-Z0-9]', '_', text.lower())
    clean_name = re.sub(r'_+', '_', clean_name).strip('_')
    # Truncate
    clean_name = clean_name[:25]
    if not clean_name:
        clean_name = "txt"
    name = f"{prefix}_{clean_name}_{len(new_strings)}"
    new_strings[text] = name
    return name

import builtins
def replacer_compose(match):
    full_match = match.group(0)
    text = match.group(1)
    # ignore strings with $ inside because they are templates
    if '$' in text or len(text) <= 1:
        return full_match
    name = get_or_add_string(text)
    return full_match.replace(f'"{text}"', f'stringResource(R.string.{name})')

def replacer_toast(match):
    full_match = match.group(0)
    text = match.group(1)
    if '$' in text or len(text) <= 1:
        return full_match
    name = get_or_add_string(text)
    return full_match.replace(f'"{text}"', f'context.getString(R.string.{name})')

# Replaces for Text("...")
settings_code = re.sub(r'Text\(\s*"([^"]+)"', replacer_compose, settings_code)
# Replaces for text = "..."
settings_code = re.sub(r'text\s*=\s*"([^"]+)"', replacer_compose, settings_code)
# Replaces for Toast.makeText(context, "...")
settings_code = re.sub(r'Toast\.makeText\([^,]+,\s*"([^"]+)"', replacer_toast, settings_code)

# MainActivity
main_code = re.sub(r'Text\(\s*"([^"]+)"', replacer_compose, main_code)
main_code = re.sub(r'text\s*=\s*"([^"]+)"', replacer_compose, main_code)

for txt, name in new_strings.items():
    elem = ET.SubElement(root, 'string')
    elem.set('name', name)
    # Escape characters for XML
    txt_escaped = txt.replace("'", "\\'").replace('"', '\\"')
    elem.text = txt_escaped

# save xml
xml_string = ET.tostring(root, encoding='utf-8', xml_declaration=True).decode('utf-8')
with open(strings_xml_path, 'w', encoding='utf-8') as f:
    f.write(xml_string)

# Ensure R is imported
if 'import androidx.compose.ui.res.stringResource' not in settings_code:
    settings_code = settings_code.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport androidx.compose.ui.res.stringResource\nimport com.offlinenotifier.app.R')
    
if 'import androidx.compose.ui.res.stringResource' not in main_code:
    main_code = main_code.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.res.stringResource')

with open(settings_path, 'w', encoding='utf-8') as f:
    f.write(settings_code)
with open(main_path, 'w', encoding='utf-8') as f:
    f.write(main_code)

print(f"Extracted {len(new_strings)} new strings.")
