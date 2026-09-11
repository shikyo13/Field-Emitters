#!/bin/zsh
set -e
cd "${0:A:h}"
if [[ ! -f "run-automation/saves/Field Emitters Demo/level.dat" ]]; then
  mkdir -p run-automation/saves
  /usr/bin/ditto -xk "demo/Automation Demo.zip" run-automation/saves
fi
exec ./gradlew runClient -PdemoWorld -PautomationQa --console=plain --max-workers=2
