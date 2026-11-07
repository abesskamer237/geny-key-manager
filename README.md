# GENY KEY MANAGER

Application Android indépendante pour l'administration des activations Geny-Abess.

## Fonctionnement prévu
- Saisir le DEVICE_ID affiché dans Geny-Abess.
- Choisir la durée : 7, 30, 90, 365 jours, durée personnalisée ou illimitée.
- Générer et copier le code d'activation.
- Consulter un historique local des activations.

## Sécurité — clé privée
Le code utilise la même paire RSA que Geny-Abess : la clé privée doit correspondre à la clé publique intégrée dans l'application Geny-Abess actuelle.

**Emplacement local attendu (jamais dans Git) :**
`private/geny-abess-private.pem`

À la racine de ce dépôt, créer le dossier et installer le fichier privé :
```bash
mkdir -p private
chmod 700 private
# Copier la clé privée originale dans :
# private/geny-abess-private.pem
chmod 600 private/geny-abess-private.pem
git check-ignore private/geny-abess-private.pem
```

Le chemin local est ignoré par Git. Ne jamais ajouter, téléverser, committer, envoyer dans un ticket/chat, ou intégrer la clé privée dans l'APK. Le dépôt ne contient aucun fichier PEM.

## Important
- Ne pas générer de nouvelle paire RSA : cela rendrait les codes incompatibles avec les APK Geny-Abess déjà distribués.
- Si la clé privée originale est perdue, il faut organiser une migration de clé publique dans une nouvelle version de Geny-Abess. Le gestionnaire ne peut pas contourner la correspondance cryptographique.
- Un APK autonome contenant la clé privée serait une fuite irréversible. Cette première version conserve la signature côté poste local et n'embarque aucun secret.
