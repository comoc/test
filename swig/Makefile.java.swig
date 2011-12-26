# vim: syntax=make
# Makefile for using SWIG and Java for C code

IFILE         = example
INTERFACE     = $(IFILE).i
WRAPFILE      = $(IFILE)_wrap.c

SRCS          = example.c
LIBS          = 

TEST_JAVA_CLASS := test

CFLAGS = -fPIC

#再帰展開する必要がないので、単純展開変数として代入（:=）
CC := gcc
RM := rm

SRCS += $(WRAPFILE)
OBJS = $(subst .c,.o,$(SRCS))

UNAME := $(shell uname)
ifeq ($(UNAME), Linux)
	TARGET		= lib$(IFILE)JNI.so
	JAVA_INCLUDE= -I/usr/java/include
	LDFLAGS		= -shared 
endif
ifeq ($(UNAME), Darwin)
	TARGET 		= lib$(IFILE)JNI.jnilib
	JAVA_INCLUDE= -I`javaconfig Headers`
	FRAMEWORKS  = -framework JavaVM
	LDFLAGS     = -dynamiclib $(FRAMEWORKS)
endif

.PHONY: all
all: $(swig)
	$(CC) -c $(CFLAGS) $(JAVA_INCLUDE) $(SRCS)
	$(CC) $(LDFLAGS) -o $(TARGET) $(LIBS) $(OBJS)
	javac *.java

.PHONY: swig
swig:
	swig -java -o $(WRAPFILE) $(INTERFACE)

.PHONY: clean
clean:
	rm -rf $(WRAPFILE)
	rm -f $(OBJS) $(TARGET)	
	rm -f *.class

.PHONY: test
test:
	java $(TEST_JAVA_CLASS)
