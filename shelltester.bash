#!/bin/bash

for i in `seq 1 $2`
do
java Client $1 8080 $3${i} & > /dev/null
done
