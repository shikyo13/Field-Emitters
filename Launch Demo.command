#!/bin/zsh
set -e
cd "${0:A:h}"
if [[ ! -f "run/saves/Field Emitters Demo/level.dat" ]]; then
  mkdir -p run/saves
  /usr/bin/ditto -xk "demo/Field Emitters Demo.zip" run/saves
fi
exec ./gradlew runClient -PdemoWorld --console=plain --max-workers=2
