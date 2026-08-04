# Varyon-World-Manager

Module de gestion par monde pour Varyon : inventaire séparé par groupe de mondes
et gamemode automatique par monde.

## Inventaire par monde

Voir `groups.yml` dans le dossier de données du plugin. Chaque clé sous `groups`
est une catégorie d'inventaire ; les noms de mondes internes du serveur qui la
composent partagent le même fichier d'inventaire. Les mondes absents de la
config vont dans le groupe `default`.

## Gamemode par monde

Voir `worldModes.json` dans le dossier de données du plugin. Strictement
opt-in : seuls les mondes listés sont gérés. Entrer dans un monde listé
applique le mode configuré (Adventure ou Creative) ; le mode précédent du
joueur est mémorisé et restauré en quittant vers un monde non listé. Les
opérateurs sont exemptés.

Pour que l'isolation d'inventaire suive les changements de gamemode (éviter
que des objets récupérés en Creative se retrouvent en Adventure), placer les
mondes en mode Creative dans un groupe d'inventaire séparé dans `groups.yml`.
