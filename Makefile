MAKEFILE_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
PROJECT_ROOT := $(MAKEFILE_DIR)

# list of source files
JAVA_SRC := codegen/Parser.java \
	codegen/Scanner.java \
	codegen/SimpleLang.java \
	codegen/Code.java \
	codegen/CodeX8664.java \
	codegen/CodeAarch64.java \
	codegen/ExampleHelloAarch64.java \
	codegen/ExampleHelloX8664.java

CLASS_FILES := $(patsubst %.java,bin/%.class,$(JAVA_SRC))


all: $(CLASS_FILES)

codegen/Parser.java: codegen/SimpleLang.ATG codegen/Parser.frame codegen/Scanner.frame
	java -jar $(PROJECT_ROOT)/libs/Coco.jar codegen/SimpleLang.ATG

bin:
	mkdir -p bin

$(CLASS_FILES): $(JAVA_SRC) bin
	javac -d bin $(JAVA_SRC)
