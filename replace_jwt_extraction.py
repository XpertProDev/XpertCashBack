#!/usr/bin/env python3
"""
Script de migration automatique pour remplacer les extractions JWT manuelles
par le nouveau service AuthenticationHelper avec UUID.

Ce script remplace les patterns:
- String jwtToken = token.substring(7);
- Long userId = jwtUtil.extractUserId(jwtToken);
- User user = usersRepository.findById(userId)...

Par:
- User user = authHelper.getAuthenticatedUserWithFallback(request);
"""

import os
import re
import glob
from typing import List, Tuple

class JwtMigrationScript:
    def __init__(self, base_path: str):
        self.base_path = base_path
        self.java_files = []
        self.modifications = []
        
    def find_java_files(self) -> List[str]:
        """Trouve tous les fichiers Java dans src/main/java"""
        pattern = os.path.join(self.base_path, "src/main/java/**/*.java")
        self.java_files = glob.glob(pattern, recursive=True)
        print(f"🔍 Trouvé {len(self.java_files)} fichiers Java")
        return self.java_files
    
    def analyze_file(self, file_path: str) -> Tuple[str, List[str]]:
        """Analyse un fichier pour détecter les patterns à remplacer"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        issues = []
        
        # Pattern 1: Extraction manuelle du token
        if re.search(r'token\.substring\(7\)', content):
            issues.append("Extraction manuelle du token")
        
        # Pattern 2: extractUserId usage
        if re.search(r'jwtUtil\.extractUserId\(', content):
            issues.append("Utilisation de extractUserId")
        
        # Pattern 3: findById avec userId du token
        if re.search(r'usersRepository\.findById\(userId\)', content):
            issues.append("Recherche utilisateur par ID du token")
        
        return content, issues
    
    def replace_jwt_extraction_pattern(self, content: str, file_path: str) -> str:
        """Remplace les patterns d'extraction JWT complets"""
        
        # Pattern complexe: extraction + récupération utilisateur
        complex_pattern = re.compile(
            r'String\s+jwtToken\s*=\s*token\.substring\(7\);.*?'
            r'Long\s+userId;.*?'
            r'try\s*\{.*?'
            r'userId\s*=\s*jwtUtil\.extractUserId\(jwtToken\);.*?'
            r'\}\s*catch\s*\([^}]*\)\s*\{.*?'
            r'throw\s+new\s+RuntimeException\([^}]*\);.*?'
            r'\}.*?'
            r'User\s+user\s*=\s*usersRepository\.findById\(userId\).*?'
            r'\.orElseThrow\([^;]*\);',
            re.DOTALL | re.MULTILINE
        )
        
        def replace_complex(match):
            return 'User user = authHelper.getAuthenticatedUserWithFallback(request);'
        
        new_content = complex_pattern.sub(replace_complex, content)
        
        # Pattern simple: juste l'extraction
        simple_pattern = re.compile(
            r'String\s+jwtToken\s*=\s*token\.substring\(7\);.*?'
            r'Long\s+userId\s*=\s*jwtUtil\.extractUserId\(jwtToken\);',
            re.DOTALL | re.MULTILINE
        )
        
        def replace_simple(match):
            return 'User user = authHelper.getAuthenticatedUser(request);'
        
        new_content = simple_pattern.sub(replace_simple, new_content)
        
        return new_content
    
    def add_imports_if_needed(self, content: str, file_path: str) -> str:
        """Ajoute les imports nécessaires si pas déjà présents"""
        
        needs_auth_helper = 'authHelper.getAuthenticated' in content
        needs_http_request = 'HttpServletRequest request' in content or 'getAuthenticatedUser' in content
        
        imports_to_add = []
        
        if needs_auth_helper and 'import com.xpertcash.service.AuthenticationHelper;' not in content:
            imports_to_add.append('import com.xpertcash.service.AuthenticationHelper;')
        
        if needs_http_request and 'import jakarta.servlet.http.HttpServletRequest;' not in content:
            imports_to_add.append('import jakarta.servlet.http.HttpServletRequest;')
        
        if imports_to_add:
            # Trouve la dernière ligne d'import
            import_pattern = re.compile(r'^import\s+[^;]+;', re.MULTILINE)
            imports = list(import_pattern.finditer(content))
            
            if imports:
                last_import = imports[-1]
                insert_pos = last_import.end()
                
                # Ajoute les nouveaux imports
                new_imports = '\n' + '\n'.join(imports_to_add)
                content = content[:insert_pos] + new_imports + content[insert_pos:]
        
        return content
    
    def add_autowired_if_needed(self, content: str) -> str:
        """Ajoute @Autowired AuthenticationHelper si nécessaire"""
        
        if 'authHelper.getAuthenticated' in content and '@Autowired' in content:
            # Cherche s'il y a déjà AuthenticationHelper
            if 'private AuthenticationHelper authHelper;' not in content:
                # Trouve le dernier @Autowired
                autowired_pattern = re.compile(r'(@Autowired\s+private\s+[^;]+;)', re.MULTILINE)
                autowired_matches = list(autowired_pattern.finditer(content))
                
                if autowired_matches:
                    last_autowired = autowired_matches[-1]
                    insert_pos = last_autowired.end()
                    
                    new_autowired = '\n    @Autowired\n    private AuthenticationHelper authHelper;'
                    content = content[:insert_pos] + new_autowired + content[insert_pos:]
        
        return content
    
    def update_method_signatures(self, content: str) -> str:
        """Met à jour les signatures de méthodes pour utiliser HttpServletRequest"""
        
        # Remplace @RequestHeader("Authorization") String token par HttpServletRequest request
        header_pattern = re.compile(
            r'@RequestHeader\("Authorization"\)\s+String\s+token',
            re.MULTILINE
        )
        
        content = header_pattern.sub('HttpServletRequest request', content)
        
        return content
    
    def process_file(self, file_path: str) -> bool:
        """Traite un fichier complet"""
        print(f"🔧 Traitement de {os.path.basename(file_path)}...")
        
        original_content, issues = self.analyze_file(file_path)
        
        if not issues:
            return False
        
        print(f"   📋 Problèmes détectés: {', '.join(issues)}")
        
        # Applique les transformations
        new_content = original_content
        new_content = self.replace_jwt_extraction_pattern(new_content, file_path)
        new_content = self.update_method_signatures(new_content)
        new_content = self.add_imports_if_needed(new_content, file_path)
        new_content = self.add_autowired_if_needed(new_content)
        
        # Vérifie s'il y a eu des changements
        if new_content != original_content:
            # Sauvegarde le fichier
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            
            self.modifications.append({
                'file': file_path,
                'issues': issues
            })
            print(f"   ✅ Fichier modifié")
            return True
        else:
            print(f"   ⏭️  Aucune modification nécessaire")
            return False
    
    def run_migration(self):
        """Lance la migration complète"""
        print("🚀 Début de la migration JWT vers UUID...")
        print("=" * 60)
        
        # Trouve tous les fichiers Java
        self.find_java_files()
        
        # Traite chaque fichier
        modified_count = 0
        for file_path in self.java_files:
            # Ignore certains fichiers
            if any(skip in file_path for skip in ['Test.java', 'UserUuidMigration.java', 'AuthenticationHelper.java']):
                continue
            
            if self.process_file(file_path):
                modified_count += 1
        
        # Résumé
        print("\n" + "=" * 60)
        print("🎉 Migration terminée!")
        print(f"📊 {modified_count} fichier(s) modifié(s) sur {len(self.java_files)}")
        
        if self.modifications:
            print("\n📋 Fichiers modifiés:")
            for mod in self.modifications:
                print(f"   • {os.path.basename(mod['file'])}: {', '.join(mod['issues'])}")
        
        print("\n⚠️  Actions à faire manuellement:")
        print("   1. Vérifier la compilation avec mvn compile")
        print("   2. Tester les endpoints modifiés")
        print("   3. Commit les changements")

def main():
    # Chemin vers votre projet
    project_path = "/Users/mac/Desktop/Tiak3da/XpertCashBack"
    
    if not os.path.exists(project_path):
        print(f"❌ Chemin du projet non trouvé: {project_path}")
        return
    
    # Lance la migration
    migrator = JwtMigrationScript(project_path)
    migrator.run_migration()

if __name__ == "__main__":
    main()
