#!/usr/bin/env python3
"""
Script simple de remplacement JWT - Version Sécurisée
Remplace les patterns d'extraction JWT par le nouveau AuthenticationHelper
"""

import os
import re
import glob

def find_files_with_jwt_extraction(base_path):
    """Trouve tous les fichiers Java avec des extractions JWT"""
    pattern = os.path.join(base_path, "src/main/java/**/*.java")
    java_files = glob.glob(pattern, recursive=True)
    
    files_with_issues = []
    
    for file_path in java_files:
        # Ignore certains fichiers
        if any(skip in file_path for skip in ['Test.java', 'UserUuidMigration.java', 'AuthenticationHelper.java', 'UsersController.java']):
            continue
            
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Cherche les patterns problématiques
            has_jwt_extraction = bool(re.search(r'jwtUtil\.extractUserId\(', content))
            has_token_substring = bool(re.search(r'token\.substring\(7\)', content))
            has_findbyid_userid = bool(re.search(r'usersRepository\.findById\(userId\)', content))
            
            if has_jwt_extraction or has_token_substring or has_findbyid_userid:
                files_with_issues.append({
                    'path': file_path,
                    'name': os.path.basename(file_path),
                    'jwt_extraction': has_jwt_extraction,
                    'token_substring': has_token_substring,
                    'findbyid': has_findbyid_userid
                })
        except Exception as e:
            print(f"⚠️  Erreur lecture {file_path}: {e}")
    
    return files_with_issues

def show_files_to_modify(files):
    """Affiche les fichiers qui nécessitent des modifications"""
    print("📋 Fichiers nécessitant des modifications:")
    print("=" * 60)
    
    for i, file_info in enumerate(files, 1):
        print(f"{i:2d}. {file_info['name']}")
        issues = []
        if file_info['jwt_extraction']:
            issues.append("extractUserId")
        if file_info['token_substring']:
            issues.append("token.substring(7)")
        if file_info['findbyid']:
            issues.append("findById(userId)")
        
        print(f"     Issues: {', '.join(issues)}")
        print(f"     Path: {file_info['path']}")
        print()

def create_replacement_patterns():
    """Définit les patterns de remplacement"""
    return [
        {
            'name': 'Pattern 1: Extraction complète avec try-catch',
            'pattern': re.compile(
                r'String\s+jwtToken\s*=\s*token\.substring\(7\);\s*'
                r'Long\s+userId;\s*'
                r'try\s*\{\s*'
                r'userId\s*=\s*jwtUtil\.extractUserId\(jwtToken\);\s*'
                r'\}\s*catch\s*\([^}]*\)\s*\{\s*'
                r'throw\s+new\s+RuntimeException\([^}]*\);\s*'
                r'\}\s*'
                r'User\s+user\s*=\s*usersRepository\.findById\(userId\)\s*'
                r'\.orElseThrow\([^;]*\);',
                re.DOTALL | re.MULTILINE
            ),
            'replacement': 'User user = authHelper.getAuthenticatedUserWithFallback(request);'
        },
        {
            'name': 'Pattern 2: Extraction simple',
            'pattern': re.compile(
                r'String\s+jwtToken\s*=\s*token\.substring\(7\);\s*'
                r'Long\s+userId\s*=\s*jwtUtil\.extractUserId\(jwtToken\);',
                re.DOTALL | re.MULTILINE
            ),
            'replacement': 'String userUuid = jwtUtil.extractUserUuid(token.substring(7));\n        User user = usersRepository.findByUuid(userUuid).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));'
        },
        {
            'name': 'Pattern 3: @RequestHeader token vers HttpServletRequest',
            'pattern': re.compile(r'@RequestHeader\("Authorization"\)\s+String\s+token'),
            'replacement': 'HttpServletRequest request'
        }
    ]

