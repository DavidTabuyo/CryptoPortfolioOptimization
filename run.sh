#!/bin/bash

# Compilar el programa Kotlin
kotlinc -d out/ src/Main.kt

#Si la compilación tuvo éxito ejecutamos el programa Kotlin
if [ $? -eq 0 ]; then
    kotlin -cp out/ MainKt
fi
