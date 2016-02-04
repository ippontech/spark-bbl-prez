# spark-prez

Spark, ou comment traiter des données à la vitesse de l’éclair

## Récap

- Démo sur les RDD et DataFrames
- Démo sur Spark Streaming
- Démo sur Spark ML

## Requis

- ElasticSearch
- Kibana
- Maven

## ElasticSearch pour les parties Streaming et ML

Lancement d'ElasticSearch sur localhost:9200
```
$ ./elastic/launch_elasticsearch.sh
```

Lancement de Kibana sur localhost:5601
```
$ ./elastic/launch_kibana.sh
```

Initialisation des index ElasticSearch
```
$ ./elastic/create_index.sh
```

## Spark

### Processing d'arbres avec les RDD & DataFrames

- input : le fichier des arbres de Paris (fichier csv disponible dans l'open data)
- process : le comptage des arbres par espèce
- output : la console

### Processing de tweets avec Spark Streaming

- input : un flux continu de tweets avec le hashtag "Android" (via Twitter4J)
- process : la détection de la langue des tweets
- output : indexation dans ElasticSearch et visualisatio via Kibana

Pour afficher le dashboard prévu dans Kibana, modifiez le contenu du script
```
$ ./elastic/print_tweets_dashboard.sh
```
pour utiliser votre navigateur web

Il est nécessaire d'ajouter vos identifiants de connexion obtenu via l'API developper de Twitter dans le fichier
```spark/src/main/resources/twitter4j.properties```

Dans le cas où vous n'avez pas de connexion internet lors de votre démo, ce projet contient une façon de collecter des tweets et de les stocker dans des fichiers pour pouvoir les processer lors de la démo avec Spark Streaming..

### Prédiction de survivants du Titanic

- input : des fichiers contenant des informations sur des passagers du Titanic (fichiers disponibles sur kaggle)
- process : la prédiction des survivants (avec l'algorithme des Randoms Forests)
- output : indexation dans ElasticSearch et visualisation via Kibana

Pour afficher le dashboard prévu dans Kibana, modifiez le contenu du script
```
$ ./elastic/print_titanic_dashboard.sh
```
pour utiliser votre navigateur web

