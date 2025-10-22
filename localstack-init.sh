#!/bin/bash

# Wait for the Kinesis service to be ready (removes race condition)
until awslocal kinesis list-streams &> /dev/null; do
  sleep 1
done

# Create the stream
awslocal kinesis create-stream \
  --stream-name my-test-stream \
  --shard-count 3
