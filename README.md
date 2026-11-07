# GENY KEY MANAGER

Application Android locale pour créer des codes d'activation compatibles avec les APK Geny-Abess existants.

## Fonctions disponibles dans l'application
- Importer le fichier PEM via le sélecteur de fichiers Android.
- Vérifier automatiquement que la clé privée correspond à la clé publique intégrée à Geny-Abess.
- Chiffrer la clé importée avec AES-GCM et Android Keystore sur cet appareil.
- Saisir un DEVICE_ID, choisir 7/30/90/365 jours, une durée personnalisée (1 à 3650 jours) ou illimitée.
- Générer une signature RSA-SHA256 au format exact attendu par Geny-Abess : `base64url(payload).base64url(signature)`.
- Copier le code et retrouver les dernières activations dans un historique local.

## Installer la clé privée — une seule fois
1. Installe et ouvre l'APK GENY KEY MANAGER sur TON téléphone de confiance.
2. Appuie sur **IMPORTER LA CLÉ PRIVÉE PEM**.
3. Sélectionne le fichier original `geny-abess-private.pem` depuis le stockage local.
4. L'application vérifie la correspondance avec la clé publique de Geny-Abess, puis chiffre la clé avec Android Keystore.
5. Après import réussi, le fichier PEM peut être retiré du dossier Téléchargements. L'application conserve uniquement sa copie chiffrée dans son espace privé.

**Le fichier original doit rester hors de GitHub.** Si tu clones le dépôt sur un ordinateur, tu peux garder une copie locale dans `private/geny-abess-private.pem`, mais ce fichier ne doit jamais être commité. Le dossier `private/` du dépôt est volontairement vide.

## Utilisation
1. Ouvre GENY KEY MANAGER.
2. Importe la clé privée originale si ce n'est pas déjà fait.
3. Colle le DEVICE_ID exact affiché dans Geny-Abess sur le téléphone du client.
4. Choisis la durée et appuie sur **GÉNÉRER LE CODE D'ACTIVATION**.
5. Copie le code et transmets-le au client en privé.

## Build
Le workflow GitHub Actions construit un APK debug et publie l'artefact `Geny-Key-Manager-debug-APK` après une compilation réussie.

## Sécurité et limites
- La clé privée n'est jamais incluse dans le dépôt, le code source livré ou l'APK; elle est importée localement puis chiffrée par Android Keystore.
- L'application vérifie le module RSA (modulus et exposant public) de la clé importée avant de signer. Une clé différente est rejetée.
- La signature est locale. Les codes créés sont compatibles avec la validation actuelle de Geny-Abess, mais ce mécanisme local ne permet pas de révoquer un code déjà délivré ni de synchroniser les activations entre plusieurs appareils administrateurs.
- Protéger le téléphone administrateur avec un verrouillage d'écran et conserver une sauvegarde sécurisée de la clé originale. Ne jamais partager le PEM.
- La version Android minimale est Android 8.0 (API 26).
