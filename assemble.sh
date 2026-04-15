#!/bin/bash
FILE_NAME=$1

if [ "${FILE_NAME}" = "" ]; then
  echo "Usage: ./assemble.sh <file_name>"
  echo -n
  echo "  This script will try to assemble the given text file and produce a .bin file with the machine code."
  echo "  This .bin file can then be used by codegen/launcher.c."
fi

OS=$(uname -s)
FILE_BIN="${FILE_NAME%.*}.bin"
FILE_OBJ="${FILE_NAME%.*}.obj"

if [ "$OS" = "Darwin" ]; then
  echo "Creating Object File: ${FILE_OBJ}"
  as ${FILE_NAME} -o ${FILE_OBJ}

  echo "Creating Bin File: ${FILE_BIN}"
  segedit ${FILE_OBJ} -extract __TEXT __text ${FILE_BIN}
elif [ "$OS" = "Linux" ]; then
  echo "Creating Object File: ${FILE_OBJ}"
  as -msyntax=intel -mnaked-reg ${FILE_NAME} -o ${FILE_OBJ}

  echo "Creating Bin File: ${FILE_BIN}"
  objcopy -O binary -j .text ${FILE_OBJ} ${FILE_BIN}
else
  echo "Unsupported OS: $OS"
  exit 1
fi
