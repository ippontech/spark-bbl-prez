#!/bin/bash

rm ../data/tweet/data
cat ../data/tweet/part-*/part-00000 >> ../data/tweet/data
rm -rf ../data/tweet/part-*
