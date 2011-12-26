CC=gcc
CFLAGS=-O2 -Wall -I/usr/X11R6/include `pkg-config --cflags gstreamer-0.10 gtk+-2.0 gtkglext-1.0`
LDFLAGS=-pthread -L/usr/X11R6/lib -lm `pkg-config --libs gstreamer-0.10 gtk+-2.0 gtkglext-1.0`
GL_LDFLAGS=-L/usr/local/lib -lGL -lGLU

all: gl-video yuv.pso

gl-video: gl-video.c
	$(CC) $(CFLAGS) $(LDFLAGS) $(GL_LDFLAGS) gl-video.c -o gl-video

yuv.pso: yuv.cg
	cgc -profile arbfp1 -o yuv.pso yuv.cg

clean:
	rm -f gl-video
