# Clé privée Geny-Abess — stockage local uniquement

Place ici, sur TON ordinateur ou téléphone, le fichier original :

`geny-abess-private.pem`

Chemin local attendu :

`private/geny-abess-private.pem`

Ce dossier dans GitHub ne contient volontairement PAS la clé. Git ignore tous les fichiers PEM. N'envoie jamais cette clé dans GitHub, dans un chat ou dans l'APK.

Après avoir cloné le dépôt, copie le fichier original dans ce dossier local et vérifie :

```bash
chmod 700 private
chmod 600 private/geny-abess-private.pem
git check-ignore private/geny-abess-private.pem
```

La commande `git check-ignore` doit afficher le chemin du fichier. Ne force jamais son ajout avec `git add -f`.

Utilise uniquement la clé privée correspondant à la clé publique déjà intégrée dans l'APK Geny-Abess actuel. Ne génère pas une nouvelle paire RSA.
