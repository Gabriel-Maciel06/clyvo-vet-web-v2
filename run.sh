#!/usr/bin/env bash
set -e

# Carrega variáveis locais de .env se o arquivo existir
if [ -f .env ]; then
    echo "Carregando variáveis de ambiente locais (.env)..."
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "Iniciando Clyvo Vet Web v2..."
mvn spring-boot:run
