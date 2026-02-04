#!/bin/bash -eux

cwd=$(pwd)

pushd $cwd/dp-dataset-api-java-client
  make lint
popd