def apply_replacements_to_file(file_path, patterns):
    """Applique les remplacements à un fichier"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        modifications_made = []
        
        for pattern_info in patterns:
            matches = list(pattern_info['pattern'].finditer(content))
            if matches:
                content = pattern_info['pattern'].sub(pattern_info['replacement'], content)
                modifications_made.append(f"{pattern_info['name']} ({len(matches)} occurrence(s))")
        
        # Ajoute les imports nécessaires si des modifications ont été faites
        if modifications_made:
            content = add_required_imports(content)
            content = add_autowired_auth_helper(content)
        
        # Sauvegarde seulement si il y a eu des changements
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return modifications_made
        
        return []
        
    except Exception as e:
        print(f"❌ Erreur lors du traitement de {file_path}: {e}")
        return []

def add_required_imports(content):
    """Ajoute les imports nécessaires"""
    imports_needed = []
    
    if 'authHelper.getAuthenticated' in content and 'import com.xpertcash.service.AuthenticationHelper;' not in content:
        imports_needed.append('import com.xpertcash.service.AuthenticationHelper;')
    
    if 'HttpServletRequest request' in content and 'import jakarta.servlet.http.HttpServletRequest;' not in content:
        imports_needed.append('import jakarta.servlet.http.HttpServletRequest;')
    
    if imports_needed:
        # Trouve la position après le dernier import
        import_pattern = re.compile(r'^import\s+[^;]+;', re.MULTILINE)
        imports = list(import_pattern.finditer(content))
        
        if imports:
            last_import = imports[-1]
            insert_pos = last_import.end()
            new_imports = '\n' + '\n'.join(imports_needed)
            content = content[:insert_pos] + new_imports + content[insert_pos:]
    
    return content

def add_autowired_auth_helper(content):
    """Ajoute @Autowired AuthenticationHelper si nécessaire"""
    if 'authHelper.getAuthenticated' in content and 'private AuthenticationHelper authHelper;' not in content:
        # Trouve le dernier @Autowired
        autowired_pattern = re.compile(r'(@Autowired\s+[^;]+;)', re.MULTILINE)
        autowired_matches = list(autowired_pattern.finditer(content))
        
        if autowired_matches:
            last_autowired = autowired_matches[-1]
            insert_pos = last_autowired.end()
            new_autowired = '\n    @Autowired\n    private AuthenticationHelper authHelper;'
            content = content[:insert_pos] + new_autowired + content[insert_pos:]
    
    return content

def main():
    base_path = "/Users/mac/Desktop/Tiak3da/XpertCashBack"
    
    print("🔍 Recherche des fichiers avec extractions JWT...")
    files_with_issues = find_files_with_jwt_extraction(base_path)
    
    if not files_with_issues:
        print("✅ Aucun fichier nécessitant des modifications trouvé!")
        return
    
    show_files_to_modify(files_with_issues)
    
    response = input(f"\n❓ Voulez-vous modifier ces {len(files_with_issues)} fichier(s)? (y/N): ")
    
    if response.lower() != 'y':
        print("❌ Opération annulée")
        return
    
    print("\n🔧 Application des modifications...")
    patterns = create_replacement_patterns()
    
    modified_files = []
    
    for file_info in files_with_issues:
        print(f"   📝 {file_info['name']}...", end=' ')
        
        modifications = apply_replacements_to_file(file_info['path'], patterns)
        
        if modifications:
            modified_files.append({
                'name': file_info['name'],
                'path': file_info['path'],
                'modifications': modifications
            })
            print("✅")
        else:
            print("⏭️")
    
    print(f"\n🎉 Migration terminée!")
    print(f"📊 {len(modified_files)} fichier(s) modifié(s)")
    
    if modified_files:
        print("\n📋 Résumé des modifications:")
        for file_info in modified_files:
            print(f"   • {file_info['name']}:")
            for mod in file_info['modifications']:
                print(f"     - {mod}")
    
    print("\n⚠️  Prochaines étapes:")
    print("   1. Vérifier la compilation: mvn compile")
    print("   2. Tester les endpoints modifiés")
    print("   3. Redémarrer l'application")

if __name__ == "__main__":
    main()
