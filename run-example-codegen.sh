#!/bin/sh
# get dir of this script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# determine CPU architecture
ARCH=$(uname -m)
if [ "$ARCH" = "x86_64" ]; then
  EXAMPLE_CLASS="codegen.ExampleHelloX8664"
elif [ "$ARCH" = "arm64" ]; then
  EXAMPLE_CLASS="codegen.ExampleHelloAarch64"
else
  echo "Unsupported architecture: $ARCH"
  exit 1
fi

exec java -cp "$SCRIPT_DIR/bin" ${EXAMPLE_CLASS}
