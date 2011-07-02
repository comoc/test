SRCS = test.m
TARGET = test

all: $(TARGET)

clean:
	rm -rf $(TARGET)
$(TARGET): $(SRCS)
	gcc -F/System/Library/PrivateFrameworks -framework MultitouchSupport $^ -o $@ -std=c99
