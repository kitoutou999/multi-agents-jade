#!/bin/bash

NB_ROBOTS=3
NB_COMP_TOTAL=5
NB_COMP_ACTIVES=3
LAMBDA1=2000
LAMBDA2=5000
LAMBDA3=1000
NB_COMP_MAX_PROD=3

CONFIG_FILE="config/config.properties"

cat > "$CONFIG_FILE" <<EOL
lambda1=$LAMBDA1
lambda2=$LAMBDA2
lambda3=$LAMBDA3
nbCompetencesTotal=$NB_COMP_TOTAL
nbCompetencesActives=$NB_COMP_ACTIVES
nbCompetencesMaxProduit=$NB_COMP_MAX_PROD
EOL

javac -d bin -cp "lib/jade.jar:src" src/*.java

AGENTS="atelier:Atelier"
for (( i=1; i<=NB_ROBOTS; i++ ))
do
   AGENTS="$AGENTS;robot$i:Robot"
done


java -cp "bin:lib/jade.jar" jade.Boot -agents "$AGENTS"
