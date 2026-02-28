import glob
import os

base_dir = "d:/PROYECTOS/ANDROID KOTLIN/All Proyects/LECTOR QR/app/src/main/java/com/scannerpro/lectorqr/presentation/ui/create"
files = glob.glob(os.path.join(base_dir, "**/*.kt"), recursive=True)

for f_path in files:
    with open(f_path, 'r', encoding='utf-8') as file:
        content = file.read()
        
    if "settingsRepository.isPremium.kotlinx.coroutines.flow.first()" in content:
        print(f"Fixing {f_path}")
        if "import kotlinx.coroutines.flow.first" not in content:
            content = content.replace("import kotlinx.coroutines.flow.update\n", "import kotlinx.coroutines.flow.update\nimport kotlinx.coroutines.flow.first\n")
        
        content = content.replace("settingsRepository.isPremium.kotlinx.coroutines.flow.first()", "settingsRepository.isPremium.first()")
        
        with open(f_path, 'w', encoding='utf-8') as file:
            file.write(content)
print("Done!")
