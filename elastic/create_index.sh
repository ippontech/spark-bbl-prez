#!/bin/bash

curl -XDELETE http://localhost:9200/streaming
curl -XDELETE http://localhost:9200/ml

curl -XPUT http://localhost:9200/streaming
curl -XPUT http://localhost:9200/ml

curl -XPOST http://localhost:9200/streaming/_mapping/tweets -d '{
    "tweets": {
        "properties": {
            "user": { "type": "string", "index": "not_analyzed" },
            "text": { "type": "string" },
            "createdAt": { "type": "date", "format": "date_time" },
            "language": { "type": "string", "index": "not_analyzed" }
        }
    }
}'

curl -XPOST http://localhost:9200/ml/_mapping/comparisons -d '{
    "comparisons": {
        "properties": {
            "PassengerId": { "type": "integer", "index": "not_analyzed" },
            "Pclass": { "type": "integer", "index": "analyzed" },
            "Sex": { "type": "string", "index": "analyzed" },
            "Age_cleaned": { "type": "double", "index": "analyzed" },
            "prediction": { "type": "double", "index": "analyzed" },
            "Survived": { "type": "integer", "index": "analyzed" }
        }
    }
}'

