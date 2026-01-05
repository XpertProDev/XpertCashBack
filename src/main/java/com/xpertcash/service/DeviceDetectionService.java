package com.xpertcash.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service pour détecter le modèle spécifique d'un appareil depuis le User-Agent
 */
@Service
public class DeviceDetectionService {

    /**
     * Détecte le nom amélioré de l'appareil avec le modèle spécifique
     * @param userAgent Le User-Agent de la requête
     * @param deviceName Le nom de l'appareil fourni par le client (optionnel)
     * @return Le nom de l'appareil avec le modèle spécifique (ex: "iPhone 13 (iOS 16) - Safari")
     */
    public String detectDeviceName(String userAgent, String deviceName) {
        if (userAgent == null || userAgent.isEmpty()) {
            return deviceName != null ? deviceName : "Unknown Device";
        }

        String ua = userAgent.toLowerCase();

        // Détection iOS (iPhone)
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return detectIOSDevice(userAgent, ua);
        }

        // Détection Android
        if (ua.contains("android")) {
            return detectAndroidDevice(userAgent, ua, deviceName);
        }

        // Détection Desktop (Windows, Mac, Linux)
        if (ua.contains("windows")) {
            return detectWindowsDevice(userAgent, ua);
        }
        if (ua.contains("macintosh") || ua.contains("mac os")) {
            return detectMacDevice(userAgent, ua);
        }
        if (ua.contains("linux")) {
            return "Linux - " + extractBrowser(userAgent);
        }

