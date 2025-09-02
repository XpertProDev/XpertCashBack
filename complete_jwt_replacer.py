#!/usr/bin/env python3
"""
Script de remplacement JWT complet - Version robuste
Gère toutes les variations d'extraction JWT dans le codebase
"""

import os
import re
import glob
from typing import List, Dict, Any

class CompletJwtReplacer:
    def __init__(self, base_path: str):
        self.base_path = base_path
        self.modifications_log = []
    
    def find_java_files(self) -> List[str]:
        """Trouve tous les fichiers Java pertinents"""
        pattern = os.path.join(self.base_path, "src/main/java/**/*.java")
        java_files = glob.glob(pattern, recursive=True)
        
        # Filtre les fichiers à ignorer
        excluded = ['Test.java', 'UserUuidMigration.java', 'AuthenticationHelper.java']
        return [f for f in java_files if not any(ex in f for ex in excluded)]
    
    def analyze_jwt_patterns(self, content: str) -> Dict[str, List[Dict]]:
        """Analyse tous les patterns JWT dans un fichier"""
        patterns_found = {
            'simple_extraction': [],
            'with_try_catch': [],
            'header_params': [],
            'findbyid_calls': []
        }
        
        # Pattern 1: Extraction simple
        simple_pattern = r'(?:String\s+jwtToken\s*=\s*token\.substring\(7\);?\s*)?(?:Long\s+userId\s*=\s*)?jwtUtil\.extractUserId\([^)]+\)'
        for match in re.finditer(simple_pattern, content, re.MULTILINE):
            patterns_found['simple_extraction'].append({
                'match': match,
                'text': match.group()
            })
        
        # Pattern 2: Avec try-catch
        try_catch_pattern = r'try\s*\{\s*userId\s*=\s*jwtUtil\.extractUserId\([^}]+\}\s*catch[^}]+\}'
        for match in re.finditer(try_catch_pattern, content, re.DOTALL):
            patterns_found['with_try_catch'].append({
                'match': match,
                'text': match.group()
            })
        
        # Pattern 3: @RequestHeader
        header_pattern = r'@RequestHeader\("Authorization"\)\s+String\s+\w+'
        for match in re.finditer(header_pattern, content):
            patterns_found['header_params'].append({
                'match': match,
                'text': match.group()
            })
        
        # Pattern 4: findById avec userId
        findbyid_pattern = r'usersRepository\.findById\(userId\)[^;]*;'
        for match in re.finditer(findbyid_pattern, content):
            patterns_found['findbyid_calls'].append({
                'match': match,
                'text': match.group()
            })
        
        return patterns_found
    
    def replace_method_signature(self, content: str) -> str:
        """Remplace les signatures de méthodes"""
        # @RequestHeader("Authorization") String token -> HttpServletRequest request
        content = re.sub(
            r'@RequestHeader\("Authorization"\)\s+String\s+(\w+)',
            r'HttpServletRequest request',
            content
        )
        return content
    
    def replace_jwt_extraction_blocks(self, content: str) -> str:
        """Remplace les blocs d'extraction JWT complets"""
        
        # Pattern complexe avec try-catch et findById
        complex_pattern = re.compile(
            r'String\s+jwtToken\s*=\s*[^;]+\.substring\(7\);.*?'
            r'Long\s+userId[^;]*;.*?'
            r'try\s*\{[^}]*userId\s*=[^}]*extractUserId[^}]*\}[^}]*catch[^}]*\{[^}]*\}.*?'
            r'(?:User\s+\w+\s*=\s*)?usersRepository\.findById\(userId\)[^;]*;',
            re.DOTALL | re.MULTILINE
        )
        
        def replace_complex_block(match):
            # Extraire le nom de la variable user si présent
            user_match = re.search(r'User\s+(\w+)\s*=', match.group())
            user_var = user_match.group(1) if user_match else 'user'
            return f'User {user_var} = authHelper.getAuthenticatedUserWithFallback(request);'
        
        content = complex_pattern.sub(replace_complex_block, content)
        
        # Pattern simple avec extraction directe
        simple_pattern = re.compile(
            r'String\s+jwtToken\s*=\s*[^;]+\.substring\(7\);.*?'
            r'Long\s+userId\s*=\s*jwtUtil\.extractUserId\([^;]+\);',
            re.DOTALL | re.MULTILINE
        )
        
        content = simple_pattern.sub(
            'User user = authHelper.getAuthenticatedUserWithFallback(request);',
            content
        )
        
        # Pattern avec juste extractUserId + findById
        extract_find_pattern = re.compile(
            r'Long\s+userId\s*=\s*jwtUtil\.extractUserId\([^;]+\);.*?'
            r'(?:User\s+\w+\s*=\s*)?usersRepository\.findById\(userId\)[^;]*;',
            re.DOTALL | re.MULTILINE
        )
        
        def replace_extract_find(match):
            user_match = re.search(r'User\s+(\w+)\s*=', match.group())
            user_var = user_match.group(1) if user_match else 'user'
            return f'User {user_var} = authHelper.getAuthenticatedUserWithFallback(request);'
        
        content = extract_find_pattern.sub(replace_extract_find, content)
        
        return content
    
    def add_required_imports(self, content: str) -> str:
        """Ajoute les imports nécessaires"""
        imports_to_add = []
        
        if 'authHelper.' in content:
            if 'import com.xpertcash.service.AuthenticationHelper;' not in content:
                imports_to_add.append('import com.xpertcash.service.AuthenticationHelper;')
        
        if 'HttpServletRequest request' in content:
            if 'import jakarta.servlet.http.HttpServletRequest;' not in content:
                imports_to_add.append('import jakarta.servlet.http.HttpServletRequest;')
        
        if imports_to_add:
            # Trouve la position après les imports existants
            import_lines = re.findall(r'^import\s+[^;]+;', content, re.MULTILINE)
            if import_lines:
                last_import = import_lines[-1]
                import_pos = content.find(last_import) + len(last_import)
                new_imports = '\n' + '\n'.join(imports_to_add)
                content = content[:import_pos] + new_imports + content[import_pos:]
        
        return content
    
    def add_autowired_field(self, content: str) -> str:
        """Ajoute @Autowired AuthenticationHelper si nécessaire"""
        if 'authHelper.' in content and 'AuthenticationHelper authHelper' not in content:
            # Trouve la classe et ajoute le field
            class_match = re.search(r'(public\s+class\s+\w+[^{]*\{)', content)
            if class_match:
                class_end = class_match.end()
                autowired_field = '\n\n    @Autowired\n    private AuthenticationHelper authHelper;'
                content = content[:class_end] + autowired_field + content[class_end:]
        
        return content
    
    def process_file(self, file_path: str) -> Dict[str, Any]:
        """Traite un fichier complet"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                original_content = f.read()
            
            # Analyse les patterns
            patterns = self.analyze_jwt_patterns(original_content)
            total_patterns = sum(len(p) for p in patterns.values())
            
            if total_patterns == 0:
                return {'modified': False, 'patterns': 0}
            
            # Applique les transformations
            content = original_content
            content = self.replace_method_signature(content)
            content = self.replace_jwt_extraction_blocks(content)
            content = self.add_required_imports(content)
            content = self.add_autowired_field(content)
            
            # Vérifie si il y a eu des changements
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                return {
                    'modified': True,
                    'patterns': total_patterns,
                    'file': file_path,
                    'patterns_detail': patterns
                }
            
            return {'modified': False, 'patterns': total_patterns}
            
        except Exception as e:
            print(f"❌ Erreur traitement {os.path.basename(file_path)}: {e}")
            return {'modified': False, 'patterns': 0, 'error': str(e)}
    
    def run_complete_migration(self):
        """Lance la migration complète"""
        print("🚀 Migration JWT vers UUID - Version Complète")
        print("=" * 60)
        
        java_files = self.find_java_files()
        print(f"🔍 {len(java_files)} fichiers Java à analyser")
        
        modified_files = []
        total_patterns = 0
        
        for file_path in java_files:
            file_name = os.path.basename(file_path)
            print(f"   📝 {file_name}...", end=' ')
            
            result = self.process_file(file_path)
            
            if result.get('error'):
                print(f"❌ {result['error']}")
            elif result['modified']:
                print(f"✅ ({result['patterns']} pattern(s))")
                modified_files.append(result)
                total_patterns += result['patterns']
            elif result['patterns'] > 0:
                print(f"⚠️  ({result['patterns']} pattern(s) non modifié(s))")
                total_patterns += result['patterns']
            else:
                print("⏭️")
        
        # Résumé
        print("\n" + "=" * 60)
        print("🎉 Migration terminée!")
        print(f"📊 {len(modified_files)} fichier(s) modifié(s)")
        print(f"🔍 {total_patterns} pattern(s) JWT détecté(s) au total")
        
        if modified_files:
            print("\n📋 Fichiers modifiés:")
            for result in modified_files:
                file_name = os.path.basename(result['file'])
                print(f"   ✅ {file_name} ({result['patterns']} pattern(s))")
        
        print("\n⚠️  Prochaines étapes:")
        print("   1. mvn compile (vérifier la compilation)")
        print("   2. Tester les endpoints modifiés")
        print("   3. Redémarrer l'application")
        print("   4. Vérifier que tout fonctionne")

def main():
    base_path = "/Users/mac/Desktop/Tiak3da/XpertCashBack"
    
    replacer = CompletJwtReplacer(base_path)
    replacer.run_complete_migration()

if __name__ == "__main__":
    main()
