COMMENT LANCER LE PROJET
------------------------

Pour démarrer la simulation (compilation, configuration et lancement des agents), utilisez le script fourni dans un terminal :

>>> chmod +x start_project.sh
>>> ./start_project.sh

CONFIGURATION
-------------

Toute la configuration du système se fait directement dans le fichier "start_project.sh".

Modifiez les variables en haut du script pour changer :
- Le nombre de robots (NB_ROBOTS)
- Les délais de production (LAMBDA1, LAMBDA2)
- Le temps de traitement (LAMBDA3)
- Le nombre de compétences totales et par robot.

Note : Le fichier "jade_runtime/config/config.properties" est généré automatiquement par le script à chaque lancement. Donc si vous le modifier et que vous lancer le script "start_project.sh", il sera écrasé.
