#!/bin/bash

if [ $# -ne 1 ]; then
        echo wrong number of parameters \(need one\)
        exit 0
fi

rm ../data/tweet/processing/*
cp ../data/tweet/data ../data/tweet/processing/$1
