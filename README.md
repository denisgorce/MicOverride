# 🎙️ MicOverride — Force le micro intégré Android

Application Android qui **force l'utilisation du micro intégré** du téléphone même quand un kit mains libres (filaire ou Bluetooth) est branché.

---

## 📱 Fonctionnalités

| Fonctionnalité | Description |
|---|---|
| 🔴 Override en temps réel | Détecte la connexion d'un kit et force immédiatement le micro intégré |
| 🔁 Service en arrière-plan | Reste actif même quand l'app est fermée (foreground service) |
| 🚀 Démarrage automatique | Redémarre après le reboot du téléphone (optionnel) |
| 📋 Liste des périphériques | Affiche tous les périphériques audio connectés |
| 🎙️ Test micro intégré | Permet de tester que le bon micro est utilisé |

---

## 🔧 Comment ça fonctionne

### Principe technique

Android utilise par défaut le micro du kit mains libres branché. Cette app intercepte les événements de connexion audio et utilise l'API `AudioRecord.setPreferredDevice()` pour forcer le micro intégré.

```
Kit branché → BroadcastReceiver (ACTION_HEADSET_PLUG)
     ↓
AudioManager.getDevices(GET_DEVICES_INPUTS)
     ↓
Sélection de TYPE_BUILTIN_MIC
     ↓
AudioRecord.setPreferredDevice(builtinMic) ← Override !
```

### APIs utilisées

- **`AudioDeviceInfo.TYPE_BUILTIN_MIC`** — Identifie le micro intégré
- **`AudioRecord.setPreferredDevice()`** — Force un périphérique d'entrée spécifique (Android 6+)
- **`AudioManager.ACTION_HEADSET_PLUG`** — Écoute les connexions/déconnexions de kit
- **`AudioManager.MODE_IN_COMMUNICATION`** — Contourne le routage automatique vers le headset

---

## 🚀 Installation

### Option 1 — Compiler depuis les sources

**Prérequis :** Android Studio, JDK 17+

```bash
git clone https://github.com/votre-pseudo/MicOverride.git
cd MicOverride
./gradlew assembleDebug
# L'APK se trouve dans app/build/outputs/apk/debug/
```

### Option 2 — Installer l'APK directement

1. Télécharger `MicOverride.apk` depuis les [Releases](../../releases)
2. Sur le téléphone : **Paramètres → Sécurité → Sources inconnues** → Autoriser
3. Ouvrir le fichier APK téléchargé

---

## 📋 Permissions requises

| Permission | Raison |
|---|---|
| `RECORD_AUDIO` | Accès au micro et sélection du périphérique |
| `FOREGROUND_SERVICE` | Maintien du service en arrière-plan |
| `FOREGROUND_SERVICE_MICROPHONE` | Android 14 : fg service avec micro |
| `MODIFY_AUDIO_SETTINGS` | Modification du routage audio |
| `RECEIVE_BOOT_COMPLETED` | Redémarrage automatique après reboot |

---

## ⚠️ Limitations connues

| Situation | Comportement |
|---|---|
| **Appels téléphoniques** | Le routage des appels est géré par le système — l'override peut ne pas fonctionner sur tous les appareils |
| **Android < 6.0** | `setPreferredDevice()` non disponible → fallback sur `MODE_IN_COMMUNICATION` |
| **Bluetooth** | Fonctionne pour les kits filaires. Le Bluetooth SCO a des restrictions supplémentaires |
| **Apps tierces** | Chaque app gère son propre `AudioRecord` — l'override s'applique uniquement à MicOverride et aux apps qui respectent le mode système |

---

## 🏗️ Structure du projet

```
MicOverride/
├── app/src/main/
│   ├── java/com/micoverride/
│   │   ├── MainActivity.kt          # UI + logique principale
│   │   ├── MicOverrideService.kt    # Service foreground persistant
│   │   └── BootReceiver.kt          # Redémarrage auto après boot
│   ├── res/layout/
│   │   └── activity_main.xml        # Interface utilisateur
│   └── AndroidManifest.xml
├── app/build.gradle
└── README.md
```

---

## 🛠️ Développement

```bash
# Build debug
./gradlew assembleDebug

# Installer sur un appareil connecté
./gradlew installDebug

# Build release (nécessite un keystore)
./gradlew assembleRelease
```

---

## 📄 Licence

MIT — Libre d'utilisation, modification et distribution.

---

## 🤝 Contributions

Les PRs sont les bienvenues ! En particulier :
- Support Bluetooth SCO amélioré
- Support pour les appels téléphoniques (nécessite permissions système)
- Tests automatisés
