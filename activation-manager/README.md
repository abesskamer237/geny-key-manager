# Gestionnaire d'activations

Ce dossier est réservé aux outils d'administration locale (scripts d'aide, import sécurisé, génération et historique). La clé privée n'est jamais stockée ici ni dans GitHub : elle reste dans `../private/geny-abess-private.pem` sur la machine de confiance qui exécute la génération.

La génération des codes compatibles avec Geny-Abess doit respecter exactement le format et la signature RSA-SHA256 de `tools/issue-activation.js` du dépôt Geny-Abess. Ne pas remplacer la clé ou modifier le format sans migration coordonnée de l'application cliente.
