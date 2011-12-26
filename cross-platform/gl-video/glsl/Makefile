CC=gcc

SRC = gl-video.c
OBJ = $(SRC:%.c=%.o)
TARGET = gl-video

UNAME=$(shell uname -s)

CFLAGS=-g -O2 -Wall `pkg-config --cflags gstreamer-0.10`
LDFLAGS=-g -lpthread `pkg-config --libs gstreamer-0.10`

ifeq ($(UNAME),Darwin)
	FRAMEWORKS=-framework OpenGL -framework GLUT
	LDFLAGS+=$(FRAMEWORKS)
else
	LDFLAGS+=-lGL -lGLU
endif


.PHONY: all clean

all: $(TARGET)

.c.o:
	$(CC) $(CFLAGS) -c $<

$(TARGET): $(OBJ) 
	$(CC) -o $@ $(OBJ) $(LDFLAGS) 

clean:
	rm -f $(TARGET) $(OBJ)
