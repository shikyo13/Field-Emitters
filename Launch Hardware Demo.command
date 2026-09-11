#!/bin/zsh
set -e
cd "${0:A:h}"
if [[ ! -f "run-visual/saves/Field Emitters Demo/level.dat" ]]; then
  mkdir -p run-visual/saves
  /usr/bin/ditto -xk "demo/Hardware Demo.zip" run-visual/saves
fi
exec ./gradlew runClient -PdemoWorld -PvisualQa --console=plain --max-workers=2