        // Fallback
        return deviceName != null ? deviceName : extractBrowser(userAgent);
    }

    /**
     * Détecte le modèle iPhone spécifique (iPhone X et supérieurs uniquement)
     */
    private String detectIOSDevice(String userAgent, String ua) {
        String iosVersion = extractIOSVersion(ua);
        String browser = extractBrowser(userAgent);
        
        // Détection du modèle iPhone via le User-Agent
        // Les iPhones récents ont souvent des identifiants spécifiques dans le User-Agent
        String model = detectIPhoneModel(userAgent, ua);
        
        if (model != null) {
            return model + (iosVersion != null ? " (" + iosVersion + ")" : "") + " - " + browser;
        }
        
        // Si on ne peut pas détecter le modèle spécifique, on retourne juste "iPhone"
        // (pour les modèles en dessous de iPhone X, on ne spécifie pas le modèle)
        return "iPhone" + (iosVersion != null ? " (" + iosVersion + ")" : "") + " - " + browser;
    }

    /**
     * Détecte le modèle iPhone spécifique depuis le User-Agent
     * Retourne null si le modèle est en dessous de iPhone X
     */
    private String detectIPhoneModel(String userAgent, String ua) {
        // Patterns pour détecter les modèles iPhone X et supérieurs
        // Les User-Agents iOS contiennent souvent des identifiants de modèle
        
        // iPhone 16 Pro Max
        if (ua.contains("iphone17,1") || ua.contains("iphone17,2") || ua.contains("iphone17,3") || ua.contains("iphone17,4")) {
            if (ua.contains("iphone17,1")) return "iPhone 16 Pro Max";
            if (ua.contains("iphone17,2")) return "iPhone 16 Pro";
            if (ua.contains("iphone17,3")) return "iPhone 16 Plus";
            if (ua.contains("iphone17,4")) return "iPhone 16";
        }
        
        // iPhone 15 Pro Max
        if (ua.contains("iphone16,1") || ua.contains("iphone16,2") || ua.contains("iphone16,3") || ua.contains("iphone16,4")) {
            if (ua.contains("iphone16,1")) return "iPhone 15 Pro Max";
            if (ua.contains("iphone16,2")) return "iPhone 15 Pro";
            if (ua.contains("iphone16,3")) return "iPhone 15 Plus";
            if (ua.contains("iphone16,4")) return "iPhone 15";
        }
        
        // iPhone 14 Pro Max
        if (ua.contains("iphone15,2") || ua.contains("iphone15,3") || ua.contains("iphone15,4") || ua.contains("iphone15,5")) {
            if (ua.contains("iphone15,2")) return "iPhone 14 Pro Max";
            if (ua.contains("iphone15,3")) return "iPhone 14 Pro";
            if (ua.contains("iphone15,4")) return "iPhone 14 Plus";
            if (ua.contains("iphone15,5")) return "iPhone 14";
        }
        
        // iPhone 13 Pro Max
        if (ua.contains("iphone14,2") || ua.contains("iphone14,3") || ua.contains("iphone14,4") || ua.contains("iphone14,5")) {
            if (ua.contains("iphone14,2")) return "iPhone 13 Pro Max";
            if (ua.contains("iphone14,3")) return "iPhone 13 Pro";
            if (ua.contains("iphone14,4")) return "iPhone 13 mini";
            if (ua.contains("iphone14,5")) return "iPhone 13";
        }
        
        // iPhone 12 Pro Max
        if (ua.contains("iphone13,1") || ua.contains("iphone13,2") || ua.contains("iphone13,3") || ua.contains("iphone13,4")) {
            if (ua.contains("iphone13,1")) return "iPhone 12 mini";
            if (ua.contains("iphone13,2")) return "iPhone 12";
            if (ua.contains("iphone13,3")) return "iPhone 12 Pro";
            if (ua.contains("iphone13,4")) return "iPhone 12 Pro Max";
        }
        
        // iPhone 11 Pro Max
        if (ua.contains("iphone12,1") || ua.contains("iphone12,3") || ua.contains("iphone12,5")) {
            if (ua.contains("iphone12,1")) return "iPhone 11";
            if (ua.contains("iphone12,3")) return "iPhone 11 Pro";
            if (ua.contains("iphone12,5")) return "iPhone 11 Pro Max";
        }
        
        // iPhone XS Max / XR
        if (ua.contains("iphone11,2") || ua.contains("iphone11,4") || ua.contains("iphone11,6") || ua.contains("iphone11,8")) {
            if (ua.contains("iphone11,2")) return "iPhone XS";
            if (ua.contains("iphone11,4") || ua.contains("iphone11,6")) return "iPhone XS Max";
            if (ua.contains("iphone11,8")) return "iPhone XR";
        }
        
        // iPhone X
        if (ua.contains("iphone10,3") || ua.contains("iphone10,6")) {
            return "iPhone X";
        }
        
        // Si aucun modèle spécifique n'est détecté, on retourne null
        // Cela signifie que c'est probablement un iPhone en dessous de iPhone X
        return null;
    }

    /**
     * Détecte le modèle Android spécifique (Samsung, Techno, Xiaomi, etc.)
     */
    private String detectAndroidDevice(String userAgent, String ua, String deviceName) {
        String androidVersion = extractAndroidVersion(ua);
        String browser = extractBrowser(userAgent);
        
        // Détection Samsung
        if (ua.contains("samsung") || ua.contains("sm-") || ua.contains("galaxy")) {
            String samsungModel = detectSamsungModel(userAgent, ua);
            if (samsungModel != null) {
                return samsungModel + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
            }
            return "Samsung Galaxy" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Détection Techno
        if (ua.contains("tecno") || ua.contains("tcl") || ua.contains("infinix")) {
            String technoModel = detectTechnoModel(userAgent, ua);
            if (technoModel != null) {
                return technoModel + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
            }
            return "Techno" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Détection Xiaomi
        if (ua.contains("xiaomi") || ua.contains("redmi") || ua.contains("mi ")) {
            String xiaomiModel = detectXiaomiModel(userAgent, ua);
            if (xiaomiModel != null) {
                return xiaomiModel + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
            }
            return "Xiaomi" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Détection OnePlus
        if (ua.contains("oneplus")) {
            return "OnePlus" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Détection Oppo
        if (ua.contains("oppo")) {
            return "Oppo" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Détection Vivo
        if (ua.contains("vivo")) {
            return "Vivo" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Détection Huawei
        if (ua.contains("huawei") || ua.contains("honor")) {
            return "Huawei" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Détection Realme
        if (ua.contains("realme")) {
            return "Realme" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        // Fallback : utiliser deviceName si fourni, sinon "Android"
        if (deviceName != null && !deviceName.isEmpty()) {
            return deviceName + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
        }
        
        return "Android" + (androidVersion != null ? " (Android " + androidVersion + ")" : "") + " - " + browser;
    }

    /**
     * Détecte le modèle Samsung spécifique
     */
    private String detectSamsungModel(String userAgent, String ua) {
        // Patterns pour Samsung Galaxy S
        Pattern sPattern = Pattern.compile("sm-s\\d+[a-z]*", Pattern.CASE_INSENSITIVE);
        Matcher sMatcher = sPattern.matcher(userAgent);
        if (sMatcher.find()) {
            String model = sMatcher.group().toUpperCase();
            // Convertir SM-S918B en "Galaxy S23 Ultra" par exemple
            if (model.contains("S918")) return "Samsung Galaxy S23 Ultra";
            if (model.contains("S911")) return "Samsung Galaxy S23";
            if (model.contains("S906")) return "Samsung Galaxy S22 Ultra";
            if (model.contains("S901")) return "Samsung Galaxy S22";
            if (model.contains("S908")) return "Samsung Galaxy S22+";
            if (model.contains("S908")) return "Samsung Galaxy S21 Ultra";
            if (model.contains("S991")) return "Samsung Galaxy S21";
            return "Samsung " + model;
        }
        
        // Patterns pour Samsung Galaxy Note
        if (ua.contains("note")) {
            Pattern notePattern = Pattern.compile("sm-n\\d+[a-z]*", Pattern.CASE_INSENSITIVE);
            Matcher noteMatcher = notePattern.matcher(userAgent);
            if (noteMatcher.find()) {
                String model = noteMatcher.group().toUpperCase();
                return "Samsung Galaxy " + model;
            }
            return "Samsung Galaxy Note";
        }
        
        // Patterns pour Samsung Galaxy A
        Pattern aPattern = Pattern.compile("sm-a\\d+[a-z]*", Pattern.CASE_INSENSITIVE);
        Matcher aMatcher = aPattern.matcher(userAgent);
        if (aMatcher.find()) {
            String model = aMatcher.group().toUpperCase();
            return "Samsung Galaxy " + model;
        }
        
        // Patterns génériques Samsung
        Pattern samsungPattern = Pattern.compile("sm-[a-z]\\d+[a-z]*", Pattern.CASE_INSENSITIVE);
        Matcher samsungMatcher = samsungPattern.matcher(userAgent);
        if (samsungMatcher.find()) {
            String model = samsungMatcher.group().toUpperCase();
            return "Samsung " + model;
        }
        
        return null;
    }

    /**
     * Détecte le modèle Techno spécifique
     */
    private String detectTechnoModel(String userAgent, String ua) {
        // Patterns pour Techno (les modèles Techno sont souvent dans le User-Agent)
        Pattern technoPattern = Pattern.compile("(tecno|tcl|infinix)[\\s-]?([a-z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher technoMatcher = technoPattern.matcher(userAgent);
        if (technoMatcher.find()) {
            String brand = technoMatcher.group(1);
            String model = technoMatcher.group(2);
            return brand.substring(0, 1).toUpperCase() + brand.substring(1) + " " + model.toUpperCase();
        }
        return null;
    }

    /**
     * Détecte le modèle Xiaomi spécifique
     */
    private String detectXiaomiModel(String userAgent, String ua) {
        // Patterns pour Xiaomi (Redmi, Mi, etc.)
        Pattern xiaomiPattern = Pattern.compile("(redmi|mi)[\\s-]?([a-z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher xiaomiMatcher = xiaomiPattern.matcher(userAgent);
        if (xiaomiMatcher.find()) {
            String series = xiaomiMatcher.group(1);
            String model = xiaomiMatcher.group(2);
            return "Xiaomi " + series.substring(0, 1).toUpperCase() + series.substring(1) + " " + model.toUpperCase();
        }
        return null;
    }

    /**
     * Détecte le modèle Windows
     */
    private String detectWindowsDevice(String userAgent, String ua) {
        String browser = extractBrowser(userAgent);
        if (ua.contains("windows nt 10.0")) {
            return "Windows 10/11 - " + browser;
        }
        if (ua.contains("windows nt 6.3")) {
            return "Windows 8.1 - " + browser;
        }
        if (ua.contains("windows nt 6.2")) {
            return "Windows 8 - " + browser;
        }
        if (ua.contains("windows nt 6.1")) {
            return "Windows 7 - " + browser;
        }
        return "Windows - " + browser;
    }

    /**
     * Détecte le modèle Mac
     */
    private String detectMacDevice(String userAgent, String ua) {
        String browser = extractBrowser(userAgent);
        if (ua.contains("intel mac")) {
            return "Mac (Intel) - " + browser;
        }
        if (ua.contains("arm64") || ua.contains("apple silicon")) {
            return "Mac (Apple Silicon) - " + browser;
        }
        return "Mac - " + browser;
    }

    /**
     * Extrait la version iOS depuis le User-Agent
     */
    private String extractIOSVersion(String ua) {
        Pattern pattern = Pattern.compile("os[\\s_]?(\\d+)[_](\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ua);
        if (matcher.find()) {
            return "iOS " + matcher.group(1) + "." + matcher.group(2);
        }
        return null;
    }

    /**
     * Extrait la version Android depuis le User-Agent
     */
    private String extractAndroidVersion(String ua) {
        Pattern pattern = Pattern.compile("android[\\s](\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ua);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extrait le nom du navigateur depuis le User-Agent
     */
    private String extractBrowser(String userAgent) {
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("chrome/") && !ua.contains("edg/")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("opera/") || ua.contains("opr/")) {
            return "Opera";
        }
        if (ua.contains("samsungbrowser/")) {
            return "Samsung Internet";
        }
        
        return "Unknown Browser";
    }
}

