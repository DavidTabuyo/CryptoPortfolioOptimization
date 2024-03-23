#!/bin/bash

# Compilar el programa Kotlin
kotlinc -cp lib/jenetics-8.0.0.jar:. -d out/ src/main.kt

# Verificar si la compilación tuvo éxito
if [ $? -eq 0 ]; then
    # Ejecutar el programa Kotlin
    kotlin -cp lib/jenetics.jar:out/ MainKt
else
    echo "Error de compilación. Por favor, corrija los errores e intente nuevamente."
fi
