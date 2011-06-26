# vim: syntax=make

TARGET	= particlizer
SRCS    = main.c

CFLAGS 	= -W -Wall
LDFLAGS	= 

DEBUG ?= 1

#再帰展開する必要がないので、単純展開変数として代入（:=）
CC := gcc
RM := rm

OBJS = $(SRCS:%.c=%.o)

UNAME := $(shell uname)
ifeq ($(UNAME), Linux)
	LIBS    	+= -lGL -lGLU -lglut
endif
ifeq ($(UNAME), Darwin)
	FRAMEWORKS  = -framework OpenGL -framework GLUT
	LDFLAGS     += $(FRAMEWORKS)
endif

ifneq ($(DEBUG), '')
	CFLAGS	+= -g
endif

.c.o:
	$(CC) $(CFLAGS) -o $@ -c $<

$(TARGET): $(OBJS)
	$(CC) $(LDFLAGS) -o $(TARGET) $(LIBS) $^

.PHONY: all
all: $(TARGET)

.PHONY: clean
clean:
	rm -f $(OBJS) $(TARGET)	

